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
class NothingGlyphProviderTest {
    private lateinit var provider: NothingGlyphProvider

    @Before
    fun setUp() {
        provider = NothingGlyphProvider(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun `isAvailable is false on non-Nothing hardware`() {
        assertFalse(provider.isAvailable())
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
    fun `toggle returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.toggle())
    }

    @Test
    fun `toggleZone returns Failure for unknown zone when not connected`() {
        // toggleZone resolves channels from deviceChannels (null on non-Nothing)
        // before checking connected, so it returns Failure("Unknown zone").
        val result = provider.toggleZone("A")
        assertTrue(result is GlyphResult.Failure)
    }

    @Test
    fun `turnOff returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.turnOff())
    }

    @Test
    fun `animate returns ServiceUnavailable when not connected`() {
        assertEquals(
            GlyphResult.ServiceUnavailable,
            provider.animate(listOf(1), periodMs = 1000, cycles = 1, intervalMs = 10),
        )
    }

    @Test
    fun `animateZone returns Failure for unknown zone when not connected`() {
        val result = provider.animateZone("B")
        assertTrue(result is GlyphResult.Failure)
    }

    @Test
    fun `displayProgress returns ServiceUnavailable when not connected`() {
        assertEquals(GlyphResult.ServiceUnavailable, provider.displayProgress(50))
    }

    @Test
    fun `displayProgressAndToggle returns ServiceUnavailable when not connected`() {
        assertEquals(
            GlyphResult.ServiceUnavailable,
            provider.displayProgressAndToggle(50),
        )
    }

    @Test
    fun `availableZones is empty on non-Nothing hardware`() {
        assertTrue(provider.availableZones().isEmpty())
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
    fun `ensureConnected does not throw when called repeatedly`() =
        runTest {
            repeat(3) { provider.ensureConnected(50) }
            assertFalse(provider.isConnected())
        }

    @Test
    fun `init called multiple times on non-Nothing hardware stays disconnected`() {
        provider.init()
        provider.init()
        assertFalse(provider.isConnected())
    }

    @Test
    fun `ensureConnected zero timeout returns false when unavailable`() =
        runTest {
            assertFalse(provider.ensureConnected(timeoutMs = 0))
        }

    @Test
    fun `toggle with explicit channels returns ServiceUnavailable when not connected`() {
        assertEquals(
            GlyphResult.ServiceUnavailable,
            provider.toggle(listOf(1, 2, 3)),
        )
    }
}
