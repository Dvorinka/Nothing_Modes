package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.MusicVisualizerStyles
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MusicVisualizerStylesTest {
    @Test
    fun `default is waveform`() {
        assertEquals("waveform", MusicVisualizerStyles.default())
    }

    @Test
    fun `ALL exposes the five styles in canonical order`() {
        assertEquals(
            listOf("waveform", "bars", "mirror", "pulse", "vinyl"),
            MusicVisualizerStyles.ALL,
        )
    }

    @Test
    fun `ALL has no duplicates`() {
        assertEquals(MusicVisualizerStyles.ALL.size, MusicVisualizerStyles.ALL.toSet().size)
    }

    @Test
    fun `normalize accepts each known style unchanged`() {
        MusicVisualizerStyles.ALL.forEach { style ->
            assertEquals(style, MusicVisualizerStyles.normalize(style))
        }
    }

    @Test
    fun `normalize coerces null to default`() {
        assertEquals(MusicVisualizerStyles.default(), MusicVisualizerStyles.normalize(null))
    }

    @Test
    fun `normalize coerces unknown style to default`() {
        assertEquals(
            MusicVisualizerStyles.default(),
            MusicVisualizerStyles.normalize("does-not-exist"),
        )
    }

    @Test
    fun `normalize is case-sensitive`() {
        // Uppercase variants are not in the canonical set and must fall back.
        assertEquals(
            MusicVisualizerStyles.default(),
            MusicVisualizerStyles.normalize("WAVEFORM"),
        )
    }

    @Test
    fun `normalize coerces blank to default`() {
        assertEquals(MusicVisualizerStyles.default(), MusicVisualizerStyles.normalize(""))
    }

    @Test
    fun `every ALL entry is non-blank`() {
        MusicVisualizerStyles.ALL.forEach { style ->
            assertTrue(style.isNotBlank(), "style '$style' should not be blank")
        }
    }
}
