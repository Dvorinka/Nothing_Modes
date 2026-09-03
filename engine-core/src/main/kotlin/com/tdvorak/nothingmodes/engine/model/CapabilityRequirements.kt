package com.tdvorak.nothingmodes.engine.model

/** Capability IDs for triggers, conditions, and actions. Used for capability detection. */
object CapabilityIds {
    // Triggers
    const val TRIGGER_TIME = "trigger_time"
    const val TRIGGER_TIME_WINDOW = "trigger_time_window"
    const val TRIGGER_IMMEDIATE = "trigger_immediate"
    const val TRIGGER_NOTIFICATION = "trigger_notification"
    const val TRIGGER_PHONE_SMS = "trigger_phone_sms"
    const val TRIGGER_PHONE_CALL = "trigger_phone_call"
    const val TRIGGER_CONNECTIVITY_WIFI = "trigger_connectivity_wifi"
    const val TRIGGER_CONNECTIVITY_WIFI_IDENTITY = "trigger_connectivity_wifi_identity"
    const val TRIGGER_CONNECTIVITY_BT = "trigger_connectivity_bt"
    const val TRIGGER_CONNECTIVITY_POWER = "trigger_connectivity_power"
    const val TRIGGER_BOOT = "trigger_boot"
    const val TRIGGER_BATTERY_LEVEL = "trigger_battery_level"
    const val TRIGGER_SCREEN_STATE = "trigger_screen_state"
    const val TRIGGER_APP_OPENED = "trigger_app_opened"
    const val TRIGGER_GEOFENCE = "trigger_geofence"

    // State readers
    const val STATE_READER_BUILTIN = "state_reader_builtin"
    const val STATE_READER_SETTING = "state_reader_setting"
    const val STATE_READER_SYSTEM_PROPERTY = "state_reader_system_property"
    const val STATE_READER_SYSFS = "state_reader_sysfs"
    const val STATE_READER_DUMPSYS_FIELD = "state_reader_dumpsys_field"
    const val STATE_FOREGROUND_APP = "state_foreground_app"
    const val STATE_LOCATION = "state_location"

    // Actions
    const val ACTION_SET_WIFI = "action_set_wifi"
    const val ACTION_SET_BLUETOOTH = "action_set_bluetooth"
    const val ACTION_SET_MOBILE_DATA = "action_set_mobile_data"
    const val ACTION_SET_DND = "action_set_dnd"
    const val ACTION_SET_RINGER = "action_set_ringer"
    const val ACTION_LAUNCH_APP = "action_launch_app"
    const val ACTION_OPEN_URL = "action_open_url"
    const val ACTION_SHOW_NOTIFICATION = "action_show_notification"
    const val ACTION_SET_VOLUME = "action_set_volume"
    const val ACTION_SET_FLASHLIGHT = "action_set_flashlight"
    const val ACTION_SET_DARK_MODE = "action_set_dark_mode"
    const val ACTION_OPEN_SETTINGS_SCREEN = "action_open_settings_screen"
    const val ACTION_VIBRATE = "action_vibrate"
    const val ACTION_SET_BRIGHTNESS = "action_set_brightness"
    const val ACTION_SET_AUTO_BRIGHTNESS = "action_set_auto_brightness"
    const val ACTION_SET_EXTRA_DIM = "action_set_extra_dim"
    const val ACTION_SET_SCREEN_TIMEOUT = "action_set_screen_timeout"
    const val ACTION_SET_GLYPH = "action_set_glyph"
    const val ACTION_SET_GLYPH_MATRIX = "action_set_glyph_matrix"
    const val ACTION_GLYPH_ANIMATE = "action_glyph_animate"
    const val ACTION_GLYPH_PROGRESS = "action_glyph_progress"
    const val ACTION_GLYPH_TEXT = "action_glyph_text"
    const val ACTION_GLYPH_SCROLLING_TEXT = "action_glyph_scrolling_text"
    const val ACTION_GLYPH_PRESET = "action_glyph_preset"
    const val ACTION_GLYPH_TURNOFF = "action_glyph_turnoff"
    const val ACTION_COPY_TEXT = "action_copy_text"
    const val ACTION_WAIT = "action_wait"
    const val ACTION_WRITE_SETTING = "action_write_setting"

    // Shizuku
    const val SHIZUKU_REQUIRED = "shizuku_required"
}

/** Derives required capabilities from an automation's trigger, actions, and conditions. */
object CapabilityRequirements {
    fun derive(
        trigger: Trigger,
        actions: List<Action>,
        conditions: Condition? = null,
    ): Set<String> {
        val caps = mutableSetOf<String>()
        caps += triggerCapabilities(trigger)
        actions.forEach { caps += actionCapabilities(it) }
        conditions?.let { caps += conditionCapabilities(it) }
        return caps
    }

