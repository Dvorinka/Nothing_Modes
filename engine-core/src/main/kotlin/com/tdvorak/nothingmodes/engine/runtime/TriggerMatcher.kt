package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.DeviceStateKeys
import com.tdvorak.nothingmodes.engine.model.Trigger
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek as JavaDayOfWeek

/** Matches a trigger against a fired event. */
class TriggerMatcher {
    fun matches(
        trigger: Trigger,
        event: TriggerEvent,
    ): Boolean =
        when (trigger) {
            is Trigger.Time -> event is TriggerEvent.TimeFired

            is Trigger.TimeWindow ->
                event is TriggerEvent.ModeWindowStart ||
                    event is TriggerEvent.ModeWindowEnd

            is Trigger.Immediate -> event is TriggerEvent.Registered

            is Trigger.Notification ->
                event is TriggerEvent.NotificationPosted &&
                    event.pkg == trigger.pkg &&
                    (trigger.sender == null || event.sender?.equals(trigger.sender, ignoreCase = true) == true) &&
                    (trigger.titleMatch == null || event.title?.contains(trigger.titleMatch, ignoreCase = true) == true) &&
                    (trigger.textMatch == null || event.text?.contains(trigger.textMatch, ignoreCase = true) == true) &&
                    (trigger.isGroup == null || event.isGroup == trigger.isGroup) &&
                    (trigger.conversationId == null || event.conversationId == trigger.conversationId)

            is Trigger.PhoneState ->
                event is TriggerEvent.PhoneStateChanged &&
                    event.event == trigger.event &&
                    (trigger.number == null || numbersMatch(event.number, trigger.number)) &&
                    (trigger.textMatch == null || event.smsText?.contains(trigger.textMatch, ignoreCase = true) == true)

            is Trigger.Connectivity ->
                when {
                    // Legacy/imported POWER triggers ride the charger event —
                    // no separate power broadcast exists.
                    trigger.medium == ConnMedium.POWER ->
                        event is TriggerEvent.ChargerConnectedChanged &&
                            (event.connected) == (trigger.state == ConnState.CONNECTED)
                    else ->
                        event is TriggerEvent.ConnectivityChanged &&
                            event.medium == trigger.medium &&
                            event.state == trigger.state &&
                            (trigger.match == null || event.match?.contains(trigger.match, ignoreCase = true) == true)
                }

            is Trigger.Boot -> event is TriggerEvent.BootCompleted

            is Trigger.BatteryLevel ->
                event is TriggerEvent.BatteryLevelChanged &&
                    (
                        event.level == trigger.level ||
                            // Threshold crossed since the last reading — a missed
                            // broadcast (doze, restart) must not skip the trigger.
                            (
                                event.previousLevel != null &&
                                    event.previousLevel != event.level &&
                                    (event.previousLevel - trigger.level).toLong() *
                                        (event.level - trigger.level).toLong() < 0
                            )
                    ) &&
                    (trigger.direction == null || matchesDirection(trigger.direction, event))

            is Trigger.ScreenStateTrigger ->
                event is TriggerEvent.ScreenStateChanged &&
                    event.state == trigger.state

            is Trigger.AppOpened ->
                event is TriggerEvent.AppForegroundChanged &&
                    event.inForeground &&
                    event.pkg == trigger.pkg

            is Trigger.Geofence ->
                event is TriggerEvent.GeofenceTriggered &&
                    event.transition == trigger.transition

            is Trigger.Manual -> event is TriggerEvent.ManualFired

            is Trigger.BluetoothDevice ->
                event is TriggerEvent.BluetoothDeviceChanged &&
                    event.state == trigger.state &&
                    (trigger.deviceName == null || event.deviceName?.equals(trigger.deviceName, ignoreCase = true) == true) &&
                    (trigger.deviceAddress == null || event.deviceAddress?.equals(trigger.deviceAddress, ignoreCase = true) == true)

            is Trigger.WifiConnected ->
                event is TriggerEvent.WifiConnectedChanged &&
                    (trigger.ssid == null || event.ssid?.contains(trigger.ssid, ignoreCase = true) == true)

            is Trigger.CalendarEvent ->
                event is TriggerEvent.CalendarEventChanged &&
                    event.direction == trigger.direction &&
                    (trigger.calendarId == null || event.calendarId == trigger.calendarId) &&
                    (trigger.titleMatch == null || event.title?.contains(trigger.titleMatch, ignoreCase = true) == true) &&
                    (trigger.events.isEmpty() || trigger.events.any { it.eventId == event.calendarEventId })

            is Trigger.ChargerConnected ->
                event is TriggerEvent.ChargerConnectedChanged &&
                    event.connected == trigger.connected &&
                    (trigger.source == null || event.source == trigger.source)

            is Trigger.DeviceUnlocked -> event is TriggerEvent.DeviceUnlockedEvent
            is Trigger.DeviceLocked -> event is TriggerEvent.DeviceLockedEvent

            is Trigger.TorchState ->
                event is TriggerEvent.TorchStateChanged &&
                    event.on == trigger.on

            is Trigger.MediaPlayback ->
                event is TriggerEvent.MediaPlaybackChanged &&
                    event.playing == trigger.playing &&
                    (trigger.packageName == null || event.packageName == trigger.packageName)

            is Trigger.DeviceState ->
                event is TriggerEvent.DeviceStateChanged &&
                    event.key == trigger.key &&
                    (trigger.value == DeviceStateKeys.ANY_VALUE || event.value == trigger.value) &&
                    event.previous != event.value
        }

