package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.tdvorak.nothingmodes.engine.model.ScreenState

/**
 * Dispatches BatteryLevel and ScreenState trigger events to AutomationService.
 * Registered dynamically by AutomationService at startup (not in manifest —
 * these are sticky broadcasts that don't need manifest declaration).
 */
class DeviceStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val percent = if (scale > 0) (level * 100) / scale else -1
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                Intent(context, AutomationService::class.java).apply {
                    action = AutomationService.ACTION_BATTERY_CHANGED
                    putExtra(EXTRA_BATTERY_LEVEL, percent)
                    putExtra(EXTRA_BATTERY_CHARGING, isCharging)
                }
            }
            Intent.ACTION_SCREEN_ON -> {
                Intent(context, AutomationService::class.java).apply {
                    action = AutomationService.ACTION_SCREEN_STATE
                    putExtra(EXTRA_SCREEN_STATE, ScreenState.ON.name)
                }
            }
            Intent.ACTION_SCREEN_OFF -> {
                Intent(context, AutomationService::class.java).apply {
                    action = AutomationService.ACTION_SCREEN_STATE
                    putExtra(EXTRA_SCREEN_STATE, ScreenState.OFF.name)
                }
            }
            else -> return
        }
        context.startForegroundService(serviceIntent)
    }

    companion object {
        const val EXTRA_BATTERY_LEVEL = "battery_level"
        const val EXTRA_BATTERY_CHARGING = "battery_charging"
        const val EXTRA_SCREEN_STATE = "screen_state"
    }
}
