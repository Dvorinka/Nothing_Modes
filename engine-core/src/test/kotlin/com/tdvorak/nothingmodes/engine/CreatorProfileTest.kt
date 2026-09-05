package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.CreatorProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CreatorProfileTest {
    @Test
    fun `default license is GPL-3_0`() {
        val profile = CreatorProfile()
        assertEquals(CreatorProfile.DEFAULT_LICENSE, profile.license)
        assertTrue(profile.isAnonymous)
    }

    @Test
    fun `sanitization trims and limits fields`() {
        val long = "x".repeat(2000)
        val profile =
            CreatorProfile(
                displayName = "  $long  ",
                handle = "@$long",
                email = "$long@example.com",
                note = long,
                license = "",
            ).sanitized()

        assertEquals(120, profile.displayName.length)
        assertEquals(120, profile.handle.length)
        assertEquals(320, profile.email.length)
        assertEquals(1000, profile.note.length)
        assertEquals(CreatorProfile.DEFAULT_LICENSE, profile.license)
        assertFalse(profile.isAnonymous)
    }

    @Test
    fun `blank fields remain anonymous`() {
        val profile = CreatorProfile(displayName = "   ")
        assertTrue(profile.isAnonymous)
    }
}
