package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tdvorak.nothingmodes.engine.model.ScreenState

/**
 * Persistent foreground service that keeps dynamic broadcast receivers
 * registered for continuous monitoring of battery and screen state.
 *
 * Unlike manifest-declared receivers, ACTION_BATTERY_CHANGED and
 * ACTION_SCREEN_ON/OFF require dynamic registration. This service
 * stays alive to maintain those registrations.
 *
 * Start via: context.startForegroundService(Intent(context, PersistentMonitorService::class.java))
 */
class PersistentMonitorService : Service() {

    private var batteryReceiver: BroadcastReceiver? = null
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceivers()
        Log.i(TAG, "Persistent monitor started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceivers()
        Log.i(TAG, "Persistent monitor stopped")
        super.onDestroy()
    }

    private fun registerReceivers() {
        // Battery state receiver (ACTION_BATTERY_CHANGED is sticky, must be dynamic)
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val level = intent.getIntExtra("level", -1)
                        val scale = intent.getIntExtra("scale", 100)
                        val percent = if (scale > 0) (level * 100) / scale else -1
                        val status = intent.getIntExtra("status", -1)
                        val isCharging = status == 2 || status == 3 // BATTERY_STATUS_CHARGING or FULL
                        val source = when (intent.getIntExtra("plugged", -1)) {
                            1 -> "ac"
                            2 -> "usb"
                            4 -> "wireless"
                            else -> "unknown"
                        }
                        val temperature = intent.getIntExtra("temperature", -1) / 10.0f

                        if (percent >= 0) {
                            val serviceIntent = Intent(context, AutomationService::class.java).apply {
                                action = AutomationService.ACTION_BATTERY_CHANGED
                                putExtra(DeviceStateReceiver.EXTRA_BATTERY_LEVEL, percent)
                                putExtra(DeviceStateReceiver.EXTRA_BATTERY_CHARGING, isCharging)
                                putExtra(EXTRA_BATTERY_SOURCE, source)
                                putExtra(EXTRA_BATTERY_TEMP, temperature)
                            }
                            context.startService(serviceIntent)
                        }
                    }
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Screen state receiver (ACTION_SCREEN_ON/OFF must be dynamic)
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val state = when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> ScreenState.ON
                    Intent.ACTION_SCREEN_OFF -> ScreenState.OFF
                    else -> return
                }
                val serviceIntent = Intent(context, AutomationService::class.java).apply {
                    action = AutomationService.ACTION_SCREEN_STATE
                    putExtra(DeviceStateReceiver.EXTRA_SCREEN_STATE, state.name)
                }
                context.startService(serviceIntent)
            }
        }
        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, screenFilter)
    }

    private fun unregisterReceivers() {
        batteryReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenReceiver?.let { runCatching { unregisterReceiver(it) } }
        batteryReceiver = null
        screenReceiver = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Persistent Monitor",
                android.app.NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Continuously monitors battery and screen state for automations"
            }
            getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Nothing Modes")
        .setContentText("Monitoring device state...")
        .setSmallIcon(android.R.drawable.ic_menu_manage)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    companion object {
        private const val TAG = "PersistentMonitor"
        private const val CHANNEL_ID = "persistent_monitor"
        private const val NOTIFICATION_ID = 1002
        const val EXTRA_BATTERY_SOURCE = "battery_source"
        const val EXTRA_BATTERY_TEMP = "battery_temp"
    }
}
