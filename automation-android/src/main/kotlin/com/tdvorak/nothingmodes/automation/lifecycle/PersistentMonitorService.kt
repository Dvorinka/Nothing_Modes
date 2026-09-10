package com.tdvorak.nothingmodes.automation.lifecycle

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.automation.R
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.phone.PhoneNumberFormatter
import java.util.Locale

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
    private var connectivityReceiver: BroadcastReceiver? = null
    private var phoneStateReceiver: BroadcastReceiver? = null
    private var torchCallback: android.hardware.camera2.CameraManager.TorchCallback? = null
    private var usageStatsMonitor: UsageStatsMonitor? = null
    private var mediaSessionMonitor: MediaSessionMonitor? = null
    private var calendarObserver: CalendarObserver? = null

    // Last observed BatteryManager.EXTRA_PLUGGED value; -1 = not yet seen.
    // Used to emit charger connect/disconnect transitions from the sticky
    // ACTION_BATTERY_CHANGED stream without double-firing.
    private var lastPlugged: Int = -1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerReceivers()
        registerTorchCallback()
        startCalendarObserver()
        Log.i(TAG, "Persistent monitor started")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceivers()
        unregisterTorchCallback()
        unregisterCallStateListener()
        usageStatsMonitor?.stop()
        usageStatsMonitor = null
        mediaSessionMonitor?.stop()
        mediaSessionMonitor = null
        calendarObserver?.stop()
        calendarObserver = null
        Log.i(TAG, "Persistent monitor stopped")
        super.onDestroy()
    }

    private fun registerReceivers() {
        // Battery state receiver (ACTION_BATTERY_CHANGED is sticky, must be dynamic)
        batteryReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    when (intent.action) {
                        Intent.ACTION_BATTERY_CHANGED -> {
                            val level = intent.getIntExtra("level", -1)
                            val scale = intent.getIntExtra("scale", 100)
                            val percent = if (scale > 0) (level * 100) / scale else -1
                            val status = intent.getIntExtra("status", -1)
                            val isCharging = status == 2 || status == 3 // BATTERY_STATUS_CHARGING or FULL
                            val source =
                                when (intent.getIntExtra("plugged", -1)) {
                                    1 -> "ac"
                                    2 -> "usb"
                                    4 -> "wireless"
                                    8 -> "dock"
                                    else -> "unknown"
                                }
                            val temperature = intent.getIntExtra("temperature", -1) / 10.0f
                            val plugged = intent.getIntExtra("plugged", 0)

                            dispatchChargerTransition(context, plugged, source)

                            if (percent >= 0) {
                                val serviceIntent =
                                    Intent(context, AutomationService::class.java).apply {
                                        action = AutomationService.ACTION_BATTERY_CHANGED
                                        putExtra(DeviceStateReceiver.EXTRA_BATTERY_LEVEL, percent)
                                        putExtra(DeviceStateReceiver.EXTRA_BATTERY_CHARGING, isCharging)
                                        putExtra(EXTRA_BATTERY_SOURCE, source)
                                        putExtra(EXTRA_BATTERY_TEMP, temperature)
                                    }
                                ContextCompat.startForegroundService(context, serviceIntent)
                            }
                        }
                    }
                }
            }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Screen state receiver (ACTION_SCREEN_ON/OFF must be dynamic)
        screenReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    if (intent.action == Intent.ACTION_USER_PRESENT) {
                        val unlockIntent =
                            Intent(context, AutomationService::class.java).apply {
                                action = AutomationService.ACTION_UNLOCKED
                            }
                        ContextCompat.startForegroundService(context, unlockIntent)
                        return
                    }
                    val state =
                        when (intent.action) {
                            Intent.ACTION_SCREEN_ON -> ScreenState.ON
                            Intent.ACTION_SCREEN_OFF -> ScreenState.OFF
                            else -> return
                        }
                    // Record the screen-off instant so "screen off for N min"
                    // conditions can evaluate without keeping the service awake.
                    // Prefs file name mirrors AndroidStateProvider.SCREEN_STATE_PREFS.
                    context
                        .getSharedPreferences("nothing_modes_state", Context.MODE_PRIVATE)
                        .edit()
                        .apply {
                            if (state == ScreenState.OFF) {
                                putLong("screen_off_since_ms", System.currentTimeMillis())
                            } else {
                                remove("screen_off_since_ms")
                            }
                            apply()
                        }
                    val serviceIntent =
                        Intent(context, AutomationService::class.java).apply {
                            action = AutomationService.ACTION_SCREEN_STATE
                            putExtra(DeviceStateReceiver.EXTRA_SCREEN_STATE, state.name)
                        }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
        val screenFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
        registerReceiver(screenReceiver, screenFilter)

        // Connectivity (WiFi state/network, Bluetooth adapter + ACL devices).
        // These are implicit broadcasts blocked for manifest-declared receivers
        // since API 26, so they must be registered dynamically here.
        connectivityReceiver = ConnectivityReceiver()
        val connFilter =
            IntentFilter().apply {
                addAction(android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            }
        runCatching { registerReceiver(connectivityReceiver, connFilter) }
            .onFailure { Log.e(TAG, "Failed to register connectivity receiver", it) }

        // SMS_RECEIVED is still delivered via broadcast.
        phoneStateReceiver = PhoneStateReceiver()
        val smsFilter =
            IntentFilter().apply {
                addAction("android.provider.Telephony.SMS_RECEIVED")
            }
        runCatching { registerReceiver(phoneStateReceiver, smsFilter) }
            .onFailure { Log.e(TAG, "Failed to register SMS receiver", it) }

        // Call state via TelephonyCallback/PhoneStateListener — more reliable than the
        // ACTION_PHONE_STATE broadcast on modern Android.
        registerCallStateListener()

        // App-foreground polling via UsageStats (no-ops without Usage Access).
        usageStatsMonitor = UsageStatsMonitor(this).also { it.start() }

        // Media session (playback / now-playing) monitoring.
        mediaSessionMonitor = MediaSessionMonitor(this).also { it.start() }
    }

    private fun dispatchChargerTransition(
        context: Context,
        plugged: Int,
        source: String,
    ) {
        val previous = lastPlugged
        lastPlugged = plugged
        if (previous == -1) return
        val wasConnected = previous != 0
        val isConnected = plugged != 0
        // Fire on connect/disconnect edges and on source changes while plugged.
        if (wasConnected == isConnected && previous == plugged) return
        val serviceIntent =
            Intent(context, AutomationService::class.java).apply {
                action = AutomationService.ACTION_CHARGER
                putExtra(EXTRA_CHARGER_CONNECTED, isConnected)
                putExtra(EXTRA_CHARGER_SOURCE, source)
            }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun unregisterReceivers() {
        batteryReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenReceiver?.let { runCatching { unregisterReceiver(it) } }
        connectivityReceiver?.let { runCatching { unregisterReceiver(it) } }
        phoneStateReceiver?.let { runCatching { unregisterReceiver(it) } }
        batteryReceiver = null
        screenReceiver = null
        connectivityReceiver = null
        phoneStateReceiver = null
    }

    private val phoneStateListeners = mutableMapOf<Int, PhoneStateListener>()

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun registerCallStateListener() {
        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
        val activeSubs =
            runCatching {
                subscriptionManager?.activeSubscriptionInfoList?.map { it.subscriptionId } ?: emptyList()
            }.getOrElse { emptyList() }

        val subIds = activeSubs.ifEmpty { listOf(-1) }

        for (subId in subIds) {
            val tm =
                if (subId != -1) {
                    (getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
                        ?.createForSubscriptionId(subId)
                } else {
                    getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                } ?: continue

            val region = detectRegion(tm)
            val listener =
                object : PhoneStateListener() {
                    override fun onCallStateChanged(
                        state: Int,
                        incomingNumber: String?,
                    ) {
                        when (state) {
                            TelephonyManager.CALL_STATE_RINGING ->
                                dispatchPhoneState("ringing", incomingNumber, region)
                            TelephonyManager.CALL_STATE_IDLE ->
                                dispatchPhoneState("idle", null, region)
                            TelephonyManager.CALL_STATE_OFFHOOK ->
                                dispatchPhoneState("offhook", incomingNumber, region)
                        }
                    }
                }
            phoneStateListeners[subId] = listener
            runCatching {
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            }.onFailure { Log.e(TAG, "Failed to register PhoneStateListener for sub $subId", it) }
        }
    }

    @Suppress("DEPRECATION")
    private fun unregisterCallStateListener() {
        val subscriptionManager = getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
        for ((subId, listener) in phoneStateListeners) {
            val tm =
                if (subId != -1) {
                    (getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager)
                        ?.createForSubscriptionId(subId)
                } else {
                    getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                } ?: continue
            runCatching { tm.listen(listener, PhoneStateListener.LISTEN_NONE) }
        }
        phoneStateListeners.clear()
    }

    private fun dispatchPhoneState(
        state: String,
        number: String?,
        region: String,
    ) {
        val normalized = number?.takeIf { it.isNotBlank() }?.let { PhoneNumberFormatter.formatToE164(it, region) ?: it }
        Log.d(TAG, "Call state callback: $state number=$normalized")
        val serviceIntent =
            Intent(this, AutomationService::class.java).apply {
                action = AutomationService.ACTION_PHONE_STATE
                putExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, state)
                putExtra(PhoneStateReceiver.EXTRA_PHONE_NUMBER, normalized ?: "")
            }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun detectRegion(tm: TelephonyManager): String =
        tm.simCountryIso.uppercase().ifBlank {
            tm.networkCountryIso.uppercase().ifBlank {
                Locale.getDefault().country
            }
        }

    private var lastTorchState: Boolean? = null

    private fun registerTorchCallback() {
        val cm = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val cb =
            object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(
                    cameraId: String,
                    enabled: Boolean,
                ) {
                    if (lastTorchState != null && lastTorchState == enabled) return
                    lastTorchState = enabled
                    val intent =
                        Intent(this@PersistentMonitorService, AutomationService::class.java).apply {
                            action = AutomationService.ACTION_TORCH_STATE
                            putExtra(EXTRA_TORCH_STATE, enabled)
                        }
                    ContextCompat.startForegroundService(this@PersistentMonitorService, intent)
                }
            }
        torchCallback = cb
        runCatching { cm.registerTorchCallback(ContextCompat.getMainExecutor(this), cb) }
    }

    private fun unregisterTorchCallback() {
        val cm = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        torchCallback?.let { cb ->
            runCatching { cm?.unregisterTorchCallback(cb) }
        }
        torchCallback = null
        lastTorchState = null
    }

    private fun startCalendarObserver() {
        calendarObserver =
            CalendarObserver(this) { event ->
                val intent =
                    Intent(this, AutomationService::class.java).apply {
                        action = AutomationService.ACTION_CALENDAR_EVENT
                        putExtra(EXTRA_CAL_DIRECTION, event.direction.name)
                        putExtra(EXTRA_CAL_TITLE, event.title)
                        putExtra(EXTRA_CAL_ID, event.calendarId)
                    }
                ContextCompat.startForegroundService(this, intent)
            }.also { it.start() }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                android.app
                    .NotificationChannel(
                        CHANNEL_ID,
                        "Persistent Monitor",
                        android.app.NotificationManager.IMPORTANCE_LOW,
                    ).apply {
                        description = "Continuously monitors battery and screen state for automations"
                    }
            getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification =
        NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle("Nothing Modes")
            .setContentText("Monitoring device state...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

    companion object {
        private const val TAG = "PersistentMonitor"
        private const val CHANNEL_ID = "persistent_monitor"
        private const val NOTIFICATION_ID = 1002
        const val EXTRA_BATTERY_SOURCE = "battery_source"
        const val EXTRA_BATTERY_TEMP = "battery_temp"
        const val EXTRA_CHARGER_CONNECTED = "charger_connected"
        const val EXTRA_CHARGER_SOURCE = "charger_source"
        const val EXTRA_CAL_DIRECTION = "cal_direction"
        const val EXTRA_CAL_TITLE = "cal_title"
        const val EXTRA_CAL_ID = "cal_id"
        const val EXTRA_TORCH_STATE = "torch_state"
    }
}
