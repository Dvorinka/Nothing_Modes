package com.tdvorak.nothingmodes.capabilities.controllers

/** Result of a controller operation. */
sealed interface ControllerResult {
    data object Success : ControllerResult
    data class Failure(val reason: String) : ControllerResult
    data object Unsupported : ControllerResult
    data object PermissionRequired : ControllerResult
}

/** Brightness control (0..255). */
interface BrightnessController {
    suspend fun setBrightness(level: Int): ControllerResult
    suspend fun setAutoBrightness(on: Boolean): ControllerResult
    suspend fun getBrightness(): Int?
    suspend fun isAutoBrightness(): Boolean?
}

/** Extra Dim (reduce_bright_colors). */
interface ExtraDimController {
    suspend fun setExtraDim(on: Boolean): ControllerResult
    suspend fun isExtraDimEnabled(): Boolean?
}

/** Do Not Disturb. */
interface DndController {
    suspend fun setDnd(mode: com.tdvorak.nothingmodes.engine.model.DndMode): ControllerResult
    suspend fun getDndMode(): com.tdvorak.nothingmodes.engine.model.DndMode?
}

/** Volume control per stream. */
interface VolumeController {
    suspend fun setVolume(stream: com.tdvorak.nothingmodes.engine.model.VolumeStream, level: Int): ControllerResult
    suspend fun getVolume(stream: com.tdvorak.nothingmodes.engine.model.VolumeStream): Int?
}

/** Screen timeout in milliseconds. */
interface ScreenTimeoutController {
    suspend fun setScreenTimeout(timeoutMs: Int): ControllerResult
    suspend fun getScreenTimeout(): Int?
}

/** Dark mode (night mode). */
interface DarkModeController {
    suspend fun setDarkMode(mode: com.tdvorak.nothingmodes.engine.model.NightMode): ControllerResult
    suspend fun getDarkMode(): com.tdvorak.nothingmodes.engine.model.NightMode?
}

/** Screen state (on/off). */
interface ScreenStateController {
    suspend fun getScreenState(): com.tdvorak.nothingmodes.engine.model.ScreenState
}

/** WiFi toggle. */
interface WifiController {
    suspend fun setWifi(on: Boolean): ControllerResult
}

/** Bluetooth toggle. */
interface BluetoothController {
    suspend fun setBluetooth(on: Boolean): ControllerResult
}

/** Flashlight toggle. */
interface FlashlightController {
    suspend fun setFlashlight(on: Boolean): ControllerResult
}

/** Ringer mode. */
interface RingerController {
    suspend fun setRinger(mode: String): ControllerResult
}
