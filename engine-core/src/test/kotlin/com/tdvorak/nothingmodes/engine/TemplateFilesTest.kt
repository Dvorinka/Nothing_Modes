package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.TemplateIndex
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Guards the shipped community templates: index parses, every bundle previews clean. */
class TemplateFilesTest {
    private val templatesDir = File("../templates")

    @Test
    fun `template index parses and every file previews without errors`() {
        val indexFile = File(templatesDir, "index.json")
        assertTrue(indexFile.exists(), "templates/index.json missing")
        val index = EngineJson.json.decodeFromString(TemplateIndex.serializer(), indexFile.readText())
        assertTrue(index.templates.isNotEmpty())

        val service = ImportExportService(InMemoryAutomationStore())
        index.templates.forEach { template ->
            val bundleFile = File(templatesDir, template.file)
            assertTrue(bundleFile.exists(), "missing template file ${template.file}")
            val preview = service.preview(bundleFile.readText())
            assertEquals(emptyList<String>(), preview.errors, "template ${template.id} failed preview")
            assertTrue(preview.automations.isNotEmpty(), "template ${template.id} has no automations")
        }
    }
}
