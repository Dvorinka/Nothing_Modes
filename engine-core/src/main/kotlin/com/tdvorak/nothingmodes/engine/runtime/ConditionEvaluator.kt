package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Snapshot of device state for condition evaluation. */
data class DeviceState(
    val batteryLevel: Int = -1,
    val isCharging: Boolean = false,
    val wifiConnected: Boolean = false,
    val wifiSsid: String? = null,
    val bluetoothConnected: Boolean = false,
    val bluetoothDeviceName: String? = null,
    val screenState: ScreenState = ScreenState.OFF,
    val foregroundApp: String? = null,
    val activeModeIds: Set<String> = emptySet(),
    val values: Map<String, String> = emptyMap(),
    val now: Long = System.currentTimeMillis(),
)

/** Evaluates conditions against device state. */
class ConditionEvaluator {

    enum class Result { MET, NOT_MET, STATE_UNAVAILABLE }

    fun result(condition: Condition, state: DeviceState): Result = when (condition) {
        is Condition.TimeWindow -> evaluateTimeWindow(condition, state)
        is Condition.DayOfWeekCondition -> evaluateDayOfWeek(condition, state)
        is Condition.BatteryLevel -> evaluateBatteryLevel(condition, state)
        is Condition.Charging -> evaluateCharging(condition, state)
        is Condition.WifiConnected -> evaluateWifi(condition, state)
        is Condition.BluetoothConnected -> evaluateBluetooth(condition, state)
        is Condition.ScreenStateCondition -> evaluateScreenState(condition, state)
        is Condition.CurrentModeActive -> evaluateCurrentMode(condition, state)
        is Condition.AppInForeground -> evaluateAppInForeground(condition, state)
        is Condition.DarkModeActive -> evaluateDarkMode(condition, state)
        is Condition.PowerSaving -> evaluatePowerSaving(condition, state)
        is Condition.MediaPlaying -> evaluateMediaPlaying(condition, state)
        is Condition.RingerMode -> evaluateRingerMode(condition, state)
        is Condition.AirplaneModeOn -> evaluateBooleanValue(condition.on, "airplane_mode", state)
        is Condition.NfcEnabled -> evaluateBooleanValue(condition.enabled, "nfc_enabled", state)
        is Condition.LocationEnabled -> evaluateBooleanValue(condition.enabled, "location_enabled", state)
        is Condition.CallStateCondition -> evaluateCallState(condition, state)
        is Condition.AlarmRinging -> evaluateAlarmRinging(condition, state)
        is Condition.And -> {
            val results = condition.all.map { result(it, state) }
            when {
                results.any { it == Result.STATE_UNAVAILABLE } -> Result.STATE_UNAVAILABLE
                results.all { it == Result.MET } -> Result.MET
                else -> Result.NOT_MET
            }
        }
        is Condition.Or -> {
            val results = condition.any.map { result(it, state) }
            when {
                results.all { it == Result.NOT_MET } -> Result.NOT_MET
                results.any { it == Result.MET } -> Result.MET
                else -> Result.STATE_UNAVAILABLE
            }
        }
        is Condition.Not -> when (result(condition.cond, state)) {
            Result.MET -> Result.NOT_MET
            Result.NOT_MET -> Result.MET
            Result.STATE_UNAVAILABLE -> Result.STATE_UNAVAILABLE
        }
    }

