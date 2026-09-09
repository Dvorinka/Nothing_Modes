package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Per-mode notification rule. Off by default; explicit rules win. */
@Serializable
sealed interface NotifyRule {
    @Serializable
    @SerialName("before")
    data class Before(
        val minutes: Int,
    ) : NotifyRule

    @Serializable
    @SerialName("on_trigger")
    data object OnTrigger : NotifyRule

    @Serializable
    @SerialName("on_end")
    data object OnEnd : NotifyRule
}
