package com.tdvorak.nothingmodes.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

/**
 * USB/MCP control surface for Nothing Modes.
 *
 * This receiver is intentionally **debug-only**: it is not exported, and the
 * only practical way to reach it is via `adb shell am broadcast -n` while USB
 * debugging is enabled. It lets an AI agent on the host read, write, run and
 * delete modes over USB.
 *
 * Commands are sent as broadcast extras:
 *   --es "command" "list|get|save|run|delete"
 *   --es "id"      "<mode id>"               (get/run/delete)
 *   --es "json"    "<automation json>"       (save)
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
        val command = intent.getStringExtra(EXTRA_COMMAND) ?: ""
        val payload = intent.getStringExtra(EXTRA_JSON) ?: ""
        val id = intent.getStringExtra(EXTRA_ID) ?: ""

        scope.launch {
            val response =
                try {
                    when (command) {
                        "list" -> listModes()
                        "get" -> getMode(id)
                        "save" -> saveMode(payload)
                        "run" -> runMode(context, id)
                        "delete" -> deleteMode(id)
                        else -> McpResponse.error("unknown command: $command")
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
        return McpResponse(ok = true, mode = mode.asMap())
    }

    private suspend fun saveMode(json: String): McpResponse {
        val mode =
            runCatching { EngineJson.json.decodeFromString(Automation.serializer(), json) }
                .getOrElse { return McpResponse.error("invalid mode json: ${it.message}") }
        store.save(mode)
        return McpResponse(ok = true, mode = mode.asMap())
    }

    private suspend fun runMode(
        context: Context,
        id: String,
    ): McpResponse {
        val mode = store.get(AutomationId(id)) ?: return McpResponse.error("mode not found")
        if (!mode.enabled || mode.trigger !is com.tdvorak.nothingmodes.engine.model.Trigger.Manual) {
            return McpResponse.error("mode is not enabled or is not manual")
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

    companion object {
        const val TAG = "NothingMcp"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_JSON = "json"
        const val EXTRA_ID = "id"
        const val RESPONSE_FILE = "mcp-response.json"
    }
}
