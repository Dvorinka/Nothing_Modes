package com.tdvorak.nothingmodes.engine.model

/**
 * Shared one-line action descriptions, used by the UI lists and by
 * run/notification summaries outside the UI module.
 */

/** Friendly DND mode names — "priority or total" reads badly raw. */
fun DndMode.displayName(): String =
    when (this) {
        DndMode.OFF -> "Off"
        DndMode.PRIORITY -> "Priority only"
        DndMode.TOTAL -> "Total silence"
    }

val screenTimeoutPresets =
    listOf(
        "15 seconds" to 15_000,
        "30 seconds" to 30_000,
        "1 minute" to 60_000,
        "2 minutes" to 120_000,
        "5 minutes" to 300_000,
        "10 minutes" to 600_000,
        "30 minutes" to 1_800_000,
        "Never" to Int.MAX_VALUE,
    )

val vibratePresets =
    listOf(
        "Short" to 100,
        "Medium" to 300,
        "Long" to 500,
        "1 second" to 1_000,
    )

val refreshRatePresets =
    listOf(
        "60 Hz" to 60,
        "90 Hz" to 90,
        "120 Hz" to 120,
        "144 Hz" to 144,
    )

private fun String.enumLabel(): String = lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }

private fun formatDuration(ms: Long): String =
    when {
        ms < 1_000 -> "${ms}ms"
        ms % 60_000 == 0L -> "${ms / 60_000}m"
        ms < 60_000 -> "${ms / 1_000}s"
        else -> "${ms / 60_000}m ${(ms % 60_000) / 1_000}s"
    }

fun actionDescription(action: Action): String =
    (
        when (action) {
            is Action.SetWifi -> "Wi-Fi: ${if (action.on) "On" else "Off"}"
            is Action.SetBluetooth -> "Bluetooth: ${if (action.on) "On" else "Off"}"
            is Action.SetMobileData -> "Mobile Data: ${if (action.on) "On" else "Off"}"
            is Action.SetDnd -> "DND: ${action.mode.displayName()}"
            is Action.SetRinger -> "Ringer: ${action.mode.replaceFirstChar { it.uppercase() }}"
            is Action.LaunchApp -> if (action.packages.isEmpty()) "Launch app" else "Launch: ${action.packages.size} app(s)"
            is Action.OpenUrl -> {
                val prefix = if (action.packageName != null) "Open in ${action.packageName}: " else "Open URL: "
                if (action.url.isBlank()) "Open URL" else prefix + action.url
            }
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
            is Action.SetWallpaper -> "Wallpaper: ${action.which} (${action.uri.take(30)})"
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
            is Action.SetGlyphInterface -> "Glyph interface: ${if (action.on) "On" else "Off"}"
            is Action.CopyText -> if (action.text.isBlank()) "Copy text" else "Copy: ${action.text.take(30)}"
            is Action.Wait -> "Wait: ${formatDuration(action.durationMs)}"
            is Action.WriteSetting -> "Write: ${action.namespace.name.enumLabel()}/${action.key}=${action.value}"
            is Action.SetAutoRotate -> "Auto-rotate: ${if (action.on) "On" else "Off"}"
            is Action.SetBatterySaver -> "Battery Saver: ${if (action.on) "On" else "Off"}"
            is Action.SetAirplaneMode -> "Airplane Mode: ${if (action.on) "On" else "Off"}"
            is Action.SetDataSaver -> "Data Saver: ${if (action.on) "On" else "Off"}"
            is Action.SetHotspot -> "Hotspot: ${if (action.on) "On" else "Off"}"
            is Action.SetNfc -> "NFC: ${if (action.on) "On" else "Off"}"
            is Action.SetStayAwake -> "Stay Awake: ${if (action.on) "On" else "Off"}"
            is Action.SetRefreshRate -> {
                val label = refreshRatePresets.firstOrNull { it.second == action.hz }?.first ?: "${action.hz}Hz"
                "Refresh Rate: $label"
            }
            is Action.SetScreenRotation -> "Rotation: ${action.orientation.name.enumLabel()}"
            is Action.MediaControl -> "Media: ${action.command.name.enumLabel()}"
            is Action.SendSms -> if (action.number.isBlank()) "Send SMS" else "SMS to ${action.number}"
            is Action.LockScreen -> "Lock screen" + if (action.force) " (override)" else ""
            is Action.SetLocationMode -> "Location: ${action.mode.name.enumLabel()}"
            is Action.SetAutoSync -> "Auto-sync: ${if (action.on) "On" else "Off"}"
            is Action.ClearNotifications -> "Clear notifications"
            is Action.SetAlwaysOnDisplay -> "AOD: ${action.mode.name.enumLabel()}"
            is Action.TakeScreenshot -> "Screenshot" + if (action.force) " (override)" else ""
            is Action.Group -> "${action.name}: ${action.actions.size} actions"
        }
    ).uppercase()
