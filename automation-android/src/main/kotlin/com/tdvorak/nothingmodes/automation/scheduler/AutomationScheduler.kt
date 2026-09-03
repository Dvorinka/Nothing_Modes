package com.tdvorak.nothingmodes.automation.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.CronSchedule
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules time-based automations via AlarmManager.
 * Uses setAlarmClock for exact alarms (Android 12+ SCHEDULE_EXACT_ALARM).
 * Uses setAndAllowWhileIdle for Doze compatibility on inexact triggers.
 */
class AutomationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Schedule all time-based triggers for an automation. */
    fun schedule(automation: Automation) {
        when (val trigger = automation.trigger) {
            is Trigger.Time -> scheduleTime(automation.id, trigger)
            is Trigger.TimeWindow -> scheduleWindow(automation.id, trigger)
            else -> Unit
        }
    }

    /** Cancel all alarms for an automation. */
    fun cancel(automationId: AutomationId) {
        alarmManager.cancel(timePendingIntent(automationId, isStart = true))
        alarmManager.cancel(timePendingIntent(automationId, isStart = false))
    }

    private fun scheduleTime(id: AutomationId, trigger: Trigger.Time) {
        val cron = trigger.cron ?: return
        val zone = ZoneId.of(trigger.tz)
        val schedule = CronSchedule(cron, zone)
        val now = ZonedDateTime.now(zone)
        val next = schedule.nextFire(now) ?: return
        val triggerAtMillis = next.toInstant().toEpochMilli()

        val pendingIntent = timePendingIntent(id, isStart = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, null),
                pendingIntent,
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    private fun scheduleWindow(id: AutomationId, trigger: Trigger.TimeWindow) {
        val zone = ZoneId.of(trigger.tz)
        val now = ZonedDateTime.now(zone)
        val startToday = parseToday(trigger.startLocal, now)
        val endToday = parseToday(trigger.endLocal, now)

        val nextStart = if (startToday.isAfter(now)) startToday else startToday.plusDays(1)
        val nextEnd = if (endToday.isAfter(now)) endToday else endToday.plusDays(1)

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(nextStart.toInstant().toEpochMilli(), null),
            windowPendingIntent(id, isStart = true),
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(nextEnd.toInstant().toEpochMilli(), null),
            windowPendingIntent(id, isStart = false),
        )
    }

    private fun parseToday(time: String, now: ZonedDateTime): ZonedDateTime {
        val parts = time.split(":")
        val hour = parts[0].toInt()
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    }

    private fun timePendingIntent(id: AutomationId, isStart: Boolean): PendingIntent {
        val intent = Intent(context, AutomationAlarmReceiver::class.java).apply {
            action = AutomationAlarmReceiver.ACTION_TIME_FIRED
            putExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID, id.value)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(id.value, isStart),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun windowPendingIntent(id: AutomationId, isStart: Boolean): PendingIntent {
        val intent = Intent(context, AutomationAlarmReceiver::class.java).apply {
            action = if (isStart) AutomationAlarmReceiver.ACTION_WINDOW_START
            else AutomationAlarmReceiver.ACTION_WINDOW_END
            putExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID, id.value)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(id.value, isStart),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun requestCode(id: String, isStart: Boolean): Int =
        (id.hashCode() and 0x7FFFFFFF) or (if (isStart) 0 else 1)

    companion object {
        const val TAG = "AutomationScheduler"
    }
}
