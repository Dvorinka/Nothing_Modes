package com.tdvorak.nothingmodes.capabilities

import com.tdvorak.nothingmodes.engine.model.CapabilityIds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityResolverTest {
    private val allCapabilities =
        setOf(
            CapabilityIds.TRIGGER_TIME,
            CapabilityIds.TRIGGER_TIME_WINDOW,
            CapabilityIds.TRIGGER_IMMEDIATE,
            CapabilityIds.TRIGGER_NOTIFICATION,
            CapabilityIds.TRIGGER_PHONE_SMS,
            CapabilityIds.TRIGGER_PHONE_CALL,
            CapabilityIds.TRIGGER_CONNECTIVITY_WIFI,
            CapabilityIds.TRIGGER_CONNECTIVITY_WIFI_IDENTITY,
            CapabilityIds.TRIGGER_CONNECTIVITY_BT,
            CapabilityIds.TRIGGER_CONNECTIVITY_POWER,
            CapabilityIds.TRIGGER_BOOT,
            CapabilityIds.TRIGGER_BATTERY_LEVEL,
            CapabilityIds.TRIGGER_SCREEN_STATE,
            CapabilityIds.TRIGGER_APP_OPENED,
            CapabilityIds.TRIGGER_GEOFENCE,
            CapabilityIds.TRIGGER_MANUAL,
            CapabilityIds.TRIGGER_BT_DEVICE,
            CapabilityIds.TRIGGER_WIFI_CONNECTED,
            CapabilityIds.TRIGGER_CALENDAR_EVENT,
            CapabilityIds.STATE_READER_BUILTIN,
            CapabilityIds.STATE_READER_SETTING,
            CapabilityIds.STATE_READER_SYSTEM_PROPERTY,
            CapabilityIds.STATE_READER_SYSFS,
            CapabilityIds.STATE_READER_DUMPSYS_FIELD,
            CapabilityIds.STATE_FOREGROUND_APP,
            CapabilityIds.STATE_LOCATION,
            CapabilityIds.ACTION_SET_WIFI,
            CapabilityIds.ACTION_SET_BLUETOOTH,
            CapabilityIds.ACTION_SET_MOBILE_DATA,
            CapabilityIds.ACTION_SET_DND,
            CapabilityIds.ACTION_SET_RINGER,
            CapabilityIds.ACTION_LAUNCH_APP,
            CapabilityIds.ACTION_OPEN_URL,
            CapabilityIds.ACTION_SHOW_NOTIFICATION,
            CapabilityIds.ACTION_SET_VOLUME,
            CapabilityIds.ACTION_SET_FLASHLIGHT,
            CapabilityIds.ACTION_SET_DARK_MODE,
            CapabilityIds.ACTION_OPEN_SETTINGS_SCREEN,
            CapabilityIds.ACTION_VIBRATE,
            CapabilityIds.ACTION_SET_BRIGHTNESS,
            CapabilityIds.ACTION_SET_AUTO_BRIGHTNESS,
            CapabilityIds.ACTION_SET_EXTRA_DIM,
            CapabilityIds.ACTION_SET_SCREEN_TIMEOUT,
            CapabilityIds.ACTION_SET_GLYPH,
            CapabilityIds.ACTION_SET_GLYPH_MATRIX,
            CapabilityIds.ACTION_GLYPH_ANIMATE,
            CapabilityIds.ACTION_GLYPH_PROGRESS,
            CapabilityIds.ACTION_GLYPH_TEXT,
            CapabilityIds.ACTION_GLYPH_SCROLLING_TEXT,
            CapabilityIds.ACTION_GLYPH_PRESET,
            CapabilityIds.ACTION_GLYPH_TURNOFF,
            CapabilityIds.ACTION_COPY_TEXT,
            CapabilityIds.ACTION_WAIT,
            CapabilityIds.ACTION_WRITE_SETTING,
            CapabilityIds.ACTION_SET_AUTO_ROTATE,
            CapabilityIds.ACTION_SET_BATTERY_SAVER,
            CapabilityIds.ACTION_SET_AIRPLANE_MODE,
            CapabilityIds.ACTION_SET_DATA_SAVER,
            CapabilityIds.ACTION_SET_HOTSPOT,
            CapabilityIds.ACTION_SET_NFC,
            CapabilityIds.ACTION_SET_REFRESH_RATE,
            CapabilityIds.ACTION_SET_SCREEN_ROTATION,
            CapabilityIds.ACTION_MEDIA_CONTROL,
            CapabilityIds.ACTION_SEND_SMS,
            CapabilityIds.ACTION_LOCK_SCREEN,
            CapabilityIds.ACTION_SET_LOCATION_MODE,
            CapabilityIds.ACTION_SET_AUTO_SYNC,
            CapabilityIds.ACTION_CLEAR_NOTIFICATIONS,
            CapabilityIds.ACTION_SET_AOD,
            CapabilityIds.ACTION_TAKE_SCREENSHOT,
            CapabilityIds.SHIZUKU_REQUIRED,
        )

    private fun everything(): DeviceCapabilities =
        DeviceCapabilities(
            hasWifi = true,
            hasBluetooth = true,
            hasLocation = true,
            hasLocationPermission = true,
            hasNotificationPolicyAccess = true,
            hasWriteSettings = true,
            hasNotificationListenerAccess = true,
            hasUsageAccess = true,
            hasFlashlight = true,
            hasVibrator = true,
            hasPostNotifications = true,
            hasGlyphLightStripe = true,
            hasGlyphMatrix = true,
            shizukuStatus = ShizukuCapabilityStatus.AUTHORIZED,
            hasTelephony = true,
            hasActiveDeviceAdmin = true,
        )

    @Test
    fun allCapabilitiesSatisfied() {
        val resolver = CapabilityResolver(everything())
        val resolution = resolver.resolve("test", allCapabilities)

        assertTrue(resolution.canRun)
        assertTrue(resolution.missing.isEmpty())
        assertEquals(allCapabilities, resolution.satisfied)
    }

    @Test
    fun shizukuRequiredMissing() {
        val caps = everything().copy(shizukuStatus = ShizukuCapabilityStatus.NOT_INSTALLED)
        val resolver = CapabilityResolver(caps)
        val resolution = resolver.resolve("test", setOf(CapabilityIds.SHIZUKU_REQUIRED))

        assertFalse(resolution.canRun)
        assertEquals(setOf(CapabilityIds.SHIZUKU_REQUIRED), resolution.missing)
        assertEquals("Shizuku required but not authorized", resolution.missingReasons[CapabilityIds.SHIZUKU_REQUIRED])
    }

    @Test
    fun notificationListenerRequiredButMissing() {
        val caps = everything().copy(hasNotificationListenerAccess = false)
        val resolver = CapabilityResolver(caps)
        val resolution = resolver.resolve("test", setOf(CapabilityIds.TRIGGER_NOTIFICATION))

        assertFalse(resolution.canRun)
        assertEquals(setOf(CapabilityIds.TRIGGER_NOTIFICATION), resolution.missing)
    }

    @Test
    fun geofenceRequiresLocationAndPermission() {
        val resolver = CapabilityResolver(everything().copy(hasLocationPermission = false))
        val resolution = resolver.resolve("test", setOf(CapabilityIds.TRIGGER_GEOFENCE))

        assertFalse(resolution.canRun)
        assertTrue(resolution.missing.contains(CapabilityIds.TRIGGER_GEOFENCE))
    }

    @Test
    fun glyphPresetSatisfiedByEitherLightStripeOrMatrix() {
        val lightOnly = everything().copy(hasGlyphMatrix = false)
        assertTrue(CapabilityResolver(lightOnly).resolve("test", setOf(CapabilityIds.ACTION_GLYPH_PRESET)).canRun)

        val matrixOnly = everything().copy(hasGlyphLightStripe = false)
        assertTrue(CapabilityResolver(matrixOnly).resolve("test", setOf(CapabilityIds.ACTION_GLYPH_PRESET)).canRun)
    }

    @Test
    fun unknownCapabilityReturnsMissingWithReason() {
        val resolver = CapabilityResolver(everything())
        val resolution = resolver.resolve("test", setOf("unknown_cap"))

        assertFalse(resolution.canRun)
        assertEquals("Capability not available: unknown_cap", resolution.missingReasons["unknown_cap"])
    }

    @Test
    fun lockScreenMissingWhenDeviceAdminNotActive() {
        val caps = everything().copy(hasActiveDeviceAdmin = false)
        val resolution = CapabilityResolver(caps).resolve("test", setOf(CapabilityIds.ACTION_LOCK_SCREEN))

        assertFalse(resolution.canRun)
        assertEquals("Detected: may not work on this device", resolution.missingReasons[CapabilityIds.ACTION_LOCK_SCREEN])
    }

    @Test
    fun screenshotMissingWhenShizukuNotAuthorized() {
        val caps = everything().copy(shizukuStatus = ShizukuCapabilityStatus.NOT_INSTALLED)
        val resolution =
            CapabilityResolver(caps).resolve(
                "test",
                setOf(CapabilityIds.ACTION_TAKE_SCREENSHOT, CapabilityIds.SHIZUKU_REQUIRED),
            )

        assertFalse(resolution.canRun)
        assertTrue(CapabilityIds.SHIZUKU_REQUIRED in resolution.missing)
        assertEquals("Detected: may not work on this device", resolution.missingReasons[CapabilityIds.ACTION_TAKE_SCREENSHOT])
    }
}
