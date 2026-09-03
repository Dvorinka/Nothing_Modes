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
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
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
            AutomationAlarmReceiver.ACTION_TIME_FIRED -> handleTimeFired(intent)
            AutomationAlarmReceiver.ACTION_WINDOW_START -> handleWindowStart(intent)
            AutomationAlarmReceiver.ACTION_WINDOW_END -> handleWindowEnd(intent)
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
        private const val CHANNEL_ID = "automation_engine"
        private const val NOTIFICATION_ID = 1001
    }
}
