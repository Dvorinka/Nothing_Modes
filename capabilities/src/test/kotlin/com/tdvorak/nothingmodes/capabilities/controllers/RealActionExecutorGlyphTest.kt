package com.tdvorak.nothingmodes.capabilities.controllers

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.nothing.NothingGlyphProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RealActionExecutorGlyphTest {
    private val ctx: Context = ApplicationProvider.getApplicationContext()
    private val fireContext =
        FireContext(
            eventId = "test",
            executionId = "exec-1",
            automationId = AutomationId("test"),
            actionIndex = 0,
            priority = 0,
        )

    private var savedManufacturer: String? = null
    private var savedModel: String? = null

    @Before
    fun saveBuild() {
        savedManufacturer = Build.MANUFACTURER
        savedModel = Build.MODEL
    }

    @After
    fun restoreBuild() {
        setBuild(savedManufacturer, savedModel)
    }

    private fun simulateNothingPhone3() = setBuild("nothing", "A024")

    private fun setBuild(
        manufacturer: String?,
        model: String?,
    ) {
        Build.MANUFACTURER = manufacturer
        Build.MODEL = model
    }

    private fun executor(
        glyphProvider: NothingGlyphProvider? = null,
        glyphMatrixProvider: NothingGlyphMatrixProvider? = null,
    ): RealActionExecutor =
        RealActionExecutor(
            context = ctx,
            brightness = FakeBrightnessController,
            extraDim = FakeExtraDimController,
            dnd = FakeDndController,
            volume = FakeVolumeController,
            screenTimeout = FakeScreenTimeoutController,
            darkMode = FakeDarkModeController,
            ringer = FakeRingerController,
            shellFactory = null,
            glyphProvider = glyphProvider,
            glyphMatrixProvider = glyphMatrixProvider,
        )

    // ── Non-glyph actions pass through to controllers ──

    @Test
    fun `Wait action returns Success`() =
        runTest {
            val result = executor().execute(Action.Wait(0), fireContext)
            assertEquals(ActionResult.Success, result)
        }

    @Test
    fun `Wait action with duration returns Success`() =
        runTest {
            val result = executor().execute(Action.Wait(10), fireContext)
            assertEquals(ActionResult.Success, result)
        }

    @Test
    fun `CopyText returns Success`() =
        runTest {
            val result = executor().execute(Action.CopyText("hello"), fireContext)
            assertEquals(ActionResult.Success, result)
        }

    @Test
    fun `SetDnd delegates to controller`() =
        runTest {
            val result = executor().execute(Action.SetDnd(DndMode.TOTAL), fireContext)
            assertEquals(ActionResult.Success, result)
        }

    @Test
    fun `SetDarkMode delegates to controller`() =
        runTest {
            val result = executor().execute(Action.SetDarkMode(NightMode.ON), fireContext)
            assertEquals(ActionResult.Success, result)
        }

    @Test
    fun `SetBrightness delegates to controller`() =
        runTest {
            val result = executor().execute(Action.SetBrightness(128), fireContext)
            assertEquals(ActionResult.Success, result)
        }

    @Test
    fun `SetVolume delegates to controller`() =
        runTest {
            val result = executor().execute(Action.SetVolume(mapOf(VolumeStream.MEDIA to 50)), fireContext)
            assertEquals(ActionResult.Success, result)
        }

    // ── Glyph preflight guard on non-Nothing hardware ──

    @Test
    fun `GlyphMusic fails preflight on non-Nothing hardware`() =
        runTest {
            val result = executor().execute(Action.GlyphMusic(), fireContext)
            assertTrue(result is ActionResult.Failure)
            assertTrue((result as ActionResult.Failure).reason.contains("glyph"))
        }

    @Test
    fun `GlyphTurnOff fails preflight on non-Nothing hardware`() =
        runTest {
            val result = executor().execute(Action.GlyphTurnOff, fireContext)
            assertTrue(result is ActionResult.Failure)
        }

    @Test
    fun `SetGlyph fails preflight on non-Nothing hardware`() =
        runTest {
            val result = executor().execute(Action.SetGlyph(true), fireContext)
            assertTrue(result is ActionResult.Failure)
        }

    @Test
    fun `GlyphCountdown fails preflight on non-Nothing hardware`() =
        runTest {
            val result = executor().execute(Action.GlyphCountdown(30), fireContext)
            assertTrue(result is ActionResult.Failure)
        }

    @Test
    fun `GlyphNumber fails preflight on non-Nothing hardware`() =
        runTest {
            val result = executor().execute(Action.GlyphNumber(5), fireContext)
            assertTrue(result is ActionResult.Failure)
        }

    // ── ensureForAction routing on simulated Nothing hardware ──

    @Test
    fun `GlyphMusic without provider fails with service not connected on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.GlyphMusic("bars"), fireContext)
            // Preflight passes (no glyph system app in Robolectric), then
            // ensureForAction -> ensureMatrix -> null provider -> false.
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `GlyphTurnOff without providers fails with service not connected on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.GlyphTurnOff, fireContext)
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `SetGlyph without provider fails with service not connected on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.SetGlyph(true), fireContext)
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `GlyphCountdown without provider fails with service not connected on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.GlyphCountdown(10), fireContext)
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `GlyphMusic with real matrix provider fails to connect on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val matrix = NothingGlyphMatrixProvider(ctx)
            val result = executor(glyphMatrixProvider = matrix).execute(Action.GlyphMusic(), fireContext)
            // ensureConnected returns false in Robolectric (no real SDK service).
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `GlyphTurnOff with real providers fails to connect on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val stripe = NothingGlyphProvider(ctx)
            val matrix = NothingGlyphMatrixProvider(ctx)
            val result =
                executor(glyphProvider = stripe, glyphMatrixProvider = matrix)
                    .execute(Action.GlyphTurnOff, fireContext)
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `GlyphProgress without providers fails with service not connected on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.GlyphProgress(50), fireContext)
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `non-glyph action still works on simulated Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.Wait(0), fireContext)
            assertEquals(ActionResult.Success, result)
        }

    @Test
    fun `SetGlyphMatrix without provider fails with service not connected on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.SetGlyphMatrix(), fireContext)
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `GlyphText without provider fails with service not connected on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.GlyphText("Hi"), fireContext)
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }

    @Test
    fun `GlyphIcon without provider fails with service not connected on Nothing hardware`() =
        runTest {
            simulateNothingPhone3()
            val result = executor().execute(Action.GlyphIcon("check"), fireContext)
            assertTrue(result is ActionResult.Failure)
            assertEquals("glyph service not connected", (result as ActionResult.Failure).reason)
        }
}

