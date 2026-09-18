package com.tdvorak.nothingmodes.ui.screens

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.ChargerSource
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.DeviceStateKeys
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.ui.util.DisplayUnits

/** Flattens a stored condition tree into display rows: `And` unwraps to its
 *  members (nested `Or`/`Not` stay as single rows describing themselves). */
fun flattenConditions(condition: Condition?): List<Condition> =
    when (condition) {
        null -> emptyList()
        is Condition.And -> condition.all.flatMap { flattenConditions(it) }
        else -> listOf(condition)
    }

fun cronToSummary(
    cron: String,
    units: DisplayUnits,
): String {
    val parts = cron.split(" ").filter { it.isNotBlank() }
    if (parts.size < 5) return cron

    val minute = parts[0]
    val hour = parts[1]
    val dayOfMonth = parts[2]
    val month = parts[3]
    val dayOfWeek = parts[4]

    if (hour == "*" || minute == "*") return "Every minute"

    val time =
        units.formatLocalTime(
            "${hour.padStart(2, '0')}:${minute.padStart(2, '0')}",
        )
    val dayLabel =
        when {
            dayOfMonth != "*" && month != "*" -> {
                val months = month.split(",").map { monthName(it) }.joinToString(", ")
                val days = dayOfMonth.split(",").joinToString(", ") { if (it.trim() == "L") "last day of $months" else "${it.trim()}.$months" }
                days
            }
            dayOfMonth != "*" -> {
                val tokens = dayOfMonth.split(",").map { it.trim() }
                val labels = tokens.map { if (it == "L") "last day" else it }
                "Monthly on ${labels.joinToString(", ")}"
            }
            dayOfWeek == "*" -> "Daily"
            dayOfWeek == "1-5" -> "Weekdays"
            dayOfWeek == "0,6" || dayOfWeek == "6,0" -> "Weekend"
            dayOfWeek == "5,6" -> "Fri, Sat"
            dayOfWeek.contains(",") -> dayOfWeek.split(",").map { dayName(it.trim()) }.joinToString(", ")
            dayOfWeek.contains("-") -> {
                val range = dayOfWeek.split("-")
                "${dayName(range[0])}-${dayName(range[1])}"
            }
            else -> dayName(dayOfWeek)
        }
    return "$dayLabel at $time"
}

private fun monthName(month: String): String =
    java.time.Month
        .of(month.trim().toIntOrNull()?.coerceIn(1, 12) ?: 1)
        .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())

