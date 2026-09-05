package com.tdvorak.nothingmodes

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tdvorak.nothingmodes.capabilities.controllers.RealActionExecutor
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.LocationMode
import com.tdvorak.nothingmodes.engine.model.MediaCommand
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.ScreenOrientation
import com.tdvorak.nothingmodes.engine.model.SettingNamespace
import com.tdvorak.nothingmodes.engine.model.SettingsScreen
import com.tdvorak.nothingmodes.engine.model.VolumeStream
import com.tdvorak.nothingmodes.engine.runtime.FireContext
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test: exercise every action type through RealActionExecutor and log the result.
 * Does not assert success for actions that legitimately fail on an emulator without
 * Shizuku, permissions, or device admin.
 */
@RunWith(AndroidJUnit4::class)
class ActionSmokeTest {

    @Test
    fun allActionsExecuteWithoutCrashing() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val executor = RealActionExecutor.create(context)
        val fireContext = FireContext(
            eventId = "smoke-test",
            executionId = "smoke-1",
            automationId = AutomationId("smoke-automation"),
            actionIndex = 0,
            priority = 50,
        )

        val actions = listOf(
            Action.SetWifi(true),
            Action.SetBluetooth(true),
            Action.SetMobileData(false),
            Action.SetDnd(DndMode.OFF),
            Action.SetRinger("normal"),
            Action.LaunchApp("com.android.settings"),
            Action.OpenUrl("https://example.com"),
            Action.ShowNotification("Smoke test", "Notification action works"),
            Action.SetVolume(VolumeStream.MEDIA, 8),
            Action.SetFlashlight(false),
            Action.SetDarkMode(NightMode.OFF),
            Action.OpenSettingsScreen(SettingsScreen.SETTINGS, null),
            Action.Vibrate(50),
            Action.SetBrightness(128),
            Action.SetAutoBrightness(false),
            Action.SetExtraDim(false),
            Action.SetScreenTimeout(15_000),
            Action.SetGlyph(true),
            Action.SetGlyphMatrix(restore = true),
            Action.GlyphAnimate(),
            Action.GlyphProgress(50),
            Action.GlyphText("HI"),
            Action.GlyphScrollingText("Nothing"),
            Action.GlyphPreset("morning"),
            Action.GlyphTurnOff,
            Action.SetMobileData(false),
            Action.CopyText("copied"),
            Action.Wait(0L),
            Action.WriteSetting(SettingNamespace.SYSTEM, "screen_brightness", "120"),
            Action.SetAutoRotate(true),
            Action.SetBatterySaver(false),
            Action.SetAirplaneMode(false),
            Action.SetDataSaver(false),
            Action.SetHotspot(false),
            Action.SetNfc(false),
            Action.SetRefreshRate(60),
            Action.SetScreenRotation(ScreenOrientation.AUTO),
            Action.MediaControl(MediaCommand.PLAY_PAUSE),
            Action.SendSms("12345", "test"),
            Action.LockScreen,
            Action.SetLocationMode(LocationMode.OFF),
            Action.SetAutoSync(false),
            Action.ClearNotifications,
            Action.SetAlwaysOnDisplay(false),
            Action.TakeScreenshot,
        )

        for ((index, action) in actions.withIndex()) {
            val result = executor.execute(action, fireContext.copy(actionIndex = index))
            Log.d("ActionSmoke", "[${action::class.simpleName}] -> $result")
        }
    }
}
