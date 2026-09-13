package com.tdvorak.nothingmodes.automation.lifecycle

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.database.ContentObserver
import android.location.LocationManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.engine.model.DeviceStateKeys

/**
 * Watches device state and emits DeviceStateChanged events through
 * AutomationService. Combines three sources:
 *
 *  - ContentObserver on the Settings providers (power saver, data saver,
 *    airplane, glyph keys, charge limits, AOD, auto-rotate, location mode)
 *  - A dynamic BroadcastReceiver for states Android announces (DND, ringer,
 *    volumes, headset plug, dark mode, wifi/bt radios, NFC, hotspot)
 *  - A slow poll for keys with no event source at all (mobile data)
 *
 * Emits only on actual transitions — the first observed value of each key is
 * recorded as the baseline without firing.
 */
class DeviceStateMonitor(
    private val context: Context,
) {
    private val last = mutableMapOf<String, String>()
    private var observerThread: HandlerThread? = null
    private var observerHandler: Handler? = null
    private var pollHandler: Handler? = null
    private var settingsObserver: ContentObserver? = null
    private var stateReceiver: BroadcastReceiver? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null
    private var tetheringCallback: Any? = null

    fun start() {
        val thread = HandlerThread("device-state-monitor").apply { start() }
        observerThread = thread
        observerHandler = Handler(thread.looper)
        pollHandler = Handler(thread.looper)
        seedBaseline()
        registerSettingsObserver()
        registerBroadcastReceiver()
        registerThermalListener()
        registerTetheringCallback()
        schedulePoll()
    }

    fun stop() {
        settingsObserver?.let { runCatching { context.contentResolver.unregisterContentObserver(it) } }
        settingsObserver = null
        stateReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        stateReceiver = null
        unregisterThermalListener()
        unregisterTetheringCallback()
        pollHandler?.removeCallbacksAndMessages(null)
        pollHandler = null
        observerThread?.quitSafely()
        observerThread = null
        observerHandler = null
        synchronized(last) { last.clear() }
    }

    // --- baseline + emit -----------------------------------------------------

    private fun seedBaseline() {
        synchronized(last) {
            readAll().forEach { (k, v) -> last[k] = v }
        }
    }

    /** Emits a value produced outside this monitor (e.g. the battery receiver). */
    fun emitExternal(
        key: String,
        value: String,
    ) = emit(key, value)

    /** Emits an event only when the value differs from the last seen one. */
    private fun emit(
        key: String,
        value: String,
    ) {
        val previous =
            synchronized(last) {
                val prev = last[key]
                if (prev == value) return
                last[key] = value
                prev
            }
        val intent =
            Intent(context, AutomationService::class.java).apply {
                action = AutomationService.ACTION_DEVICE_STATE
                putExtra(EXTRA_STATE_KEY, key)
                putExtra(EXTRA_STATE_VALUE, value)
                putExtra(EXTRA_STATE_PREVIOUS, previous)
            }
        ContextCompat.startForegroundService(context, intent)
    }

    /** Re-reads a watched key and emits on change. Null = unreadable, skip. */
    private fun update(
        key: String,
        reader: () -> String?,
    ) {
        runCatching { reader() }.getOrNull()?.let { emit(key, it) }
    }

    private fun bool(b: Boolean): String = b.toString()

    // --- readers --------------------------------------------------------------

    private fun readAll(): Map<String, String> {
        val out = mutableMapOf<String, String>()
        SETTINGS_KEYS.forEach { (key, reader) -> out[key] = reader(this) }
        RUNTIME_KEYS.forEach { (key, reader) -> out[key] = reader(this) }
        return out
    }

    private fun globalInt(key: String): String? =
        runCatching { Settings.Global.getInt(context.contentResolver, key).toString() }.getOrNull()

    private fun systemInt(key: String): String? =
        runCatching { Settings.System.getInt(context.contentResolver, key).toString() }.getOrNull()

    private fun secureInt(key: String): String? =
        runCatching { Settings.Secure.getInt(context.contentResolver, key).toString() }.getOrNull()

    private fun globalBool(key: String): String = bool(globalInt(key) == "1")

    private fun systemBool(key: String): String = bool(systemInt(key) == "1")

    private fun secureBool(key: String): String = bool(secureInt(key) == "1")

    private fun readRingerMode(): String {
        val am = context.getSystemService(AudioManager::class.java) ?: return "normal"
        return when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "silent"
            AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
            else -> "normal"
        }
    }

    private fun readVolume(stream: Int): String {
        val am = context.getSystemService(AudioManager::class.java) ?: return "0"
        return am.getStreamVolume(stream).toString()
    }

    private fun readHeadphones(): String {
        val am = context.getSystemService(AudioManager::class.java) ?: return "false"
        val devices =
            runCatching { am.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }.getOrNull()
                ?: return "false"
        val connected =
            devices.any {
                it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == android.media.AudioDeviceInfo.TYPE_USB_HEADSET
            }
        return bool(connected)
    }

    private fun readNfc(): String {
        val adapter = context.getSystemService(NfcManager::class.java)?.defaultAdapter
        return bool(adapter != null && runCatching { adapter.isEnabled }.getOrDefault(false))
    }

    private fun readLocation(): String {
        val lm = context.getSystemService(LocationManager::class.java) ?: return "false"
        return bool(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                lm.isLocationEnabled
            } else {
                @Suppress("DEPRECATION")
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF,
                ) != Settings.Secure.LOCATION_MODE_OFF
            },
        )
    }

    private fun readHotspot(): String {
        val wm = context.applicationContext.getSystemService(WifiManager::class.java) ?: return "false"
        return bool(
            runCatching {
                wm.javaClass.getMethod("isWifiApEnabled").invoke(wm) as? Boolean
            }.getOrNull() ?: false,
        )
    }

    @SuppressLint("MissingPermission")
    private fun readMobileData(): String {
        return bool(
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.getSystemService(TelephonyManager::class.java)?.isDataEnabled == true
                } else {
                    Settings.Global.getInt(context.contentResolver, "mobile_data", 0) == 1
                }
            }.getOrDefault(false),
        )
    }

    private fun readDarkMode(): String {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return bool(night == Configuration.UI_MODE_NIGHT_YES)
    }

    private fun readDnd(): String {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return "false"
        return bool(nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    private fun readPowerSaver(): String {
        val pm = context.getSystemService(PowerManager::class.java) ?: return "false"
        return bool(pm.isPowerSaveMode)
    }

    @SuppressLint("MissingPermission")
    private fun readWifiRadio(): String {
        val wm = context.applicationContext.getSystemService(WifiManager::class.java) ?: return "false"
        return bool(wm.isWifiEnabled)
    }

    @SuppressLint("MissingPermission")
    private fun readBluetoothRadio(): String {
        val bm = context.getSystemService(android.bluetooth.BluetoothManager::class.java)
        return bool(runCatching { bm?.adapter?.isEnabled }.getOrNull() == true)
    }

    private fun readThermal(): String {
        val pm = context.getSystemService(PowerManager::class.java) ?: return "0"
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.currentThermalStatus.toString()
        } else {
            "0"
        }
    }

    // --- settings observer ----------------------------------------------------

    private fun registerSettingsObserver() {
        val handler = observerHandler ?: return
        val observer =
            object : ContentObserver(handler) {
                override fun onChange(
                    selfChange: Boolean,
                    uri: android.net.Uri?,
                ) {
                    if (selfChange) return
                    onSettingsChanged(uri)
                }
            }
        settingsObserver = observer
        for (uri in
            listOf(
                Settings.Global.CONTENT_URI,
                Settings.System.CONTENT_URI,
                Settings.Secure.CONTENT_URI,
            )) {
            runCatching {
                context.contentResolver.registerContentObserver(uri, true, observer)
            }
        }
    }

    /**
     * Any Settings row change re-reads the settings-backed keys — reads are a
     * few ms each and changes are rare.
     */
    private fun onSettingsChanged(uri: android.net.Uri?) {
        if (uri == null) return
        update(DeviceStateKeys.POWER_SAVING) { globalBool("low_power") }
        update(DeviceStateKeys.DATA_SAVER) { globalBool("data_saver") }
        update(DeviceStateKeys.AIRPLANE) { globalBool("airplane_mode_on") }
        update(DeviceStateKeys.GLYPH_INTERFACE) { globalBool("led_effect_enable") }
        update(DeviceStateKeys.GLYPH_CHARGE_LED) { globalBool("led_effect_charging_enable") }
        update(DeviceStateKeys.BATTERY_SHARE) { globalBool("nt_wireless_reverse_charge") }
        update(DeviceStateKeys.BATTERY_SHARE_LIMIT) { globalInt("nt_reverse_charging_limiting_level") ?: return@update null }
        update(DeviceStateKeys.CHARGING_LIMIT) { systemInt("shutdown_battery_level") ?: return@update null }
        update(DeviceStateKeys.AUTO_ROTATE) { systemBool("accelerometer_rotation") }
        update(DeviceStateKeys.AOD) { secureBool("doze_always_on") }
        update(DeviceStateKeys.LOCATION) { readLocation() }
    }

    // --- broadcast receiver ---------------------------------------------------

    private fun registerBroadcastReceiver() {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    ctx: Context,
                    intent: Intent,
                ) {
                    when (intent.action) {
                        NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED ->
                            update(DeviceStateKeys.DND_ACTIVE) { readDnd() }
                        AudioManager.RINGER_MODE_CHANGED_ACTION ->
                            update(DeviceStateKeys.RINGER_MODE) { readRingerMode() }
                        VOLUME_CHANGED_ACTION -> {
                            update(DeviceStateKeys.VOLUME_MEDIA) { readVolume(AudioManager.STREAM_MUSIC) }
                            update(DeviceStateKeys.VOLUME_RING) { readVolume(AudioManager.STREAM_RING) }
                            update(DeviceStateKeys.VOLUME_ALARM) { readVolume(AudioManager.STREAM_ALARM) }
                        }
                        AudioManager.ACTION_HEADSET_PLUG,
                        AudioManager.ACTION_AUDIO_BECOMING_NOISY,
                        BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED,
                        -> update(DeviceStateKeys.HEADPHONES) { readHeadphones() }
                        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED ->
                            update(DeviceStateKeys.POWER_SAVING) { readPowerSaver() }
                        LocationManager.MODE_CHANGED_ACTION,
                        LocationManager.PROVIDERS_CHANGED_ACTION,
                        -> update(DeviceStateKeys.LOCATION) { readLocation() }
                        Intent.ACTION_AIRPLANE_MODE_CHANGED ->
                            update(DeviceStateKeys.AIRPLANE) { globalBool("airplane_mode_on") }
                        Intent.ACTION_CONFIGURATION_CHANGED ->
                            update(DeviceStateKeys.DARK_MODE) { readDarkMode() }
                        WifiManager.WIFI_STATE_CHANGED_ACTION ->
                            update(DeviceStateKeys.WIFI_RADIO) { readWifiRadio() }
                        BluetoothAdapter.ACTION_STATE_CHANGED ->
                            update(DeviceStateKeys.BLUETOOTH_RADIO) { readBluetoothRadio() }
                        NfcAdapter.ACTION_ADAPTER_STATE_CHANGED ->
                            update(DeviceStateKeys.NFC) { readNfc() }
                        WIFI_AP_STATE_CHANGED ->
                            update(DeviceStateKeys.HOTSPOT) { readHotspot() }
                    }
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                addAction(VOLUME_CHANGED_ACTION)
                addAction(AudioManager.ACTION_HEADSET_PLUG)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(LocationManager.MODE_CHANGED_ACTION)
                addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
                addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                addAction(Intent.ACTION_CONFIGURATION_CHANGED)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
                addAction(WIFI_AP_STATE_CHANGED)
            }
        stateReceiver = receiver
        runCatching { context.registerReceiver(receiver, filter) }
    }

    // --- thermal (API 29+) ----------------------------------------------------

    private fun registerThermalListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val pm = context.getSystemService(PowerManager::class.java) ?: return
        val listener =
            PowerManager.OnThermalStatusChangedListener { status ->
                update(DeviceStateKeys.THERMAL) { status.toString() }
            }
        thermalListener = listener
        runCatching {
            pm.addThermalStatusListener(ContextCompat.getMainExecutor(context), listener)
        }
    }

    private fun unregisterThermalListener() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val pm = context.getSystemService(PowerManager::class.java)
        thermalListener?.let { runCatching { pm?.removeThermalStatusListener(it) } }
        thermalListener = null
    }

    // --- tethering callback (public API 36+), hotspot event -------------------
    // Below 36 the class is a hidden system API — the slow poll covers
    // hotspot state there, so the callback is only wired where it's public.

    @SuppressLint("MissingPermission")
    private fun registerTetheringCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        val tm =
            context.getSystemService(android.net.TetheringManager::class.java) ?: return
        val callback =
            object : android.net.TetheringManager.TetheringEventCallback {
                override fun onTetheredInterfacesChanged(
                    interfaces: MutableSet<android.net.TetheringInterface>,
                ) {
                    update(DeviceStateKeys.HOTSPOT) { readHotspot() }
                }
            }
        tetheringCallback = callback
        runCatching {
            tm.registerTetheringEventCallback(ContextCompat.getMainExecutor(context), callback)
        }
    }

    private fun unregisterTetheringCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        val tm = context.getSystemService(android.net.TetheringManager::class.java)
        (tetheringCallback as? android.net.TetheringManager.TetheringEventCallback)?.let {
            runCatching { tm?.unregisterTetheringEventCallback(it) }
        }
        tetheringCallback = null
    }

    // --- slow poll for keys with no event source -------------------------------

    private fun schedulePoll() {
        val handler = pollHandler ?: return
        val tick =
            object : Runnable {
                override fun run() {
                    update(DeviceStateKeys.MOBILE_DATA) { readMobileData() }
                    update(DeviceStateKeys.HOTSPOT) { readHotspot() }
                    update(DeviceStateKeys.DND_ACTIVE) { readDnd() }
                    update(DeviceStateKeys.DARK_MODE) { readDarkMode() }
                    update(DeviceStateKeys.HEADPHONES) { readHeadphones() }
                    handler.postDelayed(this, POLL_INTERVAL_MS)
                }
            }
        handler.postDelayed(tick, POLL_INTERVAL_MS)
    }

    companion object {
        const val EXTRA_STATE_KEY = "device_state_key"
        const val EXTRA_STATE_VALUE = "device_state_value"
        const val EXTRA_STATE_PREVIOUS = "device_state_previous"

        /** Hidden broadcast for hotspot toggles — absent on some skins; the poll covers it. */
        private const val WIFI_AP_STATE_CHANGED = "android.net.wifi.WIFI_AP_STATE_CHANGED"

        /** No public constant on AudioManager — the literal action is stable since API 1. */
        private const val VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION"
        private const val POLL_INTERVAL_MS = 20_000L

        /** Settings-provider keys: device-state key -> reader. */
        private val SETTINGS_KEYS: List<Pair<String, DeviceStateMonitor.() -> String>> =
            listOf(
                DeviceStateKeys.POWER_SAVING to { globalBool("low_power") },
                DeviceStateKeys.DATA_SAVER to { globalBool("data_saver") },
                DeviceStateKeys.AIRPLANE to { globalBool("airplane_mode_on") },
                DeviceStateKeys.GLYPH_INTERFACE to { globalBool("led_effect_enable") },
                DeviceStateKeys.GLYPH_CHARGE_LED to { globalBool("led_effect_charging_enable") },
                DeviceStateKeys.BATTERY_SHARE to { globalBool("nt_wireless_reverse_charge") },
                DeviceStateKeys.BATTERY_SHARE_LIMIT to {
                    globalInt("nt_reverse_charging_limiting_level") ?: "0"
                },
                DeviceStateKeys.CHARGING_LIMIT to {
                    systemInt("shutdown_battery_level") ?: "0"
                },
                DeviceStateKeys.AUTO_ROTATE to { systemBool("accelerometer_rotation") },
                DeviceStateKeys.AOD to { secureBool("doze_always_on") },
            )

        /** Runtime-API keys: device-state key -> reader. */
        private val RUNTIME_KEYS: List<Pair<String, DeviceStateMonitor.() -> String>> =
            listOf(
                DeviceStateKeys.DND_ACTIVE to { readDnd() },
                DeviceStateKeys.RINGER_MODE to { readRingerMode() },
                DeviceStateKeys.VOLUME_MEDIA to { readVolume(AudioManager.STREAM_MUSIC) },
                DeviceStateKeys.VOLUME_RING to { readVolume(AudioManager.STREAM_RING) },
                DeviceStateKeys.VOLUME_ALARM to { readVolume(AudioManager.STREAM_ALARM) },
                DeviceStateKeys.HEADPHONES to { readHeadphones() },
                DeviceStateKeys.NFC to { readNfc() },
                DeviceStateKeys.LOCATION to { readLocation() },
                DeviceStateKeys.HOTSPOT to { readHotspot() },
                DeviceStateKeys.MOBILE_DATA to { readMobileData() },
                DeviceStateKeys.DARK_MODE to { readDarkMode() },
                DeviceStateKeys.WIFI_RADIO to { readWifiRadio() },
                DeviceStateKeys.BLUETOOTH_RADIO to { readBluetoothRadio() },
                DeviceStateKeys.THERMAL to { readThermal() },
            )
    }
}
