package com.tdvorak.nothingmodes.capabilities.controllers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import kotlinx.coroutines.delay

/**
 * Real ActionExecutor that maps typed actions to Android API controllers.
 * Shizuku-required actions (WiFi, Bluetooth, MobileData, WriteSetting) are
 * delegated to DeviceTools via Shizuku when authorized.
 */
class RealActionExecutor(
    private val context: Context,
    private val brightness: BrightnessController,
    private val extraDim: ExtraDimController,
    private val dnd: DndController,
    private val volume: VolumeController,
    private val screenTimeout: ScreenTimeoutController,
    private val darkMode: DarkModeController,
    private val ringer: RingerController,
) : ActionExecutor {

    override suspend fun execute(action: Action, context: FireContext): ActionResult = when (action) {
        is Action.SetDnd -> dnd.setDnd(action.mode).toActionResult()
        is Action.SetDarkMode -> darkMode.setDarkMode(action.mode).toActionResult()
        is Action.SetBrightness -> brightness.setBrightness(action.level).toActionResult()
        is Action.SetAutoBrightness -> brightness.setAutoBrightness(action.on).toActionResult()
        is Action.SetExtraDim -> extraDim.setExtraDim(action.on).toActionResult()
        is Action.SetScreenTimeout -> screenTimeout.setScreenTimeout(action.timeoutMs).toActionResult()
        is Action.SetVolume -> volume.setVolume(action.stream, action.level).toActionResult()
        is Action.SetRinger -> ringer.setRinger(action.mode).toActionResult()
        is Action.Vibrate -> vibrate(action.durationMs)
        is Action.CopyText -> copyText(action.text)
        is Action.LaunchApp -> launchApp(action.pkg)
        is Action.OpenUrl -> openUrl(action.url)
        is Action.OpenSettingsScreen -> openSettings(action.screen, action.pkg)
        is Action.ShowNotification -> showNotification(action.title, action.text)
        is Action.Wait -> {
            delay(action.durationMs)
            ActionResult.Success
        }
        // Shizuku-required actions — return ShizukuRequired for now
        is Action.SetWifi,
        is Action.SetBluetooth,
        is Action.SetMobileData,
        is Action.WriteSetting,
        -> ActionResult.ShizukuRequired

        // Nothing-specific — return Unsupported until NothingIntegrations wired
        is Action.SetGlyph,
        is Action.SetGlyphMatrix,
        -> ActionResult.Unsupported

        // Flashlight — needs camera service, deferred
        is Action.SetFlashlight -> ActionResult.Unsupported
    }

    private fun vibrate(durationMs: Int): ActionResult {
        return try {
            if (durationMs <= 0) return ActionResult.Success
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(VibratorManager::class.java)
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        }
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "vibrate failed")
        }
    }

    private fun copyText(text: String): ActionResult = try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Nothing Modes", text))
        ActionResult.Success
    } catch (e: Exception) {
        ActionResult.Failure(e.message ?: "copyText failed")
    }

    private fun launchApp(pkg: String): ActionResult = try {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            ?: return ActionResult.Failure("No launch intent for $pkg")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ActionResult.Success
    } catch (e: Exception) {
        ActionResult.Failure(e.message ?: "launchApp failed")
    }

    private fun openUrl(url: String): ActionResult = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        ActionResult.Success
    } catch (e: Exception) {
        ActionResult.Failure(e.message ?: "openUrl failed")
    }

    private fun openSettings(screen: SettingsScreen, pkg: String?): ActionResult = try {
        val action = when (screen) {
            SettingsScreen.WIFI -> android.provider.Settings.ACTION_WIFI_SETTINGS
            SettingsScreen.BLUETOOTH -> android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
            SettingsScreen.DISPLAY -> android.provider.Settings.ACTION_DISPLAY_SETTINGS
            SettingsScreen.SOUND -> android.provider.Settings.ACTION_SOUND_SETTINGS
            SettingsScreen.LOCATION -> android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
            SettingsScreen.BATTERY -> Intent.ACTION_POWER_USAGE_SUMMARY
            SettingsScreen.DATE -> android.provider.Settings.ACTION_DATE_SETTINGS
            SettingsScreen.APP_DETAILS -> {
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            }
            SettingsScreen.SETTINGS -> android.provider.Settings.ACTION_SETTINGS
        }
        val intent = Intent(action).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (screen == SettingsScreen.APP_DETAILS && pkg != null) {
            intent.data = Uri.parse("package:$pkg")
        }
        context.startActivity(intent)
        ActionResult.Success
    } catch (e: Exception) {
        ActionResult.Failure(e.message ?: "openSettings failed")
    }

    private fun showNotification(title: String, text: String): ActionResult = try {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Automation Notifications",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            nm.createNotificationChannel(channel)
        }
        val notification = androidx.core.app.NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ID_BASE + (title.hashCode() and 0xFFF), notification)
        ActionResult.Success
    } catch (e: Exception) {
        ActionResult.Failure(e.message ?: "showNotification failed")
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "automation_notifications"
        private const val NOTIFICATION_ID_BASE = 2000

        /** Factory: creates a RealActionExecutor with all Android controllers. */
        fun create(context: Context): RealActionExecutor = RealActionExecutor(
            context = context.applicationContext,
            brightness = AndroidBrightnessController(context),
            extraDim = AndroidExtraDimController(context),
            dnd = AndroidDndController(context),
            volume = AndroidVolumeController(context),
            screenTimeout = AndroidScreenTimeoutController(context),
            darkMode = AndroidDarkModeController(context),
            ringer = AndroidRingerController(context),
        )
    }
}

/** Maps ControllerResult to ActionResult. */
fun ControllerResult.toActionResult(): ActionResult = when (this) {
    is ControllerResult.Success -> ActionResult.Success
    is ControllerResult.Failure -> ActionResult.Failure(reason)
    is ControllerResult.Unsupported -> ActionResult.Unsupported
    is ControllerResult.PermissionRequired -> ActionResult.PermissionRequired
}
