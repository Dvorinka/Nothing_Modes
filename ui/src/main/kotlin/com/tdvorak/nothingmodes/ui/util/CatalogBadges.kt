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

/** Short badges for missing capability IDs, shown on catalog rows. */
fun requirementBadges(missing: Set<String>): List<String> {
    val badges = mutableSetOf<String>()
    if (CapabilityIds.SHIZUKU_REQUIRED in missing) badges += "SHIZUKU"
    if (missing.any { it.startsWith("state_reader_") && it != CapabilityIds.STATE_READER_BUILTIN }) badges += "SHIZUKU"
    if (CapabilityIds.TRIGGER_NOTIFICATION in missing || CapabilityIds.ACTION_CLEAR_NOTIFICATIONS in missing) {
        badges += "NOTIF"
    }
    if (CapabilityIds.TRIGGER_APP_OPENED in missing || CapabilityIds.STATE_FOREGROUND_APP in missing) {
        badges += "USAGE"
    }
    if (CapabilityIds.TRIGGER_PHONE_SMS in missing || CapabilityIds.TRIGGER_PHONE_CALL in missing || CapabilityIds.ACTION_SEND_SMS in missing) {
        badges += "PHONE"
    }
    if (CapabilityIds.TRIGGER_GEOFENCE in missing || CapabilityIds.STATE_LOCATION in missing) {
        badges += "LOC"
    }
    if (CapabilityIds.TRIGGER_CALENDAR_EVENT in missing) badges += "CAL"
    if (missing.any { it.startsWith("action_set_glyph") || it.startsWith("action_glyph") }) badges += "GLYPH"

    val known =
        setOf(
            CapabilityIds.SHIZUKU_REQUIRED,
            CapabilityIds.TRIGGER_NOTIFICATION,
            CapabilityIds.ACTION_CLEAR_NOTIFICATIONS,
            CapabilityIds.TRIGGER_APP_OPENED,
            CapabilityIds.STATE_FOREGROUND_APP,
            CapabilityIds.TRIGGER_PHONE_SMS,
            CapabilityIds.TRIGGER_PHONE_CALL,
            CapabilityIds.ACTION_SEND_SMS,
            CapabilityIds.TRIGGER_GEOFENCE,
            CapabilityIds.STATE_LOCATION,
            CapabilityIds.TRIGGER_CALENDAR_EVENT,
        )
    val shizukuReaders = missing.filter { it.startsWith("state_reader_") && it != CapabilityIds.STATE_READER_BUILTIN }
    val glyphActions = missing.filter { it.startsWith("action_set_glyph") || it.startsWith("action_glyph") }
    val other = missing - known - shizukuReaders.toSet() - glyphActions.toSet()
    if (other.isNotEmpty() && badges.isEmpty()) badges += "SETUP"
    return badges.toList()
}

/** Short user-facing hint for a catalog row that cannot run, derived from its
 *  requirement badges. Falls back to the raw reason when no badge matches. */
fun missingCapabilityHint(
    badges: List<String>,
    fallback: String,
): String =
    when {
        "SHIZUKU" in badges -> "Requires Shizuku"
        "LOC" in badges -> "Requires location permission"
        "PHONE" in badges -> "Requires phone permission"
        "NOTIF" in badges -> "Requires notification access"
        "USAGE" in badges -> "Requires usage access"
        "CAL" in badges -> "Requires calendar permission"
        "GLYPH" in badges -> "Requires a Nothing phone"
        "SETUP" in badges -> "Needs setup"
        else -> fallback
    }
