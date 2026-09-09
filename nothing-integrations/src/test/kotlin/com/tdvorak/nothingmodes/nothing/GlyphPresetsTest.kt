package com.tdvorak.nothingmodes.nothing

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GlyphPresetsTest {
    @Test
    fun `alarm ringing returns a stripe`() {
        assertNotNull(GlyphPresets.alarmRinging)
    }

    @Test
    fun `volume level returns a stripe with progress`() {
        val visual = GlyphPresets.volumeLevel(73)
        assertEquals(73, (visual as GlyphPresets.GlyphVisual.Stripe).progress)
    }

    @Test
    fun `volume level coerces out of range`() {
        assertEquals(0, (GlyphPresets.volumeLevel(-10) as GlyphPresets.GlyphVisual.Stripe).progress)
        assertEquals(100, (GlyphPresets.volumeLevel(150) as GlyphPresets.GlyphVisual.Stripe).progress)
    }

    @Test
    fun `brightness level returns a stripe with progress`() {
        val visual = GlyphPresets.brightnessLevel(42)
        assertEquals(42, (visual as GlyphPresets.GlyphVisual.Stripe).progress)
    }

    @Test
    fun `battery level uses red fill under 20 percent`() {
        val low = GlyphPresets.batteryLevel(15) as GlyphPresets.GlyphVisual.Matrix
        assertEquals(Color.RED, low.fillColor)
        val high = GlyphPresets.batteryLevel(25) as GlyphPresets.GlyphVisual.Matrix
        assertEquals(Color.WHITE, high.fillColor)
    }

    @Test
    fun `existing presets remain defined`() {
        assertNotNull(GlyphPresets.chargingStart)
        assertNotNull(GlyphPresets.incomingCall)
        assertNotNull(GlyphPresets.smsReceived)
        assertNotNull(GlyphPresets.notificationHigh)
        assertNotNull(GlyphPresets.timerProgress(50))
        assertNotNull(GlyphPresets.timerProgressMatrix(50))
    }
}
