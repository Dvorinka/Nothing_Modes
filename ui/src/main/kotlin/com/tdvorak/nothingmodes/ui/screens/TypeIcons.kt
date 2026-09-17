package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MobileScreenShare
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.AirplanemodeActive
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Brightness6
import androidx.compose.material.icons.outlined.BrightnessLow
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataSaverOn
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellular4Bar
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.ConnMedium
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.DeviceStateKeys
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.ui.util.BOOLEAN_STATE_ITEMS
import com.tdvorak.nothingmodes.ui.util.NUMERIC_STATE_ITEMS

/**
 * One icon vocabulary for triggers, conditions, and actions — the same glyphs
 * the catalogs use, so configured rows read identically everywhere.
 */

fun iconForTrigger(trigger: Trigger): ImageVector =
    when (trigger) {
        is Trigger.Time, is Trigger.TimeWindow -> Icons.Outlined.Schedule
        is Trigger.Immediate -> Icons.Outlined.Bolt
        is Trigger.Manual -> Icons.Outlined.TouchApp
        is Trigger.Notification -> Icons.Outlined.Notifications
        is Trigger.PhoneState -> Icons.Outlined.Phone
        is Trigger.Connectivity ->
            when (trigger.medium) {
                ConnMedium.WIFI -> Icons.Outlined.Wifi
                ConnMedium.BT -> Icons.Outlined.Bluetooth
                ConnMedium.POWER -> Icons.Outlined.Power
                ConnMedium.AIRPLANE -> Icons.Outlined.AirplanemodeActive
            }
        is Trigger.Boot -> Icons.Outlined.PowerSettingsNew
        is Trigger.BatteryLevel -> Icons.Outlined.BatteryFull
        is Trigger.ScreenStateTrigger -> Icons.Outlined.Devices
        is Trigger.AppOpened -> Icons.Outlined.Apps
        is Trigger.Geofence -> Icons.Outlined.LocationOn
        is Trigger.BluetoothDevice -> Icons.Outlined.Bluetooth
        is Trigger.WifiConnected -> Icons.Outlined.Wifi
        is Trigger.CalendarEvent -> Icons.Outlined.CalendarMonth
        is Trigger.ChargerConnected -> Icons.Outlined.BatteryChargingFull
        is Trigger.DeviceUnlocked -> Icons.Outlined.LockOpen
        is Trigger.DeviceLocked -> Icons.Outlined.Lock
        is Trigger.TorchState -> Icons.Outlined.FlashlightOn
        is Trigger.MediaPlayback -> Icons.Outlined.PlayCircle
        is Trigger.DeviceState -> iconForDeviceStateKey(trigger.key)
    }

