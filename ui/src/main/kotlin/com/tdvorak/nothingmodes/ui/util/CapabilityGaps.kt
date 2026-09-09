package com.tdvorak.nothingmodes.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.capabilities.ShizukuCapabilityStatus
import com.tdvorak.nothingmodes.engine.model.CapabilityIds

/** A missing capability with a human label and a fix action. */
data class CapabilityGap(
    val reason: String,
    val fixLabel: String,
    val onFix: () -> Unit,
)

/** Build a list of fixable gaps from the missing capability IDs. */
fun capabilityGaps(
    context: Context,
    missing: Map<String, String>,
    caps: DeviceCapabilities,
): List<CapabilityGap> =
    missing
        .map { (id, reason) ->
            CapabilityGap(
                reason = reason,
                fixLabel = fixLabelFor(id, caps),
                onFix = { openFixFor(context, id, caps) },
            )
        }.distinctBy { it.reason }

private fun fixLabelFor(
    id: String,
    caps: DeviceCapabilities,
): String =
    when (id) {
        CapabilityIds.SHIZUKU_REQUIRED,
        CapabilityIds.ACTION_SET_DARK_MODE,
        CapabilityIds.ACTION_SET_EXTRA_DIM,
        CapabilityIds.ACTION_SET_MOBILE_DATA,
        CapabilityIds.ACTION_SET_LOCATION_MODE,
        CapabilityIds.ACTION_WRITE_SETTING,
        CapabilityIds.ACTION_SET_BATTERY_SAVER,
        CapabilityIds.ACTION_SET_AIRPLANE_MODE,
        CapabilityIds.ACTION_SET_DATA_SAVER,
        CapabilityIds.ACTION_SET_HOTSPOT,
        CapabilityIds.ACTION_SET_NFC,
        CapabilityIds.ACTION_SET_AUTO_SYNC,
        CapabilityIds.ACTION_TAKE_SCREENSHOT,
        CapabilityIds.STATE_READER_SETTING,
        CapabilityIds.STATE_READER_SYSTEM_PROPERTY,
        CapabilityIds.STATE_READER_SYSFS,
        CapabilityIds.STATE_READER_DUMPSYS_FIELD,
        -> shizukuFixLabel(caps)

        CapabilityIds.ACTION_SET_DND -> "Open DND settings"
        CapabilityIds.ACTION_SET_BRIGHTNESS,
        CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS,
        CapabilityIds.ACTION_SET_SCREEN_TIMEOUT,
        CapabilityIds.ACTION_SET_AUTO_ROTATE,
        CapabilityIds.ACTION_SET_REFRESH_RATE,
        CapabilityIds.ACTION_SET_SCREEN_ROTATION,
        CapabilityIds.ACTION_SET_AOD,
        -> "Allow write settings"

        CapabilityIds.TRIGGER_NOTIFICATION,
        CapabilityIds.ACTION_CLEAR_NOTIFICATIONS,
        -> "Grant notification access"

        CapabilityIds.TRIGGER_APP_OPENED,
        CapabilityIds.STATE_FOREGROUND_APP,
        -> "Grant usage access"

        CapabilityIds.TRIGGER_PHONE_SMS,
        CapabilityIds.ACTION_SEND_SMS,
        -> "Allow SMS"

        CapabilityIds.TRIGGER_PHONE_CALL -> "Allow phone state"
        CapabilityIds.TRIGGER_GEOFENCE,
        CapabilityIds.STATE_LOCATION,
        -> "Allow location"

        CapabilityIds.TRIGGER_CALENDAR_EVENT -> "Allow calendar"
        CapabilityIds.ACTION_LOCK_SCREEN -> "Enable device admin"
        CapabilityIds.ACTION_SET_WIFI -> "Wi-Fi unavailable"
        CapabilityIds.ACTION_SET_BLUETOOTH -> "Bluetooth unavailable"
        CapabilityIds.ACTION_SET_FLASHLIGHT -> "Flashlight unavailable"
        CapabilityIds.ACTION_VIBRATE -> "Vibrator unavailable"

        in
        setOf(
            CapabilityIds.ACTION_SET_GLYPH,
            CapabilityIds.ACTION_SET_GLYPH_MATRIX,
            CapabilityIds.ACTION_GLYPH_ANIMATE,
            CapabilityIds.ACTION_GLYPH_PROGRESS,
            CapabilityIds.ACTION_GLYPH_TEXT,
            CapabilityIds.ACTION_GLYPH_SCROLLING_TEXT,
            CapabilityIds.ACTION_GLYPH_PRESET,
            CapabilityIds.ACTION_GLYPH_TURNOFF,
            CapabilityIds.ACTION_GLYPH_ICON,
            CapabilityIds.ACTION_GLYPH_NUMBER,
            CapabilityIds.ACTION_GLYPH_COUNTDOWN,
            CapabilityIds.ACTION_GLYPH_MUSIC,
        ),
        -> "Glyph not available"

        else -> "Open settings"
    }

