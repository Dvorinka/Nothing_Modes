package com.tdvorak.nothingmodes.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tdvorak.nothingmodes.BuildConfig
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.capabilities.CapabilityDetector
import com.tdvorak.nothingmodes.capabilities.CapabilityResolver
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationSchema
import com.tdvorak.nothingmodes.engine.model.CapabilityRequirements
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.io.File
import javax.inject.Inject

/**
 * USB/MCP control surface for Nothing Modes.
 *
 * This receiver is intentionally **debug-only**: it is not exported, and the
 * only practical way to reach it is via `adb shell am broadcast -n` while USB
 * debugging is enabled. It lets an AI agent on the host read, write, run and
 * delete modes over USB.
 *
 * Commands are sent as broadcast extras:
 *   --es "command" "list|get|save|run|delete|validate|explain|guide"
 *   --es "id"      "<mode id>"               (get/run/delete/explain)
 *   --es "json"    "<automation json>"       (save/validate/explain)
 *
 * The response is written to the app cache as `mcp-response.json` and echoed
 * to logcat with the tag `NothingMcp`.
 */
@AndroidEntryPoint
class ModeControlReceiver : BroadcastReceiver() {
    @Inject
    lateinit var store: AutomationStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (!BuildConfig.DEBUG) {
            val response = McpResponse.error("MCP is only available in debug builds")
            writeResponse(context, response)
            Log.i(TAG, EngineJson.json.encodeToString(response))
            return
        }

        val command = intent.getStringExtra(EXTRA_COMMAND) ?: ""
        val payload = intent.getStringExtra(EXTRA_JSON) ?: ""
        val id = intent.getStringExtra(EXTRA_ID) ?: ""

