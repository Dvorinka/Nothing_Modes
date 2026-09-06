package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class CmpOp { EQ, NEQ, GT, LT, GTE, LTE, CONTAINS }

@Serializable
sealed interface Condition {
    @Serializable
    @SerialName("time_window")
    data class TimeWindow(
        val startLocal: String,
        val endLocal: String,
        val tz: String,
    ) : Condition

    @Serializable
    @SerialName("day_of_week")
    data class DayOfWeekCondition(
        val days: List<DayOfWeek>,
    ) : Condition

    @Serializable
    @SerialName("battery_level")
    data class BatteryLevel(
        val op: CmpOp,
        val level: Int,
    ) : Condition

    @Serializable
    @SerialName("charging")
    data class Charging(
        val isCharging: Boolean,
    ) : Condition

    @Serializable
    @SerialName("wifi_connected")
    data class WifiConnected(
        val ssid: String? = null,
    ) : Condition

    @Serializable
    @SerialName("bluetooth_connected")
    data class BluetoothConnected(
        val deviceName: String? = null,
    ) : Condition

    @Serializable
    @SerialName("screen_state")
    data class ScreenStateCondition(
        val state: ScreenState,
    ) : Condition

    @Serializable
    @SerialName("current_mode_active")
    data class CurrentModeActive(
        val modeId: String,
    ) : Condition

    @Serializable
    @SerialName("app_in_foreground")
    data class AppInForeground(
        val pkg: String,
    ) : Condition

    /** Dark mode (night mode) is active or not. Reads values["dark_mode"]. */
    @Serializable
    @SerialName("dark_mode_active")
    data class DarkModeActive(
        val active: Boolean,
    ) : Condition

    /** Battery saver / power saving mode is on or off. Reads values["power_saving"]. */
    @Serializable
    @SerialName("power_saving")
    data class PowerSaving(
        val on: Boolean,
    ) : Condition

    /** Media is currently playing. Reads values["media_playing"]. */
    @Serializable
    @SerialName("media_playing")
    data class MediaPlaying(
        val playing: Boolean,
    ) : Condition

    /** Ringer mode is one of: silent, vibrate, normal. Reads values["ringer_mode"]. */
    @Serializable
    @SerialName("ringer_mode")
    data class RingerMode(
        val mode: String,
    ) : Condition

    /** Airplane mode is on or off. Reads values["airplane_mode"]. */
    @Serializable
    @SerialName("airplane_mode_on")
    data class AirplaneModeOn(
        val on: Boolean,
    ) : Condition

    /** NFC is enabled. Reads values["nfc_enabled"]. */
    @Serializable
    @SerialName("nfc_enabled")
    data class NfcEnabled(
        val enabled: Boolean,
    ) : Condition

    /** Location services are enabled. Reads values["location_enabled"]. */
    @Serializable
    @SerialName("location_enabled")
    data class LocationEnabled(
        val enabled: Boolean,
    ) : Condition

    /** Phone call state. Reads values["call_state"]. */
    @Serializable
    @SerialName("call_state")
    data class CallStateCondition(
        val state: CallState,
    ) : Condition

    /** An alarm is currently ringing. Reads values["alarm_ringing"] as comma-separated titles. */
    @Serializable
    @SerialName("alarm_ringing")
    data class AlarmRinging(
        val titleMatch: String? = null,
    ) : Condition

    /** Screen time today. Reads values["screen_time_today_ms"] and compares against minutes. */
    @Serializable
    @SerialName("screen_time")
    data class ScreenTime(
        val op: CmpOp,
        val minutes: Int,
    ) : Condition

    /** An audio output device (headset, USB, BT) is connected. Reads values["headphones_connected"]. */
    @Serializable
    @SerialName("headphones_connected")
    data class HeadphonesConnected(
        val connected: Boolean,
    ) : Condition

    /** Data Saver is restricting background data. Reads values["data_saver"]. */
    @Serializable
    @SerialName("data_saver_on")
    data class DataSaverOn(
        val on: Boolean,
    ) : Condition

    /** Master auto-sync is enabled. Reads values["auto_sync"]. */
    @Serializable
    @SerialName("auto_sync_on")
    data class AutoSyncOn(
        val on: Boolean,
    ) : Condition

    /** Auto-rotate is enabled. Reads values["auto_rotate"]. */
    @Serializable
    @SerialName("auto_rotate_on")
    data class AutoRotateOn(
        val on: Boolean,
    ) : Condition

    /** Stream volume level. Reads values["volume_<stream>"] (0..max). */
    @Serializable
    @SerialName("volume_level")
    data class VolumeLevel(
        val stream: VolumeStream,
        val op: CmpOp,
        val level: Int,
    ) : Condition

    /** Screen has been off for the given duration. Reads values["screen_off_since_ms"]. */
    @Serializable
    @SerialName("screen_off_for")
    data class ScreenOffFor(
        val op: CmpOp,
        val minutes: Int,
    ) : Condition

    /** Battery is charging from the given source. Reads values["charging_source"]. */
    @Serializable
    @SerialName("charging_source")
    data class ChargingSource(
        val source: ChargerSource,
    ) : Condition

    /** Battery temperature in Celsius. Reads values["battery_temp_c"]. */
    @Serializable
    @SerialName("battery_temp")
    data class BatteryTemp(
        val op: CmpOp,
        val celsius: Double,
    ) : Condition

    @Serializable
    @SerialName("and")
    data class And(
        val all: List<Condition>,
    ) : Condition

    @Serializable
    @SerialName("or")
    data class Or(
        val any: List<Condition>,
    ) : Condition

    @Serializable
    @SerialName("not")
    data class Not(
        val cond: Condition,
    ) : Condition
}

@Serializable
enum class CallState { IDLE, INCOMING, ACTIVE, ENDED }
