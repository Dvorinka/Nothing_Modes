package com.tdvorak.nothingmodes.automation.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService

/**
 * Receives AlarmManager broadcasts and forwards them to AutomationService.
 */
class AutomationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val automationId = intent.getStringExtra(EXTRA_AUTOMATION_ID) ?: return
        val action = intent.action ?: return

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            this.action = action
            putExtra(EXTRA_AUTOMATION_ID, automationId)
        }
        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val ACTION_TIME_FIRED = "com.tdvorak.nothingmodes.TIME_FIRED"
        const val ACTION_WINDOW_START = "com.tdvorak.nothingmodes.WINDOW_START"
        const val ACTION_WINDOW_END = "com.tdvorak.nothingmodes.WINDOW_END"
        const val EXTRA_AUTOMATION_ID = "automation_id"
    }
}
