package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.tdvorak.nothingmodes.automation.R
import com.tdvorak.nothingmodes.automation.notification.ModeNotificationHelper
import com.tdvorak.nothingmodes.automation.scheduler.AutomationAlarmReceiver
import com.tdvorak.nothingmodes.automation.scheduler.AutomationScheduler
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.ChargerSource
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.isOneShot
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.NotifyRule
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val activeJobs = mutableSetOf<Job>()
    private val progressHandler = Handler(Looper.getMainLooper())
    private val pendingProgress = mutableSetOf<Runnable>()

    // Serializes all Engine access — concurrent onTrigger calls race on
    // lifecycle snapshots and can double-restore or lose activations.
    private val engineMutex = Mutex()

    // Last observed battery level — lets the matcher detect threshold
    // crossings when a broadcast is missed (doze, process restart).
    @Volatile private var lastBatteryLevel: Int? = null

    @Volatile private var lastStartId = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        lastStartId = startId

        when (intent?.action) {
            ACTION_RESCHEDULE -> handleReschedule()
            ACTION_BOOT -> handleBoot()
            ACTION_REGISTERED -> handleRegistered(intent)
            AutomationAlarmReceiver.ACTION_TIME_FIRED -> handleTimeFired(intent)
            AutomationAlarmReceiver.ACTION_WINDOW_START -> handleWindowStart(intent)
            AutomationAlarmReceiver.ACTION_WINDOW_END -> handleWindowEnd(intent)
            AutomationAlarmReceiver.ACTION_NOTIFY_BEFORE -> handleNotifyBefore(intent)
            ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
            ACTION_CHARGER -> handleCharger(intent)
            ACTION_UNLOCKED -> handleUnlocked()
            ACTION_SCREEN_STATE -> handleScreenState(intent)
            ACTION_NOTIFICATION -> handleNotification(intent)
            ACTION_PHONE_STATE -> handlePhoneState(intent)
            ACTION_SMS -> handleSms(intent)
            ACTION_CONNECTIVITY -> handleConnectivity(intent)
            ACTION_APP_FOREGROUND -> handleAppForeground(intent)
            ACTION_GEOFENCE -> handleGeofence(intent)
            ACTION_BT_DEVICE -> handleBtDevice(intent)
            ACTION_WIFI_CONNECTED -> handleWifiConnected(intent)
            ACTION_TORCH_STATE -> handleTorchState(intent)
            ACTION_MEDIA_PLAYBACK -> handleMediaPlayback(intent)
            ACTION_MANUAL -> handleManual(intent)
            ACTION_CALENDAR_EVENT -> handleCalendarEvent(intent)
            ACTION_DEVICE_STATE -> handleDeviceState(intent)
            ACTION_AUTOMATION_REMOVED -> handleAutomationRemoved(intent)
        }

        // Stop only when all in-flight jobs are done
        maybeStop()
        return START_NOT_STICKY
    }

    private fun maybeStop() {
        if (activeJobs.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(lastStartId)
        }
    }

    private fun handleRegistered(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        dispatchEvent(
            TriggerEvent.Registered(
                eventId = "registered:${System.currentTimeMillis()}",
                automationId = AutomationId(automationId),
            ),
        )
    }

    private fun handleReschedule() {
        trackJob(
            scope.launch {
                store.armed().forEach { automation ->
                    rescheduleOne(automation)
                }
            },
        )
    }

    private fun handleBoot() {
        trackJob(
            scope.launch {
                store.armed().forEach { automation ->
                    rescheduleOne(automation)
                }
                dispatchEvent(
                    TriggerEvent.BootCompleted(
                        eventId = "boot:${System.currentTimeMillis()}",
                    ),
                )
            },
        )
    }

    // One broken automation (e.g. an invalid geofence) must not abort the
    // whole reschedule — isolate each schedule call.
    private fun rescheduleOne(automation: Automation) {
        runCatching { scheduler.schedule(automation) }
            .onFailure { android.util.Log.w("AutomationService", "Reschedule failed for ${automation.id.value}", it) }
    }

    private fun handleTimeFired(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        val id = AutomationId(automationId)
        dispatchEvent(
            TriggerEvent.TimeFired(
                eventId = "time:${System.currentTimeMillis()}",
                automationId = id,
                atMillis = System.currentTimeMillis(),
            ),
        )
        // Re-schedule next cron occurrence for recurring time triggers only
        trackJob(
            scope.launch {
                store.get(id)?.let { automation ->
                    val trigger = automation.trigger
                    if (trigger is Trigger.Time && !trigger.isOneShot()) {
                        scheduler.schedule(automation)
                    }
                }
            },
        )
    }

    private fun handleWindowStart(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        dispatchEvent(
            TriggerEvent.ModeWindowStart(
                eventId = "window_start:${System.currentTimeMillis()}",
                automationId = AutomationId(automationId),
                atMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun handleNotifyBefore(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        val minutes = intent.getIntExtra(AutomationAlarmReceiver.EXTRA_LEAD_MINUTES, 0)
        trackJob(
            scope.launch {
                val automation = store.get(AutomationId(automationId)) ?: return@launch
                // Alarms outlive edits — a BEFORE rule removed since scheduling
                // would still post a stale notification. Re-check the rule.
                val stillWanted =
                    automation.enabled &&
                        automation.notifyRules.any { it is NotifyRule.Before && it.minutes == minutes }
                if (stillWanted) {
                    ModeNotificationHelper(this@AutomationService).postBefore(automation, minutes)
                }
            },
        )
    }

    private fun handleWindowEnd(intent: Intent) {
        val automationId = intent.getStringExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID) ?: return
        val id = AutomationId(automationId)
        dispatchEvent(
            TriggerEvent.ModeWindowEnd(
                eventId = "window_end:${System.currentTimeMillis()}",
                automationId = id,
                atMillis = System.currentTimeMillis(),
            ),
        )
        // Re-schedule next window for recurring time-window triggers
        trackJob(
            scope.launch {
                store.get(id)?.let { scheduler.schedule(it) }
            },
        )
    }

    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
        if (level < 0) return
        val isCharging = intent.getBooleanExtra(EXTRA_BATTERY_CHARGING, false)
        val previousLevel = lastBatteryLevel
        lastBatteryLevel = level
        dispatchEvent(
            TriggerEvent.BatteryLevelChanged(
                eventId = "battery:${System.currentTimeMillis()}",
                level = level,
                isCharging = isCharging,
                previousLevel = previousLevel,
            ),
        )
    }

    private fun handleCharger(intent: Intent) {
        val connected = intent.getBooleanExtra(PersistentMonitorService.EXTRA_CHARGER_CONNECTED, false)
        val sourceName = intent.getStringExtra(PersistentMonitorService.EXTRA_CHARGER_SOURCE)
        val source =
            when (sourceName) {
                "ac" -> ChargerSource.AC
                "usb" -> ChargerSource.USB
                "wireless" -> ChargerSource.WIRELESS
                "dock" -> ChargerSource.DOCK
                else -> null
            }
        dispatchEvent(
            TriggerEvent.ChargerConnectedChanged(
                eventId = "charger:${System.currentTimeMillis()}",
                connected = connected,
                source = if (connected) source else null,
            ),
        )
    }

    private fun handleUnlocked() {
        dispatchEvent(TriggerEvent.DeviceUnlockedEvent(eventId = "unlock:${System.currentTimeMillis()}"))
    }

    private fun handleScreenState(intent: Intent) {
        val stateName = intent.getStringExtra(EXTRA_SCREEN_STATE) ?: return
        val state = runCatching { ScreenState.valueOf(stateName) }.getOrNull() ?: return
        dispatchEvent(
            TriggerEvent.ScreenStateChanged(
                eventId = "screen:${System.currentTimeMillis()}",
                state = state,
            ),
        )
        if (state == ScreenState.OFF) pollDeviceLocked()
    }

    /**
     * The keyguard engages a beat after the screen goes dark — poll briefly
     * and fire DeviceLockedEvent only when a lock actually engages.
     */
    private fun pollDeviceLocked() {
        val km = getSystemService(KeyguardManager::class.java) ?: return
        scope.launch {
            // ~10 s window — some devices engage the keyguard a few seconds
            // after the screen goes dark (power-off animations, double-tap).
            repeat(20) {
                if (km.isDeviceLocked) {
                    dispatchEvent(
                        TriggerEvent.DeviceLockedEvent(
                            eventId = "lock:${System.currentTimeMillis()}",
                        ),
                    )
                    return@launch
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private fun handleNotification(intent: Intent) {
        val pkg = intent.getStringExtra(AutomationNotificationListener.EXTRA_PACKAGE) ?: return
        val title = intent.getStringExtra(AutomationNotificationListener.EXTRA_TITLE) ?: ""
        val text = intent.getStringExtra(AutomationNotificationListener.EXTRA_TEXT) ?: ""
        val sender = intent.getStringExtra(AutomationNotificationListener.EXTRA_SENDER)
        dispatchEvent(
            TriggerEvent.NotificationPosted(
                eventId = "notif:${System.currentTimeMillis()}",
                pkg = pkg,
                title = title,
                text = text,
                sender = sender,
                isGroup =
                    if (intent.hasExtra(AutomationNotificationListener.EXTRA_IS_GROUP)) {
                        intent.getBooleanExtra(AutomationNotificationListener.EXTRA_IS_GROUP, false)
                    } else {
                        null
                    },
                conversationId =
                    intent.getStringExtra(AutomationNotificationListener.EXTRA_CONVERSATION_ID),
            ),
        )
    }

    private fun handlePhoneState(intent: Intent) {
        val stateStr = intent.getStringExtra(PhoneStateReceiver.EXTRA_PHONE_STATE) ?: return
        val number = intent.getStringExtra(PhoneStateReceiver.EXTRA_PHONE_NUMBER) ?: ""
        val phoneEvent =
            when (stateStr) {
                "ringing" -> PhoneEvent.INCOMING_CALL
                "idle" -> PhoneEvent.CALL_ENDED
                else -> return
            }
        dispatchEvent(
            TriggerEvent.PhoneStateChanged(
                eventId = "phone:${System.currentTimeMillis()}",
                event = phoneEvent,
                number = number,
                smsText = null,
            ),
        )
    }

    private fun handleSms(intent: Intent) {
        val sender = intent.getStringExtra(PhoneStateReceiver.EXTRA_SMS_SENDER)
        val body = intent.getStringExtra(PhoneStateReceiver.EXTRA_SMS_BODY)
        dispatchEvent(
            TriggerEvent.PhoneStateChanged(
                eventId = "sms:${System.currentTimeMillis()}",
                event = PhoneEvent.SMS_RECEIVED,
                number = sender,
                smsText = body,
            ),
        )
    }

    private fun handleConnectivity(intent: Intent) {
        val type = intent.getStringExtra(ConnectivityReceiver.EXTRA_CONNECTIVITY_TYPE) ?: return
        val stateStr = intent.getStringExtra(ConnectivityReceiver.EXTRA_CONNECTIVITY_STATE) ?: return
        val medium =
            when (type) {
                "wifi" -> ConnMedium.WIFI
                "bluetooth" -> ConnMedium.BT
                "airplane" -> ConnMedium.AIRPLANE
                else -> return
            }
        val state =
            when (stateStr) {
                "wifi_enabled", "bt_enabled", "airplane_enabled" -> ConnState.CONNECTED
                "wifi_disabled", "bt_disabled", "airplane_disabled" -> ConnState.DISCONNECTED
                else -> return
            }
        dispatchEvent(
            TriggerEvent.ConnectivityChanged(
                eventId = "conn:${System.currentTimeMillis()}",
                medium = medium,
                state = state,
                match = intent.getStringExtra(ConnectivityReceiver.EXTRA_CONNECTIVITY_MATCH),
            ),
        )
    }

    private fun handleAppForeground(intent: Intent) {
        val pkg = intent.getStringExtra(UsageStatsMonitor.EXTRA_PACKAGE) ?: return
        dispatchEvent(
            TriggerEvent.AppForegroundChanged(
                eventId = "app:${System.currentTimeMillis()}",
                pkg = pkg,
                inForeground = true,
            ),
        )
    }

    private fun handleGeofence(intent: Intent) {
        val lat = intent.getDoubleExtra(GeofenceReceiver.EXTRA_LAT, 0.0)
        val lng = intent.getDoubleExtra(GeofenceReceiver.EXTRA_LNG, 0.0)
        val transitionStr = intent.getStringExtra(GeofenceReceiver.EXTRA_TRANSITION) ?: return
        val transition =
            runCatching {
                com.tdvorak.nothingmodes.engine.model.Transition
                    .valueOf(transitionStr)
            }.getOrNull() ?: return
        dispatchEvent(
            TriggerEvent.GeofenceTriggered(
                eventId = "geo:${System.currentTimeMillis()}",
                lat = lat,
                lng = lng,
                transition = transition,
                geofenceId = intent.getStringExtra(GeofenceReceiver.EXTRA_GEOFENCE_ID),
            ),
        )
    }

    private fun handleBtDevice(intent: Intent) {
        val stateStr = intent.getStringExtra(ConnectivityReceiver.EXTRA_BT_DEVICE_STATE) ?: return
        val state =
            when (stateStr) {
                "connected" -> ConnState.CONNECTED
                "disconnected" -> ConnState.DISCONNECTED
                else -> return
            }
        dispatchEvent(
            TriggerEvent.BluetoothDeviceChanged(
                eventId = "bt_dev:${System.currentTimeMillis()}",
                state = state,
                deviceName = intent.getStringExtra(ConnectivityReceiver.EXTRA_BT_DEVICE_NAME),
                deviceAddress = intent.getStringExtra(ConnectivityReceiver.EXTRA_BT_DEVICE_ADDRESS),
            ),
        )
    }

    private fun handleWifiConnected(intent: Intent) {
        // Absent extra = the Wi-Fi link dropped (or radio off) — that null is
        // the inverse edge "while on Wi-Fi" modes wait for.
        val ssid = intent.getStringExtra(ConnectivityReceiver.EXTRA_WIFI_SSID)
        dispatchEvent(
            TriggerEvent.WifiConnectedChanged(
                eventId = "wifi_conn:${System.currentTimeMillis()}",
                ssid = ssid,
            ),
        )
        // The validated association is the only place the SSID is reliably
        // known — radio-on arrives too early. Connectivity triggers with a
        // network-name filter match here.
        if (ssid != null) {
            dispatchEvent(
                TriggerEvent.ConnectivityChanged(
                    eventId = "conn:${System.currentTimeMillis()}",
                    medium = ConnMedium.WIFI,
                    state = ConnState.CONNECTED,
                    match = ssid,
                ),
            )
        }
    }

    private fun handleAutomationRemoved(intent: Intent) {
        val json = intent.getStringExtra(EXTRA_AUTOMATION_JSON) ?: return
        val automation =
            runCatching {
                EngineJson.json.decodeFromString(Automation.serializer(), json)
            }.getOrNull() ?: return
        trackJob(scope.launch { engineMutex.withLock { engine.endAutomation(automation) } })
    }

    private fun handleTorchState(intent: Intent) {
        val on = intent.getBooleanExtra(PersistentMonitorService.EXTRA_TORCH_STATE, false)
        dispatchEvent(
            TriggerEvent.TorchStateChanged(
                eventId = "torch:${System.currentTimeMillis()}",
                on = on,
            ),
        )
    }

    private fun handleMediaPlayback(intent: Intent) {
        val playing = intent.getBooleanExtra(EXTRA_MEDIA_PLAYING, false)
        dispatchEvent(
            TriggerEvent.MediaPlaybackChanged(
                eventId = "media:${System.currentTimeMillis()}",
                playing = playing,
                packageName = intent.getStringExtra(EXTRA_MEDIA_PACKAGE),
                artist = intent.getStringExtra(EXTRA_MEDIA_ARTIST),
                title = intent.getStringExtra(EXTRA_MEDIA_TITLE),
            ),
        )
    }

    private fun handleManual(intent: Intent) {
        val idStr = intent.getStringExtra(EXTRA_MANUAL_ID) ?: return
        val automationId = AutomationId(idStr)
        dispatchEvent(
            TriggerEvent.ManualFired(
                eventId = "manual:${System.currentTimeMillis()}",
                automationId = automationId,
            ),
        )
    }

    private fun handleDeviceState(intent: Intent) {
        val key = intent.getStringExtra(DeviceStateMonitor.EXTRA_STATE_KEY) ?: return
        val value = intent.getStringExtra(DeviceStateMonitor.EXTRA_STATE_VALUE) ?: return
        dispatchEvent(
            TriggerEvent.DeviceStateChanged(
                eventId = "dstate:$key:${System.currentTimeMillis()}",
                key = key,
                value = value,
                previous = intent.getStringExtra(DeviceStateMonitor.EXTRA_STATE_PREVIOUS),
            ),
        )
    }

    private fun handleCalendarEvent(intent: Intent) {
        val directionStr = intent.getStringExtra(PersistentMonitorService.EXTRA_CAL_DIRECTION) ?: return
        val direction =
            runCatching {
                com.tdvorak.nothingmodes.engine.model.CalendarDirection
                    .valueOf(directionStr)
            }.getOrNull() ?: return
        dispatchEvent(
            TriggerEvent.CalendarEventChanged(
                eventId = "cal:${System.currentTimeMillis()}",
                direction = direction,
                title = intent.getStringExtra(PersistentMonitorService.EXTRA_CAL_TITLE),
                calendarId = intent.getStringExtra(PersistentMonitorService.EXTRA_CAL_ID),
                calendarEventId =
                    intent
                        .getLongExtra(PersistentMonitorService.EXTRA_CAL_EVENT_ID, -1L)
                        .takeIf { it >= 0 },
            ),
        )
    }

    private fun dispatchEvent(event: TriggerEvent) {
        val job =
            scope.launch {
                val progress = scheduleProgressNotification()
                try {
                    val envelope =
                        TriggerEnvelope(
                            id = event.eventId,
                            event = event,
                            receivedAtMillis = System.currentTimeMillis(),
                        )
                    val outcomes = engineMutex.withLock { engine.onTrigger(envelope) }
                    val isEnd = event is TriggerEvent.ModeWindowEnd
                    val helper = ModeNotificationHelper(this@AutomationService)
                    outcomes.forEach { outcome ->
                        if (isEnd || outcome.isDeactivation) {
                            helper.postOnEnd(outcome.automation)
                        } else {
                            helper.postOnTrigger(outcome.automation, outcome.results)
                        }
                    }
                } finally {
                    cancelProgressNotification(progress)
                }
            }
        trackJob(job)
    }

    /** Registers [job] so the service stays alive until all in-flight work finishes. */
    private fun trackJob(job: Job) {
        synchronized(activeJobs) {
            activeJobs.add(job)
        }
        job.invokeOnCompletion {
            synchronized(activeJobs) {
                activeJobs.remove(job)
            }
            maybeStop()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Automation Engine",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "Processes automation triggers in the background"
                }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Nothing Modes")
            .setContentText("Processing automation...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun buildProgressNotification(): Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Nothing Modes")
            .setContentText("Running a mode...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .build()

    private fun scheduleProgressNotification(): Runnable {
        val runnable =
            Runnable {
                val notification = buildProgressNotification()
                getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
            }
        synchronized(pendingProgress) {
            pendingProgress.add(runnable)
        }
        progressHandler.postDelayed(runnable, 300)
        return runnable
    }

    private fun cancelProgressNotification(runnable: Runnable) {
        progressHandler.removeCallbacks(runnable)
        synchronized(pendingProgress) {
            pendingProgress.remove(runnable)
        }
    }

    companion object {
        const val ACTION_RESCHEDULE = "com.tdvorak.nothingmodes.RESCHEDULE"
        const val ACTION_REGISTERED = "com.tdvorak.nothingmodes.REGISTERED"
        const val ACTION_BOOT = "com.tdvorak.nothingmodes.BOOT"
        const val ACTION_BATTERY_CHANGED = "com.tdvorak.nothingmodes.BATTERY_CHANGED"
        const val ACTION_CHARGER = "com.tdvorak.nothingmodes.CHARGER"
        const val ACTION_UNLOCKED = "com.tdvorak.nothingmodes.UNLOCKED"
        const val ACTION_SCREEN_STATE = "com.tdvorak.nothingmodes.SCREEN_STATE"
        const val ACTION_NOTIFICATION = "com.tdvorak.nothingmodes.NOTIFICATION"
        const val ACTION_PHONE_STATE = "com.tdvorak.nothingmodes.PHONE_STATE"
        const val ACTION_SMS = "com.tdvorak.nothingmodes.SMS"
        const val ACTION_CONNECTIVITY = "com.tdvorak.nothingmodes.CONNECTIVITY"
        const val ACTION_APP_FOREGROUND = "com.tdvorak.nothingmodes.APP_FOREGROUND"
        const val ACTION_GEOFENCE = "com.tdvorak.nothingmodes.GEOFENCE"
        const val ACTION_BT_DEVICE = "com.tdvorak.nothingmodes.BT_DEVICE"
        const val ACTION_WIFI_CONNECTED = "com.tdvorak.nothingmodes.WIFI_CONNECTED"
        const val ACTION_TORCH_STATE = "com.tdvorak.nothingmodes.TORCH_STATE"
        const val ACTION_MANUAL = "com.tdvorak.nothingmodes.MANUAL"
        const val ACTION_CALENDAR_EVENT = "com.tdvorak.nothingmodes.CALENDAR_EVENT"
        const val ACTION_MEDIA_PLAYBACK = "com.tdvorak.nothingmodes.MEDIA_PLAYBACK"
        const val ACTION_DEVICE_STATE = "com.tdvorak.nothingmodes.DEVICE_STATE"
        const val ACTION_AUTOMATION_REMOVED = "com.tdvorak.nothingmodes.AUTOMATION_REMOVED"
        const val EXTRA_AUTOMATION_JSON = "automation_json"
        const val EXTRA_MEDIA_PLAYING = "media_playing"
        const val EXTRA_MEDIA_PACKAGE = "media_pkg"
        const val EXTRA_MEDIA_ARTIST = "media_artist"
        const val EXTRA_MEDIA_TITLE = "media_title"
        const val EXTRA_MANUAL_ID = "manual_automation_id"
        const val EXTRA_BATTERY_LEVEL = "battery_level"
        const val EXTRA_BATTERY_CHARGING = "battery_charging"
        const val EXTRA_SCREEN_STATE = "screen_state"
        private const val CHANNEL_ID = "automation_engine"
        private const val NOTIFICATION_ID = 1001
    }
}