private fun shizukuFixLabel(caps: DeviceCapabilities): String =
    when (caps.shizukuStatus) {
        ShizukuCapabilityStatus.NOT_INSTALLED -> "Install Shizuku"
        ShizukuCapabilityStatus.INSTALLED_NOT_RUNNING -> "Start Shizuku"
        ShizukuCapabilityStatus.RUNNING_NOT_AUTHORIZED -> "Authorize Shizuku"
        ShizukuCapabilityStatus.AUTHORIZED -> "Shizuku authorized"
        ShizukuCapabilityStatus.NOT_CHECKED,
        ShizukuCapabilityStatus.UNSUPPORTED,
        -> "Check Shizuku"
    }

private fun openFixFor(
    context: Context,
    id: String,
    caps: DeviceCapabilities,
) {
    val intent =
        when (id) {
            CapabilityIds.SHIZUKU_REQUIRED,
            CapabilityIds.ACTION_SET_DARK_MODE,
            CapabilityIds.ACTION_SET_EXTRA_DIM,
            CapabilityIds.ACTION_SET_MOBILE_DATA,
            CapabilityIds.ACTION_SET_LOCATION_MODE,
            CapabilityIds.ACTION_WRITE_SETTING,
            CapabilityIds.ACTION_SET_BATTERY_SAVER,
            CapabilityIds.ACTION_SET_AIRPLANE_MODE,
            CapabilityIds.ACTION_SET_DATA_SAVER,
            CapabilityIds.ACTION_SET_HOTSPOT,
            CapabilityIds.ACTION_SET_NFC,
            CapabilityIds.ACTION_SET_AUTO_SYNC,
            CapabilityIds.ACTION_TAKE_SCREENSHOT,
            CapabilityIds.STATE_READER_SETTING,
            CapabilityIds.STATE_READER_SYSTEM_PROPERTY,
            CapabilityIds.STATE_READER_SYSFS,
            CapabilityIds.STATE_READER_DUMPSYS_FIELD,
            -> shizukuFixIntent(context, caps)

            CapabilityIds.ACTION_SET_DND -> Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            CapabilityIds.ACTION_SET_BRIGHTNESS,
            CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS,
            CapabilityIds.ACTION_SET_SCREEN_TIMEOUT,
            CapabilityIds.ACTION_SET_AUTO_ROTATE,
            CapabilityIds.ACTION_SET_REFRESH_RATE,
            CapabilityIds.ACTION_SET_SCREEN_ROTATION,
            CapabilityIds.ACTION_SET_AOD,
            ->
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }

            CapabilityIds.TRIGGER_NOTIFICATION,
            CapabilityIds.ACTION_CLEAR_NOTIFICATIONS,
            -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

            CapabilityIds.TRIGGER_APP_OPENED,
            CapabilityIds.STATE_FOREGROUND_APP,
            -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

            CapabilityIds.TRIGGER_PHONE_SMS,
            CapabilityIds.ACTION_SEND_SMS,
            ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }

            CapabilityIds.TRIGGER_PHONE_CALL ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }

            CapabilityIds.TRIGGER_GEOFENCE,
            CapabilityIds.STATE_LOCATION,
            ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                } else {
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                }

            CapabilityIds.TRIGGER_CALENDAR_EVENT ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }

            CapabilityIds.ACTION_LOCK_SCREEN -> Intent(Settings.ACTION_SECURITY_SETTINGS)
            CapabilityIds.ACTION_SET_WIFI -> Intent(Settings.ACTION_WIFI_SETTINGS)
            CapabilityIds.ACTION_SET_BLUETOOTH -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            else ->
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
        }
    runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun shizukuFixIntent(
    context: Context,
    caps: DeviceCapabilities,
): Intent =
    when (caps.shizukuStatus) {
        ShizukuCapabilityStatus.NOT_INSTALLED ->
            runCatching {
                context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download"))
            }.getOrDefault(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download")))

        ShizukuCapabilityStatus.INSTALLED_NOT_RUNNING,
        ShizukuCapabilityStatus.RUNNING_NOT_AUTHORIZED,
        ->
            context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:moe.shizuku.privileged.api")
                }

        else ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:moe.shizuku.privileged.api")
            }
    }
