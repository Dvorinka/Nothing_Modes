package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tdvorak.nothingmodes.automation.scheduler.AutomationAlarmReceiver
import com.tdvorak.nothingmodes.automation.scheduler.AutomationScheduler
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that processes trigger events.
 * Receives alarm broadcasts, loads the automation, runs the engine.
 */
@AndroidEntryPoint
class AutomationService : Service() {

    @Inject lateinit var engine: Engine
    @Inject lateinit var scheduler: AutomationScheduler
    @Inject lateinit var store: com.tdvorak.nothingmodes.engine.runtime.AutomationStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        when (intent?.action) {
            ACTION_RESCHEDULE -> handleReschedule()
            ACTION_BOOT -> handleBoot()
            AutomationAlarmReceiver.ACTION_TIME_FIRED -> handleTimeFired(intent)
            AutomationAlarmReceiver.ACTION_WINDOW_START -> handleWindowStart(intent)
            AutomationAlarmReceiver.ACTION_WINDOW_END -> handleWindowEnd(intent)
            ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
            ACTION_SCREEN_STATE -> handleScreenState(intent)
            ACTION_NOTIFICATION -> handleNotification(intent)
            ACTION_PHONE_STATE -> handlePhoneState(intent)
            ACTION_SMS -> handleSms(intent)
            ACTION_CONNECTIVITY -> handleConnectivity(intent)
            ACTION_APP_FOREGROUND -> handleAppForeground(intent)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }

    private fun handleReschedule() {
        scope.launch {
            store.armed().forEach { automation ->
                scheduler.schedule(automation)
            }
        }
    }

    private fun handleBoot() {
        scope.launch {
            store.armed().forEach { automation ->
                scheduler.schedule(automation)
            }
            dispatchEvent(TriggerEvent.BootCompleted(
                eventId = "boot:${System.currentTimeMillis()}",
            ))
        }
    }

    private fun handleTimeFired(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        dispatchEvent(TriggerEvent.TimeFired(
            eventId = "time:${System.currentTimeMillis()}",
            automationId = AutomationId(automationId),
            atMillis = System.currentTimeMillis(),
        ))
    }

    private fun handleWindowStart(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        dispatchEvent(TriggerEvent.ModeWindowStart(
            eventId = "window_start:${System.currentTimeMillis()}",
            automationId = AutomationId(automationId),
            atMillis = System.currentTimeMillis(),
        ))
    }

