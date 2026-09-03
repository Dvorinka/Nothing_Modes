package com.tdvorak.nothingmodes.capabilities.controllers

import android.content.Context
import android.provider.Settings
import com.tdvorak.nothingmodes.engine.runtime.SettingReader

/**
 * Reads setting values using public Android APIs (no Shizuku required).
 * Used by the Engine to snapshot settings before mode activation.
 */
class AndroidSettingReader(private val context: Context) : SettingReader {

    override suspend fun read(key: String): String? = try {
        Settings.System.getString(context.contentResolver, key)
            ?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}
