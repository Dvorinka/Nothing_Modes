package com.tdvorak.nothingmodes.capabilities.controllers

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.nfc.NfcManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.runtime.DeviceState
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider
import com.tdvorak.nothingmodes.engine.runtime.StateProvider
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Reads device state using public Android APIs (no Shizuku required).
 * Battery, screen, and charging state are always available.
 * WiFi SSID requires ACCESS_FINE_LOCATION on Android 8+.
 * Bluetooth name requires BLUETOOTH_CONNECT on Android 12+.
 * Foreground app requires PACKAGE_USAGE_STATS (granted via Usage Access settings).
 * Active mode IDs requires a ModeActivationProvider (backed by Room).
 * Fields left as false/null when unavailable (fail-closed).
 */
class AndroidStateProvider(
    private val context: Context,
    private val modeActivationProvider: ModeActivationProvider? = null,
) : StateProvider {
    override suspend fun read(): DeviceState {
        val powerManager = context.getSystemService(PowerManager::class.java)
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val screenState = if (powerManager.isInteractive) ScreenState.ON else ScreenState.OFF

        val batteryLevel =
            batteryIntent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) (level * 100) / scale else -1
            } ?: -1

        val isCharging =
            batteryIntent?.let {
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            } ?: false

        val (wifiConnected, wifiSsid) = readWifiState()
        val (btConnected, btName) = readBluetoothState()
        val foregroundApp = readForegroundApp()
        val activeModeIds = modeActivationProvider?.activeModeIds()?.toSet() ?: emptySet()
        val values = readValues(powerManager, batteryIntent)

        return DeviceState(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            wifiConnected = wifiConnected,
            wifiSsid = wifiSsid,
            bluetoothConnected = btConnected,
            bluetoothDeviceName = btName,
            screenState = screenState,
            foregroundApp = foregroundApp,
            activeModeIds = activeModeIds,
            values = values,
            now = System.currentTimeMillis(),
        )
    }

    private fun readValues(
        powerManager: PowerManager,
        batteryIntent: Intent?,
    ): Map<String, String> {
        val values = mutableMapOf<String, String>()

        // Dark mode is derived from the current UI mode configuration.
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        values["dark_mode"] = (nightMode == Configuration.UI_MODE_NIGHT_YES).toString()

        // Power save mode is exposed directly by PowerManager.
        values["power_saving"] = powerManager.isPowerSaveMode.toString()

        // Thermal status (API 29+): 0 none .. 6 shutdown.
        values["thermal_status"] =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                powerManager.currentThermalStatus.toString()
            } else {
                "0"
            }

        // Media playback and ringer mode come from AudioManager.
        val audioManager = context.getSystemService(AudioManager::class.java)
        if (audioManager != null) {
            values["media_playing"] = audioManager.isMusicActive.toString()
            values["ringer_mode"] =
                when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> "silent"
                    AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
                    else -> "normal"
                }
            values["headphones_connected"] = readHeadphonesConnected(audioManager).toString()
            values["volume_media"] = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toString()
            values["volume_ring"] = audioManager.getStreamVolume(AudioManager.STREAM_RING).toString()
            values["volume_alarm"] = audioManager.getStreamVolume(AudioManager.STREAM_ALARM).toString()
        }

        values["airplane_mode"] = readAirplaneMode().toString()
        values["nfc_enabled"] = readNfcEnabled().toString()
        values["location_enabled"] = readLocationEnabled().toString()

        // Companion devices (CMF Watch, Ear, ...) ride bonded Bluetooth —
        // expose their names so conditions can match on them.
        readBluetoothDevices()?.let { values["bt_devices"] = it }
        values["screen_time_today_ms"] = readScreenTimeToday().toString()
        values["auto_sync"] = runCatching { ContentResolver.getMasterSyncAutomatically() }.getOrDefault(false).toString()
        values["data_saver"] = readDataSaverEnabled().toString()
        values["auto_rotate"] =
            runCatching {
                Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
            }.getOrDefault(false).toString()

        batteryIntent?.let { intent ->
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
            values["charging_source"] =
                when (plugged) {
                    BatteryManager.BATTERY_PLUGGED_AC -> "ac"
                    BatteryManager.BATTERY_PLUGGED_USB -> "usb"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
                    BatteryManager.BATTERY_PLUGGED_DOCK -> "dock"
                    else -> "none"
                }
            val rawTemp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            if (rawTemp != Int.MIN_VALUE) {
                values["battery_temp_c"] = (rawTemp / 10.0).toString()
            }
        }

        // Nothing OS keys verified on OS 4.1 (B4.1): charge limit,
        // always-on display, glyph charging effects, reverse-charge cap.
        readSystemInt("shutdown_battery_level")?.let { values["charging_limit"] = it.toString() }
        readSecureInt("doze_always_on")?.let { values["aod_enabled"] = it.toString() }
        readGlobalInt("led_effect_charging_enable")?.let { values["glyph_charge_led"] = it.toString() }
        readGlobalInt("nt_reverse_charging_limiting_level")?.let { values["reverse_charge_limit"] = it.toString() }
        readSystemInt("glyph_toy_timeout")?.let { values["glyph_toy_timeout_ms"] = it.toString() }

        readScreenOffSince()?.let { values["screen_off_since_ms"] = it.toString() }

        readCallState()?.let { values["call_state"] = it }

        return values
    }

    // State readers for value-based conditions.
    // Missing permission is treated as unavailable (the value is omitted).

    private fun readSystemInt(key: String): Int? =
        runCatching { Settings.System.getInt(context.contentResolver, key) }.getOrNull()

    private fun readSecureInt(key: String): Int? =
        runCatching { Settings.Secure.getInt(context.contentResolver, key) }.getOrNull()

    private fun readGlobalInt(key: String): Int? =
        runCatching { Settings.Global.getInt(context.contentResolver, key) }.getOrNull()

    private fun readAirplaneMode(): Boolean =
        try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        } catch (e: SecurityException) {
            false
        }

    private fun readNfcEnabled(): Boolean =
        try {
            val nfcManager = context.getSystemService(NfcManager::class.java)
            val adapter = nfcManager?.defaultAdapter
            adapter != null && adapter.isEnabled
        } catch (e: SecurityException) {
            false
        }

    private fun readLocationEnabled(): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val locationManager = context.getSystemService(LocationManager::class.java)
                locationManager?.isLocationEnabled ?: false
            } else {
                val mode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                mode != Settings.Secure.LOCATION_MODE_OFF
            }
        } catch (e: SecurityException) {
            false
        }

    private fun readCallState(): String? {
        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return try {
            val telephonyManager = context.getSystemService(TelephonyManager::class.java)
            when (telephonyManager?.callState) {
                TelephonyManager.CALL_STATE_RINGING -> "incoming"
                TelephonyManager.CALL_STATE_OFFHOOK -> "active"
                else -> "idle"
            }
        } catch (e: SecurityException) {
            null
        }
    }

    private fun readWifiState(): Pair<Boolean, String?> =
        try {
            val wifiManager = context.getSystemService(WifiManager::class.java)
            val info = wifiManager?.connectionInfo
            if (info != null && info.ipAddress != 0) {
                val ssid = info.ssid?.removeSurrounding("\"")
                Pair(true, ssid)
            } else {
                Pair(false, null)
            }
        } catch (e: SecurityException) {
            Pair(false, null)
        }

    @SuppressLint("MissingPermission")
    private fun readBluetoothState(): Pair<Boolean, String?> =
        try {
            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            val adapter = bluetoothManager?.adapter
            if (adapter != null && adapter.isEnabled) {
                val connected = adapter.bondedDevices.isNotEmpty()
                val name = adapter.name
                Pair(connected, name)
            } else {
                Pair(false, null)
            }
        } catch (e: SecurityException) {
            Pair(false, null)
        }

    /** Comma-separated names of bonded Bluetooth devices, null when BT off/denied. */
    @SuppressLint("MissingPermission")
    private fun readBluetoothDevices(): String? =
        try {
            val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
            adapter?.takeIf { it.isEnabled }
                ?.bondedDevices
                ?.mapNotNull { it.name }
                ?.sorted()
                ?.joinToString(",")
                ?.takeIf { it.isNotBlank() }
        } catch (e: SecurityException) {
            null
        }

    @SuppressLint("MissingPermission")
    private fun readForegroundApp(): String? {
        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return null
            val now = System.currentTimeMillis()
            val stats =
                usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    now - TimeUnit.SECONDS.toMillis(5),
                    now,
                ) ?: return null
            stats
                .filter { it.lastTimeStamp > now - TimeUnit.SECONDS.toMillis(5) }
                .maxByOrNull { it.lastTimeStamp }
                ?.packageName
        } catch (e: SecurityException) {
            null
        }
    }

    private fun readHeadphonesConnected(audioManager: AudioManager): Boolean {
        val types =
            buildSet {
                add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
                add(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)
                add(AudioDeviceInfo.TYPE_USB_HEADSET)
                add(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP)
                add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
                add(AudioDeviceInfo.TYPE_HEARING_AID)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(AudioDeviceInfo.TYPE_BLE_HEADSET)
                }
            }
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { it.type in types }
    }

    private fun readDataSaverEnabled(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        return cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
    }

    // Screen-off timestamp is written by PersistentMonitorService; capabilities
    // cannot depend on automation-android, so both sides share this prefs file.
    private fun readScreenOffSince(): Long? {
        val prefs = context.getSharedPreferences(SCREEN_STATE_PREFS, Context.MODE_PRIVATE)
        val ts = prefs.getLong(KEY_SCREEN_OFF_SINCE, -1L)
        return ts.takeIf { it > 0 }
    }

    @SuppressLint("MissingPermission")
    private fun readScreenTimeToday(): Long {
        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return 0L
            val startOfDay =
                LocalDate
                    .now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            val now = System.currentTimeMillis()
            val stats =
                usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    startOfDay,
                    now,
                ) ?: return 0L
            stats.sumOf { it.totalTimeInForeground }
        } catch (e: SecurityException) {
            0L
        }
    }

    companion object {
        const val SCREEN_STATE_PREFS = "nothing_modes_state"
        const val KEY_SCREEN_OFF_SINCE = "screen_off_since_ms"
    }
}
