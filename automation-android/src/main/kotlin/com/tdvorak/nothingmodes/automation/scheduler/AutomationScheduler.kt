package com.tdvorak.nothingmodes.automation.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tdvorak.nothingmodes.automation.lifecycle.GeofenceMonitor
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.CronSchedule
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Schedules time-based automations via AlarmManager and geofence-based automations via GeofencingClient.
 * Uses setAlarmClock for exact alarms (Android 12+ SCHEDULE_EXACT_ALARM).
 * Uses setAndAllowWhileIdle for Doze compatibility on inexact triggers.
 * Geofence triggers are registered with Google Play Services Location API.
 */
class AutomationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val geofenceMonitor = GeofenceMonitor(context)
    private val registeredGeofences = mutableSetOf<String>()

    /** Schedule all triggers for an automation. */
    fun schedule(automation: Automation) {
        when (val trigger = automation.trigger) {
            is Trigger.Time -> scheduleTime(automation.id, trigger)
            is Trigger.TimeWindow -> scheduleWindow(automation.id, trigger)
            is Trigger.Geofence -> scheduleGeofence(automation.id, trigger)
            else -> Unit
        }
    }

    /** Cancel all alarms and geofences for an automation. */
    fun cancel(automationId: AutomationId) {
        alarmManager.cancel(timePendingIntent(automationId, isStart = true))
        alarmManager.cancel(timePendingIntent(automationId, isStart = false))
        alarmManager.cancel(windowPendingIntent(automationId, isStart = true))
        alarmManager.cancel(windowPendingIntent(automationId, isStart = false))
        if (registeredGeofences.remove(automationId.value)) {
            geofenceMonitor.removeGeofence(automationId.value)
        }
    }

    private fun scheduleGeofence(id: AutomationId, trigger: Trigger.Geofence) {
        geofenceMonitor.addGeofence(id.value, trigger.lat, trigger.lng, trigger.radiusM.toFloat(), trigger.transition)
        registeredGeofences.add(id.value)
        Log.i(TAG, "Geofence scheduled for ${id.value} at (${trigger.lat}, ${trigger.lng}) r=${trigger.radiusM}m")
    }

    private fun scheduleTime(id: AutomationId, trigger: Trigger.Time) {
        val zone = runCatching { ZoneId.of(trigger.tz) }.getOrNull() ?: run {
            Log.e(TAG, "Invalid timezone '${trigger.tz}' for automation ${id.value}")
            return
        }

        // One-shot scheduled by an exact date/time string (ISO-8601 ZonedDateTime)
        if (trigger.at != null) {
            val at = runCatching { ZonedDateTime.parse(trigger.at) }.getOrNull() ?: run {
                Log.e(TAG, "Invalid at time '${trigger.at}' for automation ${id.value}")
                return
            }
            val triggerAtMillis = at.toInstant().toEpochMilli()
            if (triggerAtMillis <= System.currentTimeMillis()) {
                Log.w(TAG, "One-shot time for ${id.value} is in the past; not scheduling")
                return
            }
            setAlarm(id, triggerAtMillis, isTime = true)
            return
        }

        val cron = trigger.cron ?: return
        val schedule = runCatching { CronSchedule(cron, zone) }.getOrNull() ?: run {
            Log.e(TAG, "Invalid cron '$cron' for automation ${id.value}")
            return
        }
        val now = ZonedDateTime.now(zone)
        val next = schedule.nextFire(now) ?: return
        val triggerAtMillis = next.toInstant().toEpochMilli()
        setAlarm(id, triggerAtMillis, isTime = true)
    }

    private fun setAlarm(id: AutomationId, triggerAtMillis: Long, isTime: Boolean) {
        val pendingIntent = timePendingIntent(id, isStart = isTime)
        try {
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
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule exact alarm for ${id.value}: ${e.message}")
        }
    }

    private fun scheduleWindow(id: AutomationId, trigger: Trigger.TimeWindow) {
        val zone = runCatching { ZoneId.of(trigger.tz) }.getOrNull() ?: run {
            Log.e(TAG, "Invalid timezone '${trigger.tz}' for automation ${id.value}")
            return
        }
        val now = ZonedDateTime.now(zone)
        val startToday = runCatching { parseToday(trigger.startLocal, now) }.getOrNull() ?: run {
            Log.e(TAG, "Invalid start time '${trigger.startLocal}' for automation ${id.value}")
            return
        }
        val endToday = runCatching { parseToday(trigger.endLocal, now) }.getOrNull() ?: run {
            Log.e(TAG, "Invalid end time '${trigger.endLocal}' for automation ${id.value}")
            return
        }

        val nextStart = if (startToday.isAfter(now)) startToday else startToday.plusDays(1)
        val nextEnd = if (endToday.isAfter(now)) endToday else endToday.plusDays(1)

        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextStart.toInstant().toEpochMilli(), null),
                windowPendingIntent(id, isStart = true),
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextEnd.toInstant().toEpochMilli(), null),
                windowPendingIntent(id, isStart = false),
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule window alarms for ${id.value}: ${e.message}")
        }
    }

    private fun parseToday(time: String, now: ZonedDateTime): ZonedDateTime {
        val parts = time.split(":")
        require(parts.size >= 2) { "Invalid time format: $time" }
        val hour = parts[0].toInt()
        val minute = parts[1].toInt()
        require(hour in 0..23 && minute in 0..59) { "Invalid hour/minute: $hour:$minute" }
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
