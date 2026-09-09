@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DndMode { OFF, PRIORITY, TOTAL }

@Serializable
enum class NightMode { OFF, ON, AUTO }

@Serializable
enum class VolumeStream { MEDIA, RING, ALARM, NOTIFICATION }

enum class SettingsScreen { WIFI, BLUETOOTH, DISPLAY, SOUND, LOCATION, BATTERY, DATE, APP_DETAILS, STORAGE, SECURITY, ACCESSIBILITY, NOTIFICATION, APPS, NETWORK, ACCOUNTS, SETTINGS }

/** Stable wire discriminators shared by JSON, manifest capability, and journal. */
object ActionTypeIds {
    const val SET_WIFI = "set_wifi"
    const val SET_BLUETOOTH = "set_bluetooth"
    const val SET_DND = "set_dnd"
    const val SET_RINGER = "set_ringer"
    const val LAUNCH_APP = "launch_app"
    const val OPEN_URL = "open_url"
    const val SHOW_NOTIFICATION = "show_notification"
    const val SET_VOLUME = "set_volume"
    const val SET_FLASHLIGHT = "set_flashlight"
    const val SET_DARK_MODE = "set_dark_mode"
    const val OPEN_SETTINGS_SCREEN = "open_settings_screen"
    const val VIBRATE = "vibrate"
    const val SET_BRIGHTNESS = "set_brightness"
    const val SET_AUTO_BRIGHTNESS = "set_auto_brightness"
    const val SET_EXTRA_DIM = "set_extra_dim"
    const val SET_SCREEN_TIMEOUT = "set_screen_timeout"
    const val SET_GLYPH = "set_glyph"
    const val SET_GLYPH_MATRIX = "set_glyph_matrix"
    const val GLYPH_ANIMATE = "glyph_animate"
    const val GLYPH_PROGRESS = "glyph_progress"
    const val GLYPH_TEXT = "glyph_text"
    const val GLYPH_SCROLLING_TEXT = "glyph_scrolling_text"
    const val GLYPH_PRESET = "glyph_preset"
    const val GLYPH_TURNOFF = "glyph_turnoff"
    const val GLYPH_ICON = "glyph_icon"
    const val GLYPH_NUMBER = "glyph_number"
    const val GLYPH_COUNTDOWN = "glyph_countdown"
    const val GLYPH_MUSIC = "glyph_music"
    const val SET_MOBILE_DATA = "set_mobile_data"
    const val COPY_TEXT = "copy_text"
    const val WAIT = "wait"
    const val WRITE_SETTING = "write_setting"
    const val SET_AUTO_ROTATE = "set_auto_rotate"
    const val SET_BATTERY_SAVER = "set_battery_saver"
    const val SET_AIRPLANE_MODE = "set_airplane_mode"
    const val SET_DATA_SAVER = "set_data_saver"
    const val SET_HOTSPOT = "set_hotspot"
    const val SET_NFC = "set_nfc"
    const val SET_REFRESH_RATE = "set_refresh_rate"
    const val SET_SCREEN_ROTATION = "set_screen_rotation"
    const val MEDIA_CONTROL = "media_control"
    const val SEND_SMS = "send_sms"
    const val LOCK_SCREEN = "lock_screen"
    const val SET_LOCATION_MODE = "set_location_mode"
    const val SET_AUTO_SYNC = "set_auto_sync"
    const val CLEAR_NOTIFICATIONS = "clear_notifications"
    const val SET_AOD = "set_aod"
    const val TAKE_SCREENSHOT = "take_screenshot"
}

