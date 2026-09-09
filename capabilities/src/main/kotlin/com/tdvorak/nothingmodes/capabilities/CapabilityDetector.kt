package com.tdvorak.nothingmodes.capabilities

import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager

import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.nothing.GlyphHardware
import com.tdvorak.nothingmodes.nothing.NothingDeviceDetector
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import com.tdvorak.nothingmodes.shizuku.ShizukuGatewayStatus

/** Detects device capabilities at runtime. */
class CapabilityDetector(
    private val context: Context,
    private val shizukuGateway: ShizukuGateway? = null,
) {
    fun detect(): DeviceCapabilities {
        val packageName = context.packageName
        synchronized(cache) {
            val now = SystemClock.elapsedRealtime()
            cache[packageName]?.let { (caps, at) ->
                if (now - at < CACHE_TTL_MS) return caps
            }
        }

        val pm = context.packageManager
        val isNothing = Build.MANUFACTURER.equals("nothing", ignoreCase = true)
        val model = Build.MODEL ?: ""
        val deviceName = resolveDeviceName(model)

        // Glyph detection via NothingDeviceDetector
        val detector = NothingDeviceDetector(context)
        val glyphHardware = if (isNothing) detector.detectGlyphHardware() else GlyphHardware.NONE
        val hasLightStripe = glyphHardware.isLightStripe
        val hasMatrix = glyphHardware.isMatrix
        val matrixSize = glyphHardware.matrixSize
        val hasGlyphTouch = if (isNothing) detector.hasGlyphTouch() else false

        // Nothing SDK availability: check if GlyphManager class is loadable
        val nothingSdkAvailable =
            runCatching {
                Class.forName("com.nothing.ketchum.GlyphManager")
                true
            }.getOrDefault(false)

        // Shizuku status
        val shizukuStatus =
            when (shizukuGateway?.status()) {
                ShizukuGatewayStatus.AUTHORIZED -> ShizukuCapabilityStatus.AUTHORIZED
                ShizukuGatewayStatus.RUNNING_NOT_AUTHORIZED -> ShizukuCapabilityStatus.RUNNING_NOT_AUTHORIZED
                ShizukuGatewayStatus.INSTALLED_NOT_RUNNING -> ShizukuCapabilityStatus.INSTALLED_NOT_RUNNING
                ShizukuGatewayStatus.NOT_INSTALLED -> ShizukuCapabilityStatus.NOT_INSTALLED
                ShizukuGatewayStatus.UNSUPPORTED -> ShizukuCapabilityStatus.UNSUPPORTED
                null -> ShizukuCapabilityStatus.NOT_CHECKED
            }

        val result =
            DeviceCapabilities(
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
                // Glyph detection from NothingDeviceDetector
                hasGlyphLightStripe = hasLightStripe,
                hasGlyphMatrix = hasMatrix,
                glyphMatrixSize = matrixSize,
                hasGlyphTouch = hasGlyphTouch,
                nothingSdkAvailable = nothingSdkAvailable,
                nothingSdkConnected = false, // Updated at runtime after GlyphManager.init
                // Shizuku
                shizukuStatus = shizukuStatus,
                // Permissions
                hasNotificationPolicyAccess = checkNotificationPolicyAccess(),
                hasWriteSettings =
                    android.provider.Settings.System
                        .canWrite(context),
                hasNotificationListenerAccess = checkNotificationListenerAccess(),
                hasUsageAccess = checkUsageAccess(),
                hasLocationPermission = checkLocationPermission(),
                hasActiveDeviceAdmin = checkActiveDeviceAdmin(),
                hasReadCalendar = checkPermission(android.Manifest.permission.READ_CALENDAR),
                hasReceiveSms = checkPermission(android.Manifest.permission.RECEIVE_SMS),
                hasSendSms = checkPermission(android.Manifest.permission.SEND_SMS),
                hasReadPhoneState = checkPermission(android.Manifest.permission.READ_PHONE_STATE),
                hasReadCallLog = checkPermission(android.Manifest.permission.READ_CALL_LOG),
                hasCamera = checkPermission(android.Manifest.permission.CAMERA),
                hasRecordAudio = checkPermission(android.Manifest.permission.RECORD_AUDIO),
                hasPostNotifications = checkPostNotifications(),
                hasExactAlarm = checkExactAlarm(),
                hasBluetoothConnect = checkBluetoothConnect(),
                hasBluetoothScan = checkBluetoothScan(),
            )

        synchronized(cache) {
            cache[packageName] = result to SystemClock.elapsedRealtime()
        }

        return result
    }

    private fun checkNotificationPolicyAccess(): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)
        return nm?.isNotificationPolicyAccessGranted == true
    }

    private fun checkNotificationListenerAccess(): Boolean {
        val enabledListeners =
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ) ?: return false
        return enabledListeners.contains(context.packageName)
    }

    private fun checkUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName,
                )
            }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun checkLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun checkActiveDeviceAdmin(): Boolean {
        return runCatching {
            val dpm = context.getSystemService(DevicePolicyManager::class.java) ?: return@runCatching false
            val comp =
                android.content.ComponentName(
                    context.packageName,
                    "com.tdvorak.nothingmodes.automation.lifecycle.NothingDeviceAdminReceiver",
                )
            dpm.isAdminActive(comp)
        }.getOrDefault(false)
    }

    private fun checkPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun checkPostNotifications(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    private fun checkExactAlarm(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as? android.app.AlarmManager
            am?.canScheduleExactAlarms() == true
        } else {
            true
        }

    private fun checkBluetoothConnect(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun checkBluetoothScan(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun resolveDeviceName(model: String): String =
        when {
            model.startsWith("A063") -> "Phone (1)"
            model.startsWith("A065") -> "Phone (2)"
            model.startsWith("A142") -> "Phone (2a)"
            model.startsWith("A142P") -> "Phone (2a) Plus"
            model.startsWith("A059") -> "Phone (3a)"
            model.startsWith("A059P") -> "Phone (3a) Pro"
            model.startsWith("A024") -> "Phone (3)"
            model.startsWith("A063P") -> "Phone (4a) Pro"
            model.startsWith("A172") -> "Phone (4a)"
            model.startsWith("A172P") -> "Phone (4b)"
            else -> model
        }

    companion object {
        private const val CACHE_TTL_MS = 2_000L
        private val cache = mutableMapOf<String, Pair<DeviceCapabilities, Long>>()

        /** Invalidates cached capabilities for all packages. */
        fun clearCache() = synchronized(cache) { cache.clear() }
    }
}
