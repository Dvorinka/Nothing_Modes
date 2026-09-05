package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.Serializable

/**
 * Optional attribution metadata for exported/shared routines.
 * All fields are optional so old exports and anonymous shares keep working.
 */
@Serializable
data class CreatorProfile(
    val displayName: String = "",
    val handle: String = "",
    val email: String = "",
    val note: String = "",
    val license: String = DEFAULT_LICENSE,
) {
    companion object {
        const val DEFAULT_LICENSE = "GPL-3.0"

        /** Valid SPDX-ish license tags the UI can offer. */
        val COMMON_LICENSES =
            listOf(
                "GPL-3.0",
                "GPL-2.0",
                "MIT",
                "Apache-2.0",
                "BSD-3-Clause",
                "CC0-1.0",
                "Proprietary",
                "Other",
            )
    }

    val isAnonymous: Boolean get() = displayName.isBlank() && handle.isBlank() && email.isBlank()

    /** Sanitizes values that should not be exported. */
    fun sanitized(): CreatorProfile =
        copy(
            displayName = displayName.trim().take(120),
            handle = handle.trim().take(120),
            email = email.trim().take(320),
            note = note.trim().take(1000),
            license = license.trim().take(80).ifBlank { DEFAULT_LICENSE },
        )
}
