package com.tdvorak.nothingmodes

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        val executor =
            EntryPointAccessors
                .fromApplication(context.applicationContext, DebugActionEntryPoint::class.java)
                .executor()
        val action = parse(intent) ?: run {
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

    private fun parse(intent: Intent): Action? {
        val on = intent.getBooleanExtra("on", true)
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
            "glyph_text" -> Action.GlyphText(text.ifBlank { "NM" })
            "glyph_scrolling_text" -> Action.GlyphScrollingText(text.ifBlank { "NOTHING MODES" })
            "glyph_preset" -> Action.GlyphPreset(preset.ifBlank { "charging_start" })
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
    }
}
