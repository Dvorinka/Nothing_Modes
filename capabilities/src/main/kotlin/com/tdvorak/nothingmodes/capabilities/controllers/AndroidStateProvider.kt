package com.tdvorak.nothingmodes.capabilities.controllers

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.location.LocationManager
import android.media.AudioManager
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

        val batteryLevel = batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) (level * 100) / scale else -1
        } ?: -1

        val isCharging = batteryIntent?.let {
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        } ?: false

        val (wifiConnected, wifiSsid) = readWifiState()
        val (btConnected, btName) = readBluetoothState()
        val foregroundApp = readForegroundApp()
        val activeModeIds = modeActivationProvider?.activeModeIds()?.toSet() ?: emptySet()
        val values = readValues(powerManager)

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

    private fun readValues(powerManager: PowerManager): Map<String, String> {
        val values = mutableMapOf<String, String>()

        // Dark mode is derived from the current UI mode configuration.
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        values["dark_mode"] = (nightMode == Configuration.UI_MODE_NIGHT_YES).toString()

        // Power save mode is exposed directly by PowerManager.
        values["power_saving"] = powerManager.isPowerSaveMode.toString()

        // Media playback and ringer mode come from AudioManager.
        val audioManager = context.getSystemService(AudioManager::class.java)
        if (audioManager != null) {
            values["media_playing"] = audioManager.isMusicActive.toString()
            values["ringer_mode"] = when (audioManager.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "silent"
                AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
                else -> "normal"
            }
        }

        values["airplane_mode"] = readAirplaneMode().toString()
        values["nfc_enabled"] = readNfcEnabled().toString()
        values["location_enabled"] = readLocationEnabled().toString()

        readCallState()?.let { values["call_state"] = it }

        return values
    }

    // State readers for value-based conditions.
    // Missing permission is treated as unavailable (the value is omitted).

    private fun readAirplaneMode(): Boolean = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
    } catch (e: SecurityException) {
        false
    }

    private fun readNfcEnabled(): Boolean = try {
        val nfcManager = context.getSystemService(NfcManager::class.java)
        val adapter = nfcManager?.defaultAdapter
        adapter != null && adapter.isEnabled
    } catch (e: SecurityException) {
        false
    }

    private fun readLocationEnabled(): Boolean = try {
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

    private fun readWifiState(): Pair<Boolean, String?> {
        return try {
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
    }

    @SuppressLint("MissingPermission")
    private fun readBluetoothState(): Pair<Boolean, String?> {
        return try {
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
    }

    @SuppressLint("MissingPermission")
    private fun readForegroundApp(): String? {
        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val now = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
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
}
