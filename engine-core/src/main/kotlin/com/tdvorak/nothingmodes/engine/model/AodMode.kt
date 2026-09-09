package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AodMode {
    @SerialName("off")
    OFF,

    @SerialName("tap_to_show")
    TAP_TO_SHOW,

    @SerialName("always_on")
    ALWAYS_ON,

    @SerialName("schedule")
    SCHEDULE,
}

@Serializable
data class AodSchedule(
    val startHour: Int = 0,
    val startMinute: Int = 0,
    val endHour: Int = 0,
    val endMinute: Int = 0,
)
