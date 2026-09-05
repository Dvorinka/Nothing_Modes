package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CapabilityIds
import com.tdvorak.nothingmodes.engine.model.CapabilityLabels
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportExportPreviewTest {
    private fun automation(id: String = "a1") =
        Automation(
            id = AutomationId(id),
            name = "Test $id",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Time(at = "23:30", tz = "UTC"),
            actions = listOf(Action.SetDnd(DndMode.PRIORITY), Action.SetGlyph(on = true)),
            enabled = true,
        )

    @Test
    fun `preview parses bundle and derives capabilities without persisting`() =
        runTest {
            val store = InMemoryAutomationStore()
            val service = ImportExportService(store, "9.9.9")
            store.save(automation())
            val json = service.export().json

            val preview = service.preview(json)
            assertTrue(preview.isSupported)
            assertEquals(1, preview.automations.size)
            assertEquals("9.9.9", preview.appVersion)
            assertTrue(preview.requiredCapabilities.contains(CapabilityIds.ACTION_SET_DND))
            assertTrue(preview.requiredCapabilities.contains(CapabilityIds.ACTION_SET_GLYPH))

            val empty = InMemoryAutomationStore()
            ImportExportService(empty).preview(json)
            assertTrue(empty.all().isEmpty())
        }

    @Test
    fun `preview rejects unsupported schema version`() =
        runTest {
            val service = ImportExportService(InMemoryAutomationStore())
            val preview = service.preview("""{"schemaVersion":99,"exportedAt":0,"automations":[]}""")
            assertFalse(preview.isSupported)
            assertTrue(preview.errors.first().contains("Unsupported schema version"))
        }

    @Test
    fun `preview rejects malformed json`() =
        runTest {
            val service = ImportExportService(InMemoryAutomationStore())
            val preview = service.preview("not json")
            assertFalse(preview.isSupported)
        }

    @Test
    fun `legacy bundle without new fields still imports`() =
        runTest {
            val store = InMemoryAutomationStore()
            val service = ImportExportService(store)
            val legacy =
                """
                {"schemaVersion":1,"exportedAt":1,"automations":[
                  {"id":"x1","name":"Old","type":"ROUTINE","createdBy":"USER","status":"ARMED",
                   "trigger":{"type":"time","at":"08:00","tz":"UTC"},
                   "actions":[{"type":"set_dnd","mode":"TOTAL"}]}
                ]}
                """.trimIndent()
            val result = service.import(legacy)
            assertEquals(1, result.imported)
            assertFalse(store.get(AutomationId("x1"))!!.enabled)
        }

    @Test
    fun `capability labels cover required ids`() {
        assertEquals("Do Not Disturb access", CapabilityLabels.describe(CapabilityIds.ACTION_SET_DND))
        assertTrue(CapabilityLabels.describe(CapabilityIds.ACTION_SET_GLYPH).contains("Glyph"))
        assertEquals("", CapabilityLabels.describe(CapabilityIds.TRIGGER_MANUAL))
    }
}