    /**
     * True when [event] is the inverse edge of a state-lifecycle trigger —
     * the moment a "while X" mode should end. A charger-on mode ends on
     * unplug; a flashlight-on mode ends on torch-off; a device-state mode
     * ends when the key lands on a different value.
     */
    fun isInverseEdge(
        trigger: Trigger,
        event: TriggerEvent,
    ): Boolean =
        when (trigger) {
            is Trigger.ChargerConnected ->
                event is TriggerEvent.ChargerConnectedChanged &&
                    event.connected != trigger.connected &&
                    (trigger.source == null || event.source == trigger.source || !event.connected)

            is Trigger.TorchState ->
                event is TriggerEvent.TorchStateChanged && event.on != trigger.on

            is Trigger.ScreenStateTrigger ->
                event is TriggerEvent.ScreenStateChanged && event.state != trigger.state

            is Trigger.MediaPlayback ->
                event is TriggerEvent.MediaPlaybackChanged &&
                    event.playing != trigger.playing &&
                    (trigger.packageName == null || event.packageName == trigger.packageName)

            is Trigger.BluetoothDevice ->
                event is TriggerEvent.BluetoothDeviceChanged &&
                    event.state != trigger.state &&
                    (trigger.deviceName == null || event.deviceName?.equals(trigger.deviceName, ignoreCase = true) == true) &&
                    (trigger.deviceAddress == null || event.deviceAddress?.equals(trigger.deviceAddress, ignoreCase = true) == true)

            is Trigger.WifiConnected ->
                // A null SSID means the Wi-Fi link dropped entirely; a
                // different SSID means the phone hopped networks — both end
                // an SSID-filtered "while on this network" mode.
                event is TriggerEvent.WifiConnectedChanged &&
                    (
                        event.ssid == null ||
                            (
                                trigger.ssid != null &&
                                    !event.ssid.contains(trigger.ssid, ignoreCase = true)
                            )
                    )

            is Trigger.Connectivity ->
                if (trigger.medium == ConnMedium.POWER) {
                    // Legacy/imported POWER triggers ride the charger event.
                    event is TriggerEvent.ChargerConnectedChanged &&
                        event.connected != (trigger.state == ConnState.CONNECTED)
                } else {
                    event is TriggerEvent.ConnectivityChanged &&
                        event.medium == trigger.medium &&
                        (
                            event.state != trigger.state ||
                                // A CONNECTED event for a different named network
                                // ends a match-filtered "while connected" mode.
                                (
                                    trigger.match != null &&
                                        event.state == ConnState.CONNECTED &&
                                        event.match != null &&
                                        !event.match.contains(trigger.match, ignoreCase = true)
                                )
                        )
                }

            is Trigger.DeviceUnlocked -> event is TriggerEvent.DeviceLockedEvent
            is Trigger.DeviceLocked -> event is TriggerEvent.DeviceUnlockedEvent

            is Trigger.CalendarEvent ->
                event is TriggerEvent.CalendarEventChanged &&
                    event.direction != trigger.direction &&
                    (trigger.calendarId == null || event.calendarId == trigger.calendarId) &&
                    (trigger.titleMatch == null || event.title?.contains(trigger.titleMatch, ignoreCase = true) == true) &&
                    (trigger.events.isEmpty() || trigger.events.any { it.eventId == event.calendarEventId })

            is Trigger.DeviceState ->
                event is TriggerEvent.DeviceStateChanged &&
                    event.key == trigger.key &&
                    event.value != trigger.value &&
                    trigger.value != DeviceStateKeys.ANY_VALUE

            else -> false
        }