/** Human label for enum-style names: INCOMING_CALL_ENDED -> "Incoming call ended". */
internal fun String.enumLabel(): String =
    when (this) {
        "WIFI" -> "Wi-Fi"
        "SSID" -> "Wi-Fi name"
        "AOD" -> "Always-on display"
        "DND" -> "Do Not Disturb"
        "SMS" -> "SMS"
        "BT" -> "Bluetooth"
        "NFC" -> "NFC"
        else -> lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

/** Display labels for every entry of an enum, e.g. for pickers. */
internal inline fun <reified E : Enum<E>> enumLabelList(): List<String> = enumValues<E>().map { it.name.enumLabel() }

/** Reverse lookup for [enumLabelList]. */
internal inline fun <reified E : Enum<E>> enumByLabel(label: String): E =
    enumValues<E>().firstOrNull { it.name.enumLabel() == label }
        ?: enumValues<E>().first()

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

/** Friendly label for a DeviceState key — shared by catalog rows and summaries. */
fun deviceStateLabel(key: String): String =
    when (key) {
        DeviceStateKeys.POWER_SAVING -> "Power saving"
        DeviceStateKeys.DND_ACTIVE -> "Do Not Disturb"
        DeviceStateKeys.RINGER_MODE -> "Ringer mode"
        DeviceStateKeys.VOLUME_MEDIA -> "Media volume"
        DeviceStateKeys.VOLUME_RING -> "Ring volume"
        DeviceStateKeys.VOLUME_ALARM -> "Alarm volume"
        DeviceStateKeys.HEADPHONES -> "Headphones"
        DeviceStateKeys.NFC -> "NFC"
        DeviceStateKeys.LOCATION -> "Location"
        DeviceStateKeys.DATA_SAVER -> "Data saver"
        DeviceStateKeys.HOTSPOT -> "Hotspot"
        DeviceStateKeys.WIFI_RADIO -> "Wi-Fi radio"
        DeviceStateKeys.BLUETOOTH_RADIO -> "Bluetooth radio"
        DeviceStateKeys.AIRPLANE -> "Airplane mode"
        DeviceStateKeys.MOBILE_DATA -> "Mobile data"
        DeviceStateKeys.AUTO_ROTATE -> "Auto-rotate"
        DeviceStateKeys.AOD -> "Always-on display"
        DeviceStateKeys.DARK_MODE -> "Dark mode"
        DeviceStateKeys.CHARGING_STATUS -> "Charging status"
        DeviceStateKeys.CHARGING_LIMIT -> "Charge limit"
        DeviceStateKeys.BATTERY_SHARE -> "Battery share"
        DeviceStateKeys.BATTERY_SHARE_LIMIT -> "Battery share limit"
        DeviceStateKeys.GLYPH_INTERFACE -> "Glyph interface"
        DeviceStateKeys.GLYPH_CHARGE_LED -> "Glyph charge LED"
        DeviceStateKeys.THERMAL -> "Thermal status"
        else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

/** Display label for a DeviceState value — booleans read as On/Off. */
fun deviceStateValueLabel(
    key: String,
    value: String,
): String =
    when {
        value == DeviceStateKeys.ANY_VALUE -> "Any change"
        key == DeviceStateKeys.THERMAL -> thermalStatusLabel(value)
        value == "true" -> "On"
        value == "false" -> "Off"
        else -> value.replaceFirstChar { it.uppercase() }
    }

/** PowerManager.THERMAL_STATUS_* — severity levels, not temperatures. */
private fun thermalStatusLabel(value: String): String =
    when (value) {
        "0" -> "None"
        "1" -> "Light"
        "2" -> "Moderate"
        "3" -> "Severe"
        "4" -> "Critical"
        "5" -> "Emergency"
        "6" -> "Shutdown"
        else -> value
    }

/** Shared description functions for triggers and actions. */

/** Short "what it does" line for a trigger type shown in the picker — unlike
 *  [triggerDescription] it does not leak the default config values. */
fun triggerTypeDescription(trigger: Trigger): String =
    when (trigger) {
        is Trigger.Time -> "Runs at a set time or interval"
        is Trigger.TimeWindow -> "Active between two times of day"
        is Trigger.Immediate -> "Runs when you tap Run"
        is Trigger.Notification -> "When a matching notification arrives"
        is Trigger.PhoneState -> "On call, SMS, and dialer events"
        is Trigger.Connectivity -> "When connectivity changes"
        is Trigger.Boot -> "When the phone finishes booting"
        is Trigger.BatteryLevel -> "When battery crosses a level"
        is Trigger.ScreenStateTrigger -> "When the screen turns on or off"
        is Trigger.AppOpened -> "When an app is opened"
        is Trigger.Geofence -> "When you enter or leave an area"
        is Trigger.Manual -> "Runs when you tap Run"
        is Trigger.BluetoothDevice -> "When a Bluetooth device connects"
        is Trigger.WifiConnected -> "When a Wi-Fi network connects"
        is Trigger.CalendarEvent -> "Around calendar events"
        is Trigger.ChargerConnected -> "When the charger connects or unplugs"
        is Trigger.DeviceUnlocked -> "When the device is unlocked"
        is Trigger.DeviceLocked -> "When the device is locked"
        is Trigger.TorchState -> "When the flashlight toggles"
        is Trigger.MediaPlayback -> "When media starts or stops"
        is Trigger.DeviceState -> "While ${deviceStateLabel(trigger.key).lowercase()} matches a state"
    }

fun triggerDescription(
    trigger: Trigger,
    units: DisplayUnits,
    appLabel: (String) -> String = { it },
): String =
    (
        when (trigger) {
            is Trigger.Time -> {
                trigger.at?.let { "Once · ${units.formatIsoMinute(it)}" }
                    ?: trigger.cron?.let { cronToSummary(it, units) }
                    ?: trigger.afterMs?.let { "In ${it / 1000}s" }
                    ?: "Time-based"
            }
            is Trigger.TimeWindow ->
                "${units.formatLocalTime(trigger.startLocal)}–${units.formatLocalTime(trigger.endLocal)}"
            is Trigger.Immediate -> "Immediate"
            is Trigger.Notification -> "Notification from ${appLabel(trigger.pkg)}"
            is Trigger.PhoneState -> "Phone: ${trigger.event.name.enumLabel()}"
            is Trigger.Connectivity -> "${trigger.medium.name.enumLabel()} ${trigger.state.name}"
            is Trigger.Boot -> "On boot"
            is Trigger.BatteryLevel -> "Battery at ${trigger.level}%"
            is Trigger.ScreenStateTrigger -> "Screen ${trigger.state}"
            is Trigger.AppOpened -> "App opened: ${appLabel(trigger.pkg)}"
            is Trigger.Geofence ->
                "Area · ${String.format("%.4f", trigger.lat)}, " +
                    "${String.format("%.4f", trigger.lng)} · ${units.formatDistance(trigger.radiusM)}"
            is Trigger.Manual -> "Manual"
            is Trigger.BluetoothDevice -> "Bluetooth device ${trigger.state.name.enumLabel().lowercase()}${trigger.deviceName?.let { ": $it" } ?: ""}"
            is Trigger.WifiConnected -> "Wi-Fi connected${trigger.ssid?.let { ": $it" } ?: ""}"
            is Trigger.CalendarEvent ->
                "Calendar ${trigger.direction.name.lowercase()}" +
                    if (trigger.events.isNotEmpty()) {
                        ": ${trigger.events.first().title}" +
                            (if (trigger.events.size > 1) " +${trigger.events.size - 1} more" else "")
                    } else {
                        trigger.titleMatch?.let { ": $it" } ?: ""
                    }
            is Trigger.ChargerConnected -> "${if (trigger.connected) "Charger connected" else "Charger unplugged"}${trigger.source?.let { " (${it.name.lowercase()})" } ?: ""}"
            is Trigger.DeviceUnlocked -> "Device unlocked"
            is Trigger.DeviceLocked -> "Device locked"
            is Trigger.TorchState -> "Flashlight ${if (trigger.on) "on" else "off"}"
            is Trigger.MediaPlayback -> "Media ${if (trigger.playing) "playing" else "stopped"}${trigger.packageName?.let { ": ${appLabel(it)}" } ?: ""}"
            is Trigger.DeviceState ->
                "${deviceStateLabel(trigger.key)} = ${deviceStateValueLabel(trigger.key, trigger.value)}"
        }
    ).uppercase()

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

        is Action.SetGlyphInterface ->
            "Enables or disables the whole Glyph interface (Nothing OS master switch). Needs Shizuku."

        is Action.SetWifi ->
            "Turns Wi-Fi on or off. Silent toggling needs Shizuku; without it the system Wi-Fi panel opens for one tap."

        is Action.SetBluetooth ->
            "Turns Bluetooth on or off. Silent toggling needs Shizuku; without it the system Bluetooth panel opens for one tap."

        is Action.SetMobileData ->
            "Turns mobile data on or off. Silent toggling needs Shizuku; without it the system data panel opens for one tap."

        is Action.SetAirplaneMode ->
            "Turns airplane mode on or off. Silent toggling needs Shizuku; without it the system panel opens for one tap."

        is Action.SetHotspot ->
            "Turns the mobile hotspot on or off. Silent toggling needs Shizuku; without it the system panel opens for one tap."

        is Action.SetNfc ->
            "Turns NFC on or off. Silent toggling needs Shizuku; without it the system NFC panel opens for one tap."

        is Action.SetDataSaver ->
            "Turns data saver on or off. Silent toggling needs Shizuku; without it the system panel opens for one tap."

        is Action.SetAutoSync ->
            "Turns auto-sync on or off. Silent toggling needs Shizuku; without it the system panel opens for one tap."

        is Action.SetBatterySaver ->
            "Turns battery saver on or off. Silent toggling needs Shizuku; without it the system panel opens for one tap."

        is Action.SetAlwaysOnDisplay ->
            "Turns Always-on Display on or off. Silent toggling needs Shizuku; without it the system panel opens for one tap."

        is Action.SetLocationMode ->
            "Changes the device location mode. Silent toggling needs Shizuku; without it the system location panel opens for one tap."

        is Action.SetStayAwake ->
            "Keeps the screen on while the device is charging. Needs Shizuku."

        is Action.SetNightLight ->
            "Turns Night Light on or off, optionally setting warmth. Silent toggling needs Shizuku; without it the Night Light settings page opens."

        is Action.SetColorInversion ->
            "Inverts every color on the display. Silent toggling needs Shizuku; without it the accessibility settings page opens."

        is Action.SetDaltonizer ->
            "Turns color correction (daltonizer) on or off. Silent toggling needs Shizuku; without it the accessibility settings page opens."

        is Action.SetOneHandedMode ->
            "Turns one-handed mode on or off. Silent toggling needs Shizuku; without it the display settings page opens."

        is Action.SetSensorPrivacy ->
            "Blocks or unblocks the microphone or camera device-wide. Needs Shizuku; without it the privacy settings page opens."

        is Action.SetFontScale ->
            "Needs the Write Settings permission (Settings → Permissions)."

        is Action.SetAlarm, is Action.SetTimer ->
            "Hands the request to the default clock app. While locked, it runs right after you unlock."

        is Action.OpenClock ->
            "Opens the default clock app. While locked, it runs right after you unlock."

        is Action.SetBrightness, is Action.SetAutoBrightness, is Action.SetExtraDim,
        is Action.SetScreenTimeout, is Action.SetAutoRotate, is Action.SetScreenRotation,
        is Action.SetRefreshRate, is Action.SetDarkMode,
        ->
            "Needs the Write Settings permission (Settings → Permissions)."

        is Action.SetUltraDim ->
            "Dims the screen below the minimum with a dark overlay layer. Needs 'Display over other apps' (Settings → Permissions)."

        is Action.SetDnd -> "Needs Do-Not-Disturb access (Settings → Permissions)."
        is Action.ShowNotification -> "Needs the notification permission."
        is Action.SendSms -> "Needs the SMS permission."
        is Action.LockScreen -> "Needs device admin — enable it in Settings. Toggle override to try anyway."
        is Action.WriteSetting -> "Advanced. Secure and global keys need Shizuku."
        is Action.TakeScreenshot -> "Detected: may not work on this device. Toggle override to try anyway."
        is Action.SetFlashlight -> "Toggles the camera flashlight."
        is Action.SetWallpaper -> "Sets the home or lock-screen wallpaper from an image you pick."
        is Action.SetRinger -> "Changes how calls and notifications ring."
        is Action.SetVolume -> "Adjusts the selected volume stream."
        is Action.LaunchApp -> "Opens the selected app(s) in the foreground."
        is Action.OpenUrl -> "Opens a URL in the default browser or a chosen app."
        is Action.OpenSettingsScreen -> "Opens a specific Android Settings screen."
        is Action.CopyText -> "Copies text to the clipboard."
        is Action.Wait -> "Waits the given duration before the next action runs."
        is Action.Vibrate -> "Vibrates the phone for the selected duration."
        is Action.MediaControl -> "Sends a media playback command."
        is Action.ClearNotifications -> "Clears all status bar notifications."
        else -> null
    }

/** Short feature description shown on catalog rows once the required
 *  capability is already satisfied — replaces the bare "Needs …" hint
 *  which would otherwise read as if the permission were still missing. */
fun actionFeatureDescription(action: Action): String? =
    when (action) {
        is Action.SetBrightness -> "Sets screen brightness."
        is Action.SetAutoBrightness -> "Toggles adaptive brightness."
        is Action.SetExtraDim -> "Dims the display below the usual minimum."
        is Action.SetUltraDim -> "Dims the display below the hardware minimum."
        is Action.SetScreenTimeout -> "Sets the screen-off timeout."
        is Action.SetAutoRotate -> "Toggles auto-rotation."
        is Action.SetScreenRotation -> "Forces a screen orientation."
        is Action.SetRefreshRate -> "Sets the display refresh rate."
        is Action.SetDarkMode -> "Switches dark mode on or off."
        is Action.SetStayAwake -> "Keeps the screen on while charging."
        is Action.SetDnd -> "Changes Do-Not-Disturb mode."
        is Action.ShowNotification -> "Posts a notification."
        is Action.SendSms -> "Sends an SMS message."
        is Action.LockScreen -> "Locks the device screen."
        is Action.TakeScreenshot -> "Captures the screen."
        is Action.WriteSetting -> "Writes a system, secure, or global key."
        is Action.GlyphPreset -> "Flashes a named glyph animation."
        is Action.GlyphText -> "Shows static text on the Glyph Matrix."
        is Action.GlyphScrollingText -> "Scrolls text across the Glyph Matrix."
        is Action.GlyphIcon -> "Shows an icon on the Glyph Matrix."
        is Action.GlyphNumber -> "Shows a number on the Glyph Matrix."
        is Action.GlyphCountdown -> "Runs a live countdown on the Glyph Matrix."
        is Action.GlyphMusic -> "Reacts to playing audio on the Glyph Matrix."
        is Action.GlyphProgress -> "Fills the Glyph like a progress bar."
        is Action.GlyphAnimate -> "Blinks a glyph zone in a loop."
        is Action.SetGlyph -> "Turns the light strips on or off."
        is Action.SetGlyphMatrix -> "Draws a pattern on the Glyph Matrix."
        is Action.GlyphTurnOff -> "Clears whatever is on the Glyph."
        is Action.SetGlyphInterface -> "Switches the whole Glyph interface on or off."
        is Action.SetWifi -> "Turns Wi-Fi on or off."
        is Action.SetBluetooth -> "Turns Bluetooth on or off."
        is Action.SetMobileData -> "Turns mobile data on or off."
        is Action.SetAirplaneMode -> "Turns airplane mode on or off."
        is Action.SetHotspot -> "Turns the mobile hotspot on or off."
        is Action.SetNfc -> "Turns NFC on or off."
        is Action.SetDataSaver -> "Turns data saver on or off."
        is Action.SetAutoSync -> "Turns auto-sync on or off."
        is Action.SetBatterySaver -> "Turns battery saver on or off."
        is Action.SetAlwaysOnDisplay -> "Turns Always-on Display on or off."
        is Action.SetLocationMode -> "Changes the device location mode."
        is Action.SetNightLight -> "Turns Night Light on or off."
        is Action.SetColorInversion -> "Inverts every color on the display."
        is Action.SetDaltonizer -> "Turns color correction on or off."
        is Action.SetOneHandedMode -> "Turns one-handed mode on or off."
        is Action.SetSensorPrivacy -> "Blocks or unblocks a privacy sensor."
        is Action.SetFontScale -> "Sets the system font scale."
        is Action.SetAlarm -> "Creates an alarm in the clock app."
        is Action.SetTimer -> "Starts a countdown timer."
        is Action.OpenClock -> "Opens the clock app."
        is Action.SetFlashlight -> "Toggles the camera flashlight."
        is Action.SetRinger -> "Changes how calls and notifications ring."
        is Action.SetVolume -> "Adjusts the selected volume stream."
        is Action.LaunchApp -> "Opens the selected app(s) in the foreground."
        is Action.OpenUrl -> "Opens a URL in the default browser or a chosen app."
        is Action.OpenSettingsScreen -> "Opens a specific Android Settings screen."
        is Action.CopyText -> "Copies text to the clipboard."
        is Action.Wait -> "Waits the given duration before the next action runs."
        is Action.Vibrate -> "Vibrates the phone for the selected duration."
        is Action.MediaControl -> "Sends a media playback command."
        is Action.ClearNotifications -> "Clears all status bar notifications."
        is Action.SetWallpaper -> "Sets the home or lock-screen wallpaper."
        else -> null
    }

fun chargerSourceDescription(source: ChargerSource): String =
    when (source) {
        ChargerSource.AC -> "a wall charger or mains adapter"
        ChargerSource.USB -> "a cable plugged into a computer, hub, or other USB host"
        ChargerSource.WIRELESS -> "a charging pad or stand — no cable attached"
        ChargerSource.DOCK -> "a desk or car dock that supplies power without a USB plug"
    }