    private fun handleWindowEnd(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        dispatchEvent(TriggerEvent.ModeWindowEnd(
            eventId = "window_end:${System.currentTimeMillis()}",
            automationId = AutomationId(automationId),
            atMillis = System.currentTimeMillis(),
        ))
    }

    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(DeviceStateReceiver.EXTRA_BATTERY_LEVEL, -1)
        if (level < 0) return
        val isCharging = intent.getBooleanExtra(DeviceStateReceiver.EXTRA_BATTERY_CHARGING, false)
        dispatchEvent(TriggerEvent.BatteryLevelChanged(
            eventId = "battery:${System.currentTimeMillis()}",
            level = level,
            isCharging = isCharging,
        ))
    }

    private fun handleScreenState(intent: Intent) {
        val stateName = intent.getStringExtra(DeviceStateReceiver.EXTRA_SCREEN_STATE) ?: return
        val state = runCatching { ScreenState.valueOf(stateName) }.getOrNull() ?: return
        dispatchEvent(TriggerEvent.ScreenStateChanged(
            eventId = "screen:${System.currentTimeMillis()}",
            state = state,
        ))
    }

    private fun handleNotification(intent: Intent) {
        val pkg = intent.getStringExtra(AutomationNotificationListener.EXTRA_PACKAGE) ?: return
        val title = intent.getStringExtra(AutomationNotificationListener.EXTRA_TITLE) ?: ""
        val text = intent.getStringExtra(AutomationNotificationListener.EXTRA_TEXT) ?: ""
        dispatchEvent(TriggerEvent.NotificationPosted(
            eventId = "notif:${System.currentTimeMillis()}",
            pkg = pkg,
            title = title,
            text = text,
            sender = null,
        ))
    }

    private fun handlePhoneState(intent: Intent) {
        val stateStr = intent.getStringExtra(PhoneStateReceiver.EXTRA_PHONE_STATE) ?: return
        val number = intent.getStringExtra(PhoneStateReceiver.EXTRA_PHONE_NUMBER) ?: ""
        val phoneEvent = when (stateStr) {
            "ringing" -> PhoneEvent.INCOMING_CALL
            "offhook" -> PhoneEvent.INCOMING_CALL
            "idle" -> PhoneEvent.CALL_ENDED
            else -> return
        }
        dispatchEvent(TriggerEvent.PhoneStateChanged(
            eventId = "phone:${System.currentTimeMillis()}",
            event = phoneEvent,
            number = number,
            smsText = null,
        ))
    }

    private fun handleSms(intent: Intent) {
        dispatchEvent(TriggerEvent.PhoneStateChanged(
            eventId = "sms:${System.currentTimeMillis()}",
            event = PhoneEvent.SMS_RECEIVED,
            number = null,
            smsText = null,
        ))
    }

    private fun handleConnectivity(intent: Intent) {
        val type = intent.getStringExtra(ConnectivityReceiver.EXTRA_CONNECTIVITY_TYPE) ?: return
        val stateStr = intent.getStringExtra(ConnectivityReceiver.EXTRA_CONNECTIVITY_STATE) ?: return
        val medium = when (type) {
            "wifi" -> ConnMedium.WIFI
            "bluetooth" -> ConnMedium.BT
            else -> return
        }
        val state = when (stateStr) {
            "wifi_enabled", "bt_enabled" -> ConnState.CONNECTED
            "wifi_disabled", "bt_disabled" -> ConnState.DISCONNECTED
            else -> return
        }
        dispatchEvent(TriggerEvent.ConnectivityChanged(
            eventId = "conn:${System.currentTimeMillis()}",
            medium = medium,
            state = state,
            match = null,
        ))
    }

    private fun handleAppForeground(intent: Intent) {
        val pkg = intent.getStringExtra(UsageStatsMonitor.EXTRA_PACKAGE) ?: return
        dispatchEvent(TriggerEvent.AppForegroundChanged(
            eventId = "app:${System.currentTimeMillis()}",
            pkg = pkg,
            inForeground = true,
        ))
    }

    private fun dispatchEvent(event: TriggerEvent) {
        scope.launch {
            val envelope = TriggerEnvelope(
                id = event.eventId,
                event = event,
                receivedAtMillis = System.currentTimeMillis(),
            )
            engine.onTrigger(envelope)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Automation Engine",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Processes automation triggers in the background"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Nothing Modes")
        .setContentText("Processing automation...")
        .setSmallIcon(android.R.drawable.ic_menu_manage)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    companion object {
        const val ACTION_RESCHEDULE = "com.tdvorak.nothingmodes.RESCHEDULE"
        const val ACTION_BOOT = "com.tdvorak.nothingmodes.BOOT"
        const val ACTION_BATTERY_CHANGED = "com.tdvorak.nothingmodes.BATTERY_CHANGED"
        const val ACTION_SCREEN_STATE = "com.tdvorak.nothingmodes.SCREEN_STATE"
        const val ACTION_NOTIFICATION = "com.tdvorak.nothingmodes.NOTIFICATION"
        const val ACTION_PHONE_STATE = "com.tdvorak.nothingmodes.PHONE_STATE"
        const val ACTION_SMS = "com.tdvorak.nothingmodes.SMS"
        const val ACTION_CONNECTIVITY = "com.tdvorak.nothingmodes.CONNECTIVITY"
        const val ACTION_APP_FOREGROUND = "com.tdvorak.nothingmodes.APP_FOREGROUND"
        private const val CHANNEL_ID = "automation_engine"
        private const val NOTIFICATION_ID = 1001
    }
}
