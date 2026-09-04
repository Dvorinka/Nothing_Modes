package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ExportBundle
import kotlinx.serialization.encodeToString
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportExportEdgeCasesTest {

    private fun makeAutomation(id: String, name: String = id): Automation = Automation(
        id = AutomationId(id),
        name = name,
        type = AutomationType.ROUTINE,
        createdBy = CreatedBy.USER,
        status = AutomationStatus.ARMED,
        trigger = Trigger.Boot,
        actions = listOf(Action.SetDnd(DndMode.OFF)),
    )

    @Test
    fun `import empty JSON string returns parse error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("")

        assertEquals(0, result.imported)
        assertTrue(result.errors.any { it.contains("Failed to parse") })
    }

    @Test
    fun `import empty JSON object returns zero automations`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("""{"schemaVersion":1,"exportedAt":1000,"automations":[]}""")

        assertEquals(0, result.imported)
        assertEquals(0, result.skipped)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `import corrupted JSON with truncated structure returns parse error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("""{"schemaVersion":1,"exportedAt":1000,"automations":[{"type":"time",""")

        assertEquals(0, result.imported)
        assertTrue(result.errors.any { it.contains("Failed to parse") })
    }

    @Test
    fun `import JSON with garbage bytes returns parse error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("not even close to json {{{")

        assertEquals(0, result.imported)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `import wrong schema version v0 returns unsupported error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("""{"schemaVersion":0,"exportedAt":1000,"automations":[]}""")

        assertEquals(0, result.imported)
        assertTrue(result.errors.any { it.contains("Unsupported schema version") })
    }

    @Test
    fun `import wrong schema version v999 returns unsupported error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("""{"schemaVersion":999,"exportedAt":1000,"automations":[]}""")

        assertEquals(0, result.imported)
        assertTrue(result.errors.any { it.contains("Unsupported schema version") })
    }

    @Test
    fun `import with missing automations field returns parse error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("""{"schemaVersion":1,"exportedAt":1000}""")

        assertEquals(0, result.imported)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `import with missing schemaVersion field returns parse error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("""{"exportedAt":1000,"automations":[]}""")

        assertEquals(0, result.imported)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `import automation with unknown action type returns parse error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val json = """{"schemaVersion":1,"exportedAt":1000,"automations":[{"id":"x","name":"X","type":"ROUTINE","createdBy":"USER","status":"ARMED","trigger":{"type":"boot"},"actions":[{"type":"nonexistent_action"}]}]}"""

        val result = service.import(json)

        assertEquals(0, result.imported)
        assertTrue(result.errors.any { it.contains("Failed to parse") })
    }

    @Test
    fun `import automation with unknown trigger type returns parse error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val json = """{"schemaVersion":1,"exportedAt":1000,"automations":[{"id":"x","name":"X","type":"ROUTINE","createdBy":"USER","status":"ARMED","trigger":{"type":"nonexistent_trigger"},"actions":[]}]}"""

        val result = service.import(json)

        assertEquals(0, result.imported)
        assertTrue(result.errors.any { it.contains("Failed to parse") })
    }

    @Test
    fun `import with duplicate IDs skips second when overwrite is false`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        // Build a bundle with two automations sharing the same ID
        val auto = makeAutomation("dup-1", "First")
        val bundle = ExportBundle(schemaVersion = 1, exportedAt = 1000, automations = listOf(auto, auto))
        val json = EngineJson.json.encodeToString(bundle)

        val result = service.import(json, overwrite = false)

        assertEquals(1, result.imported)
        assertEquals(1, result.skipped)
        assertTrue(result.errors.any { it.contains("already exists") })
    }

    @Test
    fun `import overwrites duplicate IDs when overwrite is true`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val auto = makeAutomation("dup-1", "First")
        val bundle = ExportBundle(schemaVersion = 1, exportedAt = 1000, automations = listOf(auto, auto))
        val json = EngineJson.json.encodeToString(bundle)

        val result = service.import(json, overwrite = true)

        assertEquals(2, result.imported)
        assertEquals(0, result.skipped)
    }

    @Test
    fun `import sets enabled to false and createdBy to IMPORT`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val auto = makeAutomation("test-1").copy(enabled = true, createdBy = CreatedBy.USER)
        val bundle = ExportBundle(schemaVersion = 1, exportedAt = 1000, automations = listOf(auto))
        val json = EngineJson.json.encodeToString(bundle)

        val result = service.import(json)

        assertEquals(1, result.imported)
        val imported = store.get(AutomationId("test-1"))!!
        assertEquals(false, imported.enabled)
        assertEquals(CreatedBy.IMPORT, imported.createdBy)
    }

    @Test
    fun `export empty store produces valid JSON with zero count`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.export()

        assertEquals(0, result.count)
        assertTrue(result.json.contains("\"schemaVersion\""))
        assertTrue(result.json.contains("\"automations\":[]"))
    }

    @Test
    fun `export by non-existent IDs produces zero count`() = runTest {
        val store = InMemoryAutomationStore()
        store.save(makeAutomation("real-1"))
        val service = ImportExportService(store) { 1000L }

        val result = service.export(listOf(AutomationId("nonexistent")))

        assertEquals(0, result.count)
    }

    @Test
    fun `import-export round trip with complex nested conditions`() = runTest {
        val store = InMemoryAutomationStore()
        val original = Automation(
            id = AutomationId("complex-1"),
            name = "Complex",
            type = AutomationType.MODE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.TimeWindow("22:00", "07:00", "Europe/Prague"),
            actions = listOf(
                Action.SetDnd(DndMode.PRIORITY),
                Action.SetBrightness(26, restore = true),
            ),
            conditions = com.tdvorak.nothingmodes.engine.model.Condition.And(listOf(
                com.tdvorak.nothingmodes.engine.model.Condition.Or(listOf(
                    com.tdvorak.nothingmodes.engine.model.Condition.Charging(true),
                    com.tdvorak.nothingmodes.engine.model.Condition.BatteryLevel(
                        com.tdvorak.nothingmodes.engine.model.CmpOp.GT, 50),
                )),
                com.tdvorak.nothingmodes.engine.model.Condition.Not(
                    com.tdvorak.nothingmodes.engine.model.Condition.WifiConnected("WorkWiFi")),
            )),
            priority = 10,
            cooldownMs = 30_000,
        )
        store.save(original)
        val service = ImportExportService(store) { 1000L }

        val exported = service.export()
        val newStore = InMemoryAutomationStore()
        val newService = ImportExportService(newStore) { 2000L }
        newService.import(exported.json)

        val restored = newStore.get(AutomationId("complex-1"))!!
        assertEquals(original.conditions, restored.conditions)
        assertEquals(original.trigger, restored.trigger)
        assertEquals(original.priority, restored.priority)
        assertEquals(original.cooldownMs, restored.cooldownMs)
    }

    @Test
    fun `import JSON with extra unknown fields succeeds (forward compatibility)`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val json = """{"schemaVersion":1,"exportedAt":1000,"automations":[],"futureField":"value","nested":{"unknown":true}}"""

        val result = service.import(json)

        assertEquals(0, result.imported)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `import JSON with null automations field returns parse error`() = runTest {
        val store = InMemoryAutomationStore()
        val service = ImportExportService(store) { 1000L }

        val result = service.import("""{"schemaVersion":1,"exportedAt":1000,"automations":null}""")

        assertEquals(0, result.imported)
        assertTrue(result.errors.isNotEmpty())
    }
}
