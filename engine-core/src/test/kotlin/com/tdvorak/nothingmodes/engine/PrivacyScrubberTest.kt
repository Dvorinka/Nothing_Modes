package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.PickedCalendarEvent
import com.tdvorak.nothingmodes.engine.model.Transition
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.ImportExportService
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import com.tdvorak.nothingmodes.engine.runtime.PrivacyScrubber
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrivacyScrubberTest {
    private fun automationWith(
        trigger: Trigger,
        actions: List<Action> = emptyList(),
        conditions: Condition? = null,
    ): Automation =
        Automation(
            id = AutomationId("test"),
            name = "Test",
            type = AutomationType.ROUTINE,
            createdBy = CreatedBy.USER,
            status = AutomationStatus.ARMED,
            trigger = trigger,
            actions = actions,
            conditions = conditions,
        )

    @Test
    fun `bluetooth trigger loses name and MAC, flags setup`() {
        val automation =
            automationWith(
                Trigger.BluetoothDevice(
                    state = ConnState.CONNECTED,
                    deviceName = "My Headphones",
                    deviceAddress = "AA:BB:CC:DD:EE:FF",
                ),
            )

        val (scrubbed, missing) = PrivacyScrubber.scrub(automation)
        val t = scrubbed.trigger as Trigger.BluetoothDevice

        assertNull(t.deviceName)
        assertNull(t.deviceAddress)
        assertEquals(ConnState.CONNECTED, t.state)
        assertTrue(missing.contains("Bluetooth device"))
    }

    @Test
    fun `wifi, phone, geofence, notification, calendar fields all scrub`() {
        val cases =
            listOf(
                Trigger.WifiConnected(ssid = "HomeNet") to "Wi-Fi network",
                Trigger.PhoneState(PhoneEvent.INCOMING_CALL, number = "+420777123456") to "phone number",
                Trigger.Geofence(lat = 50.08, lng = 14.43, radiusM = 200.0, transition = Transition.ENTER) to "location",
                Trigger.Notification(
                    pkg = "com.whatsapp",
                    conversationId = "conv-42",
                    sender = "Mom",
                    titleMatch = "dinner",
                ) to "notification filter",
                Trigger.CalendarEvent(
                    calendarId = "7",
                    titleMatch = "dentist",
                    events = listOf(PickedCalendarEvent(99L, "Dentist", "Personal")),
                ) to "calendar event",
                Trigger.Connectivity(ConnMedium.WIFI, ConnState.CONNECTED, match = "HomeNet") to "connection detail",
            )

        cases.forEach { (trigger, hint) ->
            val (scrubbed, missing) = PrivacyScrubber.scrub(automationWith(trigger))
            assertTrue(missing.contains(hint), "expected '$hint' for $trigger")
            // The scrubbed trigger keeps its shape (type survives).
            assertEquals(trigger::class, scrubbed.trigger::class)
        }
    }

    @Test
    fun `private actions scrub, nested inside groups too`() {
        val automation =
            automationWith(
                Trigger.Manual,
                actions =
                    listOf(
                        Action.SendSms("+420777123456", "on my way"),
                        Action.CopyText("my-api-key"),
                        Action.OpenUrl("https://192.168.1.10:8123/secret-dashboard"),
                        Action.SetWallpaper("content://media/0/images/42"),
                        Action.Group(
                            "inner",
                            listOf(Action.SendSms("555", "nested secret")),
                        ),
                    ),
            )

        val (scrubbed, missing) = PrivacyScrubber.scrub(automation)
        val a = scrubbed.actions

        assertEquals("", (a[0] as Action.SendSms).number)
        assertEquals("", (a[0] as Action.SendSms).text)
        assertEquals("", (a[1] as Action.CopyText).text)
        assertEquals("", (a[2] as Action.OpenUrl).url)
        assertEquals("", (a[3] as Action.SetWallpaper).uri)
        assertEquals("", ((a[4] as Action.Group).actions[0] as Action.SendSms).number)

        assertTrue(missing.contains("SMS recipient and text"))
        assertTrue(missing.contains("clipboard text"))
        assertTrue(missing.contains("URL"))
        assertTrue(missing.contains("wallpaper image"))
    }

    @Test
    fun `conditions scrub recursively including nested and or not`() {
        val automation =
            automationWith(
                Trigger.Manual,
                conditions =
                    Condition.And(
                        listOf(
                            Condition.WifiConnected(ssid = "HomeNet"),
                            Condition.Or(
                                listOf(
                                    Condition.AtLocation(lat = 50.08, lng = 14.43, radiusM = 100.0),
                                    Condition.Not(
                                        Condition.EventActive(titleMatch = "secret meeting"),
                                    ),
                                ),
                            ),
                        ),
                    ),
            )

        val (scrubbed, missing) = PrivacyScrubber.scrub(automation)
        val and = scrubbed.conditions as Condition.And
        val or = and.all[1] as Condition.Or
        val not = or.any[1] as Condition.Not

        assertNull((and.all[0] as Condition.WifiConnected).ssid)
        assertEquals(0.0, (or.any[0] as Condition.AtLocation).lat)
        assertEquals("", (not.cond as Condition.EventActive).titleMatch)

        assertTrue(missing.contains("Wi-Fi network"))
        assertTrue(missing.contains("location"))
        assertTrue(missing.contains("calendar event title"))
    }

    @Test
    fun `export scrubs and import marks automation needs review`() =
        runTest {
            val store = InMemoryAutomationStore()
            store.save(
                automationWith(
                    Trigger.BluetoothDevice(
                        state = ConnState.CONNECTED,
                        deviceName = "My Headphones",
                        deviceAddress = "AA:BB:CC:DD:EE:FF",
                    ),
                    actions = listOf(Action.LaunchApp(listOf("com.spotify.music"))),
                ),
            )

            val export = ImportExportService(store) { 1000L }.export()

            assertFalse(export.json.contains("AA:BB:CC:DD:EE:FF"))
            assertFalse(export.json.contains("My Headphones"))
            assertTrue(export.json.contains("setupRequirements"))

            val preview = ImportExportService(InMemoryAutomationStore()) { 0L }.preview(export.json)
            assertTrue(preview.requiresSetup)
            assertEquals(
                listOf("Bluetooth device"),
                preview.setupRequirements["test"],
            )

            val newStore = InMemoryAutomationStore()
            ImportExportService(newStore) { 0L }.import(export.json)

            val imported = newStore.get(AutomationId("test"))!!
            assertEquals(AutomationStatus.NEEDS_REVIEW, imported.status)
            assertFalse(imported.enabled)
            assertEquals(CreatedBy.IMPORT, imported.createdBy)
        }

    @Test
    fun `backup export keeps private fields intact`() =
        runTest {
            val store = InMemoryAutomationStore()
            store.save(
                automationWith(
                    Trigger.BluetoothDevice(
                        state = ConnState.CONNECTED,
                        deviceName = "My Headphones",
                        deviceAddress = "AA:BB:CC:DD:EE:FF",
                    ),
                ),
            )

            val export = ImportExportService(store) { 1000L }.exportBackup()

            assertTrue(export.json.contains("AA:BB:CC:DD:EE:FF"))
            assertTrue(export.json.contains("My Headphones"))

            val preview = ImportExportService(InMemoryAutomationStore()) { 0L }.preview(export.json)
            assertFalse(preview.requiresSetup)
        }

    @Test
    fun `automation with no private fields exports unchanged`() =
        runTest {
            val store = InMemoryAutomationStore()
            store.save(
                automationWith(
                    Trigger.Manual,
                    actions = listOf(Action.Vibrate(200)),
                ),
            )

            val export = ImportExportService(store) { 1000L }.export()
            val preview = ImportExportService(InMemoryAutomationStore()) { 0L }.preview(export.json)

            assertFalse(preview.requiresSetup)
            assertTrue(preview.setupRequirements.isEmpty())
        }
}
