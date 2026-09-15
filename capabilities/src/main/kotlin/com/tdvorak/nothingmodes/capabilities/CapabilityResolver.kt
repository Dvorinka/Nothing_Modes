package com.tdvorak.nothingmodes.capabilities

import com.tdvorak.nothingmodes.engine.model.CapabilityIds

/** Resolves whether an automation's required capabilities are satisfied by the device. */
class CapabilityResolver(
    private val capabilities: DeviceCapabilities,
) {
    fun resolve(
        automationId: String,
        required: Set<String>,
    ): CapabilityResolution {
        val satisfied = required.filter { isSatisfied(it) }.toSet()
        val missing = required - satisfied
        val reasons = missing.associateWith { reasonFor(it) }
        return CapabilityResolution(
            automationId = automationId,
            satisfied = satisfied,
            missing = missing,
            canRun = missing.isEmpty(),
            missingReasons = reasons,
        )
    }

    private fun isSatisfied(capability: String): Boolean =
        when (capability) {
            // Triggers — always satisfied if the hardware exists
            CapabilityIds.TRIGGER_TIME,
            CapabilityIds.TRIGGER_TIME_WINDOW,
            CapabilityIds.TRIGGER_IMMEDIATE,
            CapabilityIds.TRIGGER_BOOT,
            -> true

            CapabilityIds.TRIGGER_NOTIFICATION -> capabilities.hasNotificationListenerAccess
            CapabilityIds.TRIGGER_PHONE_SMS -> capabilities.hasReceiveSms
            CapabilityIds.TRIGGER_PHONE_CALL -> capabilities.hasReadPhoneState && capabilities.hasReadCallLog

            CapabilityIds.TRIGGER_CONNECTIVITY_WIFI,
            CapabilityIds.TRIGGER_CONNECTIVITY_WIFI_IDENTITY,
            CapabilityIds.TRIGGER_WIFI_CONNECTED,
            -> capabilities.hasWifi
            CapabilityIds.TRIGGER_CONNECTIVITY_BT,
            CapabilityIds.TRIGGER_BT_DEVICE,
            -> capabilities.hasBluetooth
            CapabilityIds.TRIGGER_CONNECTIVITY_POWER -> true
            CapabilityIds.TRIGGER_CONNECTIVITY_AIRPLANE -> true
            CapabilityIds.TRIGGER_BATTERY_LEVEL -> true
            CapabilityIds.TRIGGER_SCREEN_STATE -> true
            // DeviceStateMonitor watches all keys locally — no permission needed.
            CapabilityIds.TRIGGER_DEVICE_STATE -> true
            CapabilityIds.TRIGGER_TORCH_STATE -> capabilities.hasFlashlight
            CapabilityIds.TRIGGER_APP_OPENED -> capabilities.hasUsageAccess
            CapabilityIds.TRIGGER_GEOFENCE -> capabilities.hasLocation && capabilities.hasLocationPermission
            CapabilityIds.TRIGGER_MANUAL -> true
            CapabilityIds.TRIGGER_CALENDAR_EVENT -> capabilities.hasReadCalendar
            CapabilityIds.TRIGGER_MEDIA_PLAYBACK -> capabilities.hasNotificationListenerAccess

            // State readers
            CapabilityIds.STATE_READER_BUILTIN -> true
            CapabilityIds.STATE_READER_SETTING,
            CapabilityIds.STATE_READER_SYSTEM_PROPERTY,
            CapabilityIds.STATE_READER_SYSFS,
            CapabilityIds.STATE_READER_DUMPSYS_FIELD,
            -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED

            CapabilityIds.STATE_FOREGROUND_APP -> capabilities.hasUsageAccess
            CapabilityIds.STATE_LOCATION -> capabilities.hasLocation && capabilities.hasLocationPermission

            // Actions
            CapabilityIds.ACTION_SET_WIFI -> capabilities.hasWifi
            CapabilityIds.ACTION_SET_BLUETOOTH -> capabilities.hasBluetooth
            CapabilityIds.ACTION_SET_DND -> capabilities.hasNotificationPolicyAccess
            CapabilityIds.ACTION_SET_RINGER -> true
            CapabilityIds.ACTION_LAUNCH_APP -> true
            CapabilityIds.ACTION_OPEN_URL -> true
            CapabilityIds.ACTION_SHOW_NOTIFICATION -> capabilities.hasPostNotifications
            CapabilityIds.ACTION_SET_VOLUME -> true
            CapabilityIds.ACTION_SET_FLASHLIGHT -> capabilities.hasFlashlight
            CapabilityIds.ACTION_SET_DARK_MODE,
            CapabilityIds.ACTION_SET_EXTRA_DIM,
            CapabilityIds.ACTION_SET_MOBILE_DATA,
            CapabilityIds.ACTION_WRITE_SETTING,
            -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED

            CapabilityIds.ACTION_SET_BRIGHTNESS -> capabilities.hasWriteSettings
            CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS -> capabilities.hasWriteSettings
            CapabilityIds.ACTION_SET_SCREEN_TIMEOUT -> capabilities.hasWriteSettings
            CapabilityIds.ACTION_SET_AUTO_ROTATE,
            CapabilityIds.ACTION_SET_REFRESH_RATE,
            CapabilityIds.ACTION_SET_SCREEN_ROTATION,
            -> capabilities.hasWriteSettings
            CapabilityIds.ACTION_SET_AOD -> capabilities.hasWriteSettings
            CapabilityIds.ACTION_OPEN_SETTINGS_SCREEN -> true
            CapabilityIds.ACTION_VIBRATE -> capabilities.hasVibrator
            CapabilityIds.ACTION_SET_GLYPH -> capabilities.hasGlyphLightStripe
            CapabilityIds.ACTION_SET_GLYPH_MATRIX -> capabilities.hasGlyphMatrix
            // Animate/progress degrade to the matrix on stripe-less hardware —
            // the executor accepts either glyph surface.
            CapabilityIds.ACTION_GLYPH_ANIMATE -> capabilities.hasGlyphLightStripe || capabilities.hasGlyphMatrix
            CapabilityIds.ACTION_GLYPH_PROGRESS -> capabilities.hasGlyphLightStripe || capabilities.hasGlyphMatrix
            CapabilityIds.ACTION_GLYPH_TEXT -> capabilities.hasGlyphMatrix
            CapabilityIds.ACTION_GLYPH_SCROLLING_TEXT -> capabilities.hasGlyphMatrix
            CapabilityIds.ACTION_GLYPH_PRESET -> capabilities.hasGlyphLightStripe || capabilities.hasGlyphMatrix
            CapabilityIds.ACTION_GLYPH_TURNOFF -> capabilities.hasGlyphLightStripe || capabilities.hasGlyphMatrix
            CapabilityIds.ACTION_SET_GLYPH_INTERFACE -> capabilities.hasGlyphLightStripe || capabilities.hasGlyphMatrix
            CapabilityIds.ACTION_GLYPH_ICON,
            CapabilityIds.ACTION_GLYPH_NUMBER,
            CapabilityIds.ACTION_GLYPH_COUNTDOWN,
            CapabilityIds.ACTION_GLYPH_MUSIC,
            -> capabilities.hasGlyphMatrix
            CapabilityIds.ACTION_COPY_TEXT -> true
            CapabilityIds.ACTION_WAIT -> true
            CapabilityIds.ACTION_SET_BATTERY_SAVER,
            CapabilityIds.ACTION_SET_AIRPLANE_MODE,
            CapabilityIds.ACTION_SET_DATA_SAVER,
            CapabilityIds.ACTION_SET_HOTSPOT,
            CapabilityIds.ACTION_SET_NFC,
            CapabilityIds.ACTION_SET_AUTO_SYNC,
            -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED
            CapabilityIds.ACTION_SET_LOCATION_MODE -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED
            CapabilityIds.ACTION_SET_STAY_AWAKE -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED
            CapabilityIds.ACTION_MEDIA_CONTROL -> true
            CapabilityIds.ACTION_SEND_SMS -> capabilities.hasSendSms
            CapabilityIds.ACTION_TAKE_SCREENSHOT -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED
            CapabilityIds.ACTION_LOCK_SCREEN -> capabilities.hasActiveDeviceAdmin
            CapabilityIds.ACTION_CLEAR_NOTIFICATIONS -> capabilities.hasNotificationListenerAccess
            CapabilityIds.ACTION_SET_WALLPAPER -> true

            // Shizuku
            CapabilityIds.SHIZUKU_REQUIRED -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED

            else -> false
        }

    private fun reasonFor(capability: String): String =
        when (capability) {
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
            CapabilityIds.ACTION_SET_STAY_AWAKE,
            -> "Needs Shizuku — a free companion app that grants extra permissions"

            CapabilityIds.ACTION_SET_DND -> "Notification policy access required"
            CapabilityIds.ACTION_SET_BRIGHTNESS,
            CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS,
            CapabilityIds.ACTION_SET_SCREEN_TIMEOUT,
            CapabilityIds.ACTION_SET_AUTO_ROTATE,
            CapabilityIds.ACTION_SET_REFRESH_RATE,
            CapabilityIds.ACTION_SET_SCREEN_ROTATION,
            CapabilityIds.ACTION_SET_AOD,
            -> "'Modify system settings' permission required"

            CapabilityIds.ACTION_SET_GLYPH -> "No Glyph light stripe on this device"
            CapabilityIds.ACTION_SET_GLYPH_MATRIX -> "No Glyph Matrix on this device"
            CapabilityIds.ACTION_GLYPH_ANIMATE -> "No Glyph hardware on this device"
            CapabilityIds.ACTION_GLYPH_PROGRESS -> "No Glyph hardware on this device"
            CapabilityIds.ACTION_GLYPH_TEXT -> "No Glyph Matrix on this device"
            CapabilityIds.ACTION_GLYPH_SCROLLING_TEXT -> "No Glyph Matrix on this device"
            CapabilityIds.ACTION_GLYPH_PRESET -> "No Glyph hardware on this device"
            CapabilityIds.ACTION_GLYPH_TURNOFF -> "No Glyph hardware on this device"
            CapabilityIds.ACTION_SET_GLYPH_INTERFACE -> "No Glyph hardware on this device"
            CapabilityIds.ACTION_GLYPH_ICON,
            CapabilityIds.ACTION_GLYPH_NUMBER,
            CapabilityIds.ACTION_GLYPH_COUNTDOWN,
            CapabilityIds.ACTION_GLYPH_MUSIC,
            -> "No Glyph Matrix on this device"
            CapabilityIds.TRIGGER_NOTIFICATION -> "Notification access required — the app can't see incoming notifications"
            CapabilityIds.TRIGGER_APP_OPENED -> "Usage access required — the app can't see which app is open"
            CapabilityIds.TRIGGER_GEOFENCE -> "Location permission required"
            CapabilityIds.STATE_FOREGROUND_APP -> "Usage access required — the app can't see which app is open"
            CapabilityIds.STATE_LOCATION -> "Location permission required"
            CapabilityIds.TRIGGER_PHONE_SMS -> "SMS permission required"
            CapabilityIds.TRIGGER_PHONE_CALL -> "Phone state permission required"
            CapabilityIds.TRIGGER_CALENDAR_EVENT -> "Calendar permission required"
            CapabilityIds.TRIGGER_MEDIA_PLAYBACK -> "Notification access required — the app can't see media sessions"
            CapabilityIds.ACTION_SEND_SMS -> "SMS permission required"
            CapabilityIds.ACTION_CLEAR_NOTIFICATIONS -> "Notification access required"
            CapabilityIds.ACTION_SET_WALLPAPER -> "Wallpaper permission required"
            CapabilityIds.ACTION_TAKE_SCREENSHOT -> "Needs Shizuku — a free companion app that grants extra permissions"
            CapabilityIds.ACTION_LOCK_SCREEN -> "Needs Shizuku — a free companion app that grants extra permissions"
            CapabilityIds.ACTION_SET_WIFI -> "This device has no Wi-Fi hardware"
            CapabilityIds.ACTION_SET_BLUETOOTH -> "This device has no Bluetooth hardware"
            CapabilityIds.ACTION_SET_FLASHLIGHT -> "This device has no flashlight"
            CapabilityIds.ACTION_VIBRATE -> "This device has no vibrator"
            else -> "Capability not available: $capability"
        }
}
