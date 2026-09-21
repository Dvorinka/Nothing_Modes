package com.tdvorak.nothingmodes.quicksettings.cycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure-JVM coverage for spec encoding, step parsing, and wrap-around order. */
class CycleEngineTest {
    @Test
    fun `spec round-trips through encode and decode`() {
        val spec = CycleSpec("screen_timeout", listOf("30000", "60000", "300000"))
        assertEquals(spec, CycleSpec.decode(spec.encode()))
    }

    @Test
    fun `decode rejects specs without steps for static actions`() {
        assertNull(CycleSpec.decode("dnd|"))
        assertNull(CycleSpec.decode("screen_timeout"))
        assertNull(CycleSpec.decode(null))
        assertNull(CycleSpec.decode(""))
    }

    @Test
    fun `decode accepts an empty step list for the mode runner`() {
        val spec = CycleSpec.decode("mode_runner|")
        assertEquals(CycleActions.modeRunner.id, spec?.actionId)
        assertEquals(emptyList<String>(), spec?.steps)
    }

    @Test
    fun `nextValue wraps around the step list`() {
        val steps = listOf("30000", "60000", "300000")
        assertEquals("30000", CycleEngine.nextValue(null, steps))
        assertEquals("60000", CycleEngine.nextValue("30000", steps))
        assertEquals("300000", CycleEngine.nextValue("60000", steps))
        assertEquals("30000", CycleEngine.nextValue("300000", steps))
        // Unknown current value restarts at the first step.
        assertEquals("30000", CycleEngine.nextValue("9999", steps))
        assertNull(CycleEngine.nextValue("30000", emptyList()))
    }

    @Test
    fun `screen timeout parses shorthand durations`() {
        assertEquals(
            listOf("15000", "30000", "60000", "300000", Int.MAX_VALUE.toString()),
            CycleActions.screenTimeout.parseSteps("15s, 30s, 1m, 5 min, never"),
        )
        assertNull(CycleActions.screenTimeout.parseSteps("soon, later"))
    }

    @Test
    fun `brightness accepts auto and percents`() {
        assertEquals(
            listOf("auto", "10", "50", "100"),
            CycleActions.brightness.parseSteps("auto, 10%, 50, 100"),
        )
        assertNull(CycleActions.brightness.parseSteps("auto, 250"))
    }

    @Test
    fun `ultra dim normalizes off to zero`() {
        assertEquals(
            listOf("25", "50", "0"),
            CycleActions.ultraDim.parseSteps("25%, 50, off"),
        )
        assertNull(CycleActions.ultraDim.parseSteps("25, 140"))
    }

    @Test
    fun `media volume accepts percents only`() {
        assertEquals(listOf("0", "50", "100"), CycleActions.mediaVolume.parseSteps("0, 50%, 100"))
        assertNull(CycleActions.mediaVolume.parseSteps("loud"))
    }

    @Test
    fun `enum actions keep the ordered subset`() {
        assertEquals(
            listOf("off", "priority", "total"),
            CycleActions.dnd.allowedValues,
        )
        assertEquals("Priority", CycleActions.dnd.format("priority"))
        assertEquals("On", CycleActions.torch.format("on"))
    }
}