    private fun triggerCapabilities(trigger: Trigger): Set<String> = when (trigger) {
        is Trigger.Time -> setOf(CapabilityIds.TRIGGER_TIME)
        is Trigger.TimeWindow -> setOf(CapabilityIds.TRIGGER_TIME_WINDOW)
        is Trigger.Immediate -> setOf(CapabilityIds.TRIGGER_IMMEDIATE)
        is Trigger.Notification -> setOf(CapabilityIds.TRIGGER_NOTIFICATION)
        is Trigger.PhoneState -> when (trigger.event) {
            PhoneEvent.SMS_RECEIVED -> setOf(CapabilityIds.TRIGGER_PHONE_SMS)
            PhoneEvent.INCOMING_CALL, PhoneEvent.CALL_ENDED -> setOf(CapabilityIds.TRIGGER_PHONE_CALL)
        }
        is Trigger.Connectivity -> when (trigger.medium) {
            ConnMedium.WIFI -> setOf(CapabilityIds.TRIGGER_CONNECTIVITY_WIFI)
            ConnMedium.BT -> setOf(CapabilityIds.TRIGGER_CONNECTIVITY_BT)
            ConnMedium.POWER -> setOf(CapabilityIds.TRIGGER_CONNECTIVITY_POWER)
        }
        is Trigger.Boot -> setOf(CapabilityIds.TRIGGER_BOOT)
        is Trigger.BatteryLevel -> setOf(CapabilityIds.TRIGGER_BATTERY_LEVEL)
        is Trigger.ScreenStateTrigger -> setOf(CapabilityIds.TRIGGER_SCREEN_STATE)
        is Trigger.AppOpened -> setOf(CapabilityIds.TRIGGER_APP_OPENED)
        is Trigger.Geofence -> setOf(CapabilityIds.TRIGGER_GEOFENCE)
    }

    private fun actionCapabilities(action: Action): Set<String> = when (action) {
        is Action.SetWifi -> setOf(CapabilityIds.ACTION_SET_WIFI)
        is Action.SetBluetooth -> setOf(CapabilityIds.ACTION_SET_BLUETOOTH)
        is Action.SetMobileData -> setOf(CapabilityIds.ACTION_SET_MOBILE_DATA, CapabilityIds.SHIZUKU_REQUIRED)
        is Action.SetDnd -> setOf(CapabilityIds.ACTION_SET_DND)
        is Action.SetRinger -> setOf(CapabilityIds.ACTION_SET_RINGER)
        is Action.LaunchApp -> setOf(CapabilityIds.ACTION_LAUNCH_APP)
        is Action.OpenUrl -> setOf(CapabilityIds.ACTION_OPEN_URL)
        is Action.ShowNotification -> setOf(CapabilityIds.ACTION_SHOW_NOTIFICATION)
        is Action.SetVolume -> setOf(CapabilityIds.ACTION_SET_VOLUME)
        is Action.SetFlashlight -> setOf(CapabilityIds.ACTION_SET_FLASHLIGHT)
        is Action.SetDarkMode -> setOf(CapabilityIds.ACTION_SET_DARK_MODE, CapabilityIds.SHIZUKU_REQUIRED)
        is Action.OpenSettingsScreen -> setOf(CapabilityIds.ACTION_OPEN_SETTINGS_SCREEN)
        is Action.Vibrate -> setOf(CapabilityIds.ACTION_VIBRATE)
        is Action.SetBrightness -> setOf(CapabilityIds.ACTION_SET_BRIGHTNESS)
        is Action.SetAutoBrightness -> setOf(CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS)
        is Action.SetExtraDim -> setOf(CapabilityIds.ACTION_SET_EXTRA_DIM, CapabilityIds.SHIZUKU_REQUIRED)
        is Action.SetScreenTimeout -> setOf(CapabilityIds.ACTION_SET_SCREEN_TIMEOUT)
        is Action.SetGlyph -> setOf(CapabilityIds.ACTION_SET_GLYPH)
        is Action.SetGlyphMatrix -> setOf(CapabilityIds.ACTION_SET_GLYPH_MATRIX)
        is Action.GlyphAnimate -> setOf(CapabilityIds.ACTION_GLYPH_ANIMATE)
        is Action.GlyphProgress -> setOf(CapabilityIds.ACTION_GLYPH_PROGRESS)
        is Action.GlyphText -> setOf(CapabilityIds.ACTION_GLYPH_TEXT)
        is Action.GlyphScrollingText -> setOf(CapabilityIds.ACTION_GLYPH_SCROLLING_TEXT)
        is Action.GlyphPreset -> setOf(CapabilityIds.ACTION_GLYPH_PRESET)
        is Action.GlyphTurnOff -> setOf(CapabilityIds.ACTION_GLYPH_TURNOFF)
        is Action.CopyText -> setOf(CapabilityIds.ACTION_COPY_TEXT)
        is Action.Wait -> setOf(CapabilityIds.ACTION_WAIT)
        is Action.WriteSetting -> setOf(CapabilityIds.ACTION_WRITE_SETTING, CapabilityIds.SHIZUKU_REQUIRED)
    }

    private fun conditionCapabilities(condition: Condition): Set<String> = when (condition) {
        is Condition.TimeWindow -> emptySet()
        is Condition.DayOfWeekCondition -> emptySet()
        is Condition.BatteryLevel -> setOf(CapabilityIds.STATE_READER_BUILTIN)
        is Condition.Charging -> setOf(CapabilityIds.STATE_READER_BUILTIN)
        is Condition.WifiConnected -> setOf(CapabilityIds.STATE_READER_BUILTIN)
        is Condition.BluetoothConnected -> setOf(CapabilityIds.STATE_READER_BUILTIN)
        is Condition.ScreenStateCondition -> setOf(CapabilityIds.STATE_READER_BUILTIN)
        is Condition.CurrentModeActive -> setOf(CapabilityIds.STATE_READER_BUILTIN)
        is Condition.AppInForeground -> setOf(CapabilityIds.STATE_FOREGROUND_APP)
        is Condition.And -> condition.all.flatMap { conditionCapabilities(it) }.toSet()
        is Condition.Or -> condition.any.flatMap { conditionCapabilities(it) }.toSet()
        is Condition.Not -> conditionCapabilities(condition.cond)
    }
}
