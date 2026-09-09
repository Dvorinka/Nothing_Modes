package com.tdvorak.nothingmodes.agent

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.ConnState
import com.tdvorak.nothingmodes.engine.model.PhoneEvent
import com.tdvorak.nothingmodes.engine.model.TimePrecision
import com.tdvorak.nothingmodes.engine.model.Trigger

/** Produces human-readable explanations of modes for the MCP / agent surface. */
object ModeExplainer {
    fun explainTrigger(trigger: Trigger): String =
        when (trigger) {
            is Trigger.Time -> {
                val what =
                    when {
                        trigger.at != null -> "at ${trigger.at}"
                        trigger.afterMs != null -> "after ${trigger.afterMs}ms"
                        trigger.cron != null -> "on cron ${trigger.cron}"
                        else -> "at some time"
                    }
                val days = trigger.days?.joinToString { it.name.lowercase() }
                buildString {
                    append("time fires $what (${trigger.tz})")
                    if (days != null) append(" on $days")
                    if (trigger.precision != TimePrecision.FLEXIBLE) append(", ${trigger.precision.name.lowercase()} precision")
                }
            }
            Trigger.Immediate -> "the mode is armed"
            is Trigger.TimeWindow -> {
                val days = trigger.days?.joinToString { it.name.lowercase() }
                buildString {
                    append("the time window ${trigger.startLocal}-${trigger.endLocal} (${trigger.tz}) is active")
                    if (days != null) append(" on $days")
                }
            }
            is Trigger.Notification -> {
                val parts = mutableListOf<String>()
                if (trigger.pkg.isNotBlank()) parts.add("from ${trigger.pkg}")
                if (trigger.sender?.isNotBlank() == true) parts.add("sender '${trigger.sender}'")
                if (trigger.titleMatch?.isNotBlank() == true) parts.add("title contains '${trigger.titleMatch}'")
                if (trigger.textMatch?.isNotBlank() == true) parts.add("text contains '${trigger.textMatch}'")
                if (trigger.conversationId?.isNotBlank() == true) parts.add("conversation ${trigger.conversationId}")
                if (trigger.isGroup == true) parts.add("group notification")
                if (parts.isEmpty()) "a notification is posted" else "a notification is posted ${parts.joinToString(", ")}"
            }
            is Trigger.PhoneState -> {
                val event = describePhoneEvent(trigger.event)
                buildString {
                    append("phone state is $event")
                    if (trigger.number?.isNotBlank() == true) append(" from number ${trigger.number}")
                    if (trigger.textMatch?.isNotBlank() == true) append(" with text containing '${trigger.textMatch}'")
                }
            }
            is Trigger.Connectivity -> {
                val state = describeConnState(trigger.state)
                val medium = describeConnMedium(trigger.medium)
                buildString {
                    append("$medium $state")
                    if (trigger.match?.isNotBlank() == true) append(" matching '${trigger.match}'")
                }
            }
            Trigger.Boot -> "the device finishes booting"
            is Trigger.BatteryLevel -> {
                val direction =
                    trigger.direction
                        ?.name
                        ?.lowercase()
                        ?.replace('_', ' ')
                buildString {
                    append("battery level is ${trigger.level}%")
                    if (direction != null) append(" and $direction")
                }
            }
            is Trigger.ScreenStateTrigger -> "the screen ${describeScreenState(trigger.state)}"
            is Trigger.AppOpened -> "${trigger.pkg} comes to the foreground"
            is Trigger.Geofence -> "the device ${trigger.transition.name.lowercase()}s the area around (${trigger.lat}, ${trigger.lng}, r=${trigger.radiusM}m)"
            Trigger.Manual -> "the user taps Run"
            is Trigger.BluetoothDevice -> {
                val state = describeConnState(trigger.state)
                buildString {
                    append("bluetooth device $state")
                    if (trigger.deviceName?.isNotBlank() == true) append(" named '${trigger.deviceName}'")
                    if (trigger.deviceAddress?.isNotBlank() == true) append(" [${trigger.deviceAddress}]")
                }
            }
            is Trigger.WifiConnected -> {
                if (trigger.ssid?.isNotBlank() == true) "the device connects to WiFi '${trigger.ssid}'" else "the device connects to any WiFi"
            }
            is Trigger.CalendarEvent -> {
                val direction = trigger.direction.name.lowercase()
                buildString {
                    append("calendar event $direction")
                    if (trigger.titleMatch?.isNotBlank() == true) append(" with title containing '${trigger.titleMatch}'")
                    if (trigger.calendarId?.isNotBlank() == true) append(" in calendar ${trigger.calendarId}")
                }
            }
            is Trigger.ChargerConnected -> {
                val source =
                    trigger.source
                        ?.name
                        ?.lowercase()
                        ?.replace('_', ' ')
                buildString {
                    append("charger is ${if (trigger.connected) "connected" else "disconnected"}")
                    if (source != null) append(" from $source source")
                }
            }
            Trigger.DeviceUnlocked -> "the user unlocks the device"
            Trigger.DeviceLocked -> "the device becomes locked"
            is Trigger.TorchState -> "the flashlight is turned ${if (trigger.on) "on" else "off"}"
            is Trigger.MediaPlayback -> {
                buildString {
                    append("media playback ${if (trigger.playing) "starts" else "stops"}")
                    if (trigger.packageName?.isNotBlank() == true) append(" in ${trigger.packageName}")
                }
            }
        }

