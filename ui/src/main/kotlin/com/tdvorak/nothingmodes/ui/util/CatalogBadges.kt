package com.tdvorak.nothingmodes.ui.util

import com.tdvorak.nothingmodes.capabilities.DeviceCapabilities
import com.tdvorak.nothingmodes.engine.model.CapabilityIds

/** Capability IDs that are gated by hardware the phone simply does not have —
 *  a permission grant can never fix them, so catalog rows requiring them are
 *  hidden entirely instead of teased as unavailable. */
private val GLYPH_STRIPE_IDS =
    setOf(
        CapabilityIds.ACTION_SET_GLYPH,
        CapabilityIds.ACTION_GLYPH_ANIMATE,
        CapabilityIds.ACTION_GLYPH_PROGRESS,
        CapabilityIds.ACTION_GLYPH_TEXT,
        CapabilityIds.ACTION_GLYPH_SCROLLING_TEXT,
        CapabilityIds.ACTION_GLYPH_PRESET,
    )
private val GLYPH_MATRIX_IDS =
    setOf(
        CapabilityIds.ACTION_SET_GLYPH_MATRIX,
        CapabilityIds.ACTION_GLYPH_ICON,
        CapabilityIds.ACTION_GLYPH_NUMBER,
        CapabilityIds.ACTION_GLYPH_COUNTDOWN,
        CapabilityIds.ACTION_GLYPH_MUSIC,
    )

/** True when the device permanently lacks the hardware an entry needs —
 *  e.g. glyph stripe actions on a phone with no light stripe. */
fun isHardwareBlocked(
    missing: Set<String>,
    caps: DeviceCapabilities,
): Boolean {
    if (!caps.hasGlyphLightStripe && missing.any { it in GLYPH_STRIPE_IDS }) return true
    if (!caps.hasGlyphMatrix && missing.any { it in GLYPH_MATRIX_IDS }) return true
    if (CapabilityIds.ACTION_GLYPH_TURNOFF in missing && !caps.hasGlyphLightStripe && !caps.hasGlyphMatrix) return true
    if (!caps.hasFlashlight && CapabilityIds.ACTION_SET_FLASHLIGHT in missing) return true
    if (!caps.hasVibrator && CapabilityIds.ACTION_VIBRATE in missing) return true
    if (!caps.hasWifi && CapabilityIds.ACTION_SET_WIFI in missing) return true
    if (!caps.hasBluetooth && CapabilityIds.ACTION_SET_BLUETOOTH in missing) return true
    return false
}

/** Badges for required capability IDs, shown on catalog rows — always
 *  visible (a satisfied requirement still tells the user what the row needs).
 *  Labels stay readable words, not abbreviations. */
fun requirementBadges(required: Set<String>): List<String> {
    val badges = mutableSetOf<String>()
    if (CapabilityIds.SHIZUKU_REQUIRED in required) badges += "SHIZUKU"
    if (required.any { it.startsWith("state_reader_") && it != CapabilityIds.STATE_READER_BUILTIN }) badges += "SHIZUKU"
    if (CapabilityIds.TRIGGER_NOTIFICATION in required || CapabilityIds.ACTION_CLEAR_NOTIFICATIONS in required ||
        CapabilityIds.TRIGGER_MEDIA_PLAYBACK in required
    ) {
        badges += "NOTIFICATIONS"
    }
    if (CapabilityIds.TRIGGER_APP_OPENED in required || CapabilityIds.STATE_FOREGROUND_APP in required) {
        badges += "USAGE ACCESS"
    }
    if (CapabilityIds.TRIGGER_PHONE_SMS in required || CapabilityIds.TRIGGER_PHONE_CALL in required || CapabilityIds.ACTION_SEND_SMS in required) {
        badges += "PHONE"
    }
    if (CapabilityIds.TRIGGER_GEOFENCE in required || CapabilityIds.STATE_LOCATION in required) {
        badges += "LOCATION"
    }
    if (CapabilityIds.TRIGGER_CALENDAR_EVENT in required) badges += "CALENDAR"
    if (required.any { it.startsWith("action_set_glyph") || it.startsWith("action_glyph") }) badges += "GLYPH"

    val known =
        setOf(
            CapabilityIds.SHIZUKU_REQUIRED,
            CapabilityIds.TRIGGER_NOTIFICATION,
            CapabilityIds.ACTION_CLEAR_NOTIFICATIONS,
            CapabilityIds.TRIGGER_MEDIA_PLAYBACK,
            CapabilityIds.TRIGGER_APP_OPENED,
            CapabilityIds.STATE_FOREGROUND_APP,
            CapabilityIds.TRIGGER_PHONE_SMS,
            CapabilityIds.TRIGGER_PHONE_CALL,
            CapabilityIds.ACTION_SEND_SMS,
            CapabilityIds.TRIGGER_GEOFENCE,
            CapabilityIds.STATE_LOCATION,
            CapabilityIds.TRIGGER_CALENDAR_EVENT,
            // Always-satisfied monitor/broadcast-backed triggers — never a
            // setup step, so no badge.
            CapabilityIds.TRIGGER_DEVICE_STATE,
            CapabilityIds.TRIGGER_CONNECTIVITY_AIRPLANE,
            CapabilityIds.TRIGGER_CONNECTIVITY_POWER,
            CapabilityIds.TRIGGER_CONNECTIVITY_WIFI,
            CapabilityIds.TRIGGER_CONNECTIVITY_WIFI_IDENTITY,
            CapabilityIds.TRIGGER_CONNECTIVITY_BT,
            CapabilityIds.TRIGGER_BT_DEVICE,
            CapabilityIds.TRIGGER_WIFI_CONNECTED,
            CapabilityIds.TRIGGER_TIME,
            CapabilityIds.TRIGGER_TIME_WINDOW,
            CapabilityIds.TRIGGER_IMMEDIATE,
            CapabilityIds.TRIGGER_BOOT,
            CapabilityIds.TRIGGER_BATTERY_LEVEL,
            CapabilityIds.TRIGGER_SCREEN_STATE,
            CapabilityIds.TRIGGER_MANUAL,
            CapabilityIds.TRIGGER_TORCH_STATE,
            CapabilityIds.STATE_READER_BUILTIN,
        )
    val shizukuReaders = required.filter { it.startsWith("state_reader_") && it != CapabilityIds.STATE_READER_BUILTIN }
    val glyphActions = required.filter { it.startsWith("action_set_glyph") || it.startsWith("action_glyph") }
    val other = required - known - shizukuReaders.toSet() - glyphActions.toSet()
    if (other.isNotEmpty() && badges.isEmpty()) badges += "SETUP NEEDED"
    return badges.toList()
}

/** Short user-facing hint for a catalog row that cannot run, derived from its
 *  requirement badges. Falls back to the raw reason when no badge matches. */
fun missingCapabilityHint(
    badges: List<String>,
    fallback: String,
): String =
    when {
        "SHIZUKU" in badges -> "Requires Shizuku — a free companion app"
        "LOCATION" in badges -> "Requires location permission"
        "PHONE" in badges -> "Requires phone permission"
        "NOTIFICATIONS" in badges -> "Requires notification access"
        "USAGE ACCESS" in badges -> "Requires usage access"
        "CALENDAR" in badges -> "Requires calendar permission"
        "GLYPH" in badges -> "Requires a Nothing phone"
        "SETUP NEEDED" in badges -> "Needs setup"
        else -> fallback
    }
