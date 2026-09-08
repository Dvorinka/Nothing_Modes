package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.CapabilityIds
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.Transition
import com.tdvorak.nothingmodes.engine.model.Trigger
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CapabilityRequirementsTest {
    @Test
    fun `time trigger requires trigger_time`() {
        val caps = CapabilityRequirements.derive(Trigger.Time(cron = "0 0 * * *", tz = "UTC"), emptyList())
        assertTrue(CapabilityIds.TRIGGER_TIME in caps)
    }

    @Test
    fun `setDnd action requires dnd access`() {
        val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(Action.SetDnd(DndMode.TOTAL)))
        assertTrue(CapabilityIds.ACTION_SET_DND in caps)
    }

    @Test
    fun `setWifi action requires wifi and shizuku`() {
        val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(Action.SetWifi(true)))
        assertTrue(CapabilityIds.ACTION_SET_WIFI in caps)
        assertTrue(CapabilityIds.SHIZUKU_REQUIRED in caps)
    }

    @Test
    fun `lock screen action emits lock screen capability`() {
        val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(Action.LockScreen()))
        assertTrue(CapabilityIds.ACTION_LOCK_SCREEN in caps)
    }

    @Test
    fun `forced lock screen action emits no lock screen capability`() {
        val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(Action.LockScreen(force = true)))
        assertFalse(CapabilityIds.ACTION_LOCK_SCREEN in caps)
    }

    @Test
    fun `send sms action emits sms capability`() {
        val caps =
            CapabilityRequirements.derive(
                Trigger.Immediate,
                listOf(Action.SendSms("123", "hello")),
            )
        assertTrue(CapabilityIds.ACTION_SEND_SMS in caps)
    }

    @Test
    fun `sms received trigger emits phone sms capability`() {
        val caps = CapabilityRequirements.derive(Trigger.PhoneState(PhoneEvent.SMS_RECEIVED), emptyList())
        assertTrue(CapabilityIds.TRIGGER_PHONE_SMS in caps)
    }

    @Test
    fun `geofence trigger and location condition combine`() {
        val caps =
            CapabilityRequirements.derive(
                Trigger.Geofence(lat = 0.0, lng = 0.0, radiusM = 10.0, transition = Transition.ENTER),
                emptyList(),
                Condition.LocationEnabled(enabled = true),
            )
        assertTrue(CapabilityIds.TRIGGER_GEOFENCE in caps)
        assertTrue(CapabilityIds.STATE_LOCATION in caps)
    }

    @Test
    fun `app in foreground condition requires usage access`() {
        val caps =
            CapabilityRequirements.derive(
                Trigger.Immediate,
                emptyList(),
                Condition.AppInForeground("com.example"),
            )
        assertTrue(CapabilityIds.STATE_FOREGROUND_APP in caps)
    }

    @Test
    fun `write setting requires shizuku`() {
        val caps =
            CapabilityRequirements.derive(
                Trigger.Immediate,
                listOf(
                    Action.WriteSetting(
                        namespace = SettingNamespace.SYSTEM,
                        key = "foo",
                        value = "bar",
                    ),
                ),
            )
        assertTrue(CapabilityIds.ACTION_WRITE_SETTING in caps)
        assertTrue(CapabilityIds.SHIZUKU_REQUIRED in caps)
    }

    @Test
    fun `connectivity wifi trigger requires wifi`() {
        val caps = CapabilityRequirements.derive(Trigger.Connectivity(ConnMedium.WIFI, ConnState.CONNECTED), emptyList())
        assertTrue(CapabilityIds.TRIGGER_CONNECTIVITY_WIFI in caps)
    }

    @Test
    fun `and condition collapses to union`() {
        val caps =
            CapabilityRequirements.derive(
                Trigger.Immediate,
                emptyList(),
                Condition.And(
                    listOf(
                        Condition.BatteryLevel(op = CmpOp.GTE, level = 20),
                        Condition.WifiConnected(),
                    ),
                ),
            )
        assertTrue(CapabilityIds.STATE_READER_BUILTIN in caps)
    }
}
