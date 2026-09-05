package com.tdvorak.nothingmodes.ui.prefs

import android.content.Context
import android.content.SharedPreferences
import com.tdvorak.nothingmodes.engine.model.CreatorProfile

/** Lightweight local store for the user's publishing attribution. */
class CreatorPreferences(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun get(): CreatorProfile =
        CreatorProfile(
            displayName = prefs.getString(KEY_DISPLAY_NAME, "") ?: "",
            handle = prefs.getString(KEY_HANDLE, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            note = prefs.getString(KEY_NOTE, "") ?: "",
            license =
                prefs.getString(KEY_LICENSE, CreatorProfile.DEFAULT_LICENSE)
                    ?: CreatorProfile.DEFAULT_LICENSE,
        )

    fun save(profile: CreatorProfile) {
        val sanitized = profile.sanitized()
        prefs
            .edit()
            .putString(KEY_DISPLAY_NAME, sanitized.displayName)
            .putString(KEY_HANDLE, sanitized.handle)
            .putString(KEY_EMAIL, sanitized.email)
            .putString(KEY_NOTE, sanitized.note)
            .putString(KEY_LICENSE, sanitized.license)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "creator_profile"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_HANDLE = "handle"
        private const val KEY_EMAIL = "email"
        private const val KEY_NOTE = "note"
        private const val KEY_LICENSE = "license"
    }
}
