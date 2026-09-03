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

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_BOOT
        }
        context.startForegroundService(serviceIntent)
    }
}