    fun explainCondition(condition: Condition): String =
        when (condition) {
            is Condition.TimeWindow -> "time is between ${condition.startLocal} and ${condition.endLocal} (${condition.tz})"
            is Condition.DayOfWeekCondition -> "today is ${condition.days.joinToString { it.name.lowercase() }}"
            is Condition.BatteryLevel -> "battery level ${condition.op.name.lowercase()} ${condition.level}%"
            is Condition.Charging -> "device ${if (condition.isCharging) "is" else "is not"} charging"
            is Condition.WifiConnected -> if (condition.ssid?.isNotBlank() == true) "connected to WiFi '${condition.ssid}'" else "connected to any WiFi"
            is Condition.BluetoothConnected -> if (condition.deviceName?.isNotBlank() == true) "connected to Bluetooth '${condition.deviceName}'" else "connected to any Bluetooth"
            is Condition.ScreenStateCondition -> "the screen ${describeScreenState(condition.state)}"
            is Condition.CurrentModeActive -> "mode ${condition.modeId} is currently active"
            is Condition.AppInForeground -> "${condition.pkg} is in the foreground"
            is Condition.DarkModeActive -> "dark mode ${if (condition.active) "is" else "is not"} active"
            is Condition.PowerSaving -> "power saving ${if (condition.on) "is" else "is not"} on"
            is Condition.MediaPlaying -> "media ${if (condition.playing) "is" else "is not"} playing"
            is Condition.RingerMode -> "ringer mode is ${condition.mode}"
            is Condition.AirplaneModeOn -> "airplane mode ${if (condition.on) "is" else "is not"} on"
            is Condition.NfcEnabled -> "NFC ${if (condition.enabled) "is" else "is not"} enabled"
            is Condition.LocationEnabled -> "location ${if (condition.enabled) "is" else "is not"} enabled"
            is Condition.CallStateCondition -> "call state is ${condition.state.name.lowercase()}"
            is Condition.AlarmRinging -> if (condition.titleMatch?.isNotBlank() == true) "an alarm is ringing with title containing '${condition.titleMatch}'" else "an alarm is ringing"
            is Condition.ScreenTime -> "screen time today ${condition.op.name.lowercase()} ${condition.minutes} minutes"
            is Condition.HeadphonesConnected -> "headphones ${if (condition.connected) "are" else "are not"} connected"
            is Condition.DataSaverOn -> "data saver ${if (condition.on) "is" else "is not"} on"
            is Condition.AutoSyncOn -> "auto sync ${if (condition.on) "is" else "is not"} on"
            is Condition.AutoRotateOn -> "auto rotate ${if (condition.on) "is" else "is not"} on"
            is Condition.VolumeLevel -> "${condition.stream.name.lowercase()} volume ${condition.op.name.lowercase()} ${condition.level}"
            is Condition.ScreenOffFor -> "screen has been off for ${condition.op.name.lowercase()} ${condition.minutes} minutes"
            is Condition.ChargingSource -> "charging source is ${condition.source.name.lowercase().replace('_', ' ')}"
            is Condition.BatteryTemp -> "battery temperature ${condition.op.name.lowercase()} ${condition.celsius}°C"
            is Condition.ThermalLevel -> "thermal level ${condition.op.name.lowercase()} ${condition.level}"
            is Condition.BooleanState -> "${condition.key} is ${if (condition.on) "true" else "false"}"
            is Condition.NumericState -> "${condition.key} ${condition.op.name.lowercase()} ${condition.value}"
            is Condition.AtLocation -> "device is within ${condition.radiusM}m of (${condition.lat}, ${condition.lng})"
            is Condition.EventActive -> if (condition.titleMatch.isNotBlank()) "event '${condition.titleMatch}' is active" else "an event is active"
            is Condition.NotificationPresent ->
                "${condition.pkg} has a visible notification" +
                    if (condition.titleMatch.isNotBlank()) " titled '${condition.titleMatch}'" else ""
            is Condition.And -> "all of: [${condition.all.joinToString(" AND ") { explainCondition(it) }}]"
            is Condition.Or -> "any of: [${condition.any.joinToString(" OR ") { explainCondition(it) }}]"
            is Condition.Not -> "not (${explainCondition(condition.cond)})"
        }

