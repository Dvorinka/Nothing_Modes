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

    @Serializable @SerialName("and")
    data class And(val all: List<Condition>) : Condition

    @Serializable @SerialName("or")
    data class Or(val any: List<Condition>) : Condition

    @Serializable @SerialName("not")
    data class Not(val cond: Condition) : Condition
}