fun iconForCondition(condition: Condition): ImageVector =
    when (condition) {
        is Condition.BatteryLevel -> Icons.Outlined.BatteryFull
        is Condition.Charging -> Icons.Outlined.Power
        is Condition.ScreenStateCondition -> Icons.Outlined.Devices
        is Condition.WifiConnected -> Icons.Outlined.Wifi
        is Condition.BluetoothConnected -> Icons.Outlined.Bluetooth
        is Condition.TimeWindow -> Icons.Outlined.Schedule
        is Condition.DayOfWeekCondition -> Icons.Outlined.CalendarMonth
        is Condition.AppInForeground -> Icons.Outlined.Apps
        is Condition.CurrentModeActive -> Icons.Outlined.Star
        is Condition.PowerSaving -> Icons.Outlined.PowerSettingsNew
        is Condition.DarkModeActive -> Icons.Outlined.DarkMode
        is Condition.MediaPlaying -> Icons.Outlined.GraphicEq
        is Condition.RingerMode -> Icons.AutoMirrored.Outlined.VolumeUp
        is Condition.AirplaneModeOn -> Icons.Outlined.Flight
        is Condition.NfcEnabled -> Icons.Outlined.Nfc
        is Condition.LocationEnabled -> Icons.Outlined.LocationOn
        is Condition.CallStateCondition -> Icons.Outlined.PhoneAndroid
        is Condition.AlarmRinging -> Icons.Outlined.Alarm
        is Condition.ScreenTime -> Icons.Outlined.Timer
        is Condition.HeadphonesConnected -> Icons.Outlined.Headphones
        is Condition.DataSaverOn -> Icons.Outlined.DataSaverOn
        is Condition.AutoSyncOn -> Icons.Outlined.Sync
        is Condition.AutoRotateOn -> Icons.Outlined.ScreenRotation
        is Condition.VolumeLevel -> Icons.AutoMirrored.Outlined.VolumeUp
        is Condition.ChargingSource -> Icons.Outlined.ElectricBolt
        is Condition.BatteryTemp -> Icons.Outlined.Thermostat
        is Condition.ScreenOffFor -> Icons.Outlined.Bedtime
        is Condition.ThermalLevel -> Icons.Outlined.DeviceThermostat
        is Condition.AtLocation -> Icons.Outlined.LocationOn
        is Condition.EventActive -> Icons.Outlined.Event
        is Condition.NotificationPresent -> Icons.Outlined.NotificationsActive
        is Condition.BooleanState ->
            BOOLEAN_STATE_ITEMS.firstOrNull { it.key == condition.key }?.icon
                ?: Icons.Outlined.ToggleOn
        is Condition.NumericState ->
            NUMERIC_STATE_ITEMS.firstOrNull { it.key == condition.key }?.icon
                ?: Icons.Outlined.Timer
        is Condition.And -> Icons.Outlined.ToggleOn
        is Condition.Or -> Icons.Outlined.ToggleOn
        is Condition.Not -> Icons.Outlined.ToggleOn
    }

fun iconForAction(action: Action): ImageVector =
    when (action) {
        is Action.SetWifi -> Icons.Outlined.Wifi
        is Action.SetBluetooth -> Icons.Outlined.Bluetooth
        is Action.SetMobileData -> Icons.Outlined.SignalCellular4Bar
        is Action.SetHotspot -> Icons.Outlined.WifiTethering
        is Action.SetAirplaneMode -> Icons.Outlined.Flight
        is Action.SetDarkMode -> Icons.Outlined.DarkMode
        is Action.SetBrightness -> Icons.Outlined.Brightness6
        is Action.SetAutoBrightness -> Icons.Outlined.Lightbulb
        is Action.SetExtraDim -> Icons.Outlined.Brightness6
        is Action.SetUltraDim -> Icons.Outlined.BrightnessLow
        is Action.SetScreenTimeout -> Icons.Outlined.Timer
        is Action.SetStayAwake -> Icons.Outlined.Bedtime
        is Action.SetWallpaper -> Icons.Outlined.Wallpaper
        is Action.SetAlwaysOnDisplay -> Icons.Outlined.WatchLater
        is Action.SetDnd -> Icons.Outlined.DoNotDisturbOn
        is Action.SetVolume -> Icons.AutoMirrored.Outlined.VolumeUp
        is Action.Vibrate -> Icons.Outlined.Vibration
        is Action.SetRinger -> Icons.AutoMirrored.Outlined.VolumeUp
        is Action.SetFlashlight -> Icons.Outlined.FlashlightOn
        is Action.SetAutoRotate -> Icons.Outlined.ScreenRotation
        is Action.SetScreenRotation -> Icons.Outlined.ScreenRotation
        is Action.SetBatterySaver -> Icons.Outlined.PowerSettingsNew
        is Action.SetDataSaver -> Icons.Outlined.SignalCellular4Bar
        is Action.SetNfc -> Icons.Outlined.Nfc
        is Action.SetLocationMode -> Icons.Outlined.LocationOn
        is Action.SetRefreshRate -> Icons.Outlined.Settings
        is Action.SetAutoSync -> Icons.Outlined.Snooze
        is Action.LockScreen -> Icons.Outlined.Lock
        is Action.TakeScreenshot -> Icons.AutoMirrored.Outlined.MobileScreenShare
        is Action.ClearNotifications -> Icons.Outlined.Notifications
        is Action.ShowNotification -> Icons.Outlined.Campaign
        is Action.OpenUrl -> Icons.Outlined.Link
        is Action.LaunchApp -> Icons.Outlined.OpenInBrowser
        is Action.OpenSettingsScreen -> Icons.Outlined.Settings
        is Action.MediaControl -> Icons.AutoMirrored.Outlined.VolumeUp
        is Action.Wait -> Icons.Outlined.Snooze
        is Action.SendSms -> Icons.Outlined.Sms
        is Action.CopyText -> Icons.Outlined.ContentCopy
        is Action.WriteSetting -> Icons.Outlined.Settings
        is Action.SetGlyphInterface -> Icons.Outlined.ToggleOn
        is Action.Group -> Icons.Outlined.Bolt
        is Action.SetGlyph, is Action.SetGlyphMatrix, is Action.GlyphAnimate,
        is Action.GlyphProgress, is Action.GlyphText, is Action.GlyphScrollingText,
        is Action.GlyphPreset, is Action.GlyphIcon, is Action.GlyphNumber,
        is Action.GlyphCountdown, is Action.GlyphMusic, is Action.GlyphTurnOff,
        -> Icons.Outlined.Lightbulb
    }

