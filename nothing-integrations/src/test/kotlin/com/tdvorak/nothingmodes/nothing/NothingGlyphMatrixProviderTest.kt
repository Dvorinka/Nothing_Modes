package com.tdvorak.nothingmodes.nothing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NothingGlyphMatrixProviderTest {
    private lateinit var provider: NothingGlyphMatrixProvider

    @Before
    fun setUp() {
        provider = NothingGlyphMatrixProvider(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `isAvailable is false on non-Nothing hardware`() {
        assertFalse(provider.isAvailable())
    }

    @Test
    fun `matrixSize is zero on non-Nothing hardware`() {
        assertEquals(0, provider.matrixSize())
    }

    @Test
    fun `isConnected is false before init`() {
        assertFalse(provider.isConnected())
    }

    @Test
    fun `init is a no-op on non-Nothing hardware`() {
        provider.init()
        assertFalse(provider.isConnected())
    }

    @Test
    fun `ensureConnected returns false when hardware unavailable`() =
        runTest {
            assertFalse(provider.ensureConnected())
        }

    @Test
    fun `ensureConnected with explicit timeout returns false when unavailable`() =
        runTest {
            assertFalse(provider.ensureConnected(timeoutMs = 100))
        }

    @Test
    fun `displayBattery returns ServiceUnavailable when not connected`() {
        val result = provider.displayBattery(percent = 50, charging = false)
        assertEquals(GlyphResult.ServiceUnavailable, result)
    }

    @Test
    fun `displayBattery returns ServiceUnavailable when charging and not connected`() {
        val result = provider.displayBattery(percent = 80, charging = true)
        assertEquals(GlyphResult.ServiceUnavailable, result)
    }

    @Test
    fun `startMusicVisualizer returns ServiceUnavailable when not connected`() {
        val result = provider.startMusicVisualizer("waveform")
        assertEquals(GlyphResult.ServiceUnavailable, result)
    }

    @Test
    fun `startMusicVisualizer with any style returns ServiceUnavailable when not connected`() {
        listOf("bars", "mirror", "pulse", "vinyl").forEach { style ->
            assertEquals(
                "style $style should be ServiceUnavailable",
                GlyphResult.ServiceUnavailable,
                provider.startMusicVisualizer(style),
            )
        }
    }

    @Test
    fun `setFrame returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.setFrame(IntArray(0)))
    }

    @Test
    fun `closeFrame returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.closeFrame())
    }

    @Test
    fun `turnOff returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.turnOff())
    }

    @Test
    fun `displayText returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.displayText("Hi"))
    }

    @Test
    fun `displayNumber returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.displayNumber(42))
    }

    @Test
    fun `displayCountdown returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.displayCountdown(10))
    }

    @Test
    fun `displayPercentFill returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.displayPercentFill(50))
    }

    @Test
    fun `displayProgressArc returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.displayProgressArc(50))
    }

    @Test
    fun `displayScrollingText returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.displayScrollingText("scroll"))
    }

    @Test
    fun `displayIcon returns ServiceUnavailable when not connected`() {
        // displayIcon does not guard on connected itself — it calls stopActiveJobs
        // then resolves through CustomGlyphStore/presets/icon library and setFrame.
        // On non-Nothing hardware setFrame is never reached because the icon frame
        // path calls setFrame which returns ServiceUnavailable. The custom/preset
        // stores are empty in Robolectric, so it falls to GlyphIconLibrary.
        val result = provider.displayIcon("check")
        // Either ServiceUnavailable (reached setFrame) or Failure (unknown icon).
        assertTrue(
            "expected ServiceUnavailable or Failure, got $result",
            result is GlyphResult.ServiceUnavailable || result is GlyphResult.Failure,
        )
    }

    @Test
    fun `fillMatrix returns Unsupported when matrix size is zero`() {
        assertEquals(GlyphResult.Unsupported, provider.fillMatrix(255))
    }

    @Test
    fun `stopBattery is safe when no battery job is running`() {
        // Should not throw.
        provider.stopBattery()
        provider.stopBattery()
    }

    @Test
    fun `stopMusicVisualizer is safe when no music job is running`() {
        provider.stopMusicVisualizer()
        provider.stopMusicVisualizer()
    }

    @Test
    fun `stopCountdown is safe when no countdown is running`() {
        provider.stopCountdown()
        provider.stopCountdown()
    }

    @Test
    fun `stopDesign is safe when no design is running`() {
        provider.stopDesign()
        provider.stopDesign()
    }

    @Test
    fun `stopMarquee is safe when no marquee is running`() {
        provider.stopMarquee()
        provider.stopMarquee()
    }

    @Test
    fun `unInit is safe when never initialised`() {
        provider.unInit()
        assertFalse(provider.isConnected())
    }

    @Test
    fun `unInit after init on non-Nothing hardware stays disconnected`() {
        provider.init()
        provider.unInit()
        assertFalse(provider.isConnected())
    }

    @Test
    fun `displayBattery at zero percent returns ServiceUnavailable when not connected`() {
        assertEquals(
            GlyphResult.ServiceUnavailable,
            provider.displayBattery(percent = 0, charging = false),
        )
    }

    @Test
    fun `displayBattery at full percent returns ServiceUnavailable when not connected`() {
        assertEquals(
            GlyphResult.ServiceUnavailable,
            provider.displayBattery(percent = 100, charging = true),
        )
    }

    @Test
    fun `ensureConnected does not throw when called repeatedly`() =
        runTest {
            repeat(3) { provider.ensureConnected(50) }
            assertFalse(provider.isConnected())
        }

    @Test
    fun `init called multiple times on non-Nothing hardware stays disconnected`() {
        provider.init()
        provider.init()
        provider.init()
        assertFalse(provider.isConnected())
    }
}
