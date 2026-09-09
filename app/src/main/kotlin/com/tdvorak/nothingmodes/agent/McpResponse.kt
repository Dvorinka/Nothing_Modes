package com.tdvorak.nothingmodes.agent

import kotlinx.serialization.Serializable

@Serializable
data class McpResponse(
    val ok: Boolean,
    val detail: String = "",
    val mode: Map<String, String>? = null,
    val modes: List<Map<String, String>>? = null,
) {
    companion object {
        fun error(message: String): McpResponse = McpResponse(ok = false, detail = message)
    }
}
