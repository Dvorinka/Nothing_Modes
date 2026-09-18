package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationSchema
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.CreatorProfile
import com.tdvorak.nothingmodes.engine.model.EngineJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Versioned container for exported automations.
 *
 * New fields carry defaults and unknown keys are ignored on decode, so bundles
 * move freely between app versions in both directions.
 */
@Serializable
data class ExportBundle(
    val schemaVersion: Int,
    val exportedAt: Long,
    val automations: List<Automation>,
    /** Version name of the exporting app, e.g. "1.4.0". Empty for older exports. */
    val appVersion: String = "",
    /** Union of capability IDs every automation in this bundle needs. */
    val requiredCapabilities: List<String> = emptyList(),
    /** Optional attribution metadata for published/shared routines. */
    val creatorProfile: CreatorProfile = CreatorProfile(),
    /**
     * Automation id → private fields scrubbed for sharing that the importer
     * must configure before the automation can work. Empty in backups and
     * older bundles.
     */
    val setupRequirements: Map<String, List<String>> = emptyMap(),
)

/** Result of an import operation. */
data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: List<String>,
)

/**
 * Non-destructive look at a bundle before import: parsed automations plus the
 * capability IDs the device must satisfy. Pair with CapabilityResolver for
 * device-specific warnings.
 */
data class ImportPreview(
    val automations: List<Automation>,
    val schemaVersion: Int,
    val appVersion: String,
    val requiredCapabilities: Set<String>,
    val errors: List<String>,
    val creatorProfile: CreatorProfile = CreatorProfile(),
    /** Automation id → private fields the user must fill in after import. */
    val setupRequirements: Map<String, List<String>> = emptyMap(),
) {
    val isSupported: Boolean get() = errors.isEmpty()

    /** True when any automation in this bundle needs manual setup to work. */
    val requiresSetup: Boolean get() = setupRequirements.values.any { it.isNotEmpty() }
}

/** Result of an export operation. */
data class ExportResult(
    val json: String,
    val count: Int,
)