    fun explainAction(action: Action): String =
        when (action) {
            is Action.SetWifi -> "turn WiFi ${if (action.on) "on" else "off"}"
            is Action.SetBluetooth -> "turn Bluetooth ${if (action.on) "on" else "off"}"
            is Action.SetMobileData -> "turn mobile data ${if (action.on) "on" else "off"}"
            is Action.SetDnd -> "set DND to ${action.mode.name.lowercase()}"
            is Action.SetRinger -> "set ringer to ${action.mode}"
            is Action.LaunchApp -> "open app(s) ${action.packages.joinToString(", ")}"
            is Action.OpenUrl -> "open URL ${action.url}" + if (action.packageName?.isNotBlank() == true) " in ${action.packageName}" else ""
            is Action.ShowNotification ->
                "show notification '${action.title}'" +
                    if (action.glyphPreset?.isNotBlank() == true) " with Glyph preset ${action.glyphPreset}" else ""
            is Action.SetVolume -> "set volumes: " + action.volumes.entries.joinToString { "${it.key.name.lowercase()}=${it.value}" }
            is Action.SetFlashlight -> "turn flashlight ${if (action.on) "on" else "off"}"
            is Action.SetDarkMode -> "set dark mode to ${action.mode.name.lowercase()}"
            is Action.OpenSettingsScreen -> "open settings screen ${action.screen.name}"
            is Action.Vibrate -> "vibrate for ${action.durationMs}ms"
            is Action.SetBrightness -> "set brightness to ${action.level}"
            is Action.SetAutoBrightness -> "set auto brightness ${if (action.on) "on" else "off"}"
            is Action.SetExtraDim -> "set extra dim ${if (action.on) "on" else "off"}"
            is Action.SetScreenTimeout -> "set screen timeout to ${action.timeoutMs}ms"
            is Action.SetWallpaper -> "set wallpaper on ${action.which} from ${action.uri}"
            is Action.SetGlyph ->
                "turn glyph ${if (action.on) "on" else "off"}" +
                    if (action.channels != null) " on channels ${action.channels}" else ""
            is Action.SetGlyphMatrix -> "show a custom frame on the Glyph Matrix"
            is Action.GlyphAnimate -> "animate glyph zone ${action.zone ?: "all"}"
            is Action.GlyphProgress -> "show progress ${action.progress}%"
            is Action.GlyphText -> "show text '${action.text}' on Glyph Matrix"
            is Action.GlyphScrollingText -> "scroll text '${action.text}' on Glyph Matrix"
            is Action.GlyphPreset -> "show Glyph preset '${action.preset}'"
            is Action.GlyphIcon -> "show Glyph icon '${action.name}'"
            is Action.GlyphNumber -> "show Glyph number ${action.number}"
            is Action.GlyphCountdown -> "Glyph countdown from ${action.seconds}s"
            is Action.GlyphMusic -> "show music visualizer style ${action.style}"
            Action.GlyphTurnOff -> "turn off Glyph"
            is Action.CopyText -> "copy '${action.text}' to clipboard"
            is Action.Wait -> "wait ${action.durationMs}ms"
            is Action.WriteSetting -> "write ${action.namespace} setting ${action.key}=${action.value}"
            is Action.SetAutoRotate -> "set auto rotate ${if (action.on) "on" else "off"}"
            is Action.SetBatterySaver -> "set battery saver ${if (action.on) "on" else "off"}"
            is Action.SetAirplaneMode -> "set airplane mode ${if (action.on) "on" else "off"}"
            is Action.SetDataSaver -> "set data saver ${if (action.on) "on" else "off"}"
            is Action.SetHotspot -> "set hotspot ${if (action.on) "on" else "off"}"
            is Action.SetNfc -> "set NFC ${if (action.on) "on" else "off"}"
            is Action.SetRefreshRate -> "set refresh rate to ${action.hz}Hz"
            is Action.SetScreenRotation -> "set screen rotation to ${action.orientation.name.lowercase()}"
            is Action.MediaControl -> "control media: ${action.command.name.lowercase()}"
            is Action.SendSms -> "send SMS to ${action.number}"
            is Action.LockScreen -> "lock the screen" + if (action.force) " (forced)" else ""
            is Action.SetLocationMode -> "set location mode to ${action.mode.name.lowercase()}"
            is Action.SetAutoSync -> "set auto sync ${if (action.on) "on" else "off"}"
            Action.ClearNotifications -> "clear all notifications"
            is Action.SetAlwaysOnDisplay -> "set AOD to ${action.mode.name.lowercase()}"
            is Action.TakeScreenshot -> "take a screenshot" + if (action.force) " (forced)" else ""
            is Action.Group -> "group '${action.name}': " + action.actions.joinToString(", ") { explainAction(it) }
        }

    private fun describePhoneEvent(event: PhoneEvent): String =
        when (event) {
            PhoneEvent.INCOMING_CALL -> "an incoming call"
            PhoneEvent.CALL_ENDED -> "a call ended"
            PhoneEvent.SMS_RECEIVED -> "an SMS received"
        }

    private fun describeConnState(state: ConnState): String =
        when (state) {
            ConnState.CONNECTED -> "connects"
            ConnState.DISCONNECTED -> "disconnects"
        }

    private fun describeConnMedium(medium: ConnMedium): String =
        when (medium) {
            ConnMedium.WIFI -> "WiFi"
            ConnMedium.BT -> "Bluetooth"
            ConnMedium.AIRPLANE -> "airplane mode"
            ConnMedium.POWER -> "power"
        }

    private fun describeScreenState(state: com.tdvorak.nothingmodes.engine.model.ScreenState): String =
        when (state) {
            com.tdvorak.nothingmodes.engine.model.ScreenState.ON -> "turns on"
            com.tdvorak.nothingmodes.engine.model.ScreenState.OFF -> "turns off"
        }
}
