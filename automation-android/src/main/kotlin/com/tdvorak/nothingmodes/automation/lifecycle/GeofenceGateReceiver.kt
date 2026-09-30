package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.automation.scheduler.AutomationAlarmReceiver

/**
 * Actions from the "end pending" geofence-gate notification:
 * END forces the mode off; SNOOZE carries the user-typed RemoteInput
 * minutes. Both forward to AutomationService — receivers can't suspend.
 */
class GeofenceGateReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val automationId =
            intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        val serviceAction =
            when (intent.action) {
                ACTION_GATE_END -> AutomationService.ACTION_GATE_FORCE_END
                ACTION_GATE_SNOOZE -> AutomationService.ACTION_GATE_SNOOZE
                else -> return
            }
        val serviceIntent =
            Intent(context, AutomationService::class.java).apply {
                action = serviceAction
                putExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID, automationId)
                RemoteInput
                    .getResultsFromIntent(intent)
                    ?.getCharSequence(KEY_SNOOZE_MINUTES)
                    ?.let { putExtra(AutomationService.EXTRA_SNOOZE_MINUTES, it.toString()) }
            }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        const val ACTION_GATE_END = "com.tdvorak.nothingmodes.GATE_END_NOW"
        const val ACTION_GATE_SNOOZE = "com.tdvorak.nothingmodes.GATE_SNOOZE"
        const val KEY_SNOOZE_MINUTES = "snooze_minutes"
    }
}