@Serializable
sealed interface Action {
    @Serializable
    @SerialName(ActionTypeIds.SET_WIFI)
    data class SetWifi(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.SET_BLUETOOTH)
    data class SetBluetooth(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.SET_MOBILE_DATA)
    data class SetMobileData(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.SET_DND)
    data class SetDnd(
        val mode: DndMode,
        /** Revert to the pre-run value when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.SET_RINGER)
    data class SetRinger(
        val mode: String,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.LAUNCH_APP)
    data class LaunchApp(
        val packages: List<String> = emptyList(),
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.OPEN_URL)
    data class OpenUrl(
        val url: String,
        val packageName: String? = null,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.SHOW_NOTIFICATION)
    data class ShowNotification(
        val title: String,
        val text: String,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.SET_VOLUME)
    data class SetVolume(
        val volumes: Map<VolumeStream, Int> = emptyMap(),
        /** Revert to the pre-run level when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.SET_FLASHLIGHT)
    data class SetFlashlight(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.SET_DARK_MODE)
    data class SetDarkMode(
        val mode: NightMode,
        /** Revert to the pre-run mode when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.OPEN_SETTINGS_SCREEN)
    data class OpenSettingsScreen(
        val screen: SettingsScreen,
        val pkg: String? = null,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.VIBRATE)
    data class Vibrate(
        val durationMs: Int,
    ) : Action

    /** Brightness level 0..255. Use RESTORE to snapshot/restore previous value. */
    @Serializable
    @SerialName(ActionTypeIds.SET_BRIGHTNESS)
    data class SetBrightness(
        val level: Int,
        val restore: Boolean = false,
    ) : Action

    /** Enable/disable adaptive brightness. */
    @Serializable
    @SerialName(ActionTypeIds.SET_AUTO_BRIGHTNESS)
    data class SetAutoBrightness(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Extra Dim (reduce_bright_colors). restore = restore previous state. */
    @Serializable
    @SerialName(ActionTypeIds.SET_EXTRA_DIM)
    data class SetExtraDim(
        val on: Boolean,
        val restore: Boolean = false,
    ) : Action

    /** Screen timeout in milliseconds. restore = restore previous value. */
    @Serializable
    @SerialName(ActionTypeIds.SET_SCREEN_TIMEOUT)
    data class SetScreenTimeout(
        val timeoutMs: Int,
        val restore: Boolean = false,
    ) : Action

    /** Glyph light stripe on/off. channels = specific LED zones (null = all). */
    @Serializable
    @SerialName(ActionTypeIds.SET_GLYPH)
    data class SetGlyph(
        val on: Boolean,
        val channels: List<Int>? = null,
        val restore: Boolean = false,
    ) : Action

    /** Glyph Matrix frame. colors = 25x25 (or 13x13) int array. restore = turn off / restore. */
    @Serializable
    @SerialName(ActionTypeIds.SET_GLYPH_MATRIX)
    data class SetGlyphMatrix(
        val colors: List<Int>? = null,
        val restore: Boolean = false,
    ) : Action

    /** Animate glyph channels with breathing effect. zone = A/B/C/D/E (null = all). */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_ANIMATE)
    data class GlyphAnimate(
        val zone: String? = null,
        val channels: List<Int>? = null,
        val periodMs: Int = 3000,
        val cycles: Int = 3,
        val intervalMs: Int = 10,
    ) : Action

    /** Display progress bar on glyph (0-100). reverse = fill from top. */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_PROGRESS)
    data class GlyphProgress(
        val progress: Int,
        val reverse: Boolean = false,
    ) : Action

