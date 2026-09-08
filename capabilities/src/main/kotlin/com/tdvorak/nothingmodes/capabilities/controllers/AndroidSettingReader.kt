package com.tdvorak.nothingmodes.capabilities.controllers

import android.app.NotificationManager
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcManager
import android.provider.Settings
import com.tdvorak.nothingmodes.engine.runtime.SettingReader

/**
 * Reads setting values using public Android APIs (no Shizuku required).
 * Used by the Engine to snapshot settings before a windowed mode starts.
 *
 * Besides real Settings.System/Secure/Global keys it understands the engine's
 * semantic keys: "dnd_mode", "night_mode", "volume_<stream>" and the state
 * toggles in the action catalog. Unknown or unreadable keys return null and
 * are simply skipped.
 */
class AndroidSettingReader(
    private val context: Context,
) : SettingReader {
    override suspend fun read(key: String): String? =
        when (key) {
            "dnd_mode" -> readDndMode()
            "night_mode" -> readNightMode()
            "wifi_enabled" -> readWifiEnabled()
            "bluetooth_enabled" -> readBluetoothEnabled()
            "mobile_data_enabled" -> readMobileDataEnabled()
            "aod_enabled" -> readAodEnabled()
            "nfc_enabled" -> readNfcEnabled()
            "hotspot_enabled" -> readHotspotEnabled()
            "location_mode" -> readLocationMode()
            "auto_sync" -> readAutoSync()
            "ringer_mode" -> readRingerMode()
            "flashlight_on" -> readFlashlightState()
            in setOf("airplane_mode_on", "low_power", "data_saver") ->
                readGlobalSettingAsString(key)
            "reduce_bright_colors_activated" ->
                readSecureSettingAsString(key)
            "screen_off_timeout", "screen_brightness", "screen_brightness_mode",
            "accelerometer_rotation", "user_rotation", "peak_refresh_rate", "min_refresh_rate",
            -> readSystemSettingAsString(key)
            else ->
                when {
                    key.startsWith("volume_") -> readVolume(key.removePrefix("volume_"))
                    key.startsWith("glyph_") -> null
                    else -> readSettingsKey(key)
                }
        }

    private fun readDndMode(): String? =
        runCatching {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return null
            when (nm.currentInterruptionFilter) {
                NotificationManager.INTERRUPTION_FILTER_ALL -> "OFF"
                NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                NotificationManager.INTERRUPTION_FILTER_ALARMS,
                -> "PRIORITY"
                NotificationManager.INTERRUPTION_FILTER_NONE -> "TOTAL"
                else -> null
            }
        }.getOrNull()

    private fun readNightMode(): String =
        if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        ) {
            "ON"
        } else {
            "OFF"
        }

    private fun readVolume(streamName: String): String? =
        runCatching {
            val stream =
                when (streamName.uppercase()) {
                    "MEDIA" -> AudioManager.STREAM_MUSIC
                    "RING" -> AudioManager.STREAM_RING
                    "ALARM" -> AudioManager.STREAM_ALARM
                    "NOTIFICATION" -> AudioManager.STREAM_NOTIFICATION
                    else -> return null
                }
            val am = context.getSystemService(AudioManager::class.java) ?: return null
            am.getStreamVolume(stream).toString()
        }.getOrNull()

    /** Real keys: probe system, then secure, then global. */
    private fun readSettingsKey(key: String): String? {
        val resolver = context.contentResolver
        return readSystemSettingAsString(key)
            ?: readSecureSettingAsString(key)
            ?: readGlobalSettingAsString(key)
    }

    private fun readSystemSettingAsString(key: String): String? =
        runCatching { Settings.System.getString(context.contentResolver, key) }.getOrNull()

    private fun readSecureSettingAsString(key: String): String? =
        runCatching { Settings.Secure.getString(context.contentResolver, key) }.getOrNull()

    private fun readGlobalSettingAsString(key: String): String? =
        runCatching { Settings.Global.getString(context.contentResolver, key) }.getOrNull()

    private fun readWifiEnabled(): String? =
        runCatching {
            val wm = context.getSystemService(WifiManager::class.java) ?: return null
            wm.isWifiEnabled.toString()
        }.getOrNull()

    @Suppress("MissingPermission")
    private fun readBluetoothEnabled(): String? =
        runCatching {
            val bm = context.getSystemService(BluetoothManager::class.java) ?: return null
            bm.adapter?.isEnabled.toString()
        }.getOrNull()

    private fun readMobileDataEnabled(): String? =
        runCatching {
            val value = Settings.Global.getInt(context.contentResolver, "mobile_data", 1)
            (value == 1).toString()
        }.getOrNull()

    private fun readAodEnabled(): String? =
        runCatching {
            val value = Settings.Secure.getInt(context.contentResolver, "doze_always_on", 0)
            (value == 1).toString()
        }.getOrNull()

    private fun readNfcEnabled(): String? =
        runCatching {
            val nfcManager = context.getSystemService(NfcManager::class.java) ?: return null
            val adapter = nfcManager.defaultAdapter
            (adapter != null && adapter.isEnabled).toString()
        }.getOrNull()

    private fun readHotspotEnabled(): String? =
        runCatching {
            val wifiManager = context.getSystemService(WifiManager::class.java) ?: return null
            val method = WifiManager::class.java.getDeclaredMethod("isWifiApEnabled")
            method.invoke(wifiManager)?.toString()
        }.getOrNull()

    private fun readLocationMode(): String? =
        runCatching {
            when (
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.LOCATION_MODE,
                    Settings.Secure.LOCATION_MODE_OFF,
                )
            ) {
                Settings.Secure.LOCATION_MODE_OFF -> "OFF"
                Settings.Secure.LOCATION_MODE_SENSORS_ONLY -> "DEVICE_ONLY"
                Settings.Secure.LOCATION_MODE_BATTERY_SAVING -> "BATTERY_SAVING"
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY -> "HIGH_ACCURACY"
                else -> null
            }
        }.getOrNull()

    private fun readAutoSync(): String? =
        runCatching { ContentResolver.getMasterSyncAutomatically().toString() }.getOrNull()

    private fun readRingerMode(): String? =
        runCatching {
            when (context.getSystemService(AudioManager::class.java)?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> "SILENT"
                AudioManager.RINGER_MODE_VIBRATE -> "VIBRATE"
                AudioManager.RINGER_MODE_NORMAL -> "NORMAL"
                else -> null
            }
        }.getOrNull()

    private fun readFlashlightState(): String? {
        // jarvis: there is no reliable public API for current torch state on all devices;
        // return null so the engine skips the snapshot for flashlight actions.
        return null
    }
}
