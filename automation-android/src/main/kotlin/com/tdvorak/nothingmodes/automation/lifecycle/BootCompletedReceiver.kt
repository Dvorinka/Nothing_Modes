package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Reschedules all armed automations after device reboot.
 * Starts AutomationService in boot mode (reschedule + dispatch Boot trigger).
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        // Start automation service for boot trigger + reschedule
        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_BOOT
        }
        context.startForegroundService(serviceIntent)

        // Start persistent monitor service for battery/screen state tracking
        val monitorIntent = Intent(context, PersistentMonitorService::class.java)
        context.startForegroundService(monitorIntent)
    }
}
