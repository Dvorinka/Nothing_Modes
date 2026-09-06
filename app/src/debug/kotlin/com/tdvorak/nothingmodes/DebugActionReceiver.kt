package com.tdvorak.nothingmodes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Debug-only action test hook.
 *
 * Drive any [Action] through the real executor from adb:
 * `am broadcast -a com.tdvorak.nothingmodes.debug.TEST_ACTION -e type set_aod -e on true`
 *
 * Generic extras: `on` bool, `level` int, `mode`/`text`/`pkg`/`url`/`preset`/`stream`/`screen`
 * strings, `duration` long, `ns`/`key`/`value` for write_setting.
 * Result is logged under tag `NmDebug`.
 */
class DebugActionReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface DebugActionEntryPoint {
        fun executor(): ActionExecutor

        fun matrixProvider(): com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION) return
        if (intent.getStringExtra("type") == "profile") {
            // Recognition-layer probe: dump the full device snapshot.
            val p = com.tdvorak.nothingmodes.nothing.DeviceProfiler(context).probe()
            Log.i(TAG, "profile: ${p.summary()}")
            Log.i(TAG, "registeredToys=${p.registeredToys}")
            Log.i(TAG, "activeToys=${p.activeToys}")
            Log.i(TAG, "nothingFeatures=${p.systemFeatures}")
            return
        }
        if (intent.getStringExtra("type") == "apps") {
            // Same query the in-app pickers use — verifies package visibility.
            val pm = context.packageManager
            val main =
                Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val apps =
                pm.queryIntentActivities(main, 0)
                    .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
                    .sortedBy { it.first.lowercase() }
            Log.i(TAG, "launcher apps visible: ${apps.size}")
            apps.forEach { Log.i(TAG, "  ${it.first} -> ${it.second}") }
            return
        }
        if (intent.getStringExtra("type") == "shizuku_probe") {
            // Raw Shizuku state via reflection (rikka api is module-internal).
            runCatching {
                val s = Class.forName("rikka.shizuku.Shizuku")
                fun call(name: String) =
                    runCatching { s.getMethod(name).invoke(null) }.getOrElse { "ERR:${it.message}" }
                Log.i(TAG, "shizuku ping=${call("pingBinder")} perm=${call("checkSelfPermission")} " +
                    "preV11=${call("isPreV11")} ver=${call("getVersion")} " +
                    "uid=${call("getUid")} se=${call("getSEContext")} " +
                    "rationale=${call("shouldShowRequestPermissionRationale")}")
                Log.i(TAG, "gateway status=${com.tdvorak.nothingmodes.shizuku.ShizukuGateway(context).status()}")
            }.onFailure { Log.e(TAG, "probe failed", it) }
            return
        }
        if (intent.getStringExtra("type") == "font_probe") {
            // Can we reach Nothing's letter_ string resources via a foreign
            // package context? Probe likely owners of the NDot glyph table.
            for (pkg in listOf("com.nothing.thirdparty", "com.nothing.ntf.glyphtoys",
                "com.android.systemui", context.packageName)) {
                val info =
                    runCatching {
                        val fc = context.createPackageContext(pkg, 0)
                        val letters =
                            com.nothing.ketchum.GlyphMatrixUtils.getLetterConfigs("89", fc, null)
                        "$pkg -> ${letters?.size ?: 0} letters, dots=" +
                            (letters?.sumOf { it.dots.size } ?: 0)
                    }.getOrElse { "$pkg -> ${it.message}" }
                Log.i(TAG, "font_probe: $info")
            }
            return
        }
        if (intent.getStringExtra("type") == "ui_style") {
            // Flip the visual language without navigating settings:
            // -e style auto|nothing|classic
            val style =
                intent.getStringExtra("style").orEmpty().uppercase().let {
                    runCatching { com.tdvorak.nothingmodes.ui.theme.ThemeManager.UiStyle.valueOf(it) }
                        .getOrNull()
                } ?: com.tdvorak.nothingmodes.ui.theme.ThemeManager.UiStyle.AUTO
            com.tdvorak.nothingmodes.ui.theme.ThemeManager.init(context).setUiStyle(style)
            Log.i(TAG, "ui_style -> $style (resolved=${com.tdvorak.nothingmodes.ui.theme.ThemeManager.instance.resolvedUiStyle()})")
            return
        }
        if (intent.getStringExtra("type") == "toy_preview") {
            toyPreview(context, intent)
            return
        }
        if (intent.getStringExtra("type") == "toy_play" || intent.getStringExtra("type") == "toy_stop") {
            // Foreign-toy probe: bind another app's toy service via
            // com.nothing.glyph.TOY and drive its GlyphToy lifecycle.
            toyControl(context, intent, intent.getStringExtra("type") == "toy_play")
            return
        }
        if (intent.getStringExtra("type") == "glyph_battery") {
            glyphBattery(context, intent)
            return
        }
        val executor =
            EntryPointAccessors
                .fromApplication(context.applicationContext, DebugActionEntryPoint::class.java)
                .executor()
        val action = parse(context, intent) ?: run {
            Log.w(TAG, "unknown or malformed type: ${intent.getStringExtra("type")}")
            return
        }
        // Never runBlocking on main here: Shizuku's binder callbacks post to the
        // main looper — blocking main deadlocks bindUserService until timeout.
        val pending = goAsync()
        val type = intent.getStringExtra("type")
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            val result =
                runCatching {
                    executor.execute(
                        action,
                        FireContext(
                            eventId = "debug-broadcast",
                            executionId = UUID.randomUUID().toString(),
                            automationId = AutomationId("debug-cli"),
                            actionIndex = 0,
                            priority = 0,
                        ),
                    )
                }
            Log.i(TAG, "type=$type -> ${result.getOrNull() ?: "THREW ${result.exceptionOrNull()}"}")
            pending.finish()
        }
    }

    private fun parse(context: Context, intent: Intent): Action? {
        // `am -e on false` delivers a String — accept both bool and string extras.
        val on =
            intent.getStringExtra("on")?.toBooleanStrictOrNull()
                ?: intent.getBooleanExtra("on", true)
        val level = intent.getIntExtra("level", 128)
        val duration = intent.getIntExtra("duration", 300)
        val mode = intent.getStringExtra("mode").orEmpty()
        val text = intent.getStringExtra("text").orEmpty()
        val pkg = intent.getStringExtra("pkg").orEmpty()
        val url = intent.getStringExtra("url").orEmpty()
        val preset = intent.getStringExtra("preset").orEmpty()
        return when (intent.getStringExtra("type")?.lowercase()) {
            "set_wifi" -> Action.SetWifi(on)
            "set_bluetooth" -> Action.SetBluetooth(on)
            "set_mobile_data" -> Action.SetMobileData(on)
            "set_dnd" -> Action.SetDnd(DndMode.valueOf(mode.ifBlank { "TOTAL" }.uppercase()))
            "set_ringer" -> Action.SetRinger(mode.ifBlank { "NORMAL" })
            "launch_app" -> Action.LaunchApp(pkg)
            "open_url" -> Action.OpenUrl(url.ifBlank { "https://nothing-modes.vercel.app" })
            "show_notification" -> Action.ShowNotification("NmDebug", text.ifBlank { "test" })
            "set_volume" ->
                Action.SetVolume(
                    VolumeStream.valueOf(intent.getStringExtra("stream").orEmpty().ifBlank { "MEDIA" }.uppercase()),
                    level,
                )
            "set_flashlight" -> Action.SetFlashlight(on)
            "set_dark_mode" -> Action.SetDarkMode(NightMode.valueOf(mode.ifBlank { "ON" }.uppercase()))
            "open_settings" ->
                Action.OpenSettingsScreen(
                    SettingsScreen.valueOf(intent.getStringExtra("screen").orEmpty().ifBlank { "DISPLAY" }.uppercase()),
                )
            "vibrate" -> Action.Vibrate(duration)
            "set_brightness" -> Action.SetBrightness(level)
            "set_auto_brightness" -> Action.SetAutoBrightness(on)
            "set_extra_dim" -> Action.SetExtraDim(on)
            "set_screen_timeout" -> Action.SetScreenTimeout(duration)
            "set_glyph" -> Action.SetGlyph(on)
            "set_glyph_matrix" -> Action.SetGlyphMatrix()
            "glyph_animate" -> Action.GlyphAnimate(zone = null)
            "glyph_progress" -> Action.GlyphProgress(level.coerceIn(0, 100))
            "glyph_text" ->
                Action.GlyphText(
                    text.ifBlank { "NM" },
                    x = intent.getIntExtra("x", -1),
                    y = intent.getIntExtra("y", -1),
                )
            "glyph_scrolling_text" ->
                Action.GlyphScrollingText(
                    text.ifBlank { "NOTHING MODES" },
                    intervalMs = intent.getIntExtra("interval", 100),
                    stepPx = intent.getIntExtra("step", 1),
                )
            "glyph_preset" -> Action.GlyphPreset(preset.ifBlank { "charging_start" })
            "glyph_icon" -> Action.GlyphIcon(intent.getStringExtra("icon").orEmpty().ifBlank { "check" })
            "glyph_number" -> Action.GlyphNumber(intent.getIntExtra("number", 0).coerceIn(0, 99))
            "glyph_countdown" -> Action.GlyphCountdown(duration.coerceIn(1, 599))
            "glyph_music" -> Action.GlyphMusic
            "glyph_turnoff" -> Action.GlyphTurnOff
            "copy_text" -> Action.CopyText(text.ifBlank { "copied" })
            "wait" -> Action.Wait(duration.toLong())
            "write_setting" ->
                Action.WriteSetting(
                    SettingNamespace.valueOf(intent.getStringExtra("ns").orEmpty().ifBlank { "SYSTEM" }.uppercase()),
                    intent.getStringExtra("key").orEmpty(),
                    intent.getStringExtra("value").orEmpty(),
                )
            "set_auto_rotate" -> Action.SetAutoRotate(on)
            "set_battery_saver" -> Action.SetBatterySaver(on)
            "set_airplane_mode" -> Action.SetAirplaneMode(on)
            "set_data_saver" -> Action.SetDataSaver(on)
            "set_hotspot" -> Action.SetHotspot(on)
            "set_nfc" -> Action.SetNfc(on)
            "set_refresh_rate" -> Action.SetRefreshRate(level)
            "set_screen_rotation" ->
                Action.SetScreenRotation(ScreenOrientation.valueOf(mode.ifBlank { "AUTO" }.uppercase()))
            "media_control" -> Action.MediaControl(MediaCommand.valueOf(mode.ifBlank { "PLAY_PAUSE" }.uppercase()))
            "send_sms" -> Action.SendSms(intent.getStringExtra("number").orEmpty(), text)
            "lock_screen" -> Action.LockScreen
            "set_location_mode" -> Action.SetLocationMode(com.tdvorak.nothingmodes.engine.model.LocationMode.valueOf(mode.ifBlank { "OFF" }.uppercase()))
            "set_auto_sync" -> Action.SetAutoSync(on)
            "clear_notifications" -> Action.ClearNotifications
            "set_aod" -> Action.SetAlwaysOnDisplay(on)
            "take_screenshot" -> Action.TakeScreenshot
            else -> null
        }
    }

    companion object {
        private const val TAG = "NmDebug"
        const val ACTION = "com.tdvorak.nothingmodes.debug.TEST_ACTION"

        private var toyConnection: android.content.ServiceConnection? = null
        private var toyMessenger: android.os.Messenger? = null

        /** Bind a foreign Glyph Toy service and send prepare/start (or end+unbind). */
        private fun toyControl(context: Context, intent: Intent, play: Boolean) {
            val appContext = context.applicationContext
            if (!play) {
                val conn = toyConnection ?: run { Log.i(TAG, "toy_stop: nothing bound"); return }
                toyMessenger?.let { sendToyMsg(it, "end") }
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    runCatching { appContext.unbindService(conn) }
                    toyConnection = null
                    toyMessenger = null
                    Log.i(TAG, "toy_stop -> sent end, unbound")
                }, 300)
                return
            }
            val pkg = intent.getStringExtra("pkg").orEmpty()
            val svc = intent.getStringExtra("svc").orEmpty()
            if (pkg.isBlank() || svc.isBlank()) {
                Log.w(TAG, "toy_play needs -e pkg <package> -e svc <serviceClass>")
                return
            }
            val cls = if (svc.startsWith(".")) pkg + svc else svc
            val bindIntent =
                Intent("com.nothing.glyph.TOY").apply {
                    component = android.content.ComponentName(pkg, cls)
                }
            val conn =
                object : android.content.ServiceConnection {
                    override fun onServiceConnected(name: android.content.ComponentName?, binder: android.os.IBinder?) {
                        val messenger = android.os.Messenger(binder)
                        toyMessenger = messenger
                        sendToyMsg(messenger, "prepare")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            sendToyMsg(messenger, "start")
                            Log.i(TAG, "toy_play -> $pkg/$cls sent prepare+start")
                        }, 600)
                    }

                    override fun onServiceDisconnected(name: android.content.ComponentName?) {
                        Log.w(TAG, "toy service disconnected: $name")
                    }
                }
            val bound =
                runCatching { appContext.bindService(bindIntent, conn, Context.BIND_AUTO_CREATE) }
                    .getOrElse { Log.e(TAG, "bind threw: ${it.message}"); false }
            Log.i(TAG, "toy_play bind($pkg/$cls) -> $bound")
            if (bound) toyConnection = conn
        }

        /**
         * Pull a foreign toy's registered preview_res_id from the toy provider,
         * decode it via that app's own resources, and paint it on our matrix.
         */
        private fun toyPreview(context: Context, intent: Intent) {
            val pkg = intent.getStringExtra("pkg").orEmpty()
            if (pkg.isBlank()) {
                Log.w(TAG, "toy_preview needs -e pkg <package>")
                return
            }
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
                .launch {
                    try {
                        // Find the toy row(s) for the package.
                        val cursor =
                            context.contentResolver.query(
                                android.net.Uri.parse("content://com.nothing.glyphtoyprovider/glyph_toy"),
                                arrayOf("package_name", "service_name", "preview_res_id"),
                                null,
                                null,
                                null,
                            )
                        var previewId = 0
                        cursor?.use {
                            while (it.moveToNext()) {
                                if (it.getString(0) == pkg) {
                                    previewId = it.getInt(2)
                                    Log.i(TAG, "toy_preview: ${it.getString(1)} preview=$previewId")
                                    break
                                }
                            }
                        }
                        if (previewId == 0) {
                            Log.w(TAG, "toy_preview: no preview_res_id for $pkg")
                            return@launch
                        }
                        val res = context.packageManager.getResourcesForApplication(pkg)
                        val drawable =
                            runCatching { androidx.core.content.res.ResourcesCompat.getDrawable(res, previewId, null) }
                                .getOrNull()
                        if (drawable == null) {
                            Log.w(TAG, "toy_preview: drawable $previewId not decodable in $pkg")
                            return@launch
                        }
                        val bmp = com.nothing.ketchum.GlyphMatrixUtils.drawableToBitmap(drawable)
                        val provider =
                            EntryPointAccessors
                                .fromApplication(context.applicationContext, DebugActionEntryPoint::class.java)
                                .matrixProvider()
                        // Cold broadcasts arrive before any screen has run init().
                        if (!provider.isConnected()) provider.init()
                        var waited = 0
                        while (!provider.isConnected() && waited < 4000) {
                            kotlinx.coroutines.delay(200)
                            waited += 200
                        }
                        if (!provider.isConnected()) {
                            Log.w(TAG, "toy_preview: matrix service not connected")
                            return@launch
                        }
                        val size = provider.matrixSize()
                        val r = provider.displayImage(bmp, x = 0, y = 0, scale = 100, brightness = 255)
                        Log.i(TAG, "toy_preview -> displayImage($size) = $r")
                    } catch (e: Exception) {
                        Log.e(TAG, "toy_preview failed", e)
                    }
                }
        }

        /** Direct matrix battery probe: reads sticky BATTERY_CHANGED and animates charging. */
        private fun glyphBattery(
            context: Context,
            intent: Intent,
        ) {
            val explicit = intent.getIntExtra("percent", -1)
            val pct: Int
            val charging: Boolean
            if (explicit in 0..100) {
                pct = explicit
                charging = intent.getBooleanExtra("charging", false)
            } else {
                val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                pct = if (level > 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else 0
                charging =
                    status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            }

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val provider =
                        EntryPointAccessors
                            .fromApplication(context.applicationContext, DebugActionEntryPoint::class.java)
                            .matrixProvider()
                    if (!provider.isConnected()) {
                        if (!provider.ensureConnected(4000)) {
                            Log.w(TAG, "glyph_battery: matrix not connected")
                            return@launch
                        }
                    }
                    val r = provider.displayBattery(pct, charging)
                    Log.i(TAG, "glyph_battery -> pct=$pct charging=$charging result=$r")
                } catch (e: Exception) {
                    Log.e(TAG, "glyph_battery failed", e)
                }
            }
        }

        private fun sendToyMsg(messenger: android.os.Messenger, event: String) {
            runCatching {
                val msg =
                    android.os.Message.obtain(null, 1).apply {
                        data = android.os.Bundle().apply { putString("data", event) }
                    }
                messenger.send(msg)
            }.onFailure { Log.e(TAG, "toy msg '$event' failed: ${it.message}") }
        }
    }
}
