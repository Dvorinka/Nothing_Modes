package com.tdvorak.nothingmodes.capabilities.controllers

import android.app.NotificationManager
import android.app.UiModeManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenState
import com.tdvorak.nothingmodes.engine.model.VolumeStream

/**
 * Real Android API implementations of capability controllers.
 * Uses public APIs only — Shizuku fallback handled by CapabilityResolver.
 */
class AndroidBrightnessController(private val context: Context) : BrightnessController {

    override suspend fun setBrightness(level: Int): ControllerResult {
        if (level !in 0..255) return ControllerResult.Failure("Brightness must be 0..255")
        return try {
            if (!Settings.System.canWrite(context)) return ControllerResult.PermissionRequired
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                level,
            )
            ControllerResult.Success
        } catch (e: Exception) {
            ControllerResult.Failure(e.message ?: "setBrightness failed")
        }
    }

    override suspend fun setAutoBrightness(on: Boolean): ControllerResult {
        return try {
            if (!Settings.System.canWrite(context)) return ControllerResult.PermissionRequired
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
            )
            ControllerResult.Success
        } catch (e: Exception) {
            ControllerResult.Failure(e.message ?: "setAutoBrightness failed")
        }
    }

    override suspend fun getBrightness(): Int? = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
    } catch (_: Exception) { null }

    override suspend fun isAutoBrightness(): Boolean? = try {
        Settings.System.getInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
        ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
    } catch (_: Exception) { null }
}

class AndroidExtraDimController(private val context: Context) : ExtraDimController {

    override suspend fun setExtraDim(on: Boolean): ControllerResult {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                return ControllerResult.Unsupported
            }
            if (!Settings.System.canWrite(context)) return ControllerResult.PermissionRequired
            Settings.Secure.putInt(
                context.contentResolver,
                "reduce_bright_colors_activated",
                if (on) 1 else 0,
            )
            ControllerResult.Success
        } catch (e: Exception) {
            ControllerResult.Failure(e.message ?: "setExtraDim failed")
        }
    }

    override suspend fun isExtraDimEnabled(): Boolean? = try {
        Settings.Secure.getInt(context.contentResolver, "reduce_bright_colors_activated") == 1
    } catch (_: Exception) { null }
}

class AndroidDndController(private val context: Context) : DndController {

    private val notificationManager = context.getSystemService(NotificationManager::class.java)

    override suspend fun setDnd(mode: DndMode): ControllerResult {
        return try {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                return ControllerResult.PermissionRequired
            }
            val filter = when (mode) {
                DndMode.OFF -> NotificationManager.INTERRUPTION_FILTER_ALL
                DndMode.PRIORITY -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
                DndMode.TOTAL -> NotificationManager.INTERRUPTION_FILTER_NONE
            }
            notificationManager.setInterruptionFilter(filter)
            ControllerResult.Success
        } catch (e: Exception) {
            ControllerResult.Failure(e.message ?: "setDnd failed")
        }
    }

    override suspend fun getDndMode(): DndMode? = try {
        when (notificationManager.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> DndMode.OFF
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> DndMode.PRIORITY
            NotificationManager.INTERRUPTION_FILTER_NONE -> DndMode.TOTAL
            else -> null
        }
    } catch (_: Exception) { null }
}

class AndroidVolumeController(private val context: Context) : VolumeController {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    override suspend fun setVolume(stream: VolumeStream, level: Int): ControllerResult {
        return try {
            val androidStream = when (stream) {
                VolumeStream.MEDIA -> AudioManager.STREAM_MUSIC
                VolumeStream.RING -> AudioManager.STREAM_RING
                VolumeStream.ALARM -> AudioManager.STREAM_ALARM
                VolumeStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
            }
            val max = audioManager.getStreamMaxVolume(androidStream)
            if (level !in 0..max) return ControllerResult.Failure("Volume must be 0..$max for $stream")
            audioManager.setStreamVolume(androidStream, level, 0)
            ControllerResult.Success
        } catch (e: Exception) {
            ControllerResult.Failure(e.message ?: "setVolume failed")
        }
    }

    override suspend fun getVolume(stream: VolumeStream): Int? = try {
        val androidStream = when (stream) {
            VolumeStream.MEDIA -> AudioManager.STREAM_MUSIC
            VolumeStream.RING -> AudioManager.STREAM_RING
            VolumeStream.ALARM -> AudioManager.STREAM_ALARM
            VolumeStream.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
        }
        audioManager.getStreamVolume(androidStream)
    } catch (_: Exception) { null }
}

class AndroidScreenTimeoutController(private val context: Context) : ScreenTimeoutController {

    override suspend fun setScreenTimeout(timeoutMs: Int): ControllerResult {
        return try {
            if (!Settings.System.canWrite(context)) return ControllerResult.PermissionRequired
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                timeoutMs,
            )
            ControllerResult.Success
        } catch (e: Exception) {
            ControllerResult.Failure(e.message ?: "setScreenTimeout failed")
        }
    }

    override suspend fun getScreenTimeout(): Int? = try {
        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
    } catch (_: Exception) { null }
}

class AndroidDarkModeController(private val context: Context) : DarkModeController {

    private val uiModeManager = context.getSystemService(UiModeManager::class.java)

    override suspend fun setDarkMode(mode: NightMode): ControllerResult = try {
        when (mode) {
            NightMode.OFF -> uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_NO)
            NightMode.ON -> uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_YES)
            NightMode.AUTO -> uiModeManager.setNightMode(UiModeManager.MODE_NIGHT_AUTO)
        }
        ControllerResult.Success
    } catch (e: Exception) {
        ControllerResult.Failure(e.message ?: "setDarkMode failed")
    }

    override suspend fun getDarkMode(): NightMode? = try {
        when (uiModeManager.nightMode) {
            UiModeManager.MODE_NIGHT_NO -> NightMode.OFF
            UiModeManager.MODE_NIGHT_YES -> NightMode.ON
            UiModeManager.MODE_NIGHT_AUTO -> NightMode.AUTO
            else -> null
        }
    } catch (_: Exception) { null }
}

class AndroidScreenStateController(private val context: Context) : ScreenStateController {

    override suspend fun getScreenState(): ScreenState {
        val powerManager = context.getSystemService(PowerManager::class.java)
        return if (powerManager.isInteractive) ScreenState.ON else ScreenState.OFF
    }
}

class AndroidRingerController(private val context: Context) : RingerController {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    override suspend fun setRinger(mode: String): ControllerResult {
        return try {
            val ringerMode = when (mode.uppercase()) {
                "NORMAL" -> AudioManager.RINGER_MODE_NORMAL
                "VIBRATE" -> AudioManager.RINGER_MODE_VIBRATE
                "SILENT" -> AudioManager.RINGER_MODE_SILENT
                else -> return ControllerResult.Failure("Unknown ringer mode: $mode")
            }
            audioManager.ringerMode = ringerMode
            ControllerResult.Success
        } catch (e: Exception) {
            ControllerResult.Failure(e.message ?: "setRinger failed")
        }
    }
}
