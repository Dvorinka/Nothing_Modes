@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DndMode { OFF, PRIORITY, TOTAL }
@Serializable
enum class NightMode { OFF, ON, AUTO }

enum class VolumeStream { MEDIA, RING, ALARM, NOTIFICATION }

enum class SettingsScreen { WIFI, BLUETOOTH, DISPLAY, SOUND, LOCATION, BATTERY, DATE, APP_DETAILS, SETTINGS }

/** Discriminatori wire stabili condivisi da JSON, manifest capability e journal. */
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
    const val SET_MOBILE_DATA = "set_mobile_data"
    const val COPY_TEXT = "copy_text"
    const val WAIT = "wait"
    const val WRITE_SETTING = "write_setting"
}

@Serializable
sealed interface Action {
    @Serializable @SerialName(ActionTypeIds.SET_WIFI) data class SetWifi(val on: Boolean) : Action
    @Serializable @SerialName(ActionTypeIds.SET_BLUETOOTH) data class SetBluetooth(val on: Boolean) : Action
    @Serializable @SerialName(ActionTypeIds.SET_MOBILE_DATA) data class SetMobileData(val on: Boolean) : Action
    @Serializable @SerialName(ActionTypeIds.SET_DND) data class SetDnd(val mode: DndMode) : Action
    @Serializable @SerialName(ActionTypeIds.SET_RINGER) data class SetRinger(val mode: String) : Action
    @Serializable @SerialName(ActionTypeIds.LAUNCH_APP) data class LaunchApp(val pkg: String) : Action
    @Serializable @SerialName(ActionTypeIds.OPEN_URL) data class OpenUrl(val url: String) : Action
    @Serializable @SerialName(ActionTypeIds.SHOW_NOTIFICATION) data class ShowNotification(val title: String, val text: String) : Action
    @Serializable @SerialName(ActionTypeIds.SET_VOLUME) data class SetVolume(val stream: VolumeStream, val level: Int) : Action
    @Serializable @SerialName(ActionTypeIds.SET_FLASHLIGHT) data class SetFlashlight(val on: Boolean) : Action

    @Serializable @SerialName(ActionTypeIds.SET_DARK_MODE)
    data class SetDarkMode(val mode: NightMode) : Action

    @Serializable @SerialName(ActionTypeIds.OPEN_SETTINGS_SCREEN)
    data class OpenSettingsScreen(val screen: SettingsScreen, val pkg: String? = null) : Action

    @Serializable @SerialName(ActionTypeIds.VIBRATE) data class Vibrate(val durationMs: Int) : Action

    /** Brightness level 0..255. Use RESTORE to snapshot/restore previous value. */
    @Serializable @SerialName(ActionTypeIds.SET_BRIGHTNESS)
    data class SetBrightness(val level: Int, val restore: Boolean = false) : Action

    /** Enable/disable adaptive brightness. */
    @Serializable @SerialName(ActionTypeIds.SET_AUTO_BRIGHTNESS)
    data class SetAutoBrightness(val on: Boolean) : Action

    /** Extra Dim (reduce_bright_colors). restore = restore previous state. */
    @Serializable @SerialName(ActionTypeIds.SET_EXTRA_DIM)
    data class SetExtraDim(val on: Boolean, val restore: Boolean = false) : Action

    /** Screen timeout in milliseconds. restore = restore previous value. */
    @Serializable @SerialName(ActionTypeIds.SET_SCREEN_TIMEOUT)
    data class SetScreenTimeout(val timeoutMs: Int, val restore: Boolean = false) : Action

    /** Glyph light stripe on/off. channels = specific LED zones (null = all). */
    @Serializable @SerialName(ActionTypeIds.SET_GLYPH)
    data class SetGlyph(val on: Boolean, val channels: List<Int>? = null, val restore: Boolean = false) : Action

    /** Glyph Matrix frame. colors = 25x25 (or 13x13) int array. restore = turn off / restore. */
    @Serializable @SerialName(ActionTypeIds.SET_GLYPH_MATRIX)
    data class SetGlyphMatrix(val colors: List<Int>? = null, val restore: Boolean = false) : Action

    /** Animate glyph channels with breathing effect. zone = A/B/C/D/E (null = all). */
    @Serializable @SerialName(ActionTypeIds.GLYPH_ANIMATE)
    data class GlyphAnimate(
        val zone: String? = null,
        val channels: List<Int>? = null,
        val periodMs: Int = 3000,
        val cycles: Int = 3,
        val intervalMs: Int = 10,
    ) : Action

    /** Display progress bar on glyph (0-100). reverse = fill from top. */
    @Serializable @SerialName(ActionTypeIds.GLYPH_PROGRESS)
    data class GlyphProgress(val progress: Int, val reverse: Boolean = false) : Action

    /** Display text on Glyph Matrix. */
    @Serializable @SerialName(ActionTypeIds.GLYPH_TEXT)
    data class GlyphText(
        val text: String,
        val x: Int = 0,
        val y: Int = 0,
        val scale: Int = 100,
        val brightness: Int = 255,
    ) : Action

    /** Display scrolling text (marquee) on Glyph Matrix. */
    @Serializable @SerialName(ActionTypeIds.GLYPH_SCROLLING_TEXT)
    data class GlyphScrollingText(val text: String) : Action

    /** Display a named visual preset (sleep, morning, charging, timer, etc.). */
    @Serializable @SerialName(ActionTypeIds.GLYPH_PRESET)
    data class GlyphPreset(val preset: String) : Action

    /** Turn off all glyphs. */
    @Serializable @SerialName(ActionTypeIds.GLYPH_TURNOFF)
    data object GlyphTurnOff : Action

    @Serializable @SerialName(ActionTypeIds.COPY_TEXT) data class CopyText(val text: String) : Action

    @Serializable @SerialName(ActionTypeIds.WAIT) data class Wait(val durationMs: Long) : Action

    /** Parametric settings write (system|secure|global). Always PRIVILEGED (Shizuku). */
    @Serializable @SerialName(ActionTypeIds.WRITE_SETTING)
    data class WriteSetting(val namespace: SettingNamespace, val key: String, val value: String) : Action
}

/** Actions that support state restoration (snapshot previous value before applying). */
val Action.supportsRestore: Boolean
    get() = when (this) {
        is Action.SetBrightness -> restore
        is Action.SetExtraDim -> restore
        is Action.SetScreenTimeout -> restore
        is Action.SetGlyph -> restore
        is Action.SetGlyphMatrix -> restore
        else -> false
    }

/** All settings keys this action modifies (for conflict detection and snapshot). */
val Action.affectedSettings: Set<String>
    get() = when (this) {
        is Action.SetBrightness -> setOf("screen_brightness")
        is Action.SetAutoBrightness -> setOf("screen_brightness_mode")
        is Action.SetExtraDim -> setOf("reduce_bright_colors_activated")
        is Action.SetScreenTimeout -> setOf("screen_off_timeout")
        is Action.SetDarkMode -> setOf("night_mode")
        is Action.SetDnd -> setOf("dnd_mode")
        is Action.SetVolume -> setOf("volume_${stream.name.lowercase()}")
        is Action.SetGlyph -> setOf("glyph_state")
        is Action.SetGlyphMatrix -> setOf("glyph_matrix_state")
        else -> emptySet()
    }
