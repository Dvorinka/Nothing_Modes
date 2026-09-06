package com.tdvorak.nothingmodes.capabilities.controllers

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
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
import androidx.core.content.ContextCompat
import android.provider.Settings
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.isGlyphAction
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.FeatureFlags
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import com.tdvorak.nothingmodes.nothing.GlyphPreflight
import com.tdvorak.nothingmodes.nothing.GlyphPresets
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.nothing.NothingGlyphProvider
import com.tdvorak.nothingmodes.shizuku.PrivilegedShellFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val shellFactory: PrivilegedShellFactory? = null,
    private val glyphProvider: NothingGlyphProvider? = null,
    private val glyphMatrixProvider: NothingGlyphMatrixProvider? = null,
) : ActionExecutor {
    override suspend fun execute(
        action: Action,
        context: FireContext,
    ): ActionResult {
        // Glyph pre-flight: when the lights can't be driven (no hardware, or
        // another toy owns them), fail honestly and remind the user once —
        // never pretend a glyph action landed.
        if (action.isGlyphAction || action is Action.GlyphTurnOff) {
            val problem = GlyphPreflight.check(this.context)
            if (problem != null) {
                GlyphPreflight.notifyIfNeeded(this.context, problem)
                return ActionResult.Failure("glyph: ${GlyphPreflight.message(problem)}")
            }
            if (!ensureForAction(action)) {
                return ActionResult.Failure("glyph service not connected")
            }
        }
        return dispatch(action, context)
    }

    private suspend fun dispatch(
        action: Action,
        context: FireContext,
    ): ActionResult =
        when (action) {
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
                val capped = action.durationMs.coerceIn(0, 300_000)
                if (capped <= 0) {
                    ActionResult.Success
                } else {
                    delay(capped)
                    ActionResult.Success
                }
            }
            // Shizuku-required actions (with public-API fallbacks where possible)
            is Action.SetWifi -> setWifi(action.on)
            is Action.SetBluetooth -> setBluetooth(action.on)
            is Action.SetMobileData ->
                shellOrPanel(
                    mobileDataCommand(action.on),
                    connectivityPanel(),
                )
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
            is Action.GlyphScrollingText -> glyphScrollingText(action)
            is Action.GlyphPreset -> glyphPreset(action.preset)
            is Action.GlyphIcon -> glyphIcon(action.name)
            is Action.GlyphNumber -> glyphNumber(action.number)
            is Action.GlyphCountdown -> glyphCountdown(action.seconds)
            is Action.GlyphMusic -> glyphMusic()
            is Action.GlyphTurnOff -> glyphTurnOff()

            // System settings toggles (Phase 4)
            is Action.SetAutoRotate -> setAutoRotate(action.on)
            is Action.SetBatterySaver ->
                shellOrPanel(
                    batterySaverCommand(action.on),
                    Settings.ACTION_BATTERY_SAVER_SETTINGS,
                )
            is Action.SetAirplaneMode -> setAirplaneMode(action.on)
            is Action.SetDataSaver ->
                shellOrPanel(
                    dataSaverCommand(action.on),
                    // Not a public Settings constant; resolves on most skins.
                    "android.settings.DATA_SAVER_SETTINGS",
                )
            is Action.SetHotspot ->
                shellOrPanel(
                    hotspotCommand(action.on),
                    "android.settings.TETHER_SETTINGS",
                )
            is Action.SetNfc -> {
                // `svc nfc` is killed outright on Nothing OS 4.1 — when the
                // shell attempt fails for any reason, fall through to the panel.
                val r = executeShell(nfcCommand(action.on))
                if (r is ActionResult.Success) r else openPanel(Settings.ACTION_NFC_SETTINGS)
            }
            is Action.SetRefreshRate -> setRefreshRate(action.hz)
            is Action.SetScreenRotation -> setScreenRotation(action.orientation)
            is Action.MediaControl -> mediaControl(action.command)

            // Extended actions (Phase 5)
            is Action.SendSms -> sendSms(action.number, action.text)
            is Action.LockScreen -> lockScreen()
            is Action.SetLocationMode -> setLocationMode(action.mode)
            is Action.SetAutoSync ->
                shellOrPanel(
                    autoSyncCommand(action.on),
                    Settings.ACTION_SYNC_SETTINGS,
                )
            is Action.ClearNotifications -> clearNotifications()
            is Action.SetAlwaysOnDisplay ->
                shellOrPanel(
                    aodCommand(action.on),
                    Settings.ACTION_DISPLAY_SETTINGS,
                )
            is Action.TakeScreenshot -> ActionResult.Unsupported
        }

    // --- Shizuku shell actions ---

    private suspend fun executeShell(command: List<String>): ActionResult {
        val sh = shellFactory?.resolve() ?: return ActionResult.ShizukuRequired
        return try {
            val result = sh.run(command, priority = 0, timeoutMillis = 10_000)
            if (result.successful) {
                ActionResult.Success
            } else {
                ActionResult.Failure("exit=${result.exitCode} stderr=${result.stderrText.take(200)}")
            }
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "shell command failed")
        }
    }

    /**
     * Try the privileged shell first; when Shizuku isn't available, open the
     * relevant system panel/settings page so the user can finish the toggle.
     * Beats a dead "Shizuku required" on unprivileged phones.
     */
    private suspend fun shellOrPanel(
        command: List<String>,
        panelAction: String,
    ): ActionResult {
        val shellResult = executeShell(command)
        if (shellResult !is ActionResult.ShizukuRequired) return shellResult
        return openPanel(panelAction)
    }

    /** Open a system settings page/panel; returns NeedsUserAction on success. */
    private fun openPanel(action: String): ActionResult =
        try {
            context.startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            ActionResult.NeedsUserAction
        } catch (e: Exception) {
            ActionResult.ShizukuRequired
        }

    /** Internet connectivity panel (API 29+); falls back to Wi-Fi settings. */
    private fun connectivityPanel(): String =
        if (Build.VERSION.SDK_INT >= 29) {
            Settings.Panel.ACTION_INTERNET_CONNECTIVITY
        } else {
            Settings.ACTION_WIFI_SETTINGS
        }

    private fun wifiCommand(on: Boolean) = listOf("svc", "wifi", if (on) "enable" else "disable")

    private fun bluetoothCommand(on: Boolean) = listOf("svc", "bluetooth", if (on) "enable" else "disable")

    private fun mobileDataCommand(on: Boolean) = listOf("svc", "data", if (on) "enable" else "disable")

    private fun autoSyncCommand(on: Boolean) = listOf("settings", "put", "global", "auto_sync", if (on) "1" else "0")

    private fun aodCommand(on: Boolean) = listOf("settings", "put", "secure", "doze_always_on", if (on) "1" else "0")

    private fun writeSettingCommand(action: Action.WriteSetting): List<String> {
        val namespace =
            when (action.namespace) {
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
            val cameraId =
                torchCameraId ?: cm.cameraIdList.firstOrNull { id ->
                    val chars = cm.getCameraCharacteristics(id)
                    chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
            if (cameraId == null) return ActionResult.Unsupported
            torchCameraId = cameraId
            cm.setTorchMode(cameraId, on)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "flashlight failed")
        }
    }

    // --- Glyph light stripe ---

    private suspend fun setGlyph(
        on: Boolean,
        channels: List<Int>?,
    ): ActionResult {
        val provider = glyphProvider?.takeIf { it.isAvailable() }
        if (provider == null) {
            // No light stripe (matrix-only devices) — a full frame stands in
            // for "all channels on".
            val matrix = glyphMatrixProvider?.takeIf { it.isAvailable() }
                ?: return ActionResult.Unsupported
            return try {
                glyphResultToActionResult(
                    if (on) matrix.displayPercentFill(100) else matrix.turnOff(),
                )
            } catch (e: Exception) {
                ActionResult.Failure(e.message ?: "glyph failed")
            }
        }
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
        return executeShell(
            listOf(
                "settings",
                "put",
                "secure",
                "reduce_bright_colors_activated",
                if (on) "1" else "0",
            ),
        )
    }

    // --- Glyph Matrix ---

    private suspend fun setGlyphMatrix(
        colors: IntArray?,
        restore: Boolean,
    ): ActionResult {
        val provider =
            glyphMatrixProvider
                ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val result =
                when {
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
        val provider = glyphProvider?.takeIf { it.isAvailable() }
        if (provider == null) {
            // Matrix-only devices: degrade the zone animation to a full-frame
            // blink — on/off per cycle at roughly the requested period.
            val matrix = glyphMatrixProvider?.takeIf { it.isAvailable() }
                ?: return ActionResult.Unsupported
            return try {
                val half = (action.periodMs / 2L).coerceAtLeast(200L)
                repeat(action.cycles.coerceIn(1, 10)) {
                    glyphResultToActionResult(matrix.displayPercentFill(100))
                    delay(half)
                    glyphResultToActionResult(matrix.turnOff())
                    delay(half)
                }
                ActionResult.Success
            } catch (e: Exception) {
                ActionResult.Failure(e.message ?: "glyph animate failed")
            }
        }
        return try {
            val zone = action.zone
            val result =
                if (zone != null) {
                    provider.animateZone(zone, action.periodMs, action.cycles, action.intervalMs)
                } else {
                    provider.animate(action.channels ?: emptyList(), action.periodMs, action.cycles, action.intervalMs)
                }
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph animate failed")
        }
    }

    private suspend fun glyphProgress(
        progress: Int,
        reverse: Boolean,
    ): ActionResult {
        val provider = glyphProvider?.takeIf { it.isConnected() }
        if (provider == null) {
            // Matrix-only devices (e.g. Phone 3): use the circular arc —
            // the same renderer Nothing's own progress toys use.
            val matrix = glyphMatrixProvider?.takeIf { it.isConnected() }
                ?: return ActionResult.Unsupported
            return try {
                glyphResultToActionResult(matrix.displayProgressArc(progress, label = progress.toString()))
            } catch (e: Exception) {
                ActionResult.Failure(e.message ?: "glyph progress failed")
            }
        }
        return try {
            val result = provider.displayProgress(progress, reverse)
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph progress failed")
        }
    }

    private suspend fun glyphText(action: Action.GlyphText): ActionResult {
        val provider =
            glyphMatrixProvider
                ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val result = provider.displayText(action.text, action.x, action.y, action.scale, action.brightness)
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph text failed")
        }
    }

    private suspend fun glyphScrollingText(action: Action.GlyphScrollingText): ActionResult {
        val provider =
            glyphMatrixProvider
                ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            val result = provider.displayScrollingText(action.text, action.intervalMs, action.stepPx)
            glyphResultToActionResult(result)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph scrolling text failed")
        }
    }

    private suspend fun glyphPreset(preset: String): ActionResult {
        val visual = presetFor(preset) ?: return ActionResult.Failure("Unknown preset: $preset")
        return renderGlyphVisual(visual)
    }

    private suspend fun glyphIcon(name: String): ActionResult {
        val provider =
            glyphMatrixProvider
                ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            glyphResultToActionResult(provider.displayIcon(name))
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph icon failed")
        }
    }

    private suspend fun glyphNumber(number: Int): ActionResult {
        val provider =
            glyphMatrixProvider
                ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            glyphResultToActionResult(provider.displayNumber(number))
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph number failed")
        }
    }

    private suspend fun glyphCountdown(seconds: Int): ActionResult {
        val provider =
            glyphMatrixProvider
                ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported
        return try {
            glyphResultToActionResult(provider.displayCountdown(seconds))
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph countdown failed")
        }
    }

    private suspend fun glyphMusic(): ActionResult {
        val provider =
            glyphMatrixProvider
                ?: return ActionResult.Unsupported
        if (!provider.isAvailable()) return ActionResult.Unsupported

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return ActionResult.Failure("RECORD_AUDIO permission required for live music visualizer")
        }

        return try {
            glyphResultToActionResult(provider.startMusicVisualizer())
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "glyph music visualizer failed")
        }
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

    private fun presetFor(name: String): GlyphPresets.GlyphVisual? =
        when (name.lowercase()) {
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
            "timer_done" -> GlyphPresets.timerDone
            "notification_low", "notif_low" -> GlyphPresets.notificationLow
            "notification_high", "notif_high" -> GlyphPresets.notificationHigh
            "notification_critical", "notif_critical" -> GlyphPresets.notificationCritical
            "off" -> GlyphPresets.off
            else -> null
        }

    private suspend fun renderGlyphVisual(visual: GlyphPresets.GlyphVisual): ActionResult {
        return when (visual) {
            is GlyphPresets.GlyphVisual.Stripe -> {
                val provider = glyphProvider?.takeIf { it.isAvailable() }
                if (provider == null) {
                    // No light stripe on this device (matrix-only, e.g. Phone 3):
                    // degrade — progress presets become a percent fill, pulse
                    // presets become a full-frame flash.
                    val matrix = glyphMatrixProvider?.takeIf { it.isAvailable() }
                        ?: return ActionResult.Unsupported
                    return try {
                        glyphResultToActionResult(
                            matrix.displayPercentFill(visual.progress ?: 100),
                        )
                    } catch (e: Exception) {
                        ActionResult.Failure(e.message ?: "glyph matrix fallback failed")
                    }
                }
                val progress = visual.progress
                val zone = visual.zone
                val channels = visual.channels
                val result =
                    when {
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
                val result =
                    when {
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

    private fun glyphResultToActionResult(result: com.tdvorak.nothingmodes.nothing.GlyphResult): ActionResult =
        when (result) {
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

    private fun copyText(text: String): ActionResult =
        try {
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
            val intent =
                context.packageManager.getLaunchIntentForPackage(pkg)
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
            val intent =
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "openUrl failed")
        }
    }

    private fun openSettings(
        screen: SettingsScreen,
        pkg: String?,
    ): ActionResult =
        try {
            val action =
                when (screen) {
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

    private fun showNotification(
        title: String,
        text: String,
    ): ActionResult {
        return try {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    return ActionResult.PermissionRequired
                }
            }
            val channel =
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Automation Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            nm.createNotificationChannel(channel)
            val notification =
                androidx.core.app.NotificationCompat
                    .Builder(context, NOTIFICATION_CHANNEL_ID)
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
            val adapter =
                bm.adapter
                    ?: return executeShell(bluetoothCommand(on))
            val enabled = if (on) adapter.enable() else adapter.disable()
            if (enabled) {
                ActionResult.Success
            } else {
                shellOrPanel(bluetoothCommand(on), Settings.ACTION_BLUETOOTH_SETTINGS)
            }
        } catch (_: SecurityException) {
            shellOrPanel(bluetoothCommand(on), Settings.ACTION_BLUETOOTH_SETTINGS)
        } catch (e: Exception) {
            // Public API failed — Shizuku, else the Bluetooth settings page
            shellOrPanel(bluetoothCommand(on), Settings.ACTION_BLUETOOTH_SETTINGS)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun setWifi(on: Boolean): ActionResult {
        // WifiManager.setWifiEnabled() works on API <29. On API 29+ it throws or returns false.
        // Try public API first, fall back to Shizuku shell command.
        return try {
            val wm = context.getSystemService(android.net.wifi.WifiManager::class.java)

            @Suppress("DEPRECATION")
            val enabled = wm.setWifiEnabled(on)
            if (enabled) {
                ActionResult.Success
            } else {
                shellOrPanel(wifiCommand(on), connectivityPanel())
            }
        } catch (_: SecurityException) {
            shellOrPanel(wifiCommand(on), connectivityPanel())
        } catch (_: NoSuchMethodError) {
            // Method removed on newer API levels — Shizuku or the panel
            shellOrPanel(wifiCommand(on), connectivityPanel())
        } catch (e: Exception) {
            shellOrPanel(wifiCommand(on), connectivityPanel())
        }
    }

    private suspend fun writeSetting(action: Action.WriteSetting): ActionResult {
        // Validate key/value form before any write path (shell or public API)
        if (!com.tdvorak.nothingmodes.engine.model.WriteSettingPolicy
                .valid(action)
        ) {
            return ActionResult.Failure("Invalid setting key or value (rejected by policy)")
        }
        // System namespace can use public Settings API with WRITE_SETTINGS permission
        if (action.namespace == SettingNamespace.SYSTEM) {
            return try {
                if (!Settings.System.canWrite(context)) return ActionResult.PermissionRequired
                val put =
                    runCatching {
                        Settings.System.putString(
                            context.contentResolver,
                            action.key,
                            action.value,
                        )
                    }.getOrDefault(false)
                if (put) {
                    ActionResult.Success
                } else {
                    executeShell(writeSettingCommand(action))
                }
            } catch (_: Exception) {
                executeShell(writeSettingCommand(action))
            }
        }
        return executeShell(writeSettingCommand(action))
    }

    private suspend fun setAirplaneMode(on: Boolean): ActionResult {
        // `cmd connectivity airplane-mode` toggles the radio and updates the
        // setting atomically (Android 12+); the AIRPLANE_MODE broadcast is
        // protected even for shell, so it is not usable.
        return shellOrPanel(
            listOf(
                "cmd",
                "connectivity",
                "airplane-mode",
                if (on) "enable" else "disable",
            ),
            Settings.ACTION_AIRPLANE_MODE_SETTINGS,
        )
    }

    private fun batterySaverCommand(on: Boolean) =
        listOf(
            "settings",
            "put",
            "global",
            "low_power",
            if (on) "1" else "0",
        )

    private fun dataSaverCommand(on: Boolean) =
        listOf(
            "settings",
            "put",
            "global",
            "data_saver",
            if (on) "1" else "0",
        )

    private fun hotspotCommand(on: Boolean) =
        listOf(
            "settings",
            "put",
            "global",
            "wifi_ap_state",
            if (on) "1" else "0",
        )

    private fun nfcCommand(on: Boolean) =
        listOf(
            "svc",
            "nfc",
            if (on) "enable" else "disable",
        )

    private suspend fun setRefreshRate(hz: Int): ActionResult {
        // peak/min_refresh_rate moved to the secure table on newer builds —
        // privileged shell first, public system-table write as fallback.
        val peak =
            executeShell(listOf("settings", "put", "secure", "peak_refresh_rate", hz.toString()))
        if (peak is ActionResult.Success) {
            executeShell(listOf("settings", "put", "secure", "min_refresh_rate", hz.toString()))
            return ActionResult.Success
        }
        if (peak is ActionResult.Failure && shellFactory?.resolve() != null) return peak
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

    private fun mediaControl(command: MediaCommand): ActionResult =
        try {
            val keyCode =
                when (command) {
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
                val intent =
                    Intent(Intent.ACTION_MEDIA_BUTTON).apply {
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

    // --- Extended actions (Phase 5) ---

    @SuppressLint("MissingPermission")
    private suspend fun sendSms(
        number: String,
        text: String,
    ): ActionResult =
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val smsManager = context.getSystemService(android.telephony.SmsManager::class.java)
                smsManager.sendTextMessage(number, null, text, null, null)
            }
            ActionResult.Success
        } catch (e: SecurityException) {
            ActionResult.PermissionRequired
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "sendSms failed")
        }

    private fun lockScreen(): ActionResult {
        if (!FeatureFlags.enableLockScreen) {
            return ActionResult.Unsupported
        }
        return try {
            val dm = context.getSystemService(android.app.admin.DevicePolicyManager::class.java)
            val comp =
                android.content.ComponentName(
                    context.packageName,
                    "com.tdvorak.nothingmodes.automation.lifecycle.NothingDeviceAdminReceiver",
                )
            if (dm.isAdminActive(comp)) {
                dm.lockNow()
                ActionResult.Success
            } else {
                ActionResult.PermissionRequired
            }
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "lockScreen failed")
        }
    }

    private suspend fun setLocationMode(mode: com.tdvorak.nothingmodes.engine.model.LocationMode): ActionResult {
        val value =
            when (mode) {
                com.tdvorak.nothingmodes.engine.model.LocationMode.HIGH_ACCURACY -> 3
                com.tdvorak.nothingmodes.engine.model.LocationMode.BATTERY_SAVING -> 2
                com.tdvorak.nothingmodes.engine.model.LocationMode.DEVICE_ONLY -> 1
                com.tdvorak.nothingmodes.engine.model.LocationMode.OFF -> 0
            }
        // LOCATION_MODE lives in Settings.Secure — privileged shell first.
        val shell =
            executeShell(
                listOf("settings", "put", "secure", Settings.Secure.LOCATION_MODE, value.toString()),
            )
        if (shell !is ActionResult.ShizukuRequired) return shell
        return try {
            Settings.Secure.putInt(context.contentResolver, Settings.Secure.LOCATION_MODE, value)
            ActionResult.Success
        } catch (e: SecurityException) {
            openPanel(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "setLocationMode failed")
        }
    }

    private fun clearNotifications(): ActionResult =
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.cancelAll()
            ActionResult.Success
        } catch (e: Exception) {
            ActionResult.Failure(e.message ?: "clearNotifications failed")
        }

    /**
     * Connect the relevant Glyph provider before a glyph action runs. The DI
     * singleton may have been un-inited by another component or by the system.
     */
    private suspend fun ensureForAction(action: Action): Boolean =
        when (action) {
            is Action.SetGlyph,
            is Action.GlyphAnimate,
            -> ensureStripe()
            is Action.GlyphProgress -> ensureStripeOrMatrix()
            is Action.SetGlyphMatrix,
            is Action.GlyphText,
            is Action.GlyphScrollingText,
            is Action.GlyphPreset,
            is Action.GlyphIcon,
            is Action.GlyphNumber,
            is Action.GlyphCountdown,
            is Action.GlyphMusic,
            -> ensureMatrix()
            is Action.GlyphTurnOff -> ensureStripeOrMatrix()
            else -> true
        }

    private suspend fun ensureStripe(): Boolean =
        glyphProvider?.ensureConnected() ?: false

    private suspend fun ensureMatrix(): Boolean =
        glyphMatrixProvider?.ensureConnected() ?: false

    private suspend fun ensureStripeOrMatrix(): Boolean =
        coroutineScope {
            val s = async { glyphProvider?.ensureConnected() ?: false }
            val m = async { glyphMatrixProvider?.ensureConnected() ?: false }
            s.await() || m.await()
        }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "automation_notifications"
        private const val NOTIFICATION_ID_BASE = 2000

        /** Factory: creates a RealActionExecutor with all Android controllers. */
        fun create(
            context: Context,
            shellFactory: PrivilegedShellFactory? = null,
            glyphProvider: NothingGlyphProvider? = null,
            glyphMatrixProvider: NothingGlyphMatrixProvider? = null,
        ): RealActionExecutor =
            RealActionExecutor(
                context = context.applicationContext,
                brightness = AndroidBrightnessController(context),
                extraDim = AndroidExtraDimController(context),
                dnd = AndroidDndController(context),
                volume = AndroidVolumeController(context),
                screenTimeout = AndroidScreenTimeoutController(context),
                darkMode = AndroidDarkModeController(context),
                ringer = AndroidRingerController(context),
                shellFactory = shellFactory,
                glyphProvider = glyphProvider,
                glyphMatrixProvider = glyphMatrixProvider,
            )
    }
}

/** Maps ControllerResult to ActionResult. */
fun ControllerResult.toActionResult(): ActionResult =
    when (this) {
        is ControllerResult.Success -> ActionResult.Success
        is ControllerResult.Failure -> ActionResult.Failure(reason)
        is ControllerResult.Unsupported -> ActionResult.Unsupported
        is ControllerResult.PermissionRequired -> ActionResult.PermissionRequired
    }
