package com.tdvorak.nothingmodes.ui.screens

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Trigger

/** Shared description functions for triggers and actions. */

fun triggerDescription(trigger: Trigger): String = when (trigger) {
    is Trigger.Time -> {
        val cron = trigger.cron
        val at = trigger.at
        when {
            cron != null -> "Schedule: $cron (${trigger.tz})"
            at != null -> "At: $at (${trigger.tz})"
            else -> "Time-based (${trigger.tz})"
        }
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
}

fun actionDescription(action: Action): String = when (action) {
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
}
