package com.dvoranka.nothingmodes.automation.lifecycle

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.dvoranka.nothingmodes.automation.scheduler.AutomationAlarmReceiver
import com.dvoranka.nothingmodes.automation.scheduler.AutomationScheduler
import com.dvoranka.nothingmodes.engine.model.AutomationId
import com.dvoranka.nothingmodes.engine.runtime.Engine
import com.dvoranka.nothingmodes.engine.runtime.TriggerEnvelope
import com.dvoranka.nothingmodes.engine.runtime.TriggerEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that processes trigger events.
 * Receives alarm broadcasts, loads the automation, runs the engine.
 */
class AutomationService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var engine: Engine? = null
    @Volatile private var scheduler: AutomationScheduler? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduler = AutomationScheduler(this)
        // engine will be injected by Hilt in Phase 3
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        when (intent?.action) {
            AutomationService.ACTION_RESCHEDULE -> handleReschedule()
            AutomationAlarmReceiver.ACTION_TIME_FIRED -> handleTimeFired(intent)
            AutomationAlarmReceiver.ACTION_WINDOW_START -> handleWindowStart(intent)
            AutomationAlarmReceiver.ACTION_WINDOW_END -> handleWindowEnd(intent)
        }

        // Stop foreground when done — we only need to be alive during execution
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
    }

    private fun handleReschedule() {
        // TODO: load all armed automations from store and reschedule
        // Will be wired when Hilt DI provides the store
    }

    private fun handleTimeFired(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        val event = TriggerEvent.TimeFired(
            eventId = "time:${System.currentTimeMillis()}",
            automationId = AutomationId(automationId),
            atMillis = System.currentTimeMillis(),
        )
        dispatchEvent(event)
    }

    private fun handleWindowStart(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        val event = TriggerEvent.ModeWindowStart(
            eventId = "window_start:${System.currentTimeMillis()}",
            automationId = AutomationId(automationId),
            atMillis = System.currentTimeMillis(),
        )
        dispatchEvent(event)
    }

    private fun handleWindowEnd(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        val event = TriggerEvent.ModeWindowEnd(
            eventId = "window_end:${System.currentTimeMillis()}",
            automationId = AutomationId(automationId),
            atMillis = System.currentTimeMillis(),
        )
        dispatchEvent(event)
    }

    private fun dispatchEvent(event: TriggerEvent) {
        val engine = engine ?: return
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
        const val ACTION_RESCHEDULE = "com.dvoranka.nothingmodes.RESCHEDULE"
        private const val CHANNEL_ID = "automation_engine"
        private const val NOTIFICATION_ID = 1001
    }
}
