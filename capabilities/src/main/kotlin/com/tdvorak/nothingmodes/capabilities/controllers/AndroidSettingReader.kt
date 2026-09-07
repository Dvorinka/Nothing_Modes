package com.tdvorak.nothingmodes.capabilities.controllers

import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import android.media.AudioManager
import android.provider.Settings
import com.tdvorak.nothingmodes.engine.runtime.SettingReader

/**
 * Reads setting values using public Android APIs (no Shizuku required).
 * Used by the Engine to snapshot settings before a windowed routine starts.
 *
 * Besides real Settings.System/Secure/Global keys it understands the engine's
 * semantic keys: "dnd_mode", "night_mode" and "volume_<stream>". Unknown or
 * unreadable keys return null and are simply skipped.
 */
class AndroidSettingReader(
    private val context: Context,
) : SettingReader {
    override suspend fun read(key: String): String? =
        when {
            key == "dnd_mode" -> readDndMode()
            key == "night_mode" -> readNightMode()
            key.startsWith("volume_") -> readVolume(key.removePrefix("volume_"))
            key.startsWith("glyph_") -> null
            else -> readSettingsKey(key)
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
        return runCatching { Settings.System.getString(resolver, key) }.getOrNull()
            ?: runCatching { Settings.Secure.getString(resolver, key) }.getOrNull()
            ?: runCatching { Settings.Global.getString(resolver, key) }.getOrNull()
    }
}
