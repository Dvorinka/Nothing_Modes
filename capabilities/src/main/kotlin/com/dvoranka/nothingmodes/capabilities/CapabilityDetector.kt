package com.dvoranka.nothingmodes.capabilities

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/** Detects device capabilities at runtime. */
class CapabilityDetector(private val context: Context) {

    fun detect(): DeviceCapabilities {
        val pm = context.packageManager
        val isNothing = Build.MANUFACTURER.equals("nothing", ignoreCase = true)
        val model = Build.MODEL ?: ""
        val deviceName = resolveDeviceName(model)

        return DeviceCapabilities(
            isNothingDevice = isNothing,
            deviceModel = model,
            deviceName = deviceName,
            androidVersion = Build.VERSION.SDK_INT,
            hasFlashlight = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH),
            hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            hasWifi = pm.hasSystemFeature(PackageManager.FEATURE_WIFI),
            hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            hasLocation = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS),
            hasVibrator = context.getSystemService(Context.VIBRATOR_SERVICE) != null,
            // Glyph detection deferred to NothingIntegrations module
            hasGlyphLightStripe = false,
            hasGlyphMatrix = false,
            glyphMatrixSize = 0,
            hasGlyphTouch = false,
            nothingSdkAvailable = false,
            nothingSdkConnected = false,
            // Permissions checked separately
            hasNotificationPolicyAccess = false,
            hasWriteSettings = false,
            hasNotificationListenerAccess = false,
        )
    }

    private fun resolveDeviceName(model: String): String = when {
        model.startsWith("A063") -> "Phone (1)"
        model.startsWith("A065") -> "Phone (2)"
        model.startsWith("A142") -> "Phone (2a)"
        model.startsWith("A142P") -> "Phone (2a) Plus"
        model.startsWith("A059") -> "Phone (3a)"
        model.startsWith("A059P") -> "Phone (3a) Pro"
        model.startsWith("A001") -> "Phone (3)"
        model.startsWith("A063P") -> "Phone (4a) Pro"
        model.startsWith("A172") -> "Phone (4a)"
        model.startsWith("A172P") -> "Phone (4b)"
        else -> model
    }
}
