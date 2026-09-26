package com.tdvorak.nothingmodes.automation.scheduler

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService

/**
 * Re-arms alarms when SCHEDULE_EXACT_ALARM is granted after scheduling.
 *
 * Alarms armed while the grant was denied go through setAndAllowWhileIdle and
 * carry an inexact window (observed up to +1h under Doze). Granting the
 * permission later does not re-arm them — they stay inexact until the next
 * store mutation or boot. Call [onAppAlive] from process start and activity
 * resume so the transition is caught whichever way the grant arrives.
 */
object ExactAlarmSync {
    private const val PREFS = "exact_alarm_sync"
    private const val KEY_GRANTED = "granted_seen"
    private const val TAG = "ExactAlarmSync"

    fun onAppAlive(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val granted = alarmManager.canScheduleExactAlarms()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val wasGranted = prefs.getBoolean(KEY_GRANTED, false)
        if (granted == wasGranted) return

        if (!granted) {
            // Revoked — the OS already downgrades future setAlarmClock calls;
            // just record the state so a later re-grant triggers a reschedule.
            prefs.edit().putBoolean(KEY_GRANTED, false).apply()
            return
        }

        // Persist only after the service actually accepted the work — on a
        // background-forbidden process start the launch throws, and retrying
        // on the next alive-event is cheap.
        runCatching {
            val intent =
                Intent(context, AutomationService::class.java).apply {
                    action = AutomationService.ACTION_RESCHEDULE
                }
            ContextCompat.startForegroundService(context, intent)
        }.onSuccess {
            prefs.edit().putBoolean(KEY_GRANTED, true).apply()
            Log.i(TAG, "Exact alarm grant detected — rescheduling armed automations")
        }
    }
}
