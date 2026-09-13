@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Transition { ENTER, EXIT, DWELL }

enum class PhoneEvent { INCOMING_CALL, CALL_ENDED, SMS_RECEIVED }

enum class ConnMedium { WIFI, BT, POWER, AIRPLANE }

enum class ConnState { CONNECTED, DISCONNECTED }

enum class TimePrecision { FLEXIBLE, EXACT }

enum class ScreenState { ON, OFF }

enum class BatteryDirection { CHARGING_STARTED, CHARGING_STOPPED }

/** Physical power source reported by the battery intent's `plugged` extra. */
@Serializable
enum class ChargerSource { AC, USB, WIRELESS, DOCK }

/** Day filter for recurring triggers. */
@Serializable
enum class DayOfWeek(
    val wireName: String,
) {
    @SerialName("mon")
    MONDAY("mon"),

    @SerialName("tue")
    TUESDAY("tue"),

    @SerialName("wed")
    WEDNESDAY("wed"),

    @SerialName("thu")
    THURSDAY("thu"),

    @SerialName("fri")
    FRIDAY("fri"),

    @SerialName("sat")
    SATURDAY("sat"),

    @SerialName("sun")
    SUNDAY("sun"),
}

@Serializable
sealed interface Trigger {
    /** Cron-based recurring time trigger. */
    @Serializable
    @SerialName("time")
    data class Time(
        val cron: String? = null,
        val at: String? = null,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val afterMs: Long? = null,
        val tz: String,
        val precision: TimePrecision = TimePrecision.FLEXIBLE,
        /** Days filter for recurring triggers. null = every day. */
        @EncodeDefault(EncodeDefault.Mode.NEVER) val days: List<DayOfWeek>? = null,
    ) : Trigger

    /** Fire once at arm time. */
    @Serializable
    @SerialName("immediate")
    data object Immediate : Trigger

    /** Mode schedule: active during a time window, deactivates at end. */
    @Serializable
    @SerialName("time_window")
    data class TimeWindow(
        val startLocal: String,
        val endLocal: String,
        val tz: String,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val days: List<DayOfWeek>? = null,
    ) : Trigger

    @Serializable
    @SerialName("notification")
    data class Notification(
        val pkg: String,
        val conversationId: String? = null,
        val sender: String? = null,
        val isGroup: Boolean? = null,
        val titleMatch: String? = null,
        val textMatch: String? = null,
    ) : Trigger

    @Serializable
    @SerialName("phone_state")
    data class PhoneState(
        val event: PhoneEvent,
        val number: String? = null,
        val textMatch: String? = null,
    ) : Trigger

    @Serializable
    @SerialName("connectivity")
    data class Connectivity(
        val medium: ConnMedium,
        val state: ConnState,
        val match: String? = null,
    ) : Trigger

    @Serializable
    @SerialName("boot")
    data object Boot : Trigger

    @Serializable
    @SerialName("battery_level")
    data class BatteryLevel(
        val level: Int,
        val direction: BatteryDirection? = null,
    ) : Trigger

    @Serializable
    @SerialName("screen_state")
    data class ScreenStateTrigger(
        val state: ScreenState,
    ) : Trigger

    @Serializable
    @SerialName("app_opened")
    data class AppOpened(
        val pkg: String,
    ) : Trigger

    @Serializable
    @SerialName("geofence")
    data class Geofence(
        val lat: Double = 0.0,
        val lng: Double = 0.0,
        val radiusM: Double,
        val transition: Transition,
        @EncodeDefault(EncodeDefault.Mode.NEVER) val loiteringDelayMs: Long = 0,
    ) : Trigger

    /** Manual trigger: fires when the user taps a "Run" button in the app. */
    @Serializable
    @SerialName("manual")
    data object Manual : Trigger

    /** Bluetooth device connected/disconnected (ACL connection, not adapter state). */
    @Serializable
    @SerialName("bt_device")
    data class BluetoothDevice(
        val state: ConnState,
        val deviceName: String? = null,
        val deviceAddress: String? = null,
    ) : Trigger

    /** WiFi connected to a specific network (SSID). */
    @Serializable
    @SerialName("wifi_connected")
    data class WifiConnected(
        val ssid: String? = null,
    ) : Trigger

    /** Calendar event starts or ends. Requires calendar read permission. */
    @Serializable
    @SerialName("calendar_event")
    data class CalendarEvent(
        val calendarId: String? = null,
        val titleMatch: String? = null,
        val direction: CalendarDirection = CalendarDirection.START,
        /** Explicitly picked events; empty falls back to the title/calendar filters. */
        val events: List<PickedCalendarEvent> = emptyList(),
    ) : Trigger

