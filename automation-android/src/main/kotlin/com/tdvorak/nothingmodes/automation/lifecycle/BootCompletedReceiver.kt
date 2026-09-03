package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tdvorak.nothingmodes.automation.scheduler.AutomationAlarmReceiver

/**
 * Reschedules all armed automations after device reboot.
 * Starts AutomationService in reschedule mode.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_RESCHEDULE
        }
        context.startForegroundService(serviceIntent)
    }
}
