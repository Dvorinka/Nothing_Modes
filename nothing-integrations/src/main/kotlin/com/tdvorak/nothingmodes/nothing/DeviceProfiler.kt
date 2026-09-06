package com.tdvorak.nothingmodes.nothing

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Recognition layer: a single probe that snapshots everything the app can
 * detect about this device — identity, Nothing OS build, glyph hardware,
 * the toy ecosystem, and privileged helpers present.
 *
 * Prepared for upcoming integrations (toy-aware routines, hardware-gated
 * features, per-build capability tables). Pure read-only; safe to call
 * anywhere.
 */
class DeviceProfiler(
    private val context: Context,
) {
    data class DeviceProfile(
        val manufacturer: String,
        val model: String,
        val detectedModel: String?,
        val nothingOsBuild: String?,
        val androidRelease: String,
        val sdk: Int,
        val isNothingDevice: Boolean,
        val glyphHardware: GlyphHardware,
        val hasGlyphTouch: Boolean,
        val glyphSystemInstalled: Boolean,
        val registeredToys: List<String>,
        val activeToys: List<String>,
        val aodToy: String?,
        val shizukuInstalled: Boolean,
        val systemFeatures: Set<String>,
    ) {
        fun summary(): String =
            buildString {
                append("model=$model detected=$detectedModel os=$nothingOsBuild ")
                append("android=$androidRelease(sdk$sdk) nothing=$isNothingDevice ")
                append("glyph=$glyphHardware touch=$hasGlyphTouch ")
                append("toys=${registeredToys.size}/${activeToys.size} aodToy=$aodToy ")
                append("shizuku=$shizukuInstalled features=${systemFeatures.size}")
            }
    }

    fun probe(): DeviceProfile {
        val detector = NothingDeviceDetector(context)
        val toys = GlyphToysBridge(context)
        val registered = runCatching { toys.listRegisteredToys() }.getOrDefault(emptyList())
        val system = runCatching { toys.listSystemToys() }.getOrDefault(emptyList())
        return DeviceProfile(
            manufacturer = Build.MANUFACTURER.orEmpty(),
            model = Build.MODEL.orEmpty(),
            detectedModel = detector.detectModel(),
            nothingOsBuild = readBuildProp("ro.build.display.id"),
            androidRelease = Build.VERSION.RELEASE.orEmpty(),
            sdk = Build.VERSION.SDK_INT,
            isNothingDevice = detector.isNothingDevice(),
            glyphHardware = detector.detectGlyphHardware(),
            hasGlyphTouch = detector.hasGlyphTouch(),
            glyphSystemInstalled = runCatching { toys.isGlyphSystemInstalled() }.getOrDefault(false),
            registeredToys = registered.map { it.packageName },
            activeToys = system.filter { it.isActive }.map { it.serviceName },
            aodToy = runCatching { toys.activeAodToy() }.getOrNull(),
            shizukuInstalled = isPackageInstalled(SHIZUKU_PACKAGE),
            systemFeatures =
                context.packageManager.systemAvailableFeatures
                    .mapNotNull { it.name }
                    .filter { it.startsWith("com.nothing") || it.startsWith("nothing.") }
                    .toSet(),
        )
    }

    private fun isPackageInstalled(pkg: String): Boolean =
        try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    private fun readBuildProp(key: String): String? =
        runCatching {
            @Suppress("PrivateApi")
            val get =
                Class
                    .forName("android.os.SystemProperties")
                    .getMethod("get", String::class.java)
            (get.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
        }.getOrNull()

    companion object {
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
