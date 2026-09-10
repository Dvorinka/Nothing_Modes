package com.tdvorak.nothingmodes.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhonelinkSetup
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.ui.util.openAppPermissionPage
import com.tdvorak.nothingmodes.capabilities.ShizukuCapabilityStatus
import com.tdvorak.nothingmodes.engine.model.CapabilityIds

/** A missing capability with a human label, an icon, and a fix action. */
data class CapabilityGap(
    val reason: String,
    val fixLabel: String,
    val icon: ImageVector,
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
                icon = iconFor(id),
                onFix = { openFixFor(context, id, caps) },
            )
        }.distinctBy { it.reason }

private fun iconFor(id: String): ImageVector =
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
        -> Icons.Outlined.Terminal

        CapabilityIds.ACTION_SET_DND -> Icons.Outlined.DoNotDisturbOn
        CapabilityIds.ACTION_SET_BRIGHTNESS,
        CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS,
        CapabilityIds.ACTION_SET_SCREEN_TIMEOUT,
        CapabilityIds.ACTION_SET_AUTO_ROTATE,
        CapabilityIds.ACTION_SET_REFRESH_RATE,
        CapabilityIds.ACTION_SET_SCREEN_ROTATION,
        CapabilityIds.ACTION_SET_AOD,
        -> Icons.Outlined.PhonelinkSetup

        CapabilityIds.TRIGGER_NOTIFICATION,
        CapabilityIds.ACTION_CLEAR_NOTIFICATIONS,
        -> Icons.Outlined.Notifications

        CapabilityIds.TRIGGER_APP_OPENED,
        CapabilityIds.STATE_FOREGROUND_APP,
        -> Icons.Outlined.QueryStats

        CapabilityIds.TRIGGER_PHONE_SMS,
        CapabilityIds.ACTION_SEND_SMS,
        -> Icons.Outlined.Sms

        CapabilityIds.TRIGGER_PHONE_CALL -> Icons.Outlined.Call
        CapabilityIds.TRIGGER_GEOFENCE,
        CapabilityIds.STATE_LOCATION,
        -> Icons.Outlined.Place

        CapabilityIds.TRIGGER_CALENDAR_EVENT -> Icons.Outlined.CalendarMonth
        CapabilityIds.ACTION_LOCK_SCREEN -> Icons.Outlined.Lock
        CapabilityIds.ACTION_SET_WIFI -> Icons.Outlined.Wifi
        CapabilityIds.ACTION_SET_BLUETOOTH -> Icons.Outlined.Bluetooth
        CapabilityIds.ACTION_SET_FLASHLIGHT -> Icons.Outlined.FlashlightOn
        CapabilityIds.ACTION_VIBRATE -> Icons.Outlined.Vibration

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
        -> Icons.Outlined.Lightbulb

        else -> Icons.Outlined.Settings
    }

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
        -> runCatching { context.startActivity(shizukuFixIntent(context, caps)) }

        CapabilityIds.ACTION_SET_DND ->
            runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
        CapabilityIds.ACTION_SET_BRIGHTNESS,
        CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS,
        CapabilityIds.ACTION_SET_SCREEN_TIMEOUT,
        CapabilityIds.ACTION_SET_AUTO_ROTATE,
        CapabilityIds.ACTION_SET_REFRESH_RATE,
        CapabilityIds.ACTION_SET_SCREEN_ROTATION,
        CapabilityIds.ACTION_SET_AOD,
        ->
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    },
                )
            }

        CapabilityIds.TRIGGER_NOTIFICATION,
        CapabilityIds.ACTION_CLEAR_NOTIFICATIONS,
        -> runCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }

        CapabilityIds.TRIGGER_APP_OPENED,
        CapabilityIds.STATE_FOREGROUND_APP,
        -> runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }

        CapabilityIds.TRIGGER_PHONE_SMS,
        CapabilityIds.ACTION_SEND_SMS,
        -> openAppPermissionPage(context, android.Manifest.permission.SEND_SMS)

        CapabilityIds.TRIGGER_PHONE_CALL ->
            openAppPermissionPage(context, android.Manifest.permission.READ_PHONE_STATE)

        CapabilityIds.TRIGGER_GEOFENCE,
        CapabilityIds.STATE_LOCATION,
        ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                openAppPermissionPage(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                runCatching { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
            }

        CapabilityIds.TRIGGER_CALENDAR_EVENT ->
            openAppPermissionPage(context, android.Manifest.permission.READ_CALENDAR)

        CapabilityIds.ACTION_LOCK_SCREEN ->
            runCatching { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
        CapabilityIds.ACTION_SET_WIFI ->
            runCatching { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        CapabilityIds.ACTION_SET_BLUETOOTH ->
            runCatching { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
        else ->
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            }
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
