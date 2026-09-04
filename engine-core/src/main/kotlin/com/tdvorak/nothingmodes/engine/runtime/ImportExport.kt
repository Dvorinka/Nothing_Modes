package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationSchema
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.EngineJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/** Versioned container for exported automations. */
@Serializable
data class ExportBundle(
    val schemaVersion: Int,
    val exportedAt: Long,
    val automations: List<Automation>,
)

/** Result of an import operation. */
data class ImportResult(
    val imported: Int,
    val skipped: Int,
    val errors: List<String>,
)

/** Result of an export operation. */
data class ExportResult(
    val json: String,
    val count: Int,
)

/** Import/export service for automation backups. Pure Kotlin, testable. */
class ImportExportService(
    private val store: AutomationStore,
    private val now: () -> Long = System::currentTimeMillis,
) {

    /** Export all automations to a JSON string. */
    suspend fun export(): ExportResult {
        val automations = store.all()
        val bundle = ExportBundle(
            schemaVersion = AutomationSchema.supportedVersions.max(),
            exportedAt = now(),
            automations = automations,
        )
        val json = EngineJson.json.encodeToString(bundle)
        return ExportResult(json = json, count = automations.size)
    }

    /** Export specific automations by ID. */
    suspend fun export(ids: List<AutomationId>): ExportResult {
        val automations = ids.mapNotNull { store.get(it) }
        val bundle = ExportBundle(
            schemaVersion = AutomationSchema.supportedVersions.max(),
            exportedAt = now(),
            automations = automations,
        )
        val json = EngineJson.json.encodeToString(bundle)
        return ExportResult(json = json, count = automations.size)
    }

    /** Import automations from a JSON string. Validates schema version and deduplicates by ID. */
    suspend fun import(json: String, overwrite: Boolean = false): ImportResult {
        val errors = mutableListOf<String>()
        var imported = 0
        var skipped = 0

        val bundle = try {
            EngineJson.json.decodeFromString(ExportBundle.serializer(), json)
        } catch (e: Exception) {
            return ImportResult(
                imported = 0,
                skipped = 0,
                errors = listOf("Failed to parse JSON: ${e.message}"),
            )
        } catch (e: StackOverflowError) {
            return ImportResult(
                imported = 0,
                skipped = 0,
                errors = listOf("JSON nesting too deep (possible malformed input)"),
            )
        }

        if (!AutomationSchema.isSupportedVersion(bundle.schemaVersion)) {
            return ImportResult(
                imported = 0,
                skipped = bundle.automations.size,
                errors = listOf("Unsupported schema version: ${bundle.schemaVersion}. Supported: ${AutomationSchema.supportedVersions}"),
            )
        }

        for (automation in bundle.automations) {
            try {
                val existing = store.get(automation.id)
                if (existing != null && !overwrite) {
                    skipped++
                    errors.add("Skipped '${automation.name}' (ID ${automation.id.value} already exists)")
                    continue
                }

                val toSave = automation.copy(
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
