package com.tdvorak.nothingmodes.capabilities.controllers

import android.annotation.SuppressLint
import android.app.usage.UsageStatsManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.PowerManager
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.runtime.DeviceState
import com.tdvorak.nothingmodes.engine.runtime.StateProvider
import java.util.concurrent.TimeUnit

/**
 * Reads device state using public Android APIs (no Shizuku required).
 * Battery, screen, and charging state are always available.
 * WiFi SSID requires ACCESS_FINE_LOCATION on Android 8+.
 * Bluetooth name requires BLUETOOTH_CONNECT on Android 12+.
 * Foreground app requires PACKAGE_USAGE_STATS (granted via Usage Access settings).
 * Fields left as false/null when unavailable (fail-closed).
 */
class AndroidStateProvider(private val context: Context) : StateProvider {

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

        return DeviceState(
            batteryLevel = batteryLevel,
            isCharging = isCharging,
            wifiConnected = wifiConnected,
            wifiSsid = wifiSsid,
            bluetoothConnected = btConnected,
            bluetoothDeviceName = btName,
            screenState = screenState,
            foregroundApp = foregroundApp,
            now = System.currentTimeMillis(),
        )
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
