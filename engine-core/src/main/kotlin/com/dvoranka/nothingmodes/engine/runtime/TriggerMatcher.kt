package com.dvoranka.nothingmodes.engine.runtime

import com.dvoranka.nothingmodes.engine.model.Trigger
import com.dvoranka.nothingmodes.engine.model.DayOfWeek
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Matches a trigger against a fired event. */
class TriggerMatcher {

    fun matches(trigger: Trigger, event: TriggerEvent): Boolean = when (trigger) {
        is Trigger.Time -> event is TriggerEvent.TimeFired &&
            event.automationId == trigger.automationIdSafe()

        is Trigger.TimeWindow -> when (event) {
            is TriggerEvent.ModeWindowStart -> event.automationId == trigger.automationIdSafe()
            is TriggerEvent.ModeWindowEnd -> event.automationId == trigger.automationIdSafe()
            else -> false
        }

        is Trigger.Immediate -> event is TriggerEvent.Registered

        is Trigger.Notification -> event is TriggerEvent.NotificationPosted &&
            event.pkg == trigger.pkg &&
            (trigger.sender == null || event.sender?.equals(trigger.sender, ignoreCase = true) == true) &&
            (trigger.titleMatch == null || event.title?.contains(trigger.titleMatch, ignoreCase = true) == true) &&
            (trigger.textMatch == null || event.text?.contains(trigger.textMatch, ignoreCase = true) == true)

        is Trigger.PhoneState -> event is TriggerEvent.PhoneStateChanged &&
            event.event == trigger.event &&
            (trigger.number == null || event.number == trigger.number) &&
            (trigger.textMatch == null || event.smsText?.contains(trigger.textMatch, ignoreCase = true) == true)

        is Trigger.Connectivity -> event is TriggerEvent.ConnectivityChanged &&
            event.medium == trigger.medium &&
            event.state == trigger.state &&
            (trigger.match == null || event.match?.contains(trigger.match, ignoreCase = true) == true)

        is Trigger.Boot -> event is TriggerEvent.BootCompleted

        is Trigger.BatteryLevel -> event is TriggerEvent.BatteryLevelChanged &&
            event.level == trigger.level &&
            (trigger.direction == null || matchesDirection(trigger.direction, event))

        is Trigger.ScreenStateTrigger -> event is TriggerEvent.ScreenStateChanged &&
            event.state == trigger.state

        is Trigger.AppOpened -> event is TriggerEvent.AppForegroundChanged &&
            event.inForeground && event.pkg == trigger.pkg

        is Trigger.Geofence -> false // Geofence handled by Android location backend
    }

    /** Checks if a time trigger should fire on the given day. */
    fun shouldFireOnDay(trigger: Trigger.Time, dayOfWeek: JavaDayOfWeek): Boolean {
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
        val dayOk = trigger.days?.let { days ->
            val mapped = now.dayOfWeek.toEngineDayOfWeek() ?: return@let true
            mapped in days
        } ?: true
        if (!dayOk) return false

        val start = LocalTime.parse(trigger.startLocal, DateTimeFormatter.ofPattern("HH:mm"))
        val end = LocalTime.parse(trigger.endLocal, DateTimeFormatter.ofPattern("HH:mm"))
        val current = now.toLocalTime()

        return if (start <= end) {
            current >= start && current < end
        } else {
            // Window crosses midnight (e.g., 22:30 → 07:00)
            current >= start || current < end
        }
    }

    private fun matchesDirection(
        direction: com.dvoranka.nothingmodes.engine.model.BatteryDirection,
        event: TriggerEvent.BatteryLevelChanged,
    ): Boolean = when (direction) {
        com.dvoranka.nothingmodes.engine.model.BatteryDirection.CHARGING_STARTED -> event.isCharging
        com.dvoranka.nothingmodes.engine.model.BatteryDirection.CHARGING_STOPPED -> !event.isCharging
    }

    private fun Trigger.automationIdSafe(): com.dvoranka.nothingmodes.engine.model.AutomationId? = null

    private fun JavaDayOfWeek.toEngineDayOfWeek(): DayOfWeek? = when (this) {
        JavaDayOfWeek.MONDAY -> DayOfWeek.MONDAY
        JavaDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
        JavaDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
        JavaDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
        JavaDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
        JavaDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
        JavaDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
    }
}
