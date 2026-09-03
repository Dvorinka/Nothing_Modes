package com.dvoranka.nothingmodes.capabilities

import com.dvoranka.nothingmodes.engine.model.CapabilityIds
import com.dvoranka.nothingmodes.engine.model.CapabilityRequirements

/** Resolves whether an automation's required capabilities are satisfied by the device. */
class CapabilityResolver(private val capabilities: DeviceCapabilities) {

    fun resolve(automationId: String, required: Set<String>): CapabilityResolution {
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

    private fun isSatisfied(capability: String): Boolean = when (capability) {
        // Triggers — always satisfied if the hardware exists
        CapabilityIds.TRIGGER_TIME,
        CapabilityIds.TRIGGER_TIME_WINDOW,
        CapabilityIds.TRIGGER_IMMEDIATE,
        CapabilityIds.TRIGGER_BOOT,
        -> true

        CapabilityIds.TRIGGER_NOTIFICATION -> capabilities.hasNotificationListenerAccess
        CapabilityIds.TRIGGER_PHONE_SMS,
        CapabilityIds.TRIGGER_PHONE_CALL,
        -> capabilities.hasTelephony

        CapabilityIds.TRIGGER_CONNECTIVITY_WIFI -> capabilities.hasWifi
        CapabilityIds.TRIGGER_CONNECTIVITY_BT -> capabilities.hasBluetooth
        CapabilityIds.TRIGGER_CONNECTIVITY_POWER -> true
        CapabilityIds.TRIGGER_BATTERY_LEVEL -> true
        CapabilityIds.TRIGGER_SCREEN_STATE -> true
        CapabilityIds.TRIGGER_APP_OPENED -> true
        CapabilityIds.TRIGGER_GEOFENCE -> capabilities.hasLocation

        // State readers
        CapabilityIds.STATE_READER_BUILTIN -> true
        CapabilityIds.STATE_READER_SETTING,
        CapabilityIds.STATE_READER_SYSTEM_PROPERTY,
        CapabilityIds.STATE_READER_SYSFS,
        CapabilityIds.STATE_READER_DUMPSYS_FIELD,
        -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED

        CapabilityIds.STATE_FOREGROUND_APP -> true
        CapabilityIds.STATE_LOCATION -> capabilities.hasLocation

        // Actions
        CapabilityIds.ACTION_SET_WIFI -> capabilities.hasWifi
        CapabilityIds.ACTION_SET_BLUETOOTH -> capabilities.hasBluetooth
        CapabilityIds.ACTION_SET_DND -> capabilities.hasNotificationPolicyAccess
        CapabilityIds.ACTION_SET_RINGER -> true
        CapabilityIds.ACTION_LAUNCH_APP -> true
        CapabilityIds.ACTION_OPEN_URL -> true
        CapabilityIds.ACTION_SHOW_NOTIFICATION -> true
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
        CapabilityIds.ACTION_OPEN_SETTINGS_SCREEN -> true
        CapabilityIds.ACTION_VIBRATE -> capabilities.hasVibrator
        CapabilityIds.ACTION_SET_GLYPH -> capabilities.hasGlyphLightStripe
        CapabilityIds.ACTION_SET_GLYPH_MATRIX -> capabilities.hasGlyphMatrix
        CapabilityIds.ACTION_COPY_TEXT -> true
        CapabilityIds.ACTION_WAIT -> true

        // Shizuku
        CapabilityIds.SHIZUKU_REQUIRED -> capabilities.shizukuStatus == ShizukuCapabilityStatus.AUTHORIZED

        else -> false
    }

    private fun reasonFor(capability: String): String = when (capability) {
        CapabilityIds.SHIZUKU_REQUIRED,
        CapabilityIds.ACTION_SET_DARK_MODE,
        CapabilityIds.ACTION_SET_EXTRA_DIM,
        CapabilityIds.ACTION_SET_MOBILE_DATA,
        CapabilityIds.ACTION_WRITE_SETTING,
        -> "Shizuku required but not authorized"

        CapabilityIds.ACTION_SET_DND -> "Notification policy access required"
        CapabilityIds.ACTION_SET_BRIGHTNESS,
        CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS,
        CapabilityIds.ACTION_SET_SCREEN_TIMEOUT,
        -> "WRITE_SETTINGS permission required"

        CapabilityIds.ACTION_SET_GLYPH -> "No Glyph light stripe on this device"
        CapabilityIds.ACTION_SET_GLYPH_MATRIX -> "No Glyph Matrix on this device"
        CapabilityIds.TRIGGER_NOTIFICATION -> "Notification listener access required"
        CapabilityIds.TRIGGER_GEOFENCE -> "Location services required"
        else -> "Capability not available: $capability"
    }
}
