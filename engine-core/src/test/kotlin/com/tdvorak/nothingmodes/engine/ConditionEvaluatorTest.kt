package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.CallState
import com.tdvorak.nothingmodes.engine.model.CmpOp
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.engine.model.StateKeys
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.runtime.ConditionEvaluator
import com.tdvorak.nothingmodes.engine.runtime.DeviceState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ConditionEvaluatorTest {
    private val evaluator = ConditionEvaluator()

    // --- TimeWindow ---

    @Test
    fun `TimeWindow - MET when current time is inside window`() {
        val now =
            ZonedDateTime
                .of(2026, 6, 15, 10, 30, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        val cond = Condition.TimeWindow("08:00", "12:00", "UTC")
        assertEquals(ConditionEvaluator.Result.MET, evaluator.result(cond, DeviceState(now = now)))
    }

    @Test
    fun `TimeWindow - NOT_MET when current time is outside window`() {
        val now =
            ZonedDateTime
                .of(2026, 6, 15, 14, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        val cond = Condition.TimeWindow("08:00", "12:00", "UTC")
        assertEquals(ConditionEvaluator.Result.NOT_MET, evaluator.result(cond, DeviceState(now = now)))
    }

    @Test
    fun `TimeWindow - MET when crossing midnight (before end)`() {
        val now =
            ZonedDateTime
                .of(2026, 6, 15, 3, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        val cond = Condition.TimeWindow("22:00", "07:00", "UTC")
        assertEquals(ConditionEvaluator.Result.MET, evaluator.result(cond, DeviceState(now = now)))
    }

    @Test
    fun `TimeWindow - MET when crossing midnight (after start)`() {
        val now =
            ZonedDateTime
                .of(2026, 6, 15, 23, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        val cond = Condition.TimeWindow("22:00", "07:00", "UTC")
        assertEquals(ConditionEvaluator.Result.MET, evaluator.result(cond, DeviceState(now = now)))
    }

    @Test
    fun `TimeWindow - STATE_UNAVAILABLE for invalid timezone`() {
        val cond = Condition.TimeWindow("08:00", "12:00", "Invalid/Zone")
        assertEquals(ConditionEvaluator.Result.STATE_UNAVAILABLE, evaluator.result(cond, DeviceState(now = 0L)))
    }

    @Test
    fun `TimeWindow - STATE_UNAVAILABLE for invalid time format`() {
        val now =
            ZonedDateTime
                .of(2026, 6, 15, 10, 0, 0, 0, ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        val cond = Condition.TimeWindow("25:99", "12:00", "UTC")
        assertEquals(ConditionEvaluator.Result.STATE_UNAVAILABLE, evaluator.result(cond, DeviceState(now = now)))
    }

    // --- DayOfWeek ---

    @Test
    fun `DayOfWeek - MET when today is in the list`() {
        val monday =
            ZonedDateTime
                .of(2026, 6, 15, 10, 0, 0, 0, ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val cond = Condition.DayOfWeekCondition(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertEquals(ConditionEvaluator.Result.MET, evaluator.result(cond, DeviceState(now = monday)))
    }

    @Test
    fun `DayOfWeek - NOT_MET when today is not in the list`() {
        val tuesday =
            ZonedDateTime
                .of(2026, 6, 16, 10, 0, 0, 0, ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        val cond = Condition.DayOfWeekCondition(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY))
        assertEquals(ConditionEvaluator.Result.NOT_MET, evaluator.result(cond, DeviceState(now = tuesday)))
    }

    @Test
    fun `DayOfWeek - NOT_MET when days list is empty`() {
        val now = System.currentTimeMillis()
        val cond = Condition.DayOfWeekCondition(emptyList())
        assertEquals(ConditionEvaluator.Result.NOT_MET, evaluator.result(cond, DeviceState(now = now)))
    }

    // --- BatteryLevel ---

    @Test
    fun `BatteryLevel - EQ MET`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.BatteryLevel(CmpOp.EQ, 50), DeviceState(batteryLevel = 50)),
        )
    }

    @Test
    fun `BatteryLevel - EQ NOT_MET`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(Condition.BatteryLevel(CmpOp.EQ, 50), DeviceState(batteryLevel = 51)),
        )
    }

    @Test
    fun `BatteryLevel - NEQ MET`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.BatteryLevel(CmpOp.NEQ, 50), DeviceState(batteryLevel = 51)),
        )
    }

    @Test
    fun `BatteryLevel - GT MET`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.BatteryLevel(CmpOp.GT, 50), DeviceState(batteryLevel = 80)),
        )
    }

    @Test
    fun `BatteryLevel - GT NOT_MET`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(Condition.BatteryLevel(CmpOp.GT, 50), DeviceState(batteryLevel = 50)),
        )
    }

    @Test
    fun `BatteryLevel - LT MET`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.BatteryLevel(CmpOp.LT, 20), DeviceState(batteryLevel = 15)),
        )
    }

    @Test
    fun `BatteryLevel - GTE MET at boundary`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.BatteryLevel(CmpOp.GTE, 50), DeviceState(batteryLevel = 50)),
        )
    }

    @Test
    fun `BatteryLevel - LTE MET at boundary`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.BatteryLevel(CmpOp.LTE, 50), DeviceState(batteryLevel = 50)),
        )
    }

    @Test
    fun `BatteryLevel - STATE_UNAVAILABLE when battery is negative`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(Condition.BatteryLevel(CmpOp.GT, 50), DeviceState(batteryLevel = -1)),
        )
    }

    @Test
    fun `BatteryLevel - CONTAINS is STATE_UNAVAILABLE`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(Condition.BatteryLevel(CmpOp.CONTAINS, 50), DeviceState(batteryLevel = 50)),
        )
    }

    // --- Charging ---

    @Test
    fun `Charging - MET when charging matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.Charging(true), DeviceState(isCharging = true)),
        )
    }

    @Test
    fun `Charging - NOT_MET when charging does not match`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(Condition.Charging(true), DeviceState(isCharging = false)),
        )
    }

    @Test
    fun `Charging - MET when not charging matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.Charging(false), DeviceState(isCharging = false)),
        )
    }

    // --- WifiConnected ---

    @Test
    fun `WifiConnected - MET when connected without SSID filter`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.WifiConnected(), DeviceState(wifiConnected = true)),
        )
    }

    @Test
    fun `WifiConnected - NOT_MET when not connected`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(Condition.WifiConnected(), DeviceState(wifiConnected = false)),
        )
    }

    @Test
    fun `WifiConnected - MET when connected and SSID matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.WifiConnected("HomeWiFi"),
                DeviceState(wifiConnected = true, wifiSsid = "HomeWiFi"),
            ),
        )
    }

    @Test
    fun `WifiConnected - MET when SSID matches case-insensitive`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.WifiConnected("homewifi"),
                DeviceState(wifiConnected = true, wifiSsid = "HomeWiFi"),
            ),
        )
    }

    @Test
    fun `WifiConnected - NOT_MET when SSID does not match`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.WifiConnected("HomeWiFi"),
                DeviceState(wifiConnected = true, wifiSsid = "OtherWiFi"),
            ),
        )
    }

    // --- BluetoothConnected ---

    @Test
    fun `BluetoothConnected - MET when connected without device filter`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.BluetoothConnected(),
                DeviceState(bluetoothConnected = true),
            ),
        )
    }

    @Test
    fun `BluetoothConnected - NOT_MET when not connected`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.BluetoothConnected(),
                DeviceState(bluetoothConnected = false),
            ),
        )
    }

    @Test
    fun `BluetoothConnected - MET when device name matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.BluetoothConnected("MyBuds"),
                DeviceState(bluetoothConnected = true, bluetoothDeviceName = "MyBuds"),
            ),
        )
    }

    @Test
    fun `BluetoothConnected - NOT_MET when device name does not match`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.BluetoothConnected("MyBuds"),
                DeviceState(bluetoothConnected = true, bluetoothDeviceName = "OtherBuds"),
            ),
        )
    }

    // --- ScreenStateCondition ---

    @Test
    fun `ScreenState - MET when screen is ON`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.ScreenStateCondition(ScreenState.ON),
                DeviceState(screenState = ScreenState.ON),
            ),
        )
    }

    @Test
    fun `ScreenState - NOT_MET when screen is OFF but condition is ON`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.ScreenStateCondition(ScreenState.ON),
                DeviceState(screenState = ScreenState.OFF),
            ),
        )
    }

    // --- CurrentModeActive ---

    @Test
    fun `CurrentModeActive - MET when mode is in active set`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.CurrentModeActive("mode-sleep"),
                DeviceState(activeModeIds = setOf("mode-sleep", "mode-work")),
            ),
        )
    }

    @Test
    fun `CurrentModeActive - NOT_MET when mode is not in active set`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.CurrentModeActive("mode-sleep"),
                DeviceState(activeModeIds = setOf("mode-work")),
            ),
        )
    }

    @Test
    fun `CurrentModeActive - NOT_MET when active set is empty`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.CurrentModeActive("mode-sleep"),
                DeviceState(activeModeIds = emptySet()),
            ),
        )
    }

    // --- AppInForeground ---

    @Test
    fun `AppInForeground - MET when app matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.AppInForeground("com.android.chrome"),
                DeviceState(foregroundApp = "com.android.chrome"),
            ),
        )
    }

    @Test
    fun `AppInForeground - NOT_MET when app does not match`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.AppInForeground("com.android.chrome"),
                DeviceState(foregroundApp = "com.whatsapp"),
            ),
        )
    }

    @Test
    fun `AppInForeground - NOT_MET when foreground app is null`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.AppInForeground("com.android.chrome"),
                DeviceState(foregroundApp = null),
            ),
        )
    }

    // --- And ---

    @Test
    fun `And - MET when all conditions met`() {
        val cond =
            Condition.And(
                listOf(
                    Condition.Charging(true),
                    Condition.BatteryLevel(CmpOp.GT, 50),
                ),
            )
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(cond, DeviceState(isCharging = true, batteryLevel = 80)),
        )
    }

    @Test
    fun `And - NOT_MET when any condition not met`() {
        val cond =
            Condition.And(
                listOf(
                    Condition.Charging(true),
                    Condition.BatteryLevel(CmpOp.GT, 50),
                ),
            )
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(cond, DeviceState(isCharging = true, batteryLevel = 30)),
        )
    }

    @Test
    fun `And - STATE_UNAVAILABLE when any condition is unavailable`() {
        val cond =
            Condition.And(
                listOf(
                    Condition.Charging(true),
                    Condition.BatteryLevel(CmpOp.GT, 50),
                ),
            )
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(cond, DeviceState(isCharging = true, batteryLevel = -1)),
        )
    }

    @Test
    fun `And - MET with empty list (vacuous truth)`() {
        val cond = Condition.And(emptyList())
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(cond, DeviceState()),
        )
    }

    // --- Or ---

    @Test
    fun `Or - MET when any condition met`() {
        val cond =
            Condition.Or(
                listOf(
                    Condition.Charging(true),
                    Condition.Charging(false),
                ),
            )
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(cond, DeviceState(isCharging = true)),
        )
    }

    @Test
    fun `Or - NOT_MET when all conditions not met`() {
        val cond =
            Condition.Or(
                listOf(
                    Condition.Charging(true),
                    Condition.Charging(true),
                ),
            )
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(cond, DeviceState(isCharging = false)),
        )
    }

    @Test
    fun `Or - STATE_UNAVAILABLE when all are unavailable or not-met`() {
        val cond =
            Condition.Or(
                listOf(
                    Condition.BatteryLevel(CmpOp.GT, 50),
                ),
            )
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(cond, DeviceState(batteryLevel = -1)),
        )
    }

    @Test
    fun `Or - MET when one met and one unavailable`() {
        val cond =
            Condition.Or(
                listOf(
                    Condition.Charging(true),
                    Condition.BatteryLevel(CmpOp.GT, 50),
                ),
            )
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(cond, DeviceState(isCharging = true, batteryLevel = -1)),
        )
    }

    // --- Not ---

    @Test
    fun `Not - MET when inner condition is NOT_MET`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.Not(Condition.Charging(true)),
                DeviceState(isCharging = false),
            ),
        )
    }

    @Test
    fun `Not - NOT_MET when inner condition is MET`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.Not(Condition.Charging(true)),
                DeviceState(isCharging = true),
            ),
        )
    }

    @Test
    fun `Not - STATE_UNAVAILABLE when inner condition is unavailable`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(
                Condition.Not(Condition.BatteryLevel(CmpOp.GT, 50)),
                DeviceState(batteryLevel = -1),
            ),
        )
    }

    // --- Nested composites ---

    // --- Values-based conditions ---

    @Test
    fun `DarkModeActive - MET when value matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.DarkModeActive(true),
                DeviceState(values = mapOf("dark_mode" to "true")),
            ),
        )
    }

    @Test
    fun `DarkModeActive - NOT_MET when value mismatches`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.DarkModeActive(true),
                DeviceState(values = mapOf("dark_mode" to "false")),
            ),
        )
    }

    @Test
    fun `PowerSaving - STATE_UNAVAILABLE when value missing`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(Condition.PowerSaving(true), DeviceState()),
        )
    }

    @Test
    fun `RingerMode - MET on matching mode`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.RingerMode("vibrate"),
                DeviceState(values = mapOf("ringer_mode" to "vibrate")),
            ),
        )
    }

    @Test
    fun `MediaPlaying - MET when value is on`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.MediaPlaying(true),
                DeviceState(values = mapOf("media_playing" to "on")),
            ),
        )
    }

    // --- Connections / system ---

    @Test
    fun `AirplaneModeOn - MET when value matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.AirplaneModeOn(true),
                DeviceState(values = mapOf("airplane_mode" to "true")),
            ),
        )
    }

    @Test
    fun `NfcEnabled - MET when value matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.NfcEnabled(true),
                DeviceState(values = mapOf("nfc_enabled" to "1")),
            ),
        )
    }

    @Test
    fun `LocationEnabled - STATE_UNAVAILABLE when value missing`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(Condition.LocationEnabled(true), DeviceState()),
        )
    }

    @Test
    fun `CallStateCondition - MET on incoming call`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.CallStateCondition(CallState.INCOMING),
                DeviceState(values = mapOf("call_state" to "incoming")),
            ),
        )
    }

    @Test
    fun `AlarmRinging - MET when title matches`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.AlarmRinging("work"),
                DeviceState(values = mapOf("alarm_ringing" to "work alarm,home")),
            ),
        )
    }

    // --- Value-based device conditions ---

    @Test
    fun `HeadphonesConnected - MET when connected`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.HeadphonesConnected(true),
                DeviceState(values = mapOf("headphones_connected" to "true")),
            ),
        )
    }

    @Test
    fun `HeadphonesConnected - NOT_MET when disconnected`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.HeadphonesConnected(true),
                DeviceState(values = mapOf("headphones_connected" to "false")),
            ),
        )
    }

    @Test
    fun `DataSaverOn - MET when enabled`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.DataSaverOn(true),
                DeviceState(values = mapOf("data_saver" to "true")),
            ),
        )
    }

    @Test
    fun `AutoSyncOn - NOT_MET when off`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.AutoSyncOn(true),
                DeviceState(values = mapOf("auto_sync" to "false")),
            ),
        )
    }

    @Test
    fun `AutoRotateOn - STATE_UNAVAILABLE when value missing`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(Condition.AutoRotateOn(true), DeviceState()),
        )
    }

    @Test
    fun `VolumeLevel - MET when media volume above threshold`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.VolumeLevel(com.tdvorak.nothingmodes.engine.model.VolumeStream.MEDIA, CmpOp.GT, 5),
                DeviceState(values = mapOf("volume_media" to "10")),
            ),
        )
    }

    @Test
    fun `VolumeLevel - NOT_MET when ring volume below threshold`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.VolumeLevel(com.tdvorak.nothingmodes.engine.model.VolumeStream.RING, CmpOp.GTE, 5),
                DeviceState(values = mapOf("volume_ring" to "3")),
            ),
        )
    }

    @Test
    fun `VolumeLevel - STATE_UNAVAILABLE for unknown stream key`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(
                Condition.VolumeLevel(com.tdvorak.nothingmodes.engine.model.VolumeStream.NOTIFICATION, CmpOp.GT, 0),
                DeviceState(),
            ),
        )
    }

    @Test
    fun `ScreenOffFor - MET when screen off longer than threshold`() {
        val now = 1_000_000L
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.ScreenOffFor(CmpOp.GTE, 10),
                DeviceState(
                    screenState = ScreenState.OFF,
                    values = mapOf("screen_off_since_ms" to (now - 15 * 60_000L).toString()),
                    now = now,
                ),
            ),
        )
    }

    @Test
    fun `ScreenOffFor - NOT_MET when screen is on and threshold positive`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.ScreenOffFor(CmpOp.GT, 10),
                DeviceState(screenState = ScreenState.ON),
            ),
        )
    }

    @Test
    fun `ScreenOffFor - MET when screen is on and threshold is zero`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.ScreenOffFor(CmpOp.LT, 10),
                DeviceState(screenState = ScreenState.ON),
            ),
        )
    }

    @Test
    fun `ScreenOffFor - STATE_UNAVAILABLE when screen off without timestamp`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(
                Condition.ScreenOffFor(CmpOp.GTE, 5),
                DeviceState(screenState = ScreenState.OFF),
            ),
        )
    }

    @Test
    fun `ChargingSource - MET on wireless`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.ChargingSource(com.tdvorak.nothingmodes.engine.model.ChargerSource.WIRELESS),
                DeviceState(values = mapOf("charging_source" to "wireless")),
            ),
        )
    }

    @Test
    fun `ChargingSource - NOT_MET on different source`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.ChargingSource(com.tdvorak.nothingmodes.engine.model.ChargerSource.WIRELESS),
                DeviceState(values = mapOf("charging_source" to "ac")),
            ),
        )
    }

    @Test
    fun `BatteryTemp - MET when above threshold`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.BatteryTemp(CmpOp.GTE, 35.0),
                DeviceState(values = mapOf("battery_temp_c" to "36.5")),
            ),
        )
    }

    @Test
    fun `BatteryTemp - STATE_UNAVAILABLE when malformed`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(
                Condition.BatteryTemp(CmpOp.GTE, 35.0),
                DeviceState(values = mapOf("battery_temp_c" to "hot")),
            ),
        )
    }

    @Test
    fun `ThermalLevel - MET when throttling is severe`() {
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(
                Condition.ThermalLevel(CmpOp.GTE, 3),
                DeviceState(values = mapOf("thermal_status" to "4")),
            ),
        )
    }

    @Test
    fun `ThermalLevel - NOT_MET when cool`() {
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(
                Condition.ThermalLevel(CmpOp.GTE, 3),
                DeviceState(values = mapOf("thermal_status" to "1")),
            ),
        )
    }

    @Test
    fun `ThermalLevel - STATE_UNAVAILABLE when missing`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(Condition.ThermalLevel(CmpOp.GT, 0), DeviceState()),
        )
    }

    @Test
    fun `booleanState met when key matches expected`() {
        val state = DeviceState(values = mapOf(StateKeys.DEVICE_LOCKED to "true"))
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.BooleanState(StateKeys.DEVICE_LOCKED, true), state),
        )
    }

    @Test
    fun `booleanState notMet when key mismatches`() {
        val state = DeviceState(values = mapOf(StateKeys.DEVICE_LOCKED to "false"))
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(Condition.BooleanState(StateKeys.DEVICE_LOCKED, true), state),
        )
    }

    @Test
    fun `booleanState unavailable when key missing`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(Condition.BooleanState(StateKeys.WIFI_RADIO, true), DeviceState()),
        )
    }

    @Test
    fun `numericState met when comparison matches`() {
        val state = DeviceState(values = mapOf(StateKeys.BRIGHTNESS to "200"))
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(Condition.NumericState(StateKeys.BRIGHTNESS, CmpOp.GTE, 100.0), state),
        )
    }

    @Test
    fun `numericState notMet when comparison fails`() {
        val state = DeviceState(values = mapOf(StateKeys.REFRESH_RATE to "60"))
        assertEquals(
            ConditionEvaluator.Result.NOT_MET,
            evaluator.result(Condition.NumericState(StateKeys.REFRESH_RATE, CmpOp.GT, 90.0), state),
        )
    }

    @Test
    fun `numericState unavailable when key missing`() {
        assertEquals(
            ConditionEvaluator.Result.STATE_UNAVAILABLE,
            evaluator.result(Condition.NumericState(StateKeys.SCREEN_TIMEOUT, CmpOp.GTE, 1000.0), DeviceState()),
        )
    }

    @Test
    fun `nested And-Or-Not evaluates correctly`() {
        val cond =
            Condition.And(
                listOf(
                    Condition.Or(
                        listOf(
                            Condition.Charging(true),
                            Condition.Not(Condition.Charging(false)),
                        ),
                    ),
                    Condition.BatteryLevel(CmpOp.GTE, 20),
                ),
            )
        // isCharging=true → Charging(true)=MET → Or=MET; batteryLevel=50 >= 20 → MET; And=MET
        assertEquals(
            ConditionEvaluator.Result.MET,
            evaluator.result(cond, DeviceState(isCharging = true, batteryLevel = 50)),
        )
    }
}
