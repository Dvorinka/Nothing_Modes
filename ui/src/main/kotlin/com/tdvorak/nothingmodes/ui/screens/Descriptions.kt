package com.tdvorak.nothingmodes.ui.screens

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Trigger

fun cronToSummary(cron: String): String {
    val parts = cron.split(" ").filter { it.isNotBlank() }
    if (parts.size < 5) return cron

    val minute = parts[0]
    val hour = parts[1]
    val dayOfWeek = parts[4]

    if (hour == "*" || minute == "*") return "Every minute"

    val time = "${hour.padStart(2, '0')}:${minute.padStart(2, '0')}"
    val dayLabel = when (dayOfWeek) {
        "*" -> "Daily"
        "1-5" -> "Weekdays"
        "0,6", "6,0" -> "Weekend"
        "5,6" -> "Fri, Sat"
        else -> if (dayOfWeek.contains(",")) {
            dayOfWeek.split(",").map { dayName(it.trim()) }.joinToString(", ")
        } else if (dayOfWeek.contains("-")) {
            val range = dayOfWeek.split("-")
            "${dayName(range[0])}-${dayName(range[1])}"
        } else {
            dayName(dayOfWeek)
        }
    }
    return "$dayLabel at $time"
}

private fun dayName(day: String): String = when (day) {
    "0", "7" -> "Sun"
    "1" -> "Mon"
    "2" -> "Tue"
    "3" -> "Wed"
    "4" -> "Thu"
    "5" -> "Fri"
    "6" -> "Sat"
    else -> day
}

/** Shared description functions for triggers and actions. */

fun triggerDescription(trigger: Trigger): String = (when (trigger) {
    is Trigger.Time -> {
        trigger.at?.let { "Once · ${it.take(16).replace("T", " ")}" }
            ?: trigger.cron?.let { cronToSummary(it) }
            ?: trigger.afterMs?.let { "In ${it / 1000}s" }
            ?: "Time-based"
    }
    is Trigger.TimeWindow -> "Window: ${trigger.startLocal} - ${trigger.endLocal} (${trigger.tz})"
    is Trigger.Immediate -> "Immediate"
    is Trigger.Notification -> "Notification from ${trigger.pkg}"
    is Trigger.PhoneState -> "Phone: ${trigger.event}"
    is Trigger.Connectivity -> "${trigger.medium} ${trigger.state}"
    is Trigger.Boot -> "On boot"
    is Trigger.BatteryLevel -> "Battery at ${trigger.level}%"
    is Trigger.ScreenStateTrigger -> "Screen ${trigger.state}"
    is Trigger.AppOpened -> "App opened: ${trigger.pkg}"
    is Trigger.Geofence -> "Geofence (${trigger.lat}, ${trigger.lng}) r=${trigger.radiusM}m"
    is Trigger.Manual -> "Manual"
    is Trigger.BluetoothDevice -> "BT device ${trigger.state}${trigger.deviceName?.let { ": $it" } ?: ""}"
    is Trigger.WifiConnected -> "WiFi connected${trigger.ssid?.let { ": $it" } ?: ""}"
    is Trigger.CalendarEvent -> "Calendar ${trigger.direction.name.lowercase()}${trigger.titleMatch?.let { ": $it" } ?: ""}"
}).uppercase()

fun actionDescription(action: Action): String = (when (action) {
    is Action.SetWifi -> "Wi-Fi: ${if (action.on) "On" else "Off"}"
    is Action.SetBluetooth -> "Bluetooth: ${if (action.on) "On" else "Off"}"
    is Action.SetMobileData -> "Mobile Data: ${if (action.on) "On" else "Off"}"
    is Action.SetDnd -> "DND: ${action.mode.name.lowercase()}"
    is Action.SetRinger -> "Ringer: ${action.mode}"
    is Action.LaunchApp -> "Launch: ${action.pkg}"
    is Action.OpenUrl -> "Open URL: ${action.url}"
    is Action.ShowNotification -> "Notification: ${action.title}"
    is Action.SetVolume -> "Volume ${action.stream}: ${action.level}"
    is Action.SetFlashlight -> "Flashlight: ${if (action.on) "On" else "Off"}"
    is Action.SetDarkMode -> "Dark Mode: ${action.mode.name.lowercase()}"
    is Action.OpenSettingsScreen -> "Open Settings: ${action.screen.name.lowercase()}"
    is Action.Vibrate -> "Vibrate: ${action.durationMs}ms"
    is Action.SetBrightness -> "Brightness: ${action.level}"
    is Action.SetAutoBrightness -> "Auto Brightness: ${if (action.on) "On" else "Off"}"
    is Action.SetExtraDim -> "Extra Dim: ${if (action.on) "On" else "Off"}"
    is Action.SetScreenTimeout -> "Screen Timeout: ${action.timeoutMs}ms"
    is Action.SetGlyph -> "Glyph: ${if (action.on) "On" else "Off"}"
    is Action.SetGlyphMatrix -> "Glyph Matrix: ${if (action.restore) "Restore" else "Set"}"
    is Action.GlyphAnimate -> "Glyph Animate: ${action.zone ?: "all"} ${action.periodMs}ms x${action.cycles}"
    is Action.GlyphProgress -> "Glyph Progress: ${action.progress}%"
    is Action.GlyphText -> "Glyph Text: ${action.text.take(30)}"
    is Action.GlyphScrollingText -> "Glyph Scroll: ${action.text.take(30)}"
    is Action.GlyphPreset -> "Glyph Preset: ${action.preset}"
    is Action.GlyphTurnOff -> "Glyph Off"
    is Action.CopyText -> "Copy: ${action.text.take(30)}"
    is Action.Wait -> "Wait: ${action.durationMs}ms"
    is Action.WriteSetting -> "Write: ${action.namespace.name.lowercase()}/${action.key}=${action.value}"
    is Action.SetAutoRotate -> "Auto-rotate: ${if (action.on) "On" else "Off"}"
    is Action.SetBatterySaver -> "Battery Saver: ${if (action.on) "On" else "Off"}"
    is Action.SetAirplaneMode -> "Airplane Mode: ${if (action.on) "On" else "Off"}"
    is Action.SetDataSaver -> "Data Saver: ${if (action.on) "On" else "Off"}"
    is Action.SetHotspot -> "Hotspot: ${if (action.on) "On" else "Off"}"
    is Action.SetNfc -> "NFC: ${if (action.on) "On" else "Off"}"
    is Action.SetRefreshRate -> "Refresh Rate: ${action.hz}Hz"
    is Action.SetScreenRotation -> "Rotation: ${action.orientation.name.lowercase()}"
    is Action.MediaControl -> "Media: ${action.command.name.lowercase().replace("_", " ")}"
    is Action.SendSms -> "SMS to ${action.number}"
    is Action.LockScreen -> "Lock screen"
    is Action.SetLocationMode -> "Location: ${action.mode.name.lowercase().replace("_", " ")}"
    is Action.SetAutoSync -> "Auto-sync: ${if (action.on) "On" else "Off"}"
    is Action.ClearNotifications -> "Clear notifications"
    is Action.SetAlwaysOnDisplay -> "AOD: ${if (action.on) "On" else "Off"}"
    is Action.TakeScreenshot -> "Screenshot"
}).uppercase()
