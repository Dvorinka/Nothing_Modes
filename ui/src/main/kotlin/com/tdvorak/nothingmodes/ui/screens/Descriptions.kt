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
    val dayLabel =
        when (dayOfWeek) {
            "*" -> "Daily"
            "1-5" -> "Weekdays"
            "0,6", "6,0" -> "Weekend"
            "5,6" -> "Fri, Sat"
            else ->
                if (dayOfWeek.contains(",")) {
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

/** Human label for enum-style names: INCOMING_CALL_ENDED -> "Incoming call ended". */
internal fun String.enumLabel(): String =
    lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

/** Display labels for every entry of an enum, e.g. for pickers. */
internal inline fun <reified E : Enum<E>> enumLabelList(): List<String> =
    enumValues<E>().map { it.name.enumLabel() }

/** Reverse lookup for [enumLabelList]. */
internal inline fun <reified E : Enum<E>> enumByLabel(label: String): E =
    enumValues<E>().firstOrNull { it.name.enumLabel() == label }
        ?: enumValues<E>().first()

/** Friendly DND mode names — "priority or total" reads badly raw. */
internal fun com.tdvorak.nothingmodes.engine.model.DndMode.displayName(): String =
    when (this) {
        com.tdvorak.nothingmodes.engine.model.DndMode.OFF -> "Off"
        com.tdvorak.nothingmodes.engine.model.DndMode.PRIORITY -> "Priority only"
        com.tdvorak.nothingmodes.engine.model.DndMode.TOTAL -> "Total silence"
    }

private fun dayName(day: String): String =
    when (day) {
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

fun triggerDescription(trigger: Trigger): String =
    (
        when (trigger) {
            is Trigger.Time -> {
                trigger.at?.let { "Once · ${it.take(16).replace("T", " ")}" }
                    ?: trigger.cron?.let { cronToSummary(it) }
                    ?: trigger.afterMs?.let { "In ${it / 1000}s" }
                    ?: "Time-based"
            }
            is Trigger.TimeWindow -> "${trigger.startLocal}–${trigger.endLocal}"
            is Trigger.Immediate -> "Immediate"
            is Trigger.Notification -> "Notification from ${trigger.pkg}"
            is Trigger.PhoneState -> "Phone: ${trigger.event.name.enumLabel()}"
            is Trigger.Connectivity -> "${trigger.medium.name.enumLabel()} ${trigger.state.name}"
            is Trigger.Boot -> "On boot"
            is Trigger.BatteryLevel -> "Battery at ${trigger.level}%"
            is Trigger.ScreenStateTrigger -> "Screen ${trigger.state}"
            is Trigger.AppOpened -> "App opened: ${trigger.pkg}"
            is Trigger.Geofence -> "Geofence (${trigger.lat}, ${trigger.lng}) r=${trigger.radiusM}m"
            is Trigger.Manual -> "Manual"
            is Trigger.BluetoothDevice -> "BT device ${trigger.state}${trigger.deviceName?.let { ": $it" } ?: ""}"
            is Trigger.WifiConnected -> "WiFi connected${trigger.ssid?.let { ": $it" } ?: ""}"
            is Trigger.CalendarEvent -> "Calendar ${trigger.direction.name.lowercase()}${trigger.titleMatch?.let { ": $it" } ?: ""}"
            is Trigger.ChargerConnected -> "${if (trigger.connected) "Charger connected" else "Charger unplugged"}${trigger.source?.let { " (${it.name.lowercase()})" } ?: ""}"
            is Trigger.DeviceUnlocked -> "Device unlocked"
            is Trigger.DeviceLocked -> "Device locked"
        }
    ).uppercase()

fun actionDescription(action: Action): String =
    (
        when (action) {
            is Action.SetWifi -> "Wi-Fi: ${if (action.on) "On" else "Off"}"
            is Action.SetBluetooth -> "Bluetooth: ${if (action.on) "On" else "Off"}"
            is Action.SetMobileData -> "Mobile Data: ${if (action.on) "On" else "Off"}"
            is Action.SetDnd -> "DND: ${action.mode.displayName()}"
            is Action.SetRinger -> "Ringer: ${action.mode.replaceFirstChar { it.uppercase() }}"
            is Action.LaunchApp -> if (action.pkg.isBlank()) "Launch app" else "Launch: ${action.pkg}"
            is Action.OpenUrl -> if (action.url.isBlank()) "Open URL" else "Open URL: ${action.url}"
            is Action.ShowNotification -> if (action.title.isBlank()) "Show notification" else "Notification: ${action.title}"
            is Action.SetVolume -> {
                if (action.volumes.isEmpty()) {
                    "Volume: none set"
                } else {
                    action.volumes.entries.joinToString(
                        prefix = "Volume ",
                        transform = { "${it.key.name.enumLabel()} ${it.value}" },
                    )
                }
            }
            is Action.SetFlashlight -> "Flashlight: ${if (action.on) "On" else "Off"}"
            is Action.SetDarkMode -> "Dark Mode: ${action.mode.name.enumLabel()}"
            is Action.OpenSettingsScreen -> "Open Settings: ${action.screen.name.enumLabel()}"
            is Action.Vibrate -> {
                val label = vibratePresets.firstOrNull { it.second == action.durationMs }?.first ?: "${action.durationMs}ms"
                "Vibrate: $label"
            }
            is Action.SetBrightness -> "Brightness: ${action.level}"
            is Action.SetAutoBrightness -> "Auto Brightness: ${if (action.on) "On" else "Off"}"
            is Action.SetExtraDim -> "Extra Dim: ${if (action.on) "On" else "Off"}"
            is Action.SetScreenTimeout -> {
                val label = screenTimeoutPresets.firstOrNull { it.second == action.timeoutMs }?.first ?: "${action.timeoutMs}ms"
                "Screen Timeout: $label"
            }
            is Action.SetGlyph -> "Glyph: ${if (action.on) "On" else "Off"}"
            is Action.SetGlyphMatrix -> "Glyph Matrix: ${if (action.restore) "Restore" else "Set"}"
            is Action.GlyphAnimate -> "Glyph Animate: ${action.zone ?: "all"} ${action.periodMs}ms x${action.cycles}"
            is Action.GlyphProgress -> "Glyph Progress: ${action.progress}%"
            is Action.GlyphText -> "Glyph Text: ${action.text.take(30)}"
            is Action.GlyphScrollingText -> "Glyph Scroll: ${action.text.take(30)}"
            is Action.GlyphPreset -> "Glyph Preset: ${action.preset}"
            is Action.GlyphIcon -> "Glyph Icon: ${action.name}"
            is Action.GlyphNumber -> "Glyph Number: ${action.number}"
            is Action.GlyphCountdown -> "Glyph Countdown: ${action.seconds}s"
            is Action.GlyphMusic -> "Glyph Music: ${action.style}"
            is Action.GlyphTurnOff -> "Glyph Off"
            is Action.CopyText -> if (action.text.isBlank()) "Copy text" else "Copy: ${action.text.take(30)}"
            is Action.Wait -> "Wait: ${formatDuration(action.durationMs)}"
            is Action.WriteSetting -> "Write: ${action.namespace.name.enumLabel()}/${action.key}=${action.value}"
            is Action.SetAutoRotate -> "Auto-rotate: ${if (action.on) "On" else "Off"}"
            is Action.SetBatterySaver -> "Battery Saver: ${if (action.on) "On" else "Off"}"
            is Action.SetAirplaneMode -> "Airplane Mode: ${if (action.on) "On" else "Off"}"
            is Action.SetDataSaver -> "Data Saver: ${if (action.on) "On" else "Off"}"
            is Action.SetHotspot -> "Hotspot: ${if (action.on) "On" else "Off"}"
            is Action.SetNfc -> "NFC: ${if (action.on) "On" else "Off"}"
            is Action.SetRefreshRate -> {
                val label = refreshRatePresets.firstOrNull { it.second == action.hz }?.first ?: "${action.hz}Hz"
                "Refresh Rate: $label"
            }
            is Action.SetScreenRotation -> "Rotation: ${action.orientation.name.enumLabel()}"
            is Action.MediaControl -> "Media: ${action.command.name.enumLabel()}"
            is Action.SendSms -> if (action.number.isBlank()) "Send SMS" else "SMS to ${action.number}"
            is Action.LockScreen -> "Lock screen"
            is Action.SetLocationMode -> "Location: ${action.mode.name.enumLabel()}"
            is Action.SetAutoSync -> "Auto-sync: ${if (action.on) "On" else "Off"}"
            is Action.ClearNotifications -> "Clear notifications"
            is Action.SetAlwaysOnDisplay -> "AOD: ${action.mode.name.enumLabel()}"
            is Action.TakeScreenshot -> "Screenshot"
        }
    ).uppercase()

private fun formatDuration(ms: Long): String =
    when {
        ms < 1_000 -> "${ms}ms"
        ms % 60_000 == 0L -> "${ms / 60_000}m"
        ms < 60_000 -> "${ms / 1_000}s"
        else -> "${ms / 60_000}m ${(ms % 60_000) / 1_000}s"
    }

/**
 * One-line requirement/behavior note shown at the top of the action config
 * sheet. Keeps users from discovering silent failures at run time.
 */
fun actionRequirementHint(action: Action): String? =
    when (action) {
        is Action.SetGlyph, is Action.SetGlyphMatrix, is Action.GlyphAnimate,
        is Action.GlyphProgress, is Action.GlyphText, is Action.GlyphScrollingText,
        is Action.GlyphPreset, is Action.GlyphIcon, is Action.GlyphNumber,
        is Action.GlyphCountdown, is Action.GlyphMusic,
        ->
            "Needs a Nothing phone. Output stays lit until a \"Glyph off\" action runs or the mode ends."

        is Action.GlyphTurnOff ->
            "Clears anything currently on the Glyph."

        is Action.SetWifi, is Action.SetBluetooth, is Action.SetMobileData,
        is Action.SetAirplaneMode, is Action.SetHotspot, is Action.SetNfc,
        is Action.SetDataSaver, is Action.SetAutoSync, is Action.SetBatterySaver,
        is Action.SetAlwaysOnDisplay, is Action.SetLocationMode,
        ->
            "Silent toggle needs Shizuku — without it, the matching system panel opens for one tap."

        is Action.SetBrightness, is Action.SetAutoBrightness, is Action.SetExtraDim,
        is Action.SetScreenTimeout, is Action.SetAutoRotate, is Action.SetScreenRotation,
        is Action.SetRefreshRate, is Action.SetDarkMode,
        ->
            "Needs the Write Settings permission (Settings → Permissions)."

        is Action.SetDnd -> "Needs Do-Not-Disturb access (Settings → Permissions)."
        is Action.ShowNotification -> "Needs the notification permission."
        is Action.SendSms -> "Needs the SMS permission."
        is Action.LockScreen -> "Needs device admin — enable it in Settings."
        is Action.WriteSetting -> "Advanced. Secure and global keys need Shizuku."
        is Action.TakeScreenshot -> "Not supported on most devices yet."
        is Action.SetFlashlight -> "Toggles the camera flashlight."
        is Action.SetRinger -> "Changes how calls and notifications ring."
        is Action.SetVolume -> "Adjusts the selected volume stream."
        is Action.LaunchApp -> "Opens the selected app."
        is Action.OpenUrl -> "Opens a URL in the default browser."
        is Action.OpenSettingsScreen -> "Opens a specific Settings screen."
        is Action.CopyText -> "Copies text to the clipboard."
        is Action.Wait -> "Waits the given duration before the next action runs."
        is Action.Vibrate -> "Vibrates the phone for the selected duration."
        is Action.MediaControl -> "Sends a media playback command."
        is Action.ClearNotifications -> "Clears all status bar notifications."
        else -> null
    }
