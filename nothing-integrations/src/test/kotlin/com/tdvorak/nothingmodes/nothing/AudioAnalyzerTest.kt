package com.tdvorak.nothingmodes.nothing

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioAnalyzerTest {
    private lateinit var analyzer: AudioAnalyzer

    @Before
    fun setUp() {
        analyzer = AudioAnalyzer(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `isSimulated is true before init`() {
        assertTrue(analyzer.isSimulated)
    }

    @Test
    fun `init returns false without RECORD_AUDIO permission`() {
        // Robolectric does not grant RECORD_AUDIO by default.
        assertFalse(analyzer.init())
        assertTrue(analyzer.isSimulated)
    }

    @Test
    fun `getBands returns zeros when no music is active`() {
        val bands = analyzer.getBands(13)
        assertEquals(13, bands.size)
        assertTrue(bands.all { it == 0f })
    }

    @Test
    fun `getWaveform returns zeros when no music is active`() {
        val wave = analyzer.getWaveform(13)
        assertEquals(13, wave.size)
        assertTrue(wave.all { it == 0f })
    }

    @Test
    fun `getBands with zero count returns empty array`() {
        val bands = analyzer.getBands(0)
        assertEquals(0, bands.size)
    }

    @Test
    fun `getWaveform with zero count returns empty array`() {
        val wave = analyzer.getWaveform(0)
        assertEquals(0, wave.size)
    }

    @Test
    fun `isMusicActive is false by default in Robolectric`() {
        assertFalse(analyzer.isMusicActive)
    }

    @Test
    fun `release resets simulated flag`() {
        analyzer.release()
        assertTrue(analyzer.isSimulated)
    }

    @Test
    fun `release is idempotent`() {
        // Calling release twice must not throw.
        analyzer.release()
        analyzer.release()
        assertTrue(analyzer.isSimulated)
    }

    @Test
    fun `init then release leaves simulated true`() {
        analyzer.init()
        analyzer.release()
        assertTrue(analyzer.isSimulated)
    }

    @Test
    fun `getBands values are in zero to one range`() {
        val bands = analyzer.getBands(25)
        assertTrue(bands.all { it in 0f..1f })
    }

    @Test
    fun `getWaveform values are in minus one to one range`() {
        val wave = analyzer.getWaveform(25)
        assertTrue(wave.all { it in -1f..1f })
    }

    @Test
    fun `multiple init attempts do not throw`() {
        assertFalse(analyzer.init())
        assertFalse(analyzer.init())
        assertTrue(analyzer.isSimulated)
    }

    @Test
    fun `getBands after release returns zeros`() {
        analyzer.release()
        val bands = analyzer.getBands(13)
        assertTrue(bands.all { it == 0f })
    }

    @Test
    fun `getWaveform after release returns zeros`() {
        analyzer.release()
        val wave = analyzer.getWaveform(13)
        assertTrue(wave.all { it == 0f })
    }

    @Test
    fun `large band count returns requested size`() {
        val bands = analyzer.getBands(100)
        assertEquals(100, bands.size)
    }

    @Test
    fun `isMusicActive does not throw`() {
        // Just ensure the getter is safe to call repeatedly.
        repeat(5) { analyzer.isMusicActive }
    }
}
