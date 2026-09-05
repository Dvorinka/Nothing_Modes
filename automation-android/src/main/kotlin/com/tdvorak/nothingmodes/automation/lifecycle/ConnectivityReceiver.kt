package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.NetworkInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receives connectivity state changes (WiFi, Bluetooth) and dispatches
 * Connectivity trigger events to AutomationService.
 *
 * Manifest:
 * <receiver android:name=".automation.lifecycle.ConnectivityReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.net.wifi.WIFI_STATE_CHANGED"/>
 *         <action android:name="android.bluetooth.adapter.action.STATE_CHANGED"/>
 *     </intent-filter>
 * </receiver>
 */
class ConnectivityReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiManager.WIFI_STATE_CHANGED_ACTION -> handleWifiState(context, intent)
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
            @Suppress("DEPRECATION")
            android.net.ConnectivityManager.CONNECTIVITY_ACTION -> handleWifiConnected(context, intent)
            BluetoothAdapter.ACTION_STATE_CHANGED -> handleBtState(context, intent)
            BluetoothDevice.ACTION_ACL_CONNECTED -> handleBtDevice(context, intent, true)
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleBtDevice(context, intent, false)
        }
    }

    private fun handleWifiConnected(context: Context, intent: Intent) {
        val networkInfo = intent.getParcelableExtra<NetworkInfo>(android.net.ConnectivityManager.EXTRA_NETWORK_INFO)
        if (networkInfo?.type != android.net.ConnectivityManager.TYPE_WIFI) return
        if (networkInfo.state != NetworkInfo.State.CONNECTED) return
        val ssid = runCatching {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.ssid?.removeSurrounding("\"")
        }.getOrNull() ?: return
        Log.d(TAG, "WiFi connected: ssid=$ssid")
        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_WIFI_CONNECTED
            putExtra(EXTRA_WIFI_SSID, ssid)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun handleBtDevice(context: Context, intent: Intent, connected: Boolean) {
        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
        val hasBtConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val name = if (hasBtConnect) runCatching { device?.name }.getOrNull() else null
        val address = if (hasBtConnect) runCatching { device?.address }.getOrNull() else null
        Log.d(TAG, "BT device ${if (connected) "connected" else "disconnected"}")
        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_BT_DEVICE
            putExtra(EXTRA_BT_DEVICE_STATE, if (connected) "connected" else "disconnected")
            if (name != null) putExtra(EXTRA_BT_DEVICE_NAME, name)
            if (address != null) putExtra(EXTRA_BT_DEVICE_ADDRESS, address)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun handleWifiState(context: Context, intent: Intent) {
        val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        val wifiEvent = when (state) {
            WifiManager.WIFI_STATE_ENABLED -> "wifi_enabled"
            WifiManager.WIFI_STATE_DISABLED -> "wifi_disabled"
            else -> return
        }
        Log.d(TAG, "WiFi state: $wifiEvent")

        // Extract SSID if available for match-based triggers
        val ssid = runCatching {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.ssid?.removeSurrounding("\"")
        }.getOrNull()

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_CONNECTIVITY
            putExtra(EXTRA_CONNECTIVITY_TYPE, "wifi")
            putExtra(EXTRA_CONNECTIVITY_STATE, wifiEvent)
            if (ssid != null) putExtra(EXTRA_CONNECTIVITY_MATCH, ssid)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun handleBtState(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        val btEvent = when (state) {
            BluetoothAdapter.STATE_ON -> "bt_enabled"
            BluetoothAdapter.STATE_OFF -> "bt_disabled"
            else -> return
        }
        Log.d(TAG, "BT state: $btEvent")

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_CONNECTIVITY
            putExtra(EXTRA_CONNECTIVITY_TYPE, "bluetooth")
            putExtra(EXTRA_CONNECTIVITY_STATE, btEvent)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        private const val TAG = "ConnectivityReceiver"
        const val EXTRA_CONNECTIVITY_TYPE = "connectivity_type"
        const val EXTRA_CONNECTIVITY_STATE = "connectivity_state"
        const val EXTRA_CONNECTIVITY_MATCH = "connectivity_match"
        const val EXTRA_WIFI_SSID = "wifi_ssid"
        const val EXTRA_BT_DEVICE_STATE = "bt_device_state"
        const val EXTRA_BT_DEVICE_NAME = "bt_device_name"
        const val EXTRA_BT_DEVICE_ADDRESS = "bt_device_address"
    }
}
