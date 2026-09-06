package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.ActionTypeIds
import com.tdvorak.nothingmodes.engine.model.CapabilityIds
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.model.MusicVisualizerStyles
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.isGlyphAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActionGlyphMusicTest {
    @Test
    fun `default style matches MusicVisualizerStyles default`() {
        assertEquals(MusicVisualizerStyles.default(), Action.GlyphMusic().style)
    }

    @Test
    fun `custom style is preserved`() {
        assertEquals("bars", Action.GlyphMusic("bars").style)
    }

    @Test
    fun `isGlyphAction is true for GlyphMusic`() {
        assertTrue(Action.GlyphMusic().isGlyphAction)
    }

    @Test
    fun `GlyphTurnOff is not a glyph action for restore purposes`() {
        assertFalse(Action.GlyphTurnOff.isGlyphAction)
    }

    @Test
    fun `serializes with glyph_music discriminator`() {
        val json = EngineJson.json.encodeToString(Action.serializer(), Action.GlyphMusic("vinyl"))
        assertTrue(json.contains("\"${ActionTypeIds.GLYPH_MUSIC}\""))
        val back = EngineJson.json.decodeFromString(Action.serializer(), json)
        assertEquals(Action.GlyphMusic("vinyl"), back)
    }

    @Test
    fun `serializes default style when omitted`() {
        val json = EngineJson.json.encodeToString(Action.serializer(), Action.GlyphMusic())
        val back = EngineJson.json.decodeFromString(Action.serializer(), json) as Action.GlyphMusic
        assertEquals(MusicVisualizerStyles.default(), back.style)
    }

    @Test
    fun `deserialization tolerates unknown style strings`() {
        // The model does not validate the style on decode — it stores it as-is.
        val json = """{"type":"${ActionTypeIds.GLYPH_MUSIC}","style":"bogus"}"""
        val back = EngineJson.json.decodeFromString(Action.serializer(), json) as Action.GlyphMusic
        assertEquals("bogus", back.style)
    }

    @Test
    fun `two GlyphMusic with different styles are not equal`() {
        assertNotEquals(Action.GlyphMusic("bars"), Action.GlyphMusic("pulse"))
    }

    @Test
    fun `capability requirements include glyph music`() {
        val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(Action.GlyphMusic()))
        assertTrue(CapabilityIds.ACTION_GLYPH_MUSIC in caps)
    }

    @Test
    fun `capability requirements include glyph countdown`() {
        val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(Action.GlyphCountdown(30)))
        assertTrue(CapabilityIds.ACTION_GLYPH_COUNTDOWN in caps)
    }

    @Test
    fun `glyph music and countdown coexist in one automation`() {
        val caps =
            CapabilityRequirements.derive(
                Trigger.Immediate,
                listOf(Action.GlyphMusic("bars"), Action.GlyphCountdown(10)),
            )
        assertTrue(CapabilityIds.ACTION_GLYPH_MUSIC in caps)
        assertTrue(CapabilityIds.ACTION_GLYPH_COUNTDOWN in caps)
    }

    @Test
    fun `raw json without style field decodes to default`() {
        val json = """{"type":"${ActionTypeIds.GLYPH_MUSIC}"}"""
        val back = EngineJson.json.decodeFromString(Action.serializer(), json) as Action.GlyphMusic
        assertEquals(MusicVisualizerStyles.default(), back.style)
    }

    @Test
    fun `lenient json ignores unknown fields on GlyphMusic`() {
        val json = """{"type":"${ActionTypeIds.GLYPH_MUSIC}","style":"pulse","extra":42}"""
        val back = EngineJson.json.decodeFromString(Action.serializer(), json) as Action.GlyphMusic
        assertEquals("pulse", back.style)
    }
}
