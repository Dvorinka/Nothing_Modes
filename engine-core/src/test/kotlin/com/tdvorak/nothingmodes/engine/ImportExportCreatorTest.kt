package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.CreatorProfile
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ExportBundle
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportExportCreatorTest {
    private fun makeAutomation(): Automation =
        Automation(
            id = AutomationId("mode-sleep"),
            name = "Sleep",
            type = AutomationType.MODE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = Trigger.Time(cron = "30 22 * * *", tz = "UTC"),
            actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
        )

    @Test
    fun `export with creator metadata round-trips through preview`() =
        runTest {
            val store = InMemoryAutomationStore()
            store.save(makeAutomation())
            val service = ImportExportService(store, appVersion = "0.10.0") { 1000L }
            val creator =
                CreatorProfile(
                    displayName = "Test Author",
                    handle = "@test",
                    note = "A quiet routine",
                    license = "MIT",
                )

            val result = service.export(creator)

            val preview = service.preview(result.json)
            assertTrue(preview.isSupported)
            assertEquals("Test Author", preview.creatorProfile.displayName)
            assertEquals("@test", preview.creatorProfile.handle)
            assertEquals("A quiet routine", preview.creatorProfile.note)
            assertEquals("MIT", preview.creatorProfile.license)
        }

    @Test
    fun `bundle without creatorProfile defaults to anonymous`() =
        runTest {
            val oldBundle =
                """
                {"schemaVersion":1,"exportedAt":1000,"appVersion":"0.9.0","requiredCapabilities":[],"automations":[]}
                """.trimIndent()

            val preview = ImportExportService(InMemoryAutomationStore()) { 0L }.preview(oldBundle)

            assertTrue(preview.isSupported)
            assertTrue(preview.creatorProfile.isAnonymous)
            assertEquals(CreatorProfile.DEFAULT_LICENSE, preview.creatorProfile.license)
        }

    @Test
    fun `creator fields are sanitized on export`() =
        runTest {
            val store = InMemoryAutomationStore()
            store.save(makeAutomation())
            val service = ImportExportService(store) { 1000L }
            val dirty =
                CreatorProfile(
                    displayName = "  Author  ",
                    handle = "  ",
                    note = "x".repeat(2000),
                    license = "  ",
                )

            val result = service.export(dirty)
            val preview = service.preview(result.json)

            assertEquals("Author", preview.creatorProfile.displayName)
            assertEquals(1000, preview.creatorProfile.note.length)
            assertEquals(CreatorProfile.DEFAULT_LICENSE, preview.creatorProfile.license)
            assertEquals("", preview.creatorProfile.handle)
            assertFalse(preview.creatorProfile.isAnonymous)
        }

    @Test
    fun `json with unknown creator fields is ignored`() {
        val extra =
            """
            {"schemaVersion":1,"exportedAt":1000,"appVersion":"0.9.0","requiredCapabilities":[],
            "creatorProfile":{"displayName":"A","unknownField":"ignored"},
            "automations":[]}
            """.trimIndent()

        val preview = ImportExportService(InMemoryAutomationStore()) { 0L }.preview(extra)

        assertTrue(preview.isSupported)
        assertEquals("A", preview.creatorProfile.displayName)
    }

    @Test
    fun `encode creates valid json`() =
        runTest {
            val store = InMemoryAutomationStore()
            store.save(makeAutomation())
            val service = ImportExportService(store, appVersion = "0.10.0") { 1000L }
            val creator = CreatorProfile(displayName = "TDvorak", license = "GPL-3.0")

            val result = service.export(listOf(AutomationId("mode-sleep")), creator)

            val decoded = EngineJson.json.decodeFromString(ExportBundle.serializer(), result.json)
            assertEquals("TDvorak", decoded.creatorProfile.displayName)
            assertEquals("GPL-3.0", decoded.creatorProfile.license)
            assertEquals(
                "mode-sleep",
                decoded.automations
                    .single()
                    .id.value,
            )
        }
}
