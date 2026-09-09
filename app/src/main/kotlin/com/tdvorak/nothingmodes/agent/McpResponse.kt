package com.tdvorak.nothingmodes.agent

import kotlinx.serialization.Serializable

@Serializable
data class McpResponse(
    val ok: Boolean,
    val detail: String = "",
    val mode: Map<String, String>? = null,
    val modes: List<Map<String, String>>? = null,
    /** Human-readable description of what the mode does, or how a request was interpreted. */
    val explanation: String = "",
    /** Result of validate/validate-before-save: map of check name to status/reason. */
    val validation: Map<String, String>? = null,
    /** Required capability IDs for the mode or request being validated. */
    val requiredCapabilities: List<String>? = null,
    /** Missing capability IDs mapped to the reason they are unavailable. */
    val missingCapabilities: Map<String, String>? = null,
) {
    companion object {
        fun error(message: String): McpResponse = McpResponse(ok = false, detail = message)
    }
}
