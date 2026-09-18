package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.CapabilityIds
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.ClockSection
import com.tdvorak.nothingmodes.engine.model.PrivacySensor
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.model.affectedSettings
import com.tdvorak.nothingmodes.engine.model.canRestore
import com.tdvorak.nothingmodes.engine.model.supportsRestore
import com.tdvorak.nothingmodes.engine.model.withRestore
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Coverage for the settings & clock action batch: serialization, capability
 *  derivation, and restore wiring. */
class NewActionsBatchTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun roundTrip(action: Action): Action =
        json.decodeFromString(Action.serializer(), json.encodeToString(Action.serializer(), action))

    @Test
    fun `new actions survive serialization round-trip`() {
        val actions =
            listOf(
                Action.SetFontScale(1.15f, restore = false),
                Action.SetNightLight(on = true, temperature = 3200, restore = true),
                Action.SetNightLight(on = false),
                Action.SetColorInversion(true),
                Action.SetDaltonizer(false),
                Action.SetSensorPrivacy(PrivacySensor.CAMERA, blocked = true),
                Action.SetOneHandedMode(true),
                Action.SetAlarm(hour = 6, minute = 30, label = "wake", skipUi = false),
                Action.SetTimer(seconds = 90, label = "tea", skipUi = true),
                Action.OpenClock(ClockSection.TIMERS),
            )
        actions.forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `serial names are stable`() {
        val encoded = json.encodeToString(Action.serializer(), Action.SetFontScale(1.0f))
        assertTrue(encoded.contains("\"type\":\"set_font_scale\""))
        val timer = json.encodeToString(Action.serializer(), Action.SetTimer(60))
        assertTrue(timer.contains("\"type\":\"set_timer\""))
    }

    @Test
    fun `font scale needs only write-settings capability`() {
        val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(Action.SetFontScale(1.0f)))
        assertTrue(CapabilityIds.ACTION_SET_FONT_SCALE in caps)
        assertFalse(CapabilityIds.SHIZUKU_REQUIRED in caps)
    }

    @Test
    fun `shizuku-gated actions derive shizuku capability`() {
        val actions =
            listOf(
                Action.SetNightLight(true),
                Action.SetColorInversion(true),
                Action.SetDaltonizer(true),
                Action.SetSensorPrivacy(PrivacySensor.MIC, blocked = true),
                Action.SetOneHandedMode(true),
            )
        val expected =
            listOf(
                CapabilityIds.ACTION_SET_NIGHT_LIGHT,
                CapabilityIds.ACTION_SET_COLOR_INVERSION,
                CapabilityIds.ACTION_SET_DALTONIZER,
                CapabilityIds.ACTION_SET_SENSOR_PRIVACY,
                CapabilityIds.ACTION_SET_ONE_HANDED_MODE,
            )
        actions.forEachIndexed { i, action ->
            val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(action))
            assertTrue(expected[i] in caps, "missing ${expected[i]} for $action")
            assertTrue(CapabilityIds.SHIZUKU_REQUIRED in caps, "missing shizuku for $action")
        }
    }

    @Test
    fun `clock actions need no shizuku`() {
        val actions =
            listOf(
                Action.SetAlarm(7, 0) to CapabilityIds.ACTION_SET_ALARM,
                Action.SetTimer(60) to CapabilityIds.ACTION_SET_TIMER,
                Action.OpenClock() to CapabilityIds.ACTION_OPEN_CLOCK,
            )
        actions.forEach { (action, id) ->
            val caps = CapabilityRequirements.derive(Trigger.Immediate, listOf(action))
            assertTrue(id in caps)
            assertFalse(CapabilityIds.SHIZUKU_REQUIRED in caps)
        }
    }

    @Test
    fun `restorable actions expose affected settings`() {
        assertEquals(setOf("font_scale"), Action.SetFontScale(1.0f).affectedSettings)
        assertTrue(
            Action.SetNightLight(true).affectedSettings
                .containsAll(setOf("night_display_activated", "night_display_color_temperature")),
        )
        assertEquals(
            setOf("accessibility_display_inversion_enabled"),
            Action.SetColorInversion(true).affectedSettings,
        )
        assertEquals(
            setOf("accessibility_display_daltonizer_enabled"),
            Action.SetDaltonizer(true).affectedSettings,
        )
        assertEquals(
            setOf("one_handed_mode_enabled"),
            Action.SetOneHandedMode(true).affectedSettings,
        )
    }

    @Test
    fun `restore flags round through withRestore`() {
        val a = Action.SetFontScale(1.2f, restore = false)
        assertTrue(a.canRestore)
        assertFalse(a.supportsRestore)
        assertTrue((a.withRestore(true) as Action.SetFontScale).restore)

        assertFalse(Action.SetSensorPrivacy(PrivacySensor.MIC, blocked = true).canRestore)
        assertFalse(Action.SetAlarm(7, 0).canRestore)
        assertFalse(Action.OpenClock().canRestore)
    }
}
