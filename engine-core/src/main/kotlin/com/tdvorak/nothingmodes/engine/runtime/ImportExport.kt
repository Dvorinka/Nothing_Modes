package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationSchema
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.CreatedBy
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
) {
    val isSupported: Boolean get() = errors.isEmpty()
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
    /** Export all automations to a JSON string. */
    suspend fun export(): ExportResult {
        val automations = store.all()
        return ExportResult(json = encode(automations), count = automations.size)
    }

    /** Export specific automations by ID. */
    suspend fun export(ids: List<AutomationId>): ExportResult {
        val automations = ids.mapNotNull { store.get(it) }
        return ExportResult(json = encode(automations), count = automations.size)
    }

    private fun encode(automations: List<Automation>): String {
        val bundle =
            ExportBundle(
                schemaVersion = AutomationSchema.supportedVersions.max(),
                exportedAt = now(),
                automations = automations,
                appVersion = appVersion,
                requiredCapabilities = capabilitiesOf(automations).toList(),
            )
        return EngineJson.json.encodeToString(bundle)
    }

    /**
     * Parse and validate a bundle without persisting anything. Returns the
     * automations, the union of required capabilities, and blocking errors.
     */
    fun preview(json: String): ImportPreview {
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
            )
        }

        return ImportPreview(
            automations = bundle.automations,
            schemaVersion = bundle.schemaVersion,
            appVersion = bundle.appVersion,
            // Trust derive() over the declared field: the exporter may be older.
            requiredCapabilities = capabilitiesOf(bundle.automations),
            errors = emptyList(),
        )
    }

    private fun capabilitiesOf(automations: List<Automation>): Set<String> =
        automations
            .flatMap {
                CapabilityRequirements.derive(it.trigger, it.actions, it.conditions)
            }.toSet()

    /** Import automations from a JSON string. Validates schema version and deduplicates by ID. */
    suspend fun import(
        json: String,
        overwrite: Boolean = false,
    ): ImportResult {
        val preview = preview(json)
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

                val toSave =
                    automation.copy(
                        createdBy = CreatedBy.IMPORT,
                        enabled = false,
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
