package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class CmpOp { EQ, NEQ, GT, LT, GTE, LTE, CONTAINS }

@Serializable
sealed interface Condition {
    @Serializable @SerialName("time_window")
    data class TimeWindow(val startLocal: String, val endLocal: String, val tz: String) : Condition

    @Serializable @SerialName("day_of_week")
    data class DayOfWeekCondition(val days: List<DayOfWeek>) : Condition

    @Serializable @SerialName("battery_level")
    data class BatteryLevel(val op: CmpOp, val level: Int) : Condition

    @Serializable @SerialName("charging")
    data class Charging(val isCharging: Boolean) : Condition

    @Serializable @SerialName("wifi_connected")
    data class WifiConnected(val ssid: String? = null) : Condition

    @Serializable @SerialName("bluetooth_connected")
    data class BluetoothConnected(val deviceName: String? = null) : Condition

    @Serializable @SerialName("screen_state")
    data class ScreenStateCondition(val state: ScreenState) : Condition

    @Serializable @SerialName("current_mode_active")
    data class CurrentModeActive(val modeId: String) : Condition

    @Serializable @SerialName("app_in_foreground")
    data class AppInForeground(val pkg: String) : Condition

    /** Dark mode (night mode) is active or not. Reads values["dark_mode"]. */
    @Serializable @SerialName("dark_mode_active")
    data class DarkModeActive(val active: Boolean) : Condition

    /** Battery saver / power saving mode is on or off. Reads values["power_saving"]. */
    @Serializable @SerialName("power_saving")
    data class PowerSaving(val on: Boolean) : Condition

    /** Media is currently playing. Reads values["media_playing"]. */
    @Serializable @SerialName("media_playing")
    data class MediaPlaying(val playing: Boolean) : Condition

    /** Ringer mode is one of: silent, vibrate, normal. Reads values["ringer_mode"]. */
    @Serializable @SerialName("ringer_mode")
    data class RingerMode(val mode: String) : Condition

    @Serializable @SerialName("and")
    data class And(val all: List<Condition>) : Condition

    @Serializable @SerialName("or")
    data class Or(val any: List<Condition>) : Condition

    @Serializable @SerialName("not")
    data class Not(val cond: Condition) : Condition
}
