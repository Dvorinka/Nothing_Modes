package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.BatteryDirection
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.Transition
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import com.tdvorak.nothingmodes.engine.runtime.TriggerMatcher
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId

class TriggerMatcherTest {

    private val matcher = TriggerMatcher()

    // --- Time ---

    @Test
    fun `Time trigger matches TimeFired event`() {
        val trigger = Trigger.Time(cron = "0 8 * * *", tz = "UTC")
        val event = TriggerEvent.TimeFired("e1", com.tdvorak.nothingmodes.engine.model.AutomationId("a1"), 1000L)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Time trigger does not match BootCompleted event`() {
        val trigger = Trigger.Time(cron = "0 8 * * *", tz = "UTC")
        val event = TriggerEvent.BootCompleted("e1")
        assertFalse(matcher.matches(trigger, event))
    }

    // --- Immediate ---

    @Test
    fun `Immediate trigger matches Registered event`() {
        val trigger = Trigger.Immediate
        val event = TriggerEvent.Registered("e1", com.tdvorak.nothingmodes.engine.model.AutomationId("a1"))
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Immediate trigger does not match BootCompleted event`() {
        val trigger = Trigger.Immediate
        val event = TriggerEvent.BootCompleted("e1")
        assertFalse(matcher.matches(trigger, event))
    }

    // --- TimeWindow ---

    @Test
    fun `TimeWindow trigger matches ModeWindowStart event`() {
        val trigger = Trigger.TimeWindow("22:00", "07:00", "UTC")
        val event = TriggerEvent.ModeWindowStart("e1", com.tdvorak.nothingmodes.engine.model.AutomationId("a1"), 1000L)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `TimeWindow trigger matches ModeWindowEnd event`() {
        val trigger = Trigger.TimeWindow("22:00", "07:00", "UTC")
        val event = TriggerEvent.ModeWindowEnd("e1", com.tdvorak.nothingmodes.engine.model.AutomationId("a1"), 1000L)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `TimeWindow trigger does not match TimeFired event`() {
        val trigger = Trigger.TimeWindow("22:00", "07:00", "UTC")
        val event = TriggerEvent.TimeFired("e1", com.tdvorak.nothingmodes.engine.model.AutomationId("a1"), 1000L)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `TimeWindow isWindowActive - inside window`() {
        val trigger = Trigger.TimeWindow("08:00", "12:00", "UTC")
        val now = LocalDateTime.of(2026, 6, 15, 10, 0, 0, 0)
        assertTrue(matcher.isWindowActive(trigger, now, ZoneId.of("UTC")))
    }

    @Test
    fun `TimeWindow isWindowActive - outside window`() {
        val trigger = Trigger.TimeWindow("08:00", "12:00", "UTC")
        val now = LocalDateTime.of(2026, 6, 15, 14, 0, 0, 0)
        assertFalse(matcher.isWindowActive(trigger, now, ZoneId.of("UTC")))
    }

    @Test
    fun `TimeWindow isWindowActive - crossing midnight before end`() {
        val trigger = Trigger.TimeWindow("22:00", "07:00", "UTC")
        val now = LocalDateTime.of(2026, 6, 15, 3, 0, 0, 0)
        assertTrue(matcher.isWindowActive(trigger, now, ZoneId.of("UTC")))
    }

    @Test
    fun `TimeWindow isWindowActive - crossing midnight after start`() {
        val trigger = Trigger.TimeWindow("22:00", "07:00", "UTC")
        val now = LocalDateTime.of(2026, 6, 15, 23, 0, 0, 0)
        assertTrue(matcher.isWindowActive(trigger, now, ZoneId.of("UTC")))
    }

    @Test
    fun `TimeWindow isWindowActive - day filter excludes today`() {
        val trigger = Trigger.TimeWindow("08:00", "12:00", "UTC", days = listOf(DayOfWeek.SATURDAY))
        val monday = LocalDateTime.of(2026, 6, 15, 10, 0, 0, 0)
        assertFalse(matcher.isWindowActive(trigger, monday, ZoneId.of("UTC")))
    }

    @Test
    fun `TimeWindow isWindowActive - day filter includes today`() {
        val trigger = Trigger.TimeWindow("08:00", "12:00", "UTC", days = listOf(DayOfWeek.MONDAY))
        val monday = LocalDateTime.of(2026, 6, 15, 10, 0, 0, 0)
        assertTrue(matcher.isWindowActive(trigger, monday, ZoneId.of("UTC")))
    }

    @Test
    fun `Time shouldFireOnDay - null days fires every day`() {
        val trigger = Trigger.Time(cron = "0 8 * * *", tz = "UTC")
        assertTrue(matcher.shouldFireOnDay(trigger, java.time.DayOfWeek.MONDAY))
        assertTrue(matcher.shouldFireOnDay(trigger, java.time.DayOfWeek.SUNDAY))
    }

    @Test
    fun `Time shouldFireOnDay - filtered days`() {
        val trigger = Trigger.Time(cron = "0 8 * * *", tz = "UTC", days = listOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        assertTrue(matcher.shouldFireOnDay(trigger, java.time.DayOfWeek.MONDAY))
        assertFalse(matcher.shouldFireOnDay(trigger, java.time.DayOfWeek.TUESDAY))
        assertTrue(matcher.shouldFireOnDay(trigger, java.time.DayOfWeek.FRIDAY))
    }

    // --- Notification ---

    @Test
    fun `Notification trigger matches with package only`() {
        val trigger = Trigger.Notification(pkg = "com.whatsapp")
        val event = TriggerEvent.NotificationPosted("e1", "com.whatsapp", "Hello", "World", null)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Notification trigger does not match different package`() {
        val trigger = Trigger.Notification(pkg = "com.whatsapp")
        val event = TriggerEvent.NotificationPosted("e1", "com.telegram", "Hello", "World", null)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `Notification trigger matches with sender filter`() {
        val trigger = Trigger.Notification(pkg = "com.whatsapp", sender = "Mom")
        val event = TriggerEvent.NotificationPosted("e1", "com.whatsapp", "Hi", "msg", "Mom")
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Notification trigger does not match when sender differs`() {
        val trigger = Trigger.Notification(pkg = "com.whatsapp", sender = "Mom")
        val event = TriggerEvent.NotificationPosted("e1", "com.whatsapp", "Hi", "msg", "Dad")
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `Notification trigger matches with titleMatch contains`() {
        val trigger = Trigger.Notification(pkg = "com.whatsapp", titleMatch = "OTP")
        val event = TriggerEvent.NotificationPosted("e1", "com.whatsapp", "Your OTP code", "1234", null)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Notification trigger does not match when title does not contain match`() {
        val trigger = Trigger.Notification(pkg = "com.whatsapp", titleMatch = "OTP")
        val event = TriggerEvent.NotificationPosted("e1", "com.whatsapp", "Hello", "msg", null)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `Notification trigger matches with textMatch contains`() {
        val trigger = Trigger.Notification(pkg = "com.whatsapp", textMatch = "code")
        val event = TriggerEvent.NotificationPosted("e1", "com.whatsapp", "Msg", "Your code is 1234", null)
        assertTrue(matcher.matches(trigger, event))
    }

    // --- PhoneState ---

    @Test
    fun `PhoneState trigger matches incoming call`() {
        val trigger = Trigger.PhoneState(event = PhoneEvent.INCOMING_CALL)
        val event = TriggerEvent.PhoneStateChanged("e1", PhoneEvent.INCOMING_CALL, "+1234567890", null)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `PhoneState trigger does not match different event`() {
        val trigger = Trigger.PhoneState(event = PhoneEvent.INCOMING_CALL)
        val event = TriggerEvent.PhoneStateChanged("e1", PhoneEvent.CALL_ENDED, null, null)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `PhoneState trigger matches with number filter`() {
        val trigger = Trigger.PhoneState(event = PhoneEvent.INCOMING_CALL, number = "+1234567890")
        val event = TriggerEvent.PhoneStateChanged("e1", PhoneEvent.INCOMING_CALL, "+1234567890", null)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `PhoneState trigger does not match when number differs`() {
        val trigger = Trigger.PhoneState(event = PhoneEvent.INCOMING_CALL, number = "+1234567890")
        val event = TriggerEvent.PhoneStateChanged("e1", PhoneEvent.INCOMING_CALL, "+9999999999", null)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `PhoneState trigger matches SMS with textMatch`() {
        val trigger = Trigger.PhoneState(event = PhoneEvent.SMS_RECEIVED, textMatch = "OTP")
        val event = TriggerEvent.PhoneStateChanged("e1", PhoneEvent.SMS_RECEIVED, null, "Your OTP is 1234")
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `PhoneState trigger does not match SMS when text does not contain match`() {
        val trigger = Trigger.PhoneState(event = PhoneEvent.SMS_RECEIVED, textMatch = "OTP")
        val event = TriggerEvent.PhoneStateChanged("e1", PhoneEvent.SMS_RECEIVED, null, "Hello there")
        assertFalse(matcher.matches(trigger, event))
    }

    // --- Connectivity ---

    @Test
    fun `Connectivity trigger matches WiFi connected`() {
        val trigger = Trigger.Connectivity(medium = ConnMedium.WIFI, state = ConnState.CONNECTED)
        val event = TriggerEvent.ConnectivityChanged("e1", ConnMedium.WIFI, ConnState.CONNECTED, null)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Connectivity trigger does not match different medium`() {
        val trigger = Trigger.Connectivity(medium = ConnMedium.WIFI, state = ConnState.CONNECTED)
        val event = TriggerEvent.ConnectivityChanged("e1", ConnMedium.BT, ConnState.CONNECTED, null)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `Connectivity trigger does not match different state`() {
        val trigger = Trigger.Connectivity(medium = ConnMedium.WIFI, state = ConnState.CONNECTED)
        val event = TriggerEvent.ConnectivityChanged("e1", ConnMedium.WIFI, ConnState.DISCONNECTED, null)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `Connectivity trigger matches with SSID match filter`() {
        val trigger = Trigger.Connectivity(medium = ConnMedium.WIFI, state = ConnState.CONNECTED, match = "HomeWiFi")
        val event = TriggerEvent.ConnectivityChanged("e1", ConnMedium.WIFI, ConnState.CONNECTED, "HomeWiFi_5G")
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Connectivity trigger does not match when SSID does not contain match`() {
        val trigger = Trigger.Connectivity(medium = ConnMedium.WIFI, state = ConnState.CONNECTED, match = "HomeWiFi")
        val event = TriggerEvent.ConnectivityChanged("e1", ConnMedium.WIFI, ConnState.CONNECTED, "OtherWiFi")
        assertFalse(matcher.matches(trigger, event))
    }

    // --- Boot ---

    @Test
    fun `Boot trigger matches BootCompleted event`() {
        assertTrue(matcher.matches(Trigger.Boot, TriggerEvent.BootCompleted("e1")))
    }

    @Test
    fun `Boot trigger does not match TimeFired event`() {
        assertFalse(matcher.matches(Trigger.Boot,
            TriggerEvent.TimeFired("e1", com.tdvorak.nothingmodes.engine.model.AutomationId("a1"), 1000L)))
    }

    // --- BatteryLevel ---

    @Test
    fun `BatteryLevel trigger matches exact level`() {
        val trigger = Trigger.BatteryLevel(level = 20)
        val event = TriggerEvent.BatteryLevelChanged("e1", 20, isCharging = false)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `BatteryLevel trigger does not match different level`() {
        val trigger = Trigger.BatteryLevel(level = 20)
        val event = TriggerEvent.BatteryLevelChanged("e1", 50, isCharging = false)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `BatteryLevel trigger matches with charging direction`() {
        val trigger = Trigger.BatteryLevel(level = 20, direction = BatteryDirection.CHARGING_STARTED)
        val event = TriggerEvent.BatteryLevelChanged("e1", 20, isCharging = true)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `BatteryLevel trigger does not match when direction does not match`() {
        val trigger = Trigger.BatteryLevel(level = 20, direction = BatteryDirection.CHARGING_STARTED)
        val event = TriggerEvent.BatteryLevelChanged("e1", 20, isCharging = false)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `BatteryLevel trigger matches with charging stopped direction`() {
        val trigger = Trigger.BatteryLevel(level = 20, direction = BatteryDirection.CHARGING_STOPPED)
        val event = TriggerEvent.BatteryLevelChanged("e1", 20, isCharging = false)
        assertTrue(matcher.matches(trigger, event))
    }

    // --- ScreenStateTrigger ---

    @Test
    fun `ScreenStateTrigger matches screen ON`() {
        val trigger = Trigger.ScreenStateTrigger(state = ScreenState.ON)
        val event = TriggerEvent.ScreenStateChanged("e1", ScreenState.ON)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `ScreenStateTrigger does not match screen OFF when trigger is ON`() {
        val trigger = Trigger.ScreenStateTrigger(state = ScreenState.ON)
        val event = TriggerEvent.ScreenStateChanged("e1", ScreenState.OFF)
        assertFalse(matcher.matches(trigger, event))
    }

    // --- AppOpened ---

    @Test
    fun `AppOpened trigger matches when app comes to foreground`() {
        val trigger = Trigger.AppOpened(pkg = "com.android.chrome")
        val event = TriggerEvent.AppForegroundChanged("e1", "com.android.chrome", inForeground = true)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `AppOpened trigger does not match when app goes to background`() {
        val trigger = Trigger.AppOpened(pkg = "com.android.chrome")
        val event = TriggerEvent.AppForegroundChanged("e1", "com.android.chrome", inForeground = false)
        assertFalse(matcher.matches(trigger, event))
    }

    @Test
    fun `AppOpened trigger does not match different package`() {
        val trigger = Trigger.AppOpened(pkg = "com.android.chrome")
        val event = TriggerEvent.AppForegroundChanged("e1", "com.whatsapp", inForeground = true)
        assertFalse(matcher.matches(trigger, event))
    }

    // --- Geofence ---

    @Test
    fun `Geofence trigger matches ENTER transition`() {
        val trigger = Trigger.Geofence(lat = 50.0, lng = 14.0, radiusM = 100.0, transition = Transition.ENTER)
        val event = TriggerEvent.GeofenceTriggered("e1", 50.0, 14.0, Transition.ENTER)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Geofence trigger matches EXIT transition`() {
        val trigger = Trigger.Geofence(lat = 50.0, lng = 14.0, radiusM = 100.0, transition = Transition.EXIT)
        val event = TriggerEvent.GeofenceTriggered("e1", 50.0, 14.0, Transition.EXIT)
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `Geofence trigger does not match different transition`() {
        val trigger = Trigger.Geofence(lat = 50.0, lng = 14.0, radiusM = 100.0, transition = Transition.ENTER)
        val event = TriggerEvent.GeofenceTriggered("e1", 50.0, 14.0, Transition.EXIT)
        assertFalse(matcher.matches(trigger, event))
    }
}
