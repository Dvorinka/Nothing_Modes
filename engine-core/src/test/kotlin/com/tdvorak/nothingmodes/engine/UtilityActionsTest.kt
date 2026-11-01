package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.CapabilityIds
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.HttpMethod
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.PrivacyScrubber
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** HTTP/shell/toast/TTS actions: serialization, capability derivation, scrubbing. */
class UtilityActionsTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun roundTrip(action: Action): Action =
        json.decodeFromString(Action.serializer(), json.encodeToString(Action.serializer(), action))

    @Test
    fun `all four survive serialization round-trip`() {
        val actions =
            listOf(
                Action.HttpRequest(HttpMethod.POST, "https://hook.example.com/x", "{\"k\":1}"),
                Action.ShellCommand("pm list packages"),
                Action.ShowToast("hello"),
                Action.SpeakText("good morning"),
            )
        actions.forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `serial names are stable`() {
        assertTrue(
            json.encodeToString(Action.serializer(), Action.HttpRequest())
                .contains("\"type\":\"http_request\""),
        )
        assertTrue(
            json.encodeToString(Action.serializer(), Action.ShellCommand())
                .contains("\"type\":\"shell_command\""),
        )
        assertTrue(
            json.encodeToString(Action.serializer(), Action.ShowToast())
                .contains("\"type\":\"show_toast\""),
        )
        assertTrue(
            json.encodeToString(Action.serializer(), Action.SpeakText())
                .contains("\"type\":\"speak_text\""),
        )
    }

    @Test
    fun `only shell command needs shizuku`() {
        val caps =
            CapabilityRequirements.derive(
                Trigger.Immediate,
                listOf(
                    Action.HttpRequest(),
                    Action.ShellCommand("echo hi"),
                    Action.ShowToast("x"),
                    Action.SpeakText("x"),
                ),
            )
        assertTrue(CapabilityIds.ACTION_HTTP_REQUEST in caps)
        assertTrue(CapabilityIds.ACTION_SHELL_COMMAND in caps)
        assertTrue(CapabilityIds.ACTION_SHOW_TOAST in caps)
        assertTrue(CapabilityIds.ACTION_SPEAK_TEXT in caps)
        assertTrue(CapabilityIds.SHIZUKU_REQUIRED in caps)

        val noShell =
            CapabilityRequirements.derive(
                Trigger.Immediate,
                listOf(Action.HttpRequest(), Action.ShowToast("x"), Action.SpeakText("x")),
            )
        assertTrue(CapabilityIds.SHIZUKU_REQUIRED !in noShell)
    }

    @Test
    fun `scrubber strips webhook url and shell command`() {
        val automation =
            Automation(
                id = AutomationId("a1"),
                name = "n",
                type = AutomationType.ROUTINE,
                createdBy = CreatedBy.USER,
                status = AutomationStatus.ARMED,
                trigger = Trigger.Immediate,
                actions =
                    listOf(
                        Action.HttpRequest(HttpMethod.POST, "https://hook/secret-token", "payload"),
                        Action.ShellCommand("rm -rf /s"),
                        Action.ShowToast("private note"),
                        Action.SpeakText("private"),
                    ),
            )
        val (scrubbed, missing) = PrivacyScrubber.scrub(automation)
        assertTrue(missing.isNotEmpty())
        val http = scrubbed.actions.filterIsInstance<Action.HttpRequest>().single()
        assertEquals("", http.url)
        assertEquals("", http.body)
        assertEquals("", scrubbed.actions.filterIsInstance<Action.ShellCommand>().single().command)
        assertEquals("", scrubbed.actions.filterIsInstance<Action.ShowToast>().single().message)
        assertEquals("", scrubbed.actions.filterIsInstance<Action.SpeakText>().single().text)
    }
}
