package com.tdvorak.nothingmodes.ui.util

import com.tdvorak.nothingmodes.engine.model.CapabilityIds

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