    private fun evaluateTimeWindow(condition: Condition.TimeWindow, state: DeviceState): Result {
        val zone = runCatching { ZoneId.of(condition.tz) }.getOrNull()
            ?: return Result.STATE_UNAVAILABLE
        val now = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(state.now), zone)
        val start = parseTime(condition.startLocal) ?: return Result.STATE_UNAVAILABLE
        val end = parseTime(condition.endLocal) ?: return Result.STATE_UNAVAILABLE
        val current = now.toLocalTime()
        val inWindow = if (start <= end) {
            current >= start && current < end
        } else {
            current >= start || current < end
        }
        return if (inWindow) Result.MET else Result.NOT_MET
    }

    /** Parses "HH:mm" or "H:mm" leniently. */
    private fun parseTime(text: String): LocalTime? = runCatching {
        LocalTime.parse(text.padStart(5, '0'), DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrNull()

    private fun evaluateDayOfWeek(condition: Condition.DayOfWeekCondition, state: DeviceState): Result {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(state.now), zone)
        val mapped = when (now.dayOfWeek) {
            JavaDayOfWeek.MONDAY -> DayOfWeek.MONDAY
            JavaDayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
            JavaDayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
            JavaDayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
            JavaDayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
            JavaDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
            JavaDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
        }
        return if (mapped in condition.days) Result.MET else Result.NOT_MET
    }

    private fun evaluateBatteryLevel(condition: Condition.BatteryLevel, state: DeviceState): Result {
        if (state.batteryLevel < 0) return Result.STATE_UNAVAILABLE
        return when (condition.op) {
            CmpOp.EQ -> if (state.batteryLevel == condition.level) Result.MET else Result.NOT_MET
            CmpOp.NEQ -> if (state.batteryLevel != condition.level) Result.MET else Result.NOT_MET
            CmpOp.GT -> if (state.batteryLevel > condition.level) Result.MET else Result.NOT_MET
            CmpOp.LT -> if (state.batteryLevel < condition.level) Result.MET else Result.NOT_MET
            CmpOp.GTE -> if (state.batteryLevel >= condition.level) Result.MET else Result.NOT_MET
            CmpOp.LTE -> if (state.batteryLevel <= condition.level) Result.MET else Result.NOT_MET
            CmpOp.CONTAINS -> Result.STATE_UNAVAILABLE
        }
    }

    private fun evaluateCharging(condition: Condition.Charging, state: DeviceState): Result =
        if (state.isCharging == condition.isCharging) Result.MET else Result.NOT_MET

    private fun evaluateWifi(condition: Condition.WifiConnected, state: DeviceState): Result {
        if (!state.wifiConnected && condition.ssid == null) return Result.NOT_MET
        if (!state.wifiConnected) return Result.NOT_MET
        return if (condition.ssid == null || state.wifiSsid?.equals(condition.ssid, ignoreCase = true) == true) {
            Result.MET
        } else {
            Result.NOT_MET
        }
    }

    private fun evaluateBluetooth(condition: Condition.BluetoothConnected, state: DeviceState): Result {
        if (!state.bluetoothConnected) return Result.NOT_MET
        return if (condition.deviceName == null ||
            state.bluetoothDeviceName?.equals(condition.deviceName, ignoreCase = true) == true
        ) {
            Result.MET
        } else {
            Result.NOT_MET
        }
    }

    private fun evaluateScreenState(condition: Condition.ScreenStateCondition, state: DeviceState): Result =
        if (state.screenState == condition.state) Result.MET else Result.NOT_MET

    private fun evaluateCurrentMode(condition: Condition.CurrentModeActive, state: DeviceState): Result =
        if (condition.modeId in state.activeModeIds) Result.MET else Result.NOT_MET

    private fun evaluateAppInForeground(condition: Condition.AppInForeground, state: DeviceState): Result =
        if (state.foregroundApp == condition.pkg) Result.MET else Result.NOT_MET

    private fun evaluateDarkMode(condition: Condition.DarkModeActive, state: DeviceState): Result =
        evaluateBooleanValue(condition.active, "dark_mode", state)

    private fun evaluatePowerSaving(condition: Condition.PowerSaving, state: DeviceState): Result =
        evaluateBooleanValue(condition.on, "power_saving", state)

    private fun evaluateMediaPlaying(condition: Condition.MediaPlaying, state: DeviceState): Result =
        evaluateBooleanValue(condition.playing, "media_playing", state)

    private fun evaluateRingerMode(condition: Condition.RingerMode, state: DeviceState): Result {
        val raw = state.values["ringer_mode"] ?: return Result.STATE_UNAVAILABLE
        return if (raw.trim().lowercase() == condition.mode.trim().lowercase()) Result.MET else Result.NOT_MET
    }

    private fun evaluateBooleanValue(expected: Boolean, key: String, state: DeviceState): Result {
        val raw = state.values[key] ?: return Result.STATE_UNAVAILABLE
        val on = raw.trim().lowercase() in setOf("true", "1", "on", "yes")
        return if (on == expected) Result.MET else Result.NOT_MET
    }

    private fun evaluateCallState(condition: Condition.CallStateCondition, state: DeviceState): Result {
        val raw = state.values["call_state"] ?: return Result.STATE_UNAVAILABLE
        return if (raw.trim().lowercase() == condition.state.name.lowercase()) Result.MET else Result.NOT_MET
    }

    // ponytail: AlarmRinging only works if a RingingAlarmProvider is wired; otherwise the value is absent
    //          and the condition reports STATE_UNAVAILABLE. Upgrade by populating values["alarm_ringing"].
    private fun evaluateAlarmRinging(condition: Condition.AlarmRinging, state: DeviceState): Result {
        val titles = state.values["alarm_ringing"] ?: return Result.STATE_UNAVAILABLE
        if (titles.isBlank()) return Result.NOT_MET
        val match = condition.titleMatch?.lowercase()
        return if (match == null || titles.split(",").any { it.trim().lowercase().contains(match) }) {
            Result.MET
        } else {
            Result.NOT_MET
        }
    }
}
