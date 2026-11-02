package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.DeviceStateKeys
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import com.tdvorak.nothingmodes.engine.runtime.TriggerMatcher
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Coverage for the tier-1 trigger batch: matcher edges, inverse edges and
 *  serialization round-trips for the five new triggers plus Send broadcast. */
class NewTriggersBatchTest {
    private val matcher = TriggerMatcher()
    private val json = Json { ignoreUnknownKeys = true }

    private fun roundTrip(trigger: Trigger): Trigger =
        json.decodeFromString(Trigger.serializer(), json.encodeToString(Trigger.serializer(), trigger))

    // --- Media track changed ---

    @Test
    fun `MediaTrackChanged matches track event`() {
        val trigger = Trigger.MediaTrackChanged()
        val event = TriggerEvent.MediaTrackChanged("e1", "com.spotify", "Artist", "Title")
        assertTrue(matcher.matches(trigger, event))
    }

    @Test
    fun `MediaTrackChanged honours package filter`() {
        val trigger = Trigger.MediaTrackChanged(packageName = "com.spotify")
        assertTrue(matcher.matches(trigger, TriggerEvent.MediaTrackChanged("e1", "com.spotify", null, null)))
        assertFalse(matcher.matches(trigger, TriggerEvent.MediaTrackChanged("e2", "com.player", null, null)))
    }

    @Test
    fun `MediaTrackChanged does not match playback event`() {
        val trigger = Trigger.MediaTrackChanged()
        val event = TriggerEvent.MediaPlaybackChanged("e1", false, "com.player", null, null)
        assertFalse(matcher.matches(trigger, event))
    }

    // --- Headphones ---

    @Test
    fun `HeadphonesConnected matches state edge`() {
        val trigger = Trigger.HeadphonesConnected(connected = true)
        val event =
            TriggerEvent.DeviceStateChanged(
                "e1",
                DeviceStateKeys.HEADPHONES,
                value = "true",
                previous = "false",
            )
        assertTrue(matcher.matches(trigger, event))
        assertFalse(matcher.matches(trigger, event.copy(value = "false")))
    }

    @Test
    fun `HeadphonesConnected inverse works for disconnect`() {
        val trigger = Trigger.HeadphonesConnected(connected = false)
        assertTrue(
            matcher.isInverseEdge(
                trigger,
                TriggerEvent.DeviceStateChanged("e1", DeviceStateKeys.HEADPHONES, value = "true", previous = "false"),
            ),
        )
        assertFalse(
            matcher.isInverseEdge(
                trigger,
                TriggerEvent.DeviceStateChanged("e2", DeviceStateKeys.HEADPHONES, value = "false", previous = "true"),
            ),
        )
    }

    @Test
    fun `HeadphonesConnected ignores other state keys`() {
        val trigger = Trigger.HeadphonesConnected()
        assertFalse(
            matcher.matches(
                trigger,
                TriggerEvent.DeviceStateChanged("e1", "other_key", value = "true", previous = "false"),
            ),
        )
    }

    // --- App installed / removed ---

    @Test
    fun `AppInstalledRemoved matches by direction`() {
        val installed = Trigger.AppInstalledRemoved(installed = true)
        val removed = Trigger.AppInstalledRemoved(installed = false)
        assertTrue(matcher.matches(installed, TriggerEvent.AppPackageChanged("e1", true, "com.app")))
        assertFalse(matcher.matches(installed, TriggerEvent.AppPackageChanged("e2", false, "com.app")))
        assertTrue(matcher.matches(removed, TriggerEvent.AppPackageChanged("e3", false, "com.app")))
    }

    @Test
    fun `AppInstalledRemoved honours package filter`() {
        val trigger = Trigger.AppInstalledRemoved(installed = true, packageName = "com.app")
        assertTrue(matcher.matches(trigger, TriggerEvent.AppPackageChanged("e1", true, "com.app")))
        assertFalse(matcher.matches(trigger, TriggerEvent.AppPackageChanged("e2", true, "com.other")))
        assertFalse(matcher.matches(trigger, TriggerEvent.AppPackageChanged("e3", true, null)))
    }

    // --- Intent received ---

    @Test
    fun `IntentReceived matches exact action only`() {
        val trigger = Trigger.IntentReceived("com.example.CUSTOM")
        assertTrue(matcher.matches(trigger, TriggerEvent.IntentReceived("e1", "com.example.CUSTOM")))
        assertFalse(matcher.matches(trigger, TriggerEvent.IntentReceived("e2", "com.example.OTHER")))
    }

    // --- Failed login ---

    @Test
    fun `FailedLogin matches login event only`() {
        val trigger = Trigger.FailedLogin
        assertTrue(matcher.matches(trigger, TriggerEvent.LoginFailed("e1")))
        assertFalse(matcher.matches(trigger, TriggerEvent.BootCompleted("e2")))
    }

    // --- Shake ---

    @Test
    fun `Shake matches shake event only`() {
        val trigger = Trigger.Shake
        assertTrue(matcher.matches(trigger, TriggerEvent.Shake("e1")))
        assertFalse(matcher.matches(trigger, TriggerEvent.LoginFailed("e2")))
    }

    @Test
    fun `DeviceState orientation and proximity events match by key and value`() {
        val portrait = Trigger.DeviceState(DeviceStateKeys.ORIENTATION, "portrait")
        assertTrue(
            matcher.matches(
                portrait,
                TriggerEvent.DeviceStateChanged("e1", DeviceStateKeys.ORIENTATION, value = "portrait", previous = "landscape"),
            ),
        )
        assertFalse(
            matcher.matches(
                portrait,
                TriggerEvent.DeviceStateChanged("e2", DeviceStateKeys.PROXIMITY, value = "portrait", previous = "far"),
            ),
        )
        val near = Trigger.DeviceState(DeviceStateKeys.PROXIMITY, "near")
        assertTrue(
            matcher.matches(
                near,
                TriggerEvent.DeviceStateChanged("e3", DeviceStateKeys.PROXIMITY, value = "near", previous = "far"),
            ),
        )
    }

    // --- Serialization ---

    @Test
    fun `new triggers survive serialization round-trip`() {
        val triggers =
            listOf(
                Trigger.MediaTrackChanged(packageName = "com.spotify"),
                Trigger.MediaTrackChanged(),
                Trigger.HeadphonesConnected(connected = false),
                Trigger.AppInstalledRemoved(installed = true, packageName = "com.app"),
                Trigger.AppInstalledRemoved(installed = false),
                Trigger.IntentReceived("com.example.CUSTOM"),
                Trigger.FailedLogin,
                Trigger.Shake,
            )
        triggers.forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `trigger serial names are stable`() {
        assertTrue(
            json.encodeToString(Trigger.serializer(), Trigger.HeadphonesConnected()).contains(
                "\"type\":\"headphones_connected\"",
            ),
        )
        assertTrue(
            json.encodeToString(Trigger.serializer(), Trigger.FailedLogin).contains(
                "\"type\":\"failed_login\"",
            ),
        )
        assertTrue(
            json.encodeToString(Trigger.serializer(), Trigger.Shake).contains(
                "\"type\":\"shake\"",
            ),
        )
    }
}