/** Icon for a device-state key — mirrors the trigger catalog's spec icons. */
private fun iconForDeviceStateKey(key: String): ImageVector =
    when (key) {
        DeviceStateKeys.POWER_SAVING -> Icons.Outlined.BatterySaver
        DeviceStateKeys.CHARGING_LIMIT, DeviceStateKeys.BATTERY_SHARE,
        DeviceStateKeys.BATTERY_SHARE_LIMIT,
        -> Icons.Outlined.BatteryChargingFull
        DeviceStateKeys.DND_ACTIVE -> Icons.Outlined.DoNotDisturbOn
        DeviceStateKeys.RINGER_MODE, DeviceStateKeys.VOLUME_MEDIA,
        DeviceStateKeys.VOLUME_RING, DeviceStateKeys.VOLUME_ALARM,
        -> Icons.AutoMirrored.Outlined.VolumeUp
        DeviceStateKeys.HEADPHONES -> Icons.Outlined.Headphones
        DeviceStateKeys.DARK_MODE -> Icons.Outlined.DarkMode
        DeviceStateKeys.AUTO_ROTATE -> Icons.Outlined.ScreenRotation
        DeviceStateKeys.AOD -> Icons.Outlined.WatchLater
        DeviceStateKeys.THERMAL -> Icons.Outlined.Thermostat
        DeviceStateKeys.WIFI_RADIO -> Icons.Outlined.Wifi
        DeviceStateKeys.BLUETOOTH_RADIO -> Icons.Outlined.Bluetooth
        DeviceStateKeys.NFC -> Icons.Outlined.Nfc
        DeviceStateKeys.LOCATION -> Icons.Outlined.LocationOn
        DeviceStateKeys.HOTSPOT -> Icons.Outlined.WifiTethering
        DeviceStateKeys.MOBILE_DATA -> Icons.Outlined.SignalCellular4Bar
        DeviceStateKeys.DATA_SAVER -> Icons.Outlined.DataSaverOn
        DeviceStateKeys.AIRPLANE -> Icons.Outlined.AirplanemodeActive
        DeviceStateKeys.GLYPH_INTERFACE, DeviceStateKeys.GLYPH_CHARGE_LED ->
            Icons.Outlined.Lightbulb
        else -> Icons.Outlined.Settings
    }

/** Package name carried by a trigger/condition/action, if any — for app icons. */
internal fun appPackageOf(item: Any): String? =
    when (item) {
        is Trigger.AppOpened -> item.pkg
        is Trigger.Notification -> item.pkg
        is Trigger.MediaPlayback -> item.packageName
        is Condition.AppInForeground -> item.pkg
        is Condition.NotificationPresent -> item.pkg
        is Action.LaunchApp -> item.packages.firstOrNull()
        is Action.OpenUrl -> item.packageName
        is Action.OpenSettingsScreen -> item.pkg
        else -> null
    }