        scope.launch {
            val response =
                try {
                    when (command) {
                        "list" -> listModes()
                        "get" -> getMode(id)
                        "save" -> saveMode(payload, context)
                        "run" -> runMode(context, id)
                        "delete" -> deleteMode(id)
                        "validate" -> validateMode(payload, context)
                        "explain" -> explainMode(payload, id)
                        "guide" -> guide()
                        else -> McpResponse.error("unknown command: $command. Use 'guide'.")
                    }
                } catch (e: Exception) {
                    McpResponse.error(e.message ?: e.toString())
                }

            writeResponse(context, response)
            Log.i(TAG, EngineJson.json.encodeToString(response))
        }
    }

    private suspend fun listModes(): McpResponse {
        val modes = store.all()
        return McpResponse(ok = true, modes = modes.map { it.asMap() })
    }

    private suspend fun getMode(id: String): McpResponse {
        val mode = store.get(AutomationId(id)) ?: return McpResponse.error("mode not found")
        return McpResponse(ok = true, mode = mode.asMap(), explanation = explain(mode))
    }

    private suspend fun saveMode(
        json: String,
        context: Context,
    ): McpResponse {
        val automation = parseAutomation(json) ?: return McpResponse.error("invalid mode json")
        store.save(automation)
        val validation = validateAutomation(automation, context)
        return McpResponse(
            ok = true,
            mode = automation.asMap(),
            explanation = explain(automation),
            validation = validation.checks,
            requiredCapabilities = validation.requiredCapabilities.toList(),
            missingCapabilities = validation.missingCapabilities,
            detail =
                if (validation.missingCapabilities.isEmpty()) {
                    "saved and ready to run"
                } else {
                    "saved, but missing capabilities: ${validation.missingCapabilities.keys.joinToString()}"
                },
        )
    }

    private suspend fun runMode(
        context: Context,
        id: String,
    ): McpResponse {
        val mode = store.get(AutomationId(id)) ?: return McpResponse.error("mode not found")
        if (!mode.enabled || mode.trigger !is com.tdvorak.nothingmodes.engine.model.Trigger.Manual) {
            return McpResponse.error("mode is not enabled or is not manual")
        }

        val validation = validateAutomation(mode, context)
        if (validation.missingCapabilities.isNotEmpty()) {
            return McpResponse.error(
                "cannot run: missing capabilities ${validation.missingCapabilities.keys}",
            )
        }

        val service =
            Intent(context, AutomationService::class.java).apply {
                action = AutomationService.ACTION_MANUAL
                putExtra(AutomationService.EXTRA_MANUAL_ID, id)
            }
        context.startForegroundService(service)
        return McpResponse(ok = true, detail = "started manual run for $id")
    }

    private suspend fun deleteMode(id: String): McpResponse {
        store.delete(AutomationId(id))
        return McpResponse(ok = true, detail = "deleted $id")
    }

    private fun validateMode(
        json: String,
        context: Context,
    ): McpResponse {
        val automation = parseAutomation(json) ?: return McpResponse.error("invalid mode json")
        val validation = validateAutomation(automation, context)
        return McpResponse(
            ok = validation.missingCapabilities.isEmpty() && validation.schemaError == null,
            detail = validation.schemaError ?: validation.summary,
            explanation = explain(automation),
            validation = validation.checks,
            requiredCapabilities = validation.requiredCapabilities.toList(),
            missingCapabilities = validation.missingCapabilities,
        )
    }

    private suspend fun explainMode(
        json: String,
        id: String,
    ): McpResponse {
        val automation =
            if (json.isNotBlank()) {
                parseAutomation(json) ?: return McpResponse.error("invalid mode json")
            } else if (id.isNotBlank()) {
                store.get(AutomationId(id)) ?: return McpResponse.error("mode not found")
            } else {
                return McpResponse.error("supply 'id' or 'json'")
            }

        val capabilities = CapabilityRequirements.derive(automation.trigger, automation.actions, automation.conditions)
        return McpResponse(
            ok = true,
            mode = automation.asMap(),
            explanation = explain(automation),
            requiredCapabilities = capabilities.toList(),
        )
    }

    private fun guide(): McpResponse =
        McpResponse(
            ok = true,
            detail = "MCP control surface active",
            explanation =
                """
                MCP lets an AI agent on the host control Nothing Modes over USB.

                Commands:
                  list                -> list all modes
                  get --es id <id>    -> inspect one mode
                  save --es json <json> -> save a new or overwrite an existing mode
                  validate --es json <json> -> validate without saving
                  explain --es id <id> | --es json <json>
                                      -> human-readable summary of a mode
                  run --es id <id>    -> run a manual mode immediately
                  delete --es id <id> -> delete a mode
                  guide               -> show this message

                JSON schema:
                {
                  "id": "my-mode",
                  "name": "My Mode",
                  "type": "MODE",
                  "createdBy": "LLM",
                  "status": "ARMED",
                  "trigger": {"type": "manual"},
                  "actions": [{"type": "wait", "durationMs": 0}],
                  "enabled": true
                }

                Trigger type values: manual, time, time_window, notification, phone_state, connectivity,
                boot, battery_level, screen_state, app_opened, geofence, bt_device, wifi_connected,
                calendar_event, charger_connected, device_unlocked, device_locked, torch_state, media_playback.

                Action type values: set_wifi, set_bluetooth, set_dnd, set_volume, set_brightness,
                set_wallpaper, set_glyph, glyph_preset, glyph_turnoff, wait, vibrate, media_control, ...

                validate and save both report missing device capabilities so the agent knows what
                will or will not work on the connected phone.
                """.trimIndent(),
        )

    private fun parseAutomation(json: String): Automation? =
        runCatching {
            EngineJson.json.decodeFromString(Automation.serializer(), json)
        }.getOrNull()

    private fun validateAutomation(
        automation: Automation,
        context: Context,
    ): ValidationResult {
        val checks = mutableMapOf<String, String>()

        if (!AutomationSchema.isSupportedVersion(automation.schemaVersion)) {
            checks["schema"] = "unsupported schema version ${automation.schemaVersion}"
            return ValidationResult(checks, schemaError = "unsupported schema version: ${automation.schemaVersion}")
        }
        checks["schema"] = "ok"

        if (automation.actions.isEmpty()) {
            checks["actions"] = "no actions defined"
        } else {
            checks["actions"] = "${automation.actions.size} action(s) defined"
        }

        val required = CapabilityRequirements.derive(automation.trigger, automation.actions, automation.conditions)
        val capabilities = CapabilityDetector(context, ShizukuGateway(context)).detect()
        val resolution = CapabilityResolver(capabilities).resolve(automation.id.value, required)

        val missing = resolution.missing.associateWith { resolution.missingReasons[it] ?: "missing" }
        checks["capabilities"] =
            if (resolution.canRun) {
                "all required capabilities available"
            } else {
                "missing: ${missing.keys.joinToString()}"
            }

        val summary =
            if (resolution.canRun) {
                "valid and can run on this device"
            } else {
                "valid JSON, but cannot run because: ${resolution.missingReasons.values.joinToString("; ")}"
            }

        return ValidationResult(
            checks = checks,
            requiredCapabilities = required,
            missingCapabilities = missing,
            summary = summary,
        )
    }

    private fun explain(automation: Automation): String {
        val triggerText = ModeExplainer.explainTrigger(automation.trigger)
        val actionText = automation.actions.map { ModeExplainer.explainAction(it) }
        val conditionText = automation.conditions?.let { " when ${ModeExplainer.explainCondition(it)}" } ?: ""
        return "When $triggerText$conditionText, then ${actionText.joinToString(", then ")}."
    }

    private fun writeResponse(
        context: Context,
        response: McpResponse,
    ) {
        try {
            File(context.cacheDir, RESPONSE_FILE).writeText(
                EngineJson.json.encodeToString(response),
            )
        } catch (e: Exception) {
            Log.e(TAG, "failed to write response: ${e.message}")
        }
    }

    private fun Automation.asMap(): Map<String, String> =
        mapOf(
            "id" to id.value,
            "name" to name,
            "enabled" to enabled.toString(),
            "trigger" to trigger::class.simpleName.orEmpty(),
            "actions" to actions.size.toString(),
        )

    private data class ValidationResult(
        val checks: Map<String, String>,
        val requiredCapabilities: Set<String> = emptySet(),
        val missingCapabilities: Map<String, String> = emptyMap(),
        val summary: String = "",
        val schemaError: String? = null,
    )

    companion object {
        const val TAG = "NothingMcp"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_JSON = "json"
        const val EXTRA_ID = "id"
        const val RESPONSE_FILE = "mcp-response.json"
    }
}
