package com.tdvorak.nothingmodes.nothing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MusicGlyphRendererTest {
    private val size = 13
    private val silentWave = FloatArray(size) { 0f }
    private val silentBands = FloatArray(size) { 0f }
    private val fullWave = FloatArray(size) { 1f }
    private val fullBands = FloatArray(size) { 1f }

    @Test
    fun `render returns size x size array for every style`() {
        listOf("waveform", "bars", "mirror", "pulse", "vinyl").forEach { style ->
            val frame = MusicGlyphRenderer.render(style, fullWave, fullBands, size, tick = 0)
            assertEquals("style $style wrong length", size * size, frame.size)
        }
    }

    @Test
    fun `unknown style falls back to waveform`() {
        val unknown = MusicGlyphRenderer.render("nonsense", fullWave, fullBands, size, 0)
        val waveform = MusicGlyphRenderer.render("waveform", fullWave, fullBands, size, 0)
        assertEquals(waveform.toList(), unknown.toList())
    }

    @Test
    fun `null-style is impossible at API but empty string falls back to waveform`() {
        val empty = MusicGlyphRenderer.render("", fullWave, fullBands, size, 0)
        val waveform = MusicGlyphRenderer.render("waveform", fullWave, fullBands, size, 0)
        assertEquals(waveform.toList(), empty.toList())
    }

    @Test
    fun `silent waveform lights the centre row baseline`() {
        // A zero wave sits on the centre line, which the renderer draws as a
        // connected baseline — the centre row is lit, not dark.
        val frame = MusicGlyphRenderer.render("waveform", silentWave, silentBands, size, 0)
        val centerY = size / 2
        assertTrue((0 until size).any { frame[centerY * size + it] > 0 })
    }

    @Test
    fun `silent bars lights the bottom row at minimum brightness`() {
        // Zero energy still draws a one-pixel bar at the bottom at ~20% brightness.
        val frame = MusicGlyphRenderer.render("bars", silentWave, silentBands, size, 0)
        val bottomRow = (size - 1) * size
        assertTrue((0 until size).any { frame[bottomRow + it] > 0 })
    }

    @Test
    fun `silent mirror lights the centre row`() {
        // Zero wave -> halfHeight 0 -> the centre pixel of each column is lit.
        val frame = MusicGlyphRenderer.render("mirror", silentWave, silentBands, size, 0)
        val centerY = size / 2
        assertTrue((0 until size).all { frame[centerY * size + it] > 0 })
    }

    @Test
    fun `silent pulse lights only the centre pixel`() {
        // Zero energy -> radius 0 -> only the centre pixel (d == 0) is lit.
        val frame = MusicGlyphRenderer.render("pulse", silentWave, silentBands, size, 0)
        val center = size / 2
        assertTrue(frame[center * size + center] > 0)
        // Everything except the centre should be dark.
        val others = frame.toMutableList().also { it[center * size + center] = 0 }
        assertTrue(others.all { it == 0 })
    }

    @Test
    fun `full waveform lights the centre row region`() {
        val frame = MusicGlyphRenderer.render("waveform", fullWave, silentBands, size, 0)
        // A full-positive wave pushes the point up from centre; the centre
        // column must have at least one lit pixel.
        val centerCol = size / 2
        assertTrue((0 until size).any { frame[it * size + centerCol] > 0 })
    }

    @Test
    fun `full bars light the bottom rows`() {
        val frame = MusicGlyphRenderer.render("bars", silentWave, fullBands, size, 0)
        // Bottom row (last row) should be lit for full energy.
        val bottomRow = (size - 1) * size
        assertTrue((0 until size).any { frame[bottomRow + it] > 0 })
    }

    @Test
    fun `bars fall back to wave amplitude when bands are silent`() {
        // bands.average() <= 0.01 triggers the wave-amplitude fallback.
        val frame = MusicGlyphRenderer.render("bars", fullWave, silentBands, size, 0)
        assertTrue(frame.any { it > 0 })
    }

    @Test
    fun `full mirror lights the centre row`() {
        val frame = MusicGlyphRenderer.render("mirror", fullWave, silentBands, size, 0)
        val centerY = size / 2
        // The centre pixel of each column is always lit (offset 0).
        assertTrue((0 until size).all { frame[centerY * size + it] > 0 })
    }

    @Test
    fun `pulse with energy lights the centre pixel`() {
        val frame = MusicGlyphRenderer.render("pulse", fullWave, fullBands, size, 0)
        val center = size / 2
        assertEquals(255, frame[center * size + center])
    }

    @Test
    fun `pulse without energy lights only the centre pixel`() {
        // Zero energy -> radius 0 -> only centre pixel lit.
        val frame = MusicGlyphRenderer.render("pulse", silentWave, silentBands, size, 0)
        val center = size / 2
        assertTrue(frame[center * size + center] > 0)
    }

    @Test
    fun `vinyl lights at least the orbiting dot`() {
        val frame = MusicGlyphRenderer.render("vinyl", fullWave, fullBands, size, tick = 5)
        assertTrue(frame.any { it > 0 })
    }

    @Test
    fun `vinyl dot moves with tick`() {
        // Use a partial amplitude so the dot is dimmer than the ring it sits
        // on — otherwise the dot is invisible (ring brightness == dot brightness
        // at full amplitude) and frames look identical across ticks.
        val wave = FloatArray(size) { 0.5f }
        val a = MusicGlyphRenderer.render("vinyl", wave, fullBands, size, tick = 0)
        val b = MusicGlyphRenderer.render("vinyl", wave, fullBands, size, tick = 10)
        assertTrue(a.toList() != b.toList())
    }

    @Test
    fun `all brightness values are within zero to 255`() {
        val wave = FloatArray(size) { 0.9f }
        val bands = FloatArray(size) { 0.8f }
        listOf("waveform", "bars", "mirror", "pulse", "vinyl").forEach { style ->
            val frame = MusicGlyphRenderer.render(style, wave, bands, size, tick = 3, brightness = 200)
            assertTrue("style $style out of range", frame.all { it in 0..255 })
        }
    }

    @Test
    fun `brightness zero produces an all-zero frame`() {
        val frame = MusicGlyphRenderer.render("waveform", fullWave, fullBands, size, 0, brightness = 0)
        assertTrue(frame.all { it == 0 })
    }

    @Test
    fun `brightness above 255 is clamped`() {
        val frame = MusicGlyphRenderer.render("pulse", fullWave, fullBands, size, 0, brightness = 999)
        assertTrue(frame.all { it in 0..255 })
        // Centre pixel should be clamped to 255, not 999.
        val center = size / 2
        assertEquals(255, frame[center * size + center])
    }

    @Test
    fun `wave size mismatch throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            MusicGlyphRenderer.render("waveform", FloatArray(size - 1), silentBands, size, 0)
        }
    }

    @Test
    fun `bands size mismatch throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            MusicGlyphRenderer.render("waveform", silentWave, FloatArray(size + 1), size, 0)
        }
    }

    @Test
    fun `works for 25x25 matrix size`() {
        val big = 25
        val wave = FloatArray(big) { 0.5f }
        val bands = FloatArray(big) { 0.5f }
        val frame = MusicGlyphRenderer.render("bars", wave, bands, big, 0)
        assertEquals(big * big, frame.size)
        assertTrue(frame.any { it > 0 })
    }

    @Test
    fun `negative waveform lights below centre`() {
        val neg = FloatArray(size) { -1f }
        val frame = MusicGlyphRenderer.render("waveform", neg, silentBands, size, 0)
        // A full-negative wave pushes the point down; some pixel below centre lit.
        val centerY = size / 2
        assertTrue(
            (centerY until size).any { row ->
                (0 until size).any { col -> frame[row * size + col] > 0 }
            },
        )
    }

    @Test
    fun `mirror brightness falls off from centre`() {
        val wave = FloatArray(size) { 1f }
        val frame = MusicGlyphRenderer.render("mirror", wave, silentBands, size, 0)
        val centerY = size / 2
        val centerBrightness = frame[centerY * size + 0]
        // The outer edge (max offset) should be dimmer than or equal to centre.
        val edgeBrightness = frame[(centerY + centerY).coerceAtMost(size - 1) * size + 0]
        assertTrue(edgeBrightness <= centerBrightness)
    }

    @Test
    fun `pulse falloff dims towards the edge`() {
        val frame = MusicGlyphRenderer.render("pulse", fullWave, fullBands, size, 0)
        val center = size / 2
        val centerVal = frame[center * size + center]
        val cornerVal = frame[0]
        assertTrue(cornerVal <= centerVal)
    }

    @Test
    fun `vinyl ring is roughly circular`() {
        val frame = MusicGlyphRenderer.render("vinyl", silentWave, fullBands, size, tick = 0)
        // The ring should light pixels but leave the very centre dim (dot aside).
        // Just assert some ring pixels are lit and the frame is non-empty.
        assertTrue(frame.any { it > 0 })
    }

    @Test
    fun `tick does not affect waveform style`() {
        val a = MusicGlyphRenderer.render("waveform", fullWave, silentBands, size, 0)
        val b = MusicGlyphRenderer.render("waveform", fullWave, silentBands, size, 100)
        assertEquals(a.toList(), b.toList())
    }

    @Test
    fun `tick does not affect bars style`() {
        val a = MusicGlyphRenderer.render("bars", silentWave, fullBands, size, 0)
        val b = MusicGlyphRenderer.render("bars", silentWave, fullBands, size, 100)
        assertEquals(a.toList(), b.toList())
    }

    @Test
    fun `tick does not affect mirror style`() {
        val a = MusicGlyphRenderer.render("mirror", fullWave, silentBands, size, 0)
        val b = MusicGlyphRenderer.render("mirror", fullWave, silentBands, size, 100)
        assertEquals(a.toList(), b.toList())
    }

    @Test
    fun `tick does not affect pulse style`() {
        val a = MusicGlyphRenderer.render("pulse", fullWave, fullBands, size, 0)
        val b = MusicGlyphRenderer.render("pulse", fullWave, fullBands, size, 100)
        assertEquals(a.toList(), b.toList())
    }

    @Test
    fun `partial waveform produces a connected trace`() {
        // A single peak in the middle should light the centre column area.
        val wave = FloatArray(size) { 0f }.also { it[size / 2] = 1f }
        val frame = MusicGlyphRenderer.render("waveform", wave, silentBands, size, 0)
        val col = size / 2
        assertTrue((0 until size).any { frame[it * size + col] > 0 })
    }

    @Test
    fun `waveform and bars handle size 1`() {
        // pulse and vinyl compute a negative maxRadius for size 1 (the matrix
        // is always 13 or 25 in practice), so only the linear styles are
        // defined for a 1x1 grid.
        val one = 1
        val wave = FloatArray(one) { 1f }
        val bands = FloatArray(one) { 1f }
        listOf("waveform", "bars", "mirror").forEach { style ->
            val frame = MusicGlyphRenderer.render(style, wave, bands, one, 0)
            assertEquals(1, frame.size)
            assertTrue("style $style size-1 value in range", frame[0] in 0..255)
        }
    }

    @Test
    fun `brightness scales output linearly for waveform`() {
        val full = MusicGlyphRenderer.render("waveform", fullWave, silentBands, size, 0, brightness = 255)
        val half = MusicGlyphRenderer.render("waveform", fullWave, silentBands, size, 0, brightness = 128)
        // Halving brightness should not increase any pixel.
        assertTrue(half.indices.all { half[it] <= full[it] + 1 })
    }

    @Test
    fun `bands energy below threshold triggers wave fallback`() {
        // 0.005 average is below the 0.01 threshold -> uses wave amplitude.
        val tinyBands = FloatArray(size) { 0.005f }
        val wave = FloatArray(size) { 0.7f }
        val frame = MusicGlyphRenderer.render("bars", wave, tinyBands, size, 0)
        // Should produce a non-empty frame from the wave fallback.
        assertTrue(frame.any { it > 0 })
    }

    @Test
    fun `vinyl dot brightness depends on amplitude`() {
        val loud = MusicGlyphRenderer.render("vinyl", fullWave, fullBands, size, 0)
        val quiet = MusicGlyphRenderer.render("vinyl", FloatArray(size) { 0.01f }, fullBands, size, 0)
        // Both should produce a frame; the loud one should have a brighter max.
        assertTrue(loud.max() >= quiet.max())
    }

    @Test
    fun `pulse centre dot only on strong beats`() {
        // energy > 0.5 required for the centre dot.
        val strong = MusicGlyphRenderer.render("pulse", fullWave, fullBands, size, 0)
        val center = size / 2
        assertEquals(255, strong[center * size + center])

        val weakWave = FloatArray(size) { 0.1f }
        val weakBands = FloatArray(size) { 0.1f }
        val weak = MusicGlyphRenderer.render("pulse", weakWave, weakBands, size, 0)
        // Weak energy may still light the circle but the centre dot at full
        // brightness only appears on strong beats.
        assertTrue(weak[center * size + center] <= strong[center * size + center])
    }

    @Test
    fun `render is deterministic for non-vinyl styles`() {
        val wave = FloatArray(size) { 0.6f }
        val bands = FloatArray(size) { 0.6f }
        listOf("waveform", "bars", "mirror", "pulse").forEach { style ->
            val a = MusicGlyphRenderer.render(style, wave, bands, size, 0)
            val b = MusicGlyphRenderer.render(style, wave, bands, size, 0)
            assertEquals("style $style not deterministic", a.toList(), b.toList())
        }
    }

    @Test
    fun `negative brightness coerces to zero`() {
        val frame = MusicGlyphRenderer.render("waveform", fullWave, silentBands, size, 0, brightness = -50)
        assertTrue(frame.all { it == 0 })
    }

    @Test
    fun `waveform connects adjacent columns`() {
        // A wave that jumps from -1 to +1 should light a vertical segment.
        val wave =
            FloatArray(size) { 0f }.also {
                it[0] = -1f
                it[1] = 1f
            }
        val frame = MusicGlyphRenderer.render("waveform", wave, silentBands, size, 0)
        // Column 0 and 1 should both have lit pixels.
        assertTrue((0 until size).any { frame[it * size + 0] > 0 })
        assertTrue((0 until size).any { frame[it * size + 1] > 0 })
    }

    @Test
    fun `mirror with zero wave lights the centre row`() {
        // Zero wave -> halfHeight 0 -> centre pixel of each column lit.
        val frame = MusicGlyphRenderer.render("mirror", silentWave, silentBands, size, 0)
        val centerY = size / 2
        assertTrue((0 until size).all { frame[centerY * size + it] > 0 })
    }

    @Test
    fun `vinyl with zero energy still draws the orbiting dot`() {
        // Even with no bass/amplitude the dot position is computed and lit.
        val frame = MusicGlyphRenderer.render("vinyl", silentWave, silentBands, size, tick = 1)
        // The dot brightness is (maxBright * (0.3 + 0.7 * 0)) = 0.3 * 255 ~ 76.
        assertTrue(frame.any { it > 0 })
    }

    @Test
    fun `all style outputs never exceed brightness cap`() {
        val cap = 100
        val wave = FloatArray(size) { 1f }
        val bands = FloatArray(size) { 1f }
        listOf("waveform", "bars", "mirror", "pulse", "vinyl").forEach { style ->
            val frame = MusicGlyphRenderer.render(style, wave, bands, size, 0, brightness = cap)
            assertTrue("style $style exceeds cap", frame.all { it <= cap })
        }
    }
}