    /** Display text on Glyph Matrix. */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_TEXT)
    data class GlyphText(
        val text: String,
        val x: Int = -1,
        val y: Int = -1,
        val scale: Int = 100,
        val brightness: Int = 255,
    ) : Action

    /** Display scrolling text (marquee) on Glyph Matrix. */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_SCROLLING_TEXT)
    data class GlyphScrollingText(
        val text: String,
        /** Milliseconds between marquee ticks. */
        val intervalMs: Int = 100,
        /** Matrix dots shifted per tick. */
        val stepPx: Int = 1,
    ) : Action

    /** Display a named visual preset (sleep, morning, charging, timer, etc.). */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_PRESET)
    data class GlyphPreset(
        val preset: String,
    ) : Action

    /** Display a baked 25x25 icon on the Glyph Matrix by name. */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_ICON)
    data class GlyphIcon(
        val name: String,
    ) : Action

    /** Display a 0-99 number centered on the Glyph Matrix. */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_NUMBER)
    data class GlyphNumber(
        val number: Int,
    ) : Action

    /** Countdown timer: renders the remaining seconds on the matrix, ticking once per second. */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_COUNTDOWN)
    data class GlyphCountdown(
        val seconds: Int,
    ) : Action

    /** Live music-reactive visualizer. Runs until another glyph action or [GlyphTurnOff] cancels it. */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_MUSIC)
    data class GlyphMusic(
        val style: String = MusicVisualizerStyles.default(),
    ) : Action

    /** Turn off all glyphs. */
    @Serializable
    @SerialName(ActionTypeIds.GLYPH_TURNOFF)
    data object GlyphTurnOff : Action

    @Serializable
    @SerialName(ActionTypeIds.COPY_TEXT)
    data class CopyText(
        val text: String,
    ) : Action

    @Serializable
    @SerialName(ActionTypeIds.WAIT)
    data class Wait(
        val durationMs: Long,
    ) : Action

    /** Parametric settings write (system|secure|global). Always PRIVILEGED (Shizuku). */
    @Serializable
    @SerialName(ActionTypeIds.WRITE_SETTING)
    data class WriteSetting(
        val namespace: SettingNamespace,
        val key: String,
        val value: String,
    ) : Action

    // ── System settings toggles (Phase 4) ──

    /** Toggle auto-rotate. Uses Settings.System.ACCELEROMETER_ROTATION. */
    @Serializable
    @SerialName(ActionTypeIds.SET_AUTO_ROTATE)
    data class SetAutoRotate(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Toggle battery saver. Uses Settings.Global.LOW_POWER_MODE (requires Shizuku or WRITE_SECURE_SETTINGS). */
    @Serializable
    @SerialName(ActionTypeIds.SET_BATTERY_SAVER)
    data class SetBatterySaver(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Toggle airplane mode. Requires Shizuku (settings put global airplane_mode_on). */
    @Serializable
    @SerialName(ActionTypeIds.SET_AIRPLANE_MODE)
    data class SetAirplaneMode(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Toggle data saver. Uses Settings.Global.DATA_SAVER (requires Shizuku). */
    @Serializable
    @SerialName(ActionTypeIds.SET_DATA_SAVER)
    data class SetDataSaver(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Toggle hotspot. Requires Shizuku. */
    @Serializable
    @SerialName(ActionTypeIds.SET_HOTSPOT)
    data class SetHotspot(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Toggle NFC. Requires Shizuku. */
    @Serializable
    @SerialName(ActionTypeIds.SET_NFC)
    data class SetNfc(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Set display refresh rate (Hz). Uses Settings.System. */
    @Serializable
    @SerialName(ActionTypeIds.SET_REFRESH_RATE)
    data class SetRefreshRate(
        val hz: Int,
        /** Revert to the pre-run rate when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Lock screen rotation to a specific orientation. */
    @Serializable
    @SerialName(ActionTypeIds.SET_SCREEN_ROTATION)
    data class SetScreenRotation(
        val orientation: ScreenOrientation,
        /** Revert to the pre-run rotation when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Media playback control. */
    @Serializable
    @SerialName(ActionTypeIds.MEDIA_CONTROL)
    data class MediaControl(
        val command: MediaCommand,
    ) : Action

    // ── Extended actions (Phase 5) ──

    /** Send an SMS to a phone number. Requires SEND_SMS permission. */
    @Serializable
    @SerialName(ActionTypeIds.SEND_SMS)
    data class SendSms(
        val number: String,
        val text: String,
    ) : Action

    /** Lock the screen. Requires Device Admin or accessibility service. */
    @Serializable
    @SerialName(ActionTypeIds.LOCK_SCREEN)
    data class LockScreen(
        /** Force-run even if device-admin capability is not detected. */
        val force: Boolean = false,
    ) : Action

    /** Set location mode (high accuracy, battery saving, device only, off). Requires Shizuku. */
    @Serializable
    @SerialName(ActionTypeIds.SET_LOCATION_MODE)
    data class SetLocationMode(
        val mode: LocationMode,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Toggle auto-sync (background data sync). Requires Shizuku. */
    @Serializable
    @SerialName(ActionTypeIds.SET_AUTO_SYNC)
    data class SetAutoSync(
        val on: Boolean,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Clear all notifications. Requires notification listener access. */
    @Serializable
    @SerialName(ActionTypeIds.CLEAR_NOTIFICATIONS)
    data object ClearNotifications : Action

    /** Toggle Always-On Display. Requires Shizuku. */
    @Serializable
    @SerialName(ActionTypeIds.SET_AOD)
    data class SetAlwaysOnDisplay(
        val mode: AodMode = AodMode.OFF,
        val schedule: AodSchedule? = null,
        /** Revert to the pre-run state when a windowed mode ends. */
        val restore: Boolean = true,
    ) : Action

    /** Take a screenshot. Requires MediaProjection (user consent per capture). */
    @Serializable
    @SerialName(ActionTypeIds.TAKE_SCREENSHOT)
    data class TakeScreenshot(
        /** Force-run even if MediaProjection is not detected. */
        val force: Boolean = false,
    ) : Action
}

@Serializable
enum class ScreenOrientation { AUTO, PORTRAIT, LANDSCAPE }

@Serializable
enum class MediaCommand { PLAY_PAUSE, NEXT, PREVIOUS, STOP }

@Serializable
enum class LocationMode { HIGH_ACCURACY, BATTERY_SAVING, DEVICE_ONLY, OFF }

/** Actions that draw on Glyph hardware — used to auto-clear output on mode end. */
val Action.isGlyphAction: Boolean
    get() =
        when (this) {
            is Action.SetGlyph,
            is Action.SetGlyphMatrix,
            is Action.GlyphAnimate,
            is Action.GlyphProgress,
            is Action.GlyphText,
            is Action.GlyphScrollingText,
            is Action.GlyphPreset,
            is Action.GlyphIcon,
            is Action.GlyphNumber,
            is Action.GlyphCountdown,
            is Action.GlyphMusic,
            -> true
            is Action.GlyphTurnOff -> false
            else -> false
        }

/** Whether this action type can revert at all (has a restorable setting). */
val Action.canRestore: Boolean
    get() =
        when (this) {
            is Action.SetBrightness,
            is Action.SetAutoBrightness,
            is Action.SetExtraDim,
            is Action.SetScreenTimeout,
            is Action.SetDnd,
            is Action.SetVolume,
            is Action.SetDarkMode,
            is Action.SetAutoRotate,
            is Action.SetBatterySaver,
            is Action.SetAirplaneMode,
            is Action.SetDataSaver,
            is Action.SetRefreshRate,
            is Action.SetScreenRotation,
            is Action.SetWifi,
            is Action.SetBluetooth,
            is Action.SetMobileData,
            is Action.SetFlashlight,
            is Action.SetAlwaysOnDisplay,
            is Action.SetNfc,
            is Action.SetHotspot,
            is Action.SetLocationMode,
            is Action.SetAutoSync,
            is Action.SetRinger,
            -> true
            else -> false
        }

/** Copy with the revert flag set; returns this unchanged when not restorable. */
fun Action.withRestore(restore: Boolean): Action =
    when (this) {
        is Action.SetBrightness -> copy(restore = restore)
        is Action.SetAutoBrightness -> copy(restore = restore)
        is Action.SetExtraDim -> copy(restore = restore)
        is Action.SetScreenTimeout -> copy(restore = restore)
        is Action.SetDnd -> copy(restore = restore)
        is Action.SetVolume -> copy(restore = restore)
        is Action.SetDarkMode -> copy(restore = restore)
        is Action.SetAutoRotate -> copy(restore = restore)
        is Action.SetBatterySaver -> copy(restore = restore)
        is Action.SetAirplaneMode -> copy(restore = restore)
        is Action.SetDataSaver -> copy(restore = restore)
        is Action.SetRefreshRate -> copy(restore = restore)
        is Action.SetScreenRotation -> copy(restore = restore)
        is Action.SetWifi -> copy(restore = restore)
        is Action.SetBluetooth -> copy(restore = restore)
        is Action.SetMobileData -> copy(restore = restore)
        is Action.SetFlashlight -> copy(restore = restore)
        is Action.SetAlwaysOnDisplay -> copy(restore = restore)
        is Action.SetNfc -> copy(restore = restore)
        is Action.SetHotspot -> copy(restore = restore)
        is Action.SetLocationMode -> copy(restore = restore)
        is Action.SetAutoSync -> copy(restore = restore)
        is Action.SetRinger -> copy(restore = restore)
        else -> this
    }

/** Actions opted into state restoration (snapshot previous value before applying). */
val Action.supportsRestore: Boolean
    get() =
        when (this) {
            is Action.SetBrightness -> restore
            is Action.SetAutoBrightness -> restore
            is Action.SetExtraDim -> restore
            is Action.SetScreenTimeout -> restore
            is Action.SetDnd -> restore
            is Action.SetVolume -> restore
            is Action.SetDarkMode -> restore
            is Action.SetAutoRotate -> restore
            is Action.SetBatterySaver -> restore
            is Action.SetAirplaneMode -> restore
            is Action.SetDataSaver -> restore
            is Action.SetRefreshRate -> restore
            is Action.SetScreenRotation -> restore
            is Action.SetGlyph -> restore
            is Action.SetGlyphMatrix -> restore
            is Action.SetWifi -> restore
            is Action.SetBluetooth -> restore
            is Action.SetMobileData -> restore
            is Action.SetFlashlight -> restore
            is Action.SetAlwaysOnDisplay -> restore
            is Action.SetNfc -> restore
            is Action.SetHotspot -> restore
            is Action.SetLocationMode -> restore
            is Action.SetAutoSync -> restore
            is Action.SetRinger -> restore
            else -> false
        }

/** All settings keys this action modifies (for conflict detection and snapshot). */
val Action.affectedSettings: Set<String>
    get() =
        when (this) {
            is Action.SetBrightness -> setOf("screen_brightness")
            is Action.SetAutoBrightness -> setOf("screen_brightness_mode")
            is Action.SetExtraDim -> setOf("reduce_bright_colors_activated")
            is Action.SetScreenTimeout -> setOf("screen_off_timeout")
            is Action.SetDarkMode -> setOf("night_mode")
            is Action.SetDnd -> setOf("dnd_mode")
            is Action.SetVolume -> volumes.keys.map { "volume_${it.name.lowercase()}" }.toSet()
            is Action.SetGlyph -> setOf("glyph_state")
            is Action.SetGlyphMatrix -> setOf("glyph_matrix_state")
            is Action.SetAutoRotate -> setOf("accelerometer_rotation")
            is Action.SetBatterySaver -> setOf("low_power")
            is Action.SetAirplaneMode -> setOf("airplane_mode_on")
            is Action.SetDataSaver -> setOf("data_saver")
            is Action.SetRefreshRate -> setOf("peak_refresh_rate", "min_refresh_rate")
            is Action.SetScreenRotation -> setOf("accelerometer_rotation", "user_rotation")
            is Action.SetWifi -> setOf("wifi_enabled")
            is Action.SetBluetooth -> setOf("bluetooth_enabled")
            is Action.SetMobileData -> setOf("mobile_data_enabled")
            is Action.SetFlashlight -> setOf("flashlight_on")
            is Action.SetAlwaysOnDisplay -> setOf("aod_enabled")
            is Action.SetNfc -> setOf("nfc_enabled")
            is Action.SetHotspot -> setOf("hotspot_enabled")
            is Action.SetLocationMode -> setOf("location_mode")
            is Action.SetAutoSync -> setOf("auto_sync")
            is Action.SetRinger -> setOf("ringer_mode")
            else -> emptySet()
        }
