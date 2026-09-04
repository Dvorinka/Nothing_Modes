package com.tdvorak.nothingmodes.capabilities.controllers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import android.provider.Settings
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import com.tdvorak.nothingmodes.nothing.GlyphPresets
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.nothing.NothingGlyphProvider
import com.tdvorak.nothingmodes.shizuku.PrivilegedShell
import com.tdvorak.nothingmodes.shizuku.ShizukuGatewayStatus
import kotlinx.coroutines.delay

/**
 * Real ActionExecutor that maps typed actions to Android API controllers.
 * Shizuku-required actions (WiFi, Bluetooth, MobileData, WriteSetting) are
 * delegated to PrivilegedShell when available.
 * Glyph actions use NothingGlyphProvider / NothingGlyphMatrixProvider.
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
    private val shell: PrivilegedShell? = null,
    private val glyphProvider: NothingGlyphProvider? = null,
    private val glyphMatrixProvider: NothingGlyphMatrixProvider? = null,
) : ActionExecutor {

    override suspend fun execute(action: Action, context: FireContext): ActionResult = when (action) {
        is Action.SetDnd -> dnd.setDnd(action.mode).toActionResult()
        is Action.SetDarkMode -> darkMode.setDarkMode(action.mode).toActionResult()
        is Action.SetBrightness -> brightness.setBrightness(action.level).toActionResult()
        is Action.SetAutoBrightness -> brightness.setAutoBrightness(action.on).toActionResult()
        is Action.SetExtraDim -> setExtraDim(action.on)
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
            if (action.durationMs <= 0) ActionResult.Success
            else { delay(action.durationMs); ActionResult.Success }
        }
        // Shizuku-required actions (with public-API fallbacks where possible)
        is Action.SetWifi -> executeShell(wifiCommand(action.on))
        is Action.SetBluetooth -> setBluetooth(action.on)
        is Action.SetMobileData -> executeShell(mobileDataCommand(action.on))
        is Action.WriteSetting -> writeSetting(action)

        // Flashlight
        is Action.SetFlashlight -> setFlashlight(action.on)

        // Glyph light stripe
        is Action.SetGlyph -> setGlyph(action.on, action.channels)

        // Glyph Matrix
        is Action.SetGlyphMatrix -> setGlyphMatrix(action.colors?.toIntArray(), action.restore)

        // Advanced Glyph actions
        is Action.GlyphAnimate -> glyphAnimate(action)
        is Action.GlyphProgress -> glyphProgress(action.progress, action.reverse)
        is Action.GlyphText -> glyphText(action)
        is Action.GlyphScrollingText -> glyphScrollingText(action.text)
        is Action.GlyphPreset -> glyphPreset(action.preset)
        is Action.GlyphTurnOff -> glyphTurnOff()

        // System settings toggles (Phase 4)
        is Action.SetAutoRotate -> setAutoRotate(action.on)
        is Action.SetBatterySaver -> executeShell(batterySaverCommand(action.on))
        is Action.SetAirplaneMode -> setAirplaneMode(action.on)
        is Action.SetDataSaver -> executeShell(dataSaverCommand(action.on))
        is Action.SetHotspot -> executeShell(hotspotCommand(action.on))
        is Action.SetNfc -> executeShell(nfcCommand(action.on))
        is Action.SetRefreshRate -> setRefreshRate(action.hz)
        is Action.SetScreenRotation -> setScreenRotation(action.orientation)
        is Action.MediaControl -> mediaControl(action.command)
    }

    // --- Shizuku shell actions ---

    private suspend fun executeShell(command: List<String>): ActionResult {
        val sh = shell ?: return ActionResult.ShizukuRequired
        return try {
            val result = sh.run(command, priority = 0, timeoutMillis = 10_000)
            if (result.successful) ActionResult.Success
            else ActionResult.Failure("exit=${result.exitCode} stderr=${result.stderrText.take(200)}")
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "shell command failed")
        }
    }

    private fun wifiCommand(on: Boolean) = listOf("svc", "wifi", if (on) "enable" else "disable")
    private fun bluetoothCommand(on: Boolean) = listOf("svc", "bluetooth", if (on) "enable" else "disable")
    private fun mobileDataCommand(on: Boolean) = listOf("svc", "data", if (on) "enable" else "disable")

    private fun writeSettingCommand(action: Action.WriteSetting): List<String> {
        val namespace = when (action.namespace) {
            SettingNamespace.SYSTEM -> "system"
            SettingNamespace.SECURE -> "secure"
            SettingNamespace.GLOBAL -> "global"
        }
        return listOf("settings", "put", namespace, action.key, action.value)
    }

    // --- Flashlight via CameraManager ---

    private var torchCameraId: String? = null

    private suspend fun setFlashlight(on: Boolean): ActionResult {
        return try {
            val cm = context.getSystemService(CameraManager::class.java)
            if (torchCameraId == null) {
                torchCameraId = cm.cameraIdList.firstOrNull { id ->
                    val chars = cm.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return ActionResult.Unsupported
            }
            cm.setTorchMode(torchCameraId!!, on)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "flashlight failed")
        }
    }

    // --- Glyph light stripe ---

    private suspend fun setGlyph(on: Boolean, channels: List<Int>?): ActionResult {
        val provider = glyphProvider
            ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val result = if (on) provider.toggle(channels) else provider.turnOff()
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph failed")
        }
    }

    // --- Extra Dim with Shizuku fallback ---

    private suspend fun setExtraDim(on: Boolean): ActionResult {
        val result = extraDim.setExtraDim(on).toActionResult()
        if (result !is ActionResult.PermissionRequired) return result
        return executeShell(listOf(
            "settings", "put", "secure", "reduce_bright_colors_activated", if (on) "1" else "0",
        ))
    }

    // --- Glyph Matrix ---

    private suspend fun setGlyphMatrix(colors: IntArray?, restore: Boolean): ActionResult {
        val provider = glyphMatrixProvider
            ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val result = when {
                restore -> provider.closeFrame()
                colors != null && colors.isNotEmpty() -> provider.setFrame(colors)
                else -> provider.turnOff()
            }
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph matrix failed")
        }
    }

    // --- Advanced Glyph actions ---

    private suspend fun glyphAnimate(action: Action.GlyphAnimate): ActionResult {
        val provider = glyphProvider
            ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val zone = action.zone
            val result = if (zone != null) {
                provider.animateZone(zone, action.periodMs, action.cycles, action.intervalMs)
            } else {
                provider.animate(action.channels ?: emptyList(), action.periodMs, action.cycles, action.intervalMs)
            }
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph animate failed")
        }
    }

    private suspend fun glyphProgress(progress: Int, reverse: Boolean): ActionResult {
        val provider = glyphProvider
            ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val result = provider.displayProgress(progress, reverse)
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph progress failed")
        }
    }

    private suspend fun glyphText(action: Action.GlyphText): ActionResult {
        val provider = glyphMatrixProvider
            ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val result = provider.displayText(action.text, action.x, action.y, action.scale, action.brightness)
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph text failed")
        }
    }

    private suspend fun glyphScrollingText(text: String): ActionResult {
        val provider = glyphMatrixProvider
            ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val result = provider.displayScrollingText(text)
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph scrolling text failed")
        }
    }

    private suspend fun glyphPreset(preset: String): ActionResult {
        val visual = presetFor(preset) ?: return ActionResult.Failure("Unknown preset: $preset")
        return renderGlyphVisual(visual)
    }

    private suspend fun glyphTurnOff(): ActionResult {
        val stripeResult = glyphProvider?.takeIf { it.isAvailable() }?.turnOff()
        val matrixResult = glyphMatrixProvider?.takeIf { it.isAvailable() }?.turnOff()
        return when {
            stripeResult != null -> glyphResultToActionResult(stripeResult)
            matrixResult != null -> glyphResultToActionResult(matrixResult)
            else -> ActionResult.Unsupported
        }
    }

    private fun presetFor(name: String): GlyphPresets.GlyphVisual? = when (name.lowercase()) {
        "sleep", "sleep_mode" -> GlyphPresets.sleepMode
        "morning" -> GlyphPresets.morning
        "work", "work_focus" -> GlyphPresets.workFocus
        "dnd", "dnd_active" -> GlyphPresets.dndActive
        "dnd_off" -> GlyphPresets.dndOff
        "automation_fired", "fired" -> GlyphPresets.automationFired
        "error" -> GlyphPresets.error
        "success" -> GlyphPresets.success
        "charging", "charging_start" -> GlyphPresets.chargingStart
        "charging_complete" -> GlyphPresets.chargingComplete
        "incoming_call", "call" -> GlyphPresets.incomingCall
        "sms", "sms_received" -> GlyphPresets.smsReceived
        "timer", "timer_fired" -> GlyphPresets.timerFired
        "off" -> GlyphPresets.off
        else -> null
    }

    private suspend fun renderGlyphVisual(visual: GlyphPresets.GlyphVisual): ActionResult {
        return when (visual) {
            is GlyphPresets.GlyphVisual.Stripe -> {
                val provider = glyphProvider ?: return ActionResult.Unsupported
                if (!provider.isAvailable()) return ActionResult.Unsupported
                val progress = visual.progress
                val zone = visual.zone
                val channels = visual.channels
                val result = when {
                    progress != null -> provider.displayProgress(progress)
                    visual.periodMs > 0 || visual.cycles > 0 -> {
                        if (zone != null) {
                            provider.animateZone(zone, visual.periodMs, visual.cycles, visual.intervalMs)
                        } else {
                            provider.animate(channels ?: emptyList(), visual.periodMs, visual.cycles, visual.intervalMs)
                        }
                    }
                    zone != null -> provider.toggleZone(zone)
                    else -> provider.toggle(channels)
                }
                glyphResultToActionResult(result)
            }
            is GlyphPresets.GlyphVisual.Matrix -> {
                val provider = glyphMatrixProvider ?: return ActionResult.Unsupported
                if (!provider.isAvailable()) return ActionResult.Unsupported
                val color = visual.color
                val text = visual.text
                val scrollingText = visual.scrollingText
                val percentFill = visual.percentFill
                val number = visual.number
                val result = when {
                    color != null -> provider.fillMatrix(color)
                    text != null -> provider.displayText(text)
                    scrollingText != null -> provider.displayScrollingText(scrollingText)
                    percentFill != null -> provider.displayPercentFill(percentFill, visual.fillColor)
                    number != null -> provider.displayNumber(number)
                    else -> provider.turnOff()
                }
                glyphResultToActionResult(result)
            }
            GlyphPresets.GlyphVisual.Off -> glyphTurnOff()
        }
    }

    private fun glyphResultToActionResult(result: com.tdvorak.nothingmodes.nothing.GlyphResult): ActionResult = when (result) {
        com.tdvorak.nothingmodes.nothing.GlyphResult.Success -> ActionResult.Success
        is com.tdvorak.nothingmodes.nothing.GlyphResult.Failure -> ActionResult.Failure(result.reason)
        com.tdvorak.nothingmodes.nothing.GlyphResult.Unsupported -> ActionResult.Unsupported
        com.tdvorak.nothingmodes.nothing.GlyphResult.PermissionRequired -> ActionResult.PermissionRequired
        com.tdvorak.nothingmodes.nothing.GlyphResult.ServiceUnavailable -> ActionResult.Failure("glyph service unavailable")
    }

    // --- Local actions ---

    @SuppressLint("MissingPermission")
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

    private fun launchApp(pkg: String): ActionResult {
        return try {
            // Validate package name format to prevent intent injection
            if (!pkg.matches(Regex("^[A-Za-z0-9][A-Za-z0-9_.]*$"))) {
                return ActionResult.Failure("Invalid package name: $pkg")
            }
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                ?: return ActionResult.Failure("No launch intent for $pkg")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "launchApp failed")
        }
    }

    private fun openUrl(url: String): ActionResult {
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase()
            // Restrict to http/https to prevent arbitrary deep-link/intent injection
            if (scheme != "http" && scheme != "https") {
                return ActionResult.Failure("Only http/https URLs are allowed")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "openUrl failed")
        }
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
            SettingsScreen.APP_DETAILS -> android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
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

    private fun showNotification(title: String, text: String): ActionResult {
        return try {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) return ActionResult.PermissionRequired
            }
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Automation Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        nm.createNotificationChannel(channel)
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
    }

    // --- System settings toggles (Phase 4) ---

    private fun setAutoRotate(on: Boolean): ActionResult {
        return try {
            if (!Settings.System.canWrite(context)) return ActionResult.PermissionRequired
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (on) 1 else 0,
            )
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "setAutoRotate failed")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun setBluetooth(on: Boolean): ActionResult {
        // Try public API first (deprecated on API 33+ but functional on most Nothing OS builds)
        return try {
            val bm = context.getSystemService(BluetoothManager::class.java)
            val adapter = bm.adapter
                ?: return executeShell(bluetoothCommand(on))
            val enabled = if (on) adapter.enable() else adapter.disable()
            if (enabled) ActionResult.Success
            else executeShell(bluetoothCommand(on))
        } catch (_: SecurityException) {
            executeShell(bluetoothCommand(on))
        } catch (e: Exception) {
            // Public API failed — fall back to Shizuku
            executeShell(bluetoothCommand(on))
        }
    }

    private suspend fun writeSetting(action: Action.WriteSetting): ActionResult {
        // Validate key/value form before any write path (shell or public API)
        if (!com.tdvorak.nothingmodes.engine.model.WriteSettingPolicy.valid(action)) {
            return ActionResult.Failure("Invalid setting key or value (rejected by policy)")
        }
        // System namespace can use public Settings API with WRITE_SETTINGS permission
        if (action.namespace == SettingNamespace.SYSTEM) {
            return try {
                if (!Settings.System.canWrite(context)) return ActionResult.PermissionRequired
                val put = runCatching {
                    Settings.System.putString(
                        context.contentResolver,
                        action.key,
                        action.value,
                    )
                }.getOrDefault(false)
                if (put) ActionResult.Success
                else executeShell(writeSettingCommand(action))
            } catch (_: Exception) {
                executeShell(writeSettingCommand(action))
            }
        }
        return executeShell(writeSettingCommand(action))
    }

    private suspend fun setAirplaneMode(on: Boolean): ActionResult {
        // Write the setting via Shizuku, then broadcast via shell so the radio toggles.
        // ACTION_AIRPLANE_MODE_CHANGED is a protected broadcast that third-party apps cannot send
        // on Android 12+, so we use `am broadcast` through the privileged shell.
        val writeResult = executeShell(airplaneModeCommand(on))
        if (writeResult !is ActionResult.Success) return writeResult
        return executeShell(listOf(
            "am", "broadcast", "-a", "android.intent.action.AIRPLANE_MODE",
            "--ez", "state", on.toString(),
        ))
    }

    private fun batterySaverCommand(on: Boolean) = listOf(
        "settings", "put", "global", "low_power", if (on) "1" else "0",
    )
    private fun airplaneModeCommand(on: Boolean) = listOf(
        "settings", "put", "global", "airplane_mode_on", if (on) "1" else "0",
    )
    private fun dataSaverCommand(on: Boolean) = listOf(
        "settings", "put", "global", "data_saver", if (on) "1" else "0",
    )
    private fun hotspotCommand(on: Boolean) = listOf(
        "settings", "put", "global", "wifi_ap_state", if (on) "1" else "0",
    )
    private fun nfcCommand(on: Boolean) = listOf(
        "svc", "nfc", if (on) "enable" else "disable",
    )

    private fun setRefreshRate(hz: Int): ActionResult {
        return try {
            if (!Settings.System.canWrite(context)) return ActionResult.PermissionRequired
            Settings.System.putInt(context.contentResolver, "peak_refresh_rate", hz)
            Settings.System.putInt(context.contentResolver, "min_refresh_rate", hz)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "setRefreshRate failed")
        }
    }

    private fun setScreenRotation(orientation: ScreenOrientation): ActionResult {
        return try {
            if (!Settings.System.canWrite(context)) return ActionResult.PermissionRequired
            when (orientation) {
                ScreenOrientation.AUTO -> {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,
                        1,
                    )
                }
                ScreenOrientation.PORTRAIT -> {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,
                        0,
                    )
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.USER_ROTATION,
                        0, // PORTRAIT
                    )
                }
                ScreenOrientation.LANDSCAPE -> {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.ACCELEROMETER_ROTATION,
                        0,
                    )
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.USER_ROTATION,
                        1, // LANDSCAPE
                    )
                }
            }
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "setScreenRotation failed")
        }
    }

    private fun mediaControl(command: MediaCommand): ActionResult {
        return try {
            val keyCode = when (command) {
                MediaCommand.PLAY_PAUSE -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                MediaCommand.NEXT -> android.view.KeyEvent.KEYCODE_MEDIA_NEXT
                MediaCommand.PREVIOUS -> android.view.KeyEvent.KEYCODE_MEDIA_PREVIOUS
                MediaCommand.STOP -> android.view.KeyEvent.KEYCODE_MEDIA_STOP
            }
            val audioManager = context.getSystemService(android.media.AudioManager::class.java)
            // Try dispatchMediaKeyEvent first (works if app is media session owner)
            try {
                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode),
                )
                audioManager.dispatchMediaKeyEvent(
                    android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode),
                )
                ActionResult.Success
            } catch (_: Exception) {
                // Fallback: send media button broadcast intent
                val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                    putExtra(
                        Intent.EXTRA_KEY_EVENT,
                        android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, keyCode),
                    )
                }
                context.sendOrderedBroadcast(intent, null)
                intent.putExtra(
                    Intent.EXTRA_KEY_EVENT,
                    android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, keyCode),
                )
                context.sendOrderedBroadcast(intent, null)
                ActionResult.Success
            }
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "mediaControl failed")
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "automation_notifications"
        private const val NOTIFICATION_ID_BASE = 2000

        /** Factory: creates a RealActionExecutor with all Android controllers. */
        fun create(
            context: Context,
            shell: PrivilegedShell? = null,
            glyphProvider: NothingGlyphProvider? = null,
            glyphMatrixProvider: NothingGlyphMatrixProvider? = null,
        ): RealActionExecutor = RealActionExecutor(
            context = context.applicationContext,
            brightness = AndroidBrightnessController(context),
            extraDim = AndroidExtraDimController(context),
            dnd = AndroidDndController(context),
            volume = AndroidVolumeController(context),
            screenTimeout = AndroidScreenTimeoutController(context),
            darkMode = AndroidDarkModeController(context),
            ringer = AndroidRingerController(context),
            shell = shell,
            glyphProvider = glyphProvider,
            glyphMatrixProvider = glyphMatrixProvider,
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