    /** Checks if a time trigger should fire on the given day. */
    fun shouldFireOnDay(
        trigger: Trigger.Time,
        dayOfWeek: JavaDayOfWeek,
    ): Boolean {
        val days = trigger.days ?: return true
        val mapped = dayOfWeek.toEngineDayOfWeek() ?: return true
        return mapped in days
    }

    /** Checks if a time window trigger is active at the given time. */
    fun isWindowActive(
        trigger: Trigger.TimeWindow,
        now: LocalDateTime,
        zone: ZoneId,
    ): Boolean {
        val start = LocalTime.parse(trigger.startLocal, DateTimeFormatter.ofPattern("HH:mm"))
        val end = LocalTime.parse(trigger.endLocal, DateTimeFormatter.ofPattern("HH:mm"))
        val current = now.toLocalTime()

        val sameDay = start <= end
        val inWindow =
            if (sameDay) {
                current >= start && current < end
            } else {
                // Window crosses midnight (e.g., 22:30 → 07:00)
                current >= start || current < end
            }
        if (!inWindow) return false

        // Day filter applies to the day the window STARTED. In the post-midnight
        // tail of an overnight window that's yesterday, not today — otherwise a
        // "Fri 22:00-07:00" window dies at midnight.
        val effectiveDate =
            if (!sameDay && current < end) {
                now.toLocalDate().minusDays(1)
            } else {
                now.toLocalDate()
            }
        return trigger.days?.let { days ->
            val mapped = effectiveDate.dayOfWeek.toEngineDayOfWeek() ?: return@let true
            mapped in days
        } ?: true
    }

    private fun matchesDirection(
        direction: com.tdvorak.nothingmodes.engine.model.BatteryDirection,
        event: TriggerEvent.BatteryLevelChanged,
    ): Boolean =
        when (direction) {
            com.tdvorak.nothingmodes.engine.model.BatteryDirection.CHARGING_STARTED -> event.isCharging
            com.tdvorak.nothingmodes.engine.model.BatteryDirection.CHARGING_STOPPED -> !event.isCharging
        }

    /**
     * Number filters may be stored as E.164 (+420…) or free-typed national
     * format (777 123 456). Compare digits-only and accept a suffix match so
     * "+420777123456" matches "777123456".
     */
    private fun numbersMatch(
        eventNumber: String?,
        triggerNumber: String,
    ): Boolean {
        if (eventNumber == null) return false
        val eventDigits = eventNumber.filter { it.isDigit() }
        val triggerDigits = triggerNumber.filter { it.isDigit() }
        if (eventDigits.isEmpty() || triggerDigits.isEmpty()) return eventNumber == triggerNumber
        if (eventDigits == triggerDigits) return true
        // Suffix match only when the shorter side is a plausible national
        // number — a 3-digit filter must not match any number ending in it.
        val (short, long) = if (eventDigits.length <= triggerDigits.length) eventDigits to triggerDigits else triggerDigits to eventDigits
        return short.length >= 7 && long.endsWith(short)
    }

    private fun JavaDayOfWeek.toEngineDayOfWeek(): DayOfWeek? =
        when (this) {
            JavaDayOfWeek.MONDAY -> DayOfWeek.MONDAY
            JavaDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            JavaDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            JavaDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            JavaDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            JavaDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            JavaDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
}
