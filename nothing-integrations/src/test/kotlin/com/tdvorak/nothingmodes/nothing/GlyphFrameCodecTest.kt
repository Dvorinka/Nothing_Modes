package com.tdvorak.nothingmodes.nothing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GlyphFrameCodecTest {

    @Test
    fun `round-trip preserves lit pixels inside the circle`() {
        val frame = IntArray(625) { 0 }
        // Center pixel (row 12, col 12) and top row first LED.
        frame[12 * 25 + 12] = 4095
        frame[0 * 25 + GlyphFrameCodec.rowStart(25, 0)] = 2048
        val json = GlyphFrameCodec.encode(listOf(frame), 25)
        val design = GlyphFrameCodec.decode(json)
        assertEquals(1, design.version)
        assertEquals(25, design.gridSize)
        assertEquals(1, design.frames.size)
        // 2048 quantizes to 127/128 on the 0-255 leg; decoded stays > 0.
        val back = design.frames[0].pixels
        assertEquals(4095, back[12 * 25 + 12])
        assert(back[0 * 25 + GlyphFrameCodec.rowStart(25, 0)] > 0)
    }

    @Test
    fun `encode writes 489 leds and drops out-of-circle pixels`() {
        val frame = IntArray(625) { 4095 }
        val json = GlyphFrameCodec.encode(listOf(frame), 25)
        val design = GlyphFrameCodec.decode(json)
        // Corner pixels (0,0) are outside the circle — must be off after round-trip.
        assertEquals(0, design.frames[0].pixels[0])
        assertEquals(0, design.frames[0].pixels[24])
        assertEquals(4095, design.frames[0].pixels[12 * 25 + 12])
    }

    @Test
    fun `decode reads animation durations`() {
        val frames = listOf(IntArray(625) { 4095 }, IntArray(625))
        val json = GlyphFrameCodec.encode(frames, 25, durationsMs = listOf(200, 350))
        val design = GlyphFrameCodec.decode(json)
        assert(design.animated)
        assertEquals(200, design.frames[0].durationMs)
        assertEquals(350, design.frames[1].durationMs)
    }

    @Test
    fun `decode preserves meta attribution`() {
        val json = """{"v":1,"meta":{"author":"pauwma","postId":123,"url":"https://app.glyphmuseum.com/post/123"},"frames":[{"p":[${IntArray(489) { 0 }.joinToString(",")}]}]}"""
        val design = GlyphFrameCodec.decode(json)
        assertEquals("pauwma", design.author)
        assertEquals(123L, design.postId)
        assertEquals("https://app.glyphmuseum.com/post/123", design.url)
    }

    @Test
    fun `decode tolerates missing meta and unknown keys`() {
        val json = """{"v":1,"unknown":"x","frames":[{"p":[${IntArray(489) { 100 }.joinToString(",")}]}],"extra":[1]}"""
        val design = GlyphFrameCodec.decode(json)
        assertNull(design.author)
        assertEquals(1, design.frames.size)
    }

    @Test
    fun `decode rejects wrong pixel counts`() {
        assertThrows(IllegalArgumentException::class.java) {
            GlyphFrameCodec.decode("""{"v":1,"frames":[{"p":[1,2,3]}]}""")
        }
    }

    @Test
    fun `decode maps v4 to 13x13`() {
        val p = IntArray(137) { 255 }.joinToString(",")
        val design = GlyphFrameCodec.decode("""{"v":4,"frames":[{"p":[$p]}]}""")
        assertEquals(13, design.gridSize)
        assertEquals(4095, design.frames[0].pixels[6 * 13 + 6])
    }
}