    /** Charger plugged in or unplugged. source = optional filter (e.g. WIRELESS). */
    @Serializable
    @SerialName("charger_connected")
    data class ChargerConnected(
        val connected: Boolean = true,
        val source: ChargerSource? = null,
    ) : Trigger

    /** Device unlocked (ACTION_USER_PRESENT). */
    @Serializable
    @SerialName("device_unlocked")
    data object DeviceUnlocked : Trigger

    /**
     * Device locked: fires when the screen turns off while a secure keyguard
     * is (or becomes) engaged. Distinct from screen-off — the screen can be
     * off without the device being locked.
     */
    @Serializable
    @SerialName("device_locked")
    data object DeviceLocked : Trigger

    /** Flashlight turned on or off. Requires `CameraManager.registerTorchCallback`. */
    @Serializable
    @SerialName("torch_state")
    data class TorchState(
        val on: Boolean = true,
    ) : Trigger

    /** Media playback started or stopped on any active session. */
    @Serializable
    @SerialName("media_playback")
    data class MediaPlayback(
        val playing: Boolean = true,
        val packageName: String? = null,
    ) : Trigger

    /**
     * Generic device-state edge: fires when the named state key transitions
     * TO [value]. `"*"` matches any change. Keys come from
     * [DeviceStateKeys]; values match the strings written into
     * `DeviceState.values` by the state providers.
     */
    @Serializable
    @SerialName("device_state")
    data class DeviceState(
        val key: String,
        val value: String,
    ) : Trigger
}

/** State keys the monitor services can emit as change events. Shared with the
 *  UI catalog so trigger rows and receivers agree on names. Values mirror the
 *  strings produced by `AndroidStateProvider` / the platform broadcasts. */
object DeviceStateKeys {
    const val POWER_SAVING = "power_saving"
    const val DND_ACTIVE = "dnd_active"
    const val RINGER_MODE = "ringer_mode"
    const val VOLUME_MEDIA = "volume_media"
    const val VOLUME_RING = "volume_ring"
    const val VOLUME_ALARM = "volume_alarm"
    const val HEADPHONES = "headphones_connected"
    const val NFC = "nfc_enabled"
    const val LOCATION = "location_enabled"
    const val DATA_SAVER = "data_saver"
    const val HOTSPOT = "hotspot_enabled"
    const val WIFI_RADIO = "wifi_radio"
    const val BLUETOOTH_RADIO = "bluetooth_radio"
    const val AIRPLANE = "airplane_mode"
    const val MOBILE_DATA = "mobile_data"
    const val AUTO_ROTATE = "auto_rotate"
    const val AOD = "aod_enabled"
    const val DARK_MODE = "dark_mode"
    const val CHARGING_STATUS = "charging_status"
    const val CHARGING_LIMIT = "charging_limit"
    const val BATTERY_SHARE = "battery_share"
    const val BATTERY_SHARE_LIMIT = "battery_share_limit"
    const val GLYPH_INTERFACE = "glyph_interface"
    const val GLYPH_CHARGE_LED = "glyph_charge_led"
    const val THERMAL = "thermal_status"

    /** Wildcard: fires on any change of the key. */
    const val ANY_VALUE = "*"
}

/**
 * Triggers describing a sustained device state rather than a one-shot event.
 * A mode armed with one of these is "active while the state holds": the
 * engine ends it — restoring snapshots and clearing glyph output — when the
 * inverse edge fires (e.g. charger unplugged ends a "charging on" mode).
 */
val Trigger.hasStateLifecycle: Boolean
    get() =
        when (this) {
            is Trigger.TimeWindow,
            is Trigger.ChargerConnected,
            is Trigger.TorchState,
            is Trigger.ScreenStateTrigger,
            is Trigger.MediaPlayback,
            is Trigger.BluetoothDevice,
            is Trigger.WifiConnected,
            is Trigger.Connectivity,
            is Trigger.DeviceUnlocked,
            is Trigger.DeviceLocked,
            -> true
            // A wildcard ("any change") device-state trigger has no defined
            // end edge — it is a one-shot routine, not a lifecycle mode.
            is Trigger.DeviceState -> value != DeviceStateKeys.ANY_VALUE
            is Trigger.CalendarEvent -> direction == CalendarDirection.START
            else -> false
        }

@Serializable
enum class CalendarDirection { START, END }

/** A calendar event chosen in the picker. [eventId] is the provider's stable ID —
 *  recurring instances share it, so picking one occurrence selects the series. */
@Serializable
data class PickedCalendarEvent(
    val eventId: Long,
    val title: String,
    val calendarName: String = "",
)

fun Trigger.Time.isOneShot(): Boolean = at != null || afterMs != null
