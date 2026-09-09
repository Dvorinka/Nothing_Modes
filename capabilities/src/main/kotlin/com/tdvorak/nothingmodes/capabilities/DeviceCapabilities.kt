package com.tdvorak.nothingmodes.capabilities

/** Snapshot of all device capabilities detected at runtime. */
data class DeviceCapabilities(
    val isNothingDevice: Boolean = false,
    val deviceModel: String = "",
    val deviceName: String = "",
    val androidVersion: Int = 0,
    val nothingOsVersion: String = "",
    // Glyph
    val hasGlyphLightStripe: Boolean = false,
    val hasGlyphMatrix: Boolean = false,
    val glyphMatrixSize: Int = 0,
    val hasGlyphTouch: Boolean = false,
    // Android features
    val hasFlashlight: Boolean = false,
    val hasTelephony: Boolean = false,
    val hasWifi: Boolean = false,
    val hasBluetooth: Boolean = false,
    val hasLocation: Boolean = false,
    val hasVibrator: Boolean = false,
    // Permissions
    val hasNotificationPolicyAccess: Boolean = false,
    val hasWriteSettings: Boolean = false,
    val hasNotificationListenerAccess: Boolean = false,
    val hasUsageAccess: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasActiveDeviceAdmin: Boolean = false,
    val hasReadCalendar: Boolean = false,
    val hasReceiveSms: Boolean = false,
    val hasSendSms: Boolean = false,
    val hasReadPhoneState: Boolean = false,
    val hasReadCallLog: Boolean = false,
    val hasCamera: Boolean = false,
    val hasRecordAudio: Boolean = false,
    val hasPostNotifications: Boolean = false,
    val hasExactAlarm: Boolean = false,
    val hasBluetoothConnect: Boolean = false,
    val hasBluetoothScan: Boolean = false,
    // Shizuku
    val shizukuStatus: ShizukuCapabilityStatus = ShizukuCapabilityStatus.NOT_CHECKED,
    // Nothing SDK
    val nothingSdkAvailable: Boolean = false,
    val nothingSdkConnected: Boolean = false,
)

enum class ShizukuCapabilityStatus {
    NOT_CHECKED,
    NOT_INSTALLED,
    INSTALLED_NOT_RUNNING,
    RUNNING_NOT_AUTHORIZED,
    AUTHORIZED,
    UNSUPPORTED,
}

/** Result of capability resolution for a specific automation. */
data class CapabilityResolution(
    val automationId: String,
    val satisfied: Set<String>,
    val missing: Set<String>,
    val canRun: Boolean,
    val missingReasons: Map<String, String> = emptyMap(),
) {
    val isFullyCapable: Boolean get() = missing.isEmpty()
}