/** Import/export service for automation backups. Pure Kotlin, testable. */
class ImportExportService(
    private val store: AutomationStore,
    private val appVersion: String = "",
    // now stays last: existing call sites pass it as a trailing lambda
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Export automations as a shareable template — private fields are
     * scrubbed (Bluetooth MAC/name, Wi-Fi SSID, phone numbers, locations,
     * message content, ...) and replaced by setup requirements.
     *
     * For a full-fidelity local backup use [exportBackup] instead.
     */
    suspend fun export(creator: CreatorProfile = CreatorProfile()): ExportResult {
        val automations = store.all()
        return ExportResult(json = encode(automations, creator, sanitize = true), count = automations.size)
    }

    /** Export specific automations by ID — private fields scrubbed. */
    suspend fun export(
        ids: List<AutomationId>,
        creator: CreatorProfile = CreatorProfile(),
    ): ExportResult {
        val automations = ids.mapNotNull { store.get(it) }
        return ExportResult(json = encode(automations, creator, sanitize = true), count = automations.size)
    }

    /** Full-fidelity export for local backup — nothing is scrubbed. */
    suspend fun exportBackup(creator: CreatorProfile = CreatorProfile()): ExportResult {
        val automations = store.all()
        return ExportResult(json = encode(automations, creator, sanitize = false), count = automations.size)
    }

    /** Full-fidelity export of specific automations for local backup. */
    suspend fun exportBackup(
        ids: List<AutomationId>,
        creator: CreatorProfile = CreatorProfile(),
    ): ExportResult {
        val automations = ids.mapNotNull { store.get(it) }
        return ExportResult(json = encode(automations, creator, sanitize = false), count = automations.size)
    }

    private fun encode(
        automations: List<Automation>,
        creator: CreatorProfile,
        sanitize: Boolean,
    ): String {
        val scrubbed =
            if (sanitize) {
                automations.map(PrivacyScrubber::scrub)
            } else {
                automations.map { it to emptyList() }
            }
        val out = scrubbed.map { it.first }
        val setupRequirements =
            scrubbed
                .filter { it.second.isNotEmpty() }
                .associate { it.first.id.value to it.second }
        val bundle =
            ExportBundle(
                schemaVersion = AutomationSchema.supportedVersions.max(),
                exportedAt = now(),
                automations = out,
                appVersion = appVersion,
                requiredCapabilities = capabilitiesOf(out).toList(),
                creatorProfile = creator.sanitized(),
                setupRequirements = setupRequirements,
            )
        return EngineJson.json.encodeToString(bundle)
    }

    /**
     * Parse and validate a bundle without persisting anything. Returns the
     * automations, the union of required capabilities, and blocking errors.
     * @param expectedContentHash optional SHA-256 of canonical JSON for integrity checks.
     */
    fun preview(
        json: String,
        expectedContentHash: String? = null,
    ): ImportPreview {
        val bundle =
            try {
                EngineJson.json.decodeFromString(ExportBundle.serializer(), json)
            } catch (e: Exception) {
                return ImportPreview(
                    automations = emptyList(),
                    schemaVersion = 0,
                    appVersion = "",
                    requiredCapabilities = emptySet(),
                    errors = listOf("Failed to parse JSON: ${e.message}"),
                )
            } catch (e: StackOverflowError) {
                return ImportPreview(
                    automations = emptyList(),
                    schemaVersion = 0,
                    appVersion = "",
                    requiredCapabilities = emptySet(),
                    errors = listOf("JSON nesting too deep (possible malformed input)"),
                )
            }

        if (!AutomationSchema.isSupportedVersion(bundle.schemaVersion)) {
            return ImportPreview(
                automations = emptyList(),
                schemaVersion = bundle.schemaVersion,
                appVersion = bundle.appVersion,
                requiredCapabilities = emptySet(),
                errors = listOf("Unsupported schema version: ${bundle.schemaVersion}. Supported: ${AutomationSchema.supportedVersions}"),
                creatorProfile = bundle.creatorProfile,
            )
        }

        // Blank hashes (seed fallback, legacy rows) mean "no hash available",
        // not an expected value — verifying them guarantees a false mismatch.
        if (!expectedContentHash.isNullOrBlank()) {
            val actual =
                com.tdvorak.nothingmodes.engine
                    .jsonSha256(json)
            if (!actual.equals(expectedContentHash, ignoreCase = true)) {
                return ImportPreview(
                    automations = emptyList(),
                    schemaVersion = bundle.schemaVersion,
                    appVersion = bundle.appVersion,
                    requiredCapabilities = emptySet(),
                    errors = listOf("Content hash mismatch — the file may have been modified in transit."),
                    creatorProfile = bundle.creatorProfile,
                )
            }
        }

        return ImportPreview(
            automations = bundle.automations,
            schemaVersion = bundle.schemaVersion,
            appVersion = bundle.appVersion,
            // Trust derive() over the declared field: the exporter may be older.
            requiredCapabilities = capabilitiesOf(bundle.automations),
            errors = emptyList(),
            creatorProfile = bundle.creatorProfile,
            setupRequirements = bundle.setupRequirements,
        )
    }

    private fun capabilitiesOf(automations: List<Automation>): Set<String> =
        automations
            .flatMap {
                CapabilityRequirements.derive(it.trigger, it.actions, it.conditions, it.endActions)
            }.toSet()

    /** Import automations from a JSON string. Validates schema version and deduplicates by ID. */
    suspend fun import(
        json: String,
        overwrite: Boolean = false,
        expectedContentHash: String? = null,
    ): ImportResult {
        val preview = preview(json, expectedContentHash)
        if (!preview.isSupported) {
            return ImportResult(imported = 0, skipped = 0, errors = preview.errors)
        }

        val errors = mutableListOf<String>()
        var imported = 0
        var skipped = 0

        for (automation in preview.automations) {
            try {
                val existing = store.get(automation.id)
                if (existing != null && !overwrite) {
                    skipped++
                    errors.add("Skipped '${automation.name}' (ID ${automation.id.value} already exists)")
                    continue
                }

                val needsSetup = preview.setupRequirements[automation.id.value].orEmpty().isNotEmpty()
                val toSave =
                    automation.copy(
                        createdBy = CreatedBy.IMPORT,
                        enabled = false,
                        status =
                            if (needsSetup) {
                                AutomationStatus.NEEDS_REVIEW
                            } else {
                                automation.status
                            },
                    )
                store.save(toSave)
                imported++
            } catch (e: Exception) {
                errors.add("Failed to import '${automation.name}' (${automation.id.value}): ${e.message}")
            }
        }

        return ImportResult(imported = imported, skipped = skipped, errors = errors)
    }
}
