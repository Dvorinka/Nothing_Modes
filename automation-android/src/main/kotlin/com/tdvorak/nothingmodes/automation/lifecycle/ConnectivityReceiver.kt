package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.bluetooth.BluetoothAdapter
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
            BluetoothAdapter.ACTION_STATE_CHANGED -> handleBtState(context, intent)
        }
    }

    private fun handleWifiState(context: Context, intent: Intent) {
        val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        val wifiEvent = when (state) {
            WifiManager.WIFI_STATE_ENABLED -> "wifi_enabled"
            WifiManager.WIFI_STATE_DISABLED -> "wifi_disabled"
            else -> return
        }
        Log.d(TAG, "WiFi state: $wifiEvent")

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_CONNECTIVITY
            putExtra(EXTRA_CONNECTIVITY_TYPE, "wifi")
            putExtra(EXTRA_CONNECTIVITY_STATE, wifiEvent)
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
    }
}
