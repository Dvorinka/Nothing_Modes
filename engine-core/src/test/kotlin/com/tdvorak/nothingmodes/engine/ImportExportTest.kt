package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ExportBundle
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportExportTest {

    private fun makeAutomation(id: String, name: String): Automation = Automation(
        id = AutomationId(id),
        name = name,
        type = AutomationType.MODE,
        createdBy = CreatedBy.USER,
        status = AutomationStatus.ARMED,
        trigger = Trigger.Time(cron = "30 22 * * *", tz = "Europe/Prague"),
        actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
    )

    @Test
    fun `export produces valid JSON with all automations`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("mode-sleep", "Sleep"))
        store.save(makeAutomation("mode-work", "Work Focus"))
        val service = ImportExportService(store) { 1000L }

        val result = service.export()

        assertEquals(2, result.count)
        assertTrue(result.json.contains("Sleep"))
        assertTrue(result.json.contains("Work Focus"))
        assertTrue(result.json.contains("\"schemaVersion\""))
    }

    @Test
    fun `export by IDs selects only specified automations`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("mode-sleep", "Sleep"))
        store.save(makeAutomation("mode-work", "Work Focus"))
        val service = ImportExportService(store) { 1000L }

        val result = service.export(listOf(AutomationId("mode-sleep")))

        assertEquals(1, result.count)
        assertTrue(result.json.contains("Sleep"))
        assertTrue(!result.json.contains("Work Focus"))
    }

    @Test
    fun `import restores automations from valid JSON`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("mode-sleep", "Sleep"))
        val service = ImportExportService(store) { 1000L }

        val exportResult = service.export()
        val newStore = InMemoryAutomationStore()
        val newService = ImportExportService(newStore) { 2000L }

        val importResult = newService.import(exportResult.json)

        assertEquals(1, importResult.imported)
        assertEquals(0, importResult.skipped)
        val imported = newStore.get(AutomationId("mode-sleep"))
        assertNotNull(imported)
        assertEquals("Sleep", imported!!.name)
        assertEquals(CreatedBy.IMPORT, imported.createdBy)
        assertEquals(false, imported.enabled)
    }

    @Test
    fun `import skips existing automations when overwrite is false`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("mode-sleep", "Sleep"))
        val service = ImportExportService(store) { 1000L }
        val exportResult = service.export()

        val importResult = service.import(exportResult.json, overwrite = false)

        assertEquals(0, importResult.imported)
        assertEquals(1, importResult.skipped)
        assertTrue(importResult.errors.isNotEmpty())
    }

    @Test
    fun `import overwrites existing automations when overwrite is true`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("mode-sleep", "Sleep"))
        val service = ImportExportService(store) { 1000L }
        val exportResult = service.export()

        val importResult = service.import(exportResult.json, overwrite = true)

        assertEquals(1, importResult.imported)
        assertEquals(0, importResult.skipped)
    }

    @Test
    fun `import rejects malformed JSON`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("not valid json")

        assertEquals(0, result.imported)
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.errors.any { it.contains("Failed to parse") })
    }

    @Test
    fun `import rejects unsupported schema version`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val badJson = """{"schemaVersion":999,"exportedAt":1000,"automations":[]}"""

        val result = service.import(badJson)

        assertEquals(0, result.imported)
        assertTrue(result.errors.any { it.contains("Unsupported schema version") })
    }

    @Test
    fun `export-import round trip preserves automation data`() = runTest {
        val store = InMemoryAutomationStore()
        val original = Automation(
            id = AutomationId("mode-test"),
            name = "Test Mode",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Time(cron = "0 8 * * 1-5", tz = "UTC"),
            actions = listOf(
                Action.SetWifi(true),
                Action.SetBrightness(128, restore = true),
                Action.SetDnd(DndMode.TOTAL),
            ),
            priority = 5,
            cooldownMs = 60000,
        )
        store.save(original)
        val service = ImportExportService(store) { 1000L }

        val exported = service.export()
        val newStore = InMemoryAutomationStore()
        val newService = ImportExportService(newStore) { 2000L }
        newService.import(exported.json)

        val restored = newStore.get(AutomationId("mode-test"))
        assertNotNull(restored)
        assertEquals(original.name, restored!!.name)
        assertEquals(original.type, restored.type)
        assertEquals(original.trigger, restored.trigger)
        assertEquals(original.actions, restored.actions)
        assertEquals(original.priority, restored.priority)
        assertEquals(original.cooldownMs, restored.cooldownMs)
    }
}