// ── Minimal fake controllers ──

private object FakeBrightnessController : BrightnessController {
    override suspend fun setBrightness(level: Int) = ControllerResult.Success

    override suspend fun setAutoBrightness(on: Boolean) = ControllerResult.Success

    override suspend fun getBrightness(): Int? = null

    override suspend fun isAutoBrightness(): Boolean? = null
}

private object FakeExtraDimController : ExtraDimController {
    override suspend fun setExtraDim(on: Boolean) = ControllerResult.Success

    override suspend fun isExtraDimEnabled(): Boolean? = null
}

private object FakeDndController : DndController {
    override suspend fun setDnd(mode: DndMode) = ControllerResult.Success

    override suspend fun getDndMode(): DndMode? = null
}

private object FakeVolumeController : VolumeController {
    override suspend fun setVolume(
        stream: VolumeStream,
        level: Int,
    ) = ControllerResult.Success

    override suspend fun getVolume(stream: VolumeStream): Int? = null
}

private object FakeScreenTimeoutController : ScreenTimeoutController {
    override suspend fun setScreenTimeout(timeoutMs: Int) = ControllerResult.Success

    override suspend fun getScreenTimeout(): Int? = null
}

private object FakeDarkModeController : DarkModeController {
    override suspend fun setDarkMode(mode: NightMode) = ControllerResult.Success

    override suspend fun getDarkMode(): NightMode? = null
}

private object FakeRingerController : RingerController {
    override suspend fun setRinger(mode: String) = ControllerResult.Success
}
