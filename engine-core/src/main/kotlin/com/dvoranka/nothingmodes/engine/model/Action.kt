@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.dvoranka.nothingmodes.engine.model

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
        is Action.SetBrightness -> if (restore) emptySet() else setOf("screen_brightness")
        is Action.SetAutoBrightness -> setOf("screen_brightness_mode")
        is Action.SetExtraDim -> if (restore) emptySet() else setOf("reduce_bright_colors_activated")
        is Action.SetScreenTimeout -> if (restore) emptySet() else setOf("screen_off_timeout")
        is Action.SetDarkMode -> setOf("night_mode")
        is Action.SetDnd -> setOf("dnd_mode")
        is Action.SetVolume -> setOf("volume_${stream.name.lowercase()}")
        is Action.SetGlyph -> setOf("glyph_state")
        is Action.SetGlyphMatrix -> setOf("glyph_matrix_state")
        else -> emptySet()
    }
