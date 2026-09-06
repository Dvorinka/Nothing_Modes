package com.tdvorak.nothingmodes.nothing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * Bridge to Nothing's system Glyph Toys app (`com.nothing.thirdparty`).
 *
 * What we can do as a third-party app:
 * - Enumerate toys registered on the system (intent query + the glyphtoy provider).
 * - Open the system Glyph Toys manager, the always-on toy picker and the toy
 *   timeout settings so the user configures them in Nothing's own UI.
 * - Read which toy is currently selected for always-on display.
 *
 * What we cannot do: `setGlyphMatrixTimeout` over the binder is gated to a
 * first-party allowlist, so timeout/flip settings must go through the system UI.
 */
class GlyphToysBridge(
    private val context: Context,
) {
    data class ToyInfo(
        val packageName: String,
        val serviceName: String,
        val label: String,
        val isOurs: Boolean,
    )

    /** Whether the Nothing glyph system app exists on this device. */
    fun isGlyphSystemInstalled(): Boolean =
        try {
            context.packageManager.getPackageInfo(SYSTEM_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    /** All services that registered the `com.nothing.glyph.TOY` intent filter. */
    fun listRegisteredToys(): List<ToyInfo> =
        try {
            val intent = Intent(TOY_ACTION)
            context.packageManager
                .queryIntentServices(intent, PackageManager.GET_META_DATA or PackageManager.MATCH_ALL)
                .map { resolve ->
                    val info = resolve.serviceInfo
                    val label =
                        info.metaData
                            ?.let { md ->
                                // Prefer the declared toy.name resource; fall back to the app label.
                                val nameRes = md.getInt(META_TOY_NAME, 0)
                                if (nameRes != 0) {
                                    runCatching {
                                        context.packageManager
                                            .getResourcesForApplication(info.packageName)
                                            .getString(nameRes)
                                    }.getOrNull()
                                } else {
                                    md.getString(META_TOY_NAME)
                                }
                            }
                            ?: resolve.loadLabel(context.packageManager).toString()
                    ToyInfo(
                        packageName = info.packageName,
                        serviceName = info.name,
                        label = label,
                        isOurs = info.packageName == context.packageName,
                    )
                }
        } catch (e: Exception) {
            Log.w(TAG, "toy query failed", e)
            emptyList()
        }

    /**
     * A toy row from the system provider. Column names verified against
     * Nothing OS 4.1 (`B4.1` build) — still read defensively by index lookup.
     */
    data class SystemToy(
        val packageName: String,
        val serviceName: String,
        val isActive: Boolean,
        val isAod: Boolean,
        val isAodActive: Boolean,
        val hasLongpress: Boolean,
        val order: Int,
    ) {
        /** Short display name: `GlyphMatrixBatteryService` -> `Battery`. */
        val shortName: String
            get() =
                serviceName
                    .substringAfterLast('.')
                    .removePrefix("GlyphMatrix")
                    .removeSuffix("Service")
                    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                    .ifBlank { serviceName }
    }

    /**
     * Toys known to the system provider (`content://…/glyph_toy`), parsed
     * into structured rows. Returns empty when the provider is absent.
     */
    fun listSystemToys(): List<SystemToy> =
        try {
            val out = mutableListOf<SystemToy>()
            context.contentResolver
                .query(Uri.parse(TOY_PROVIDER_URI), null, null, null, null)
                ?.use { c ->
                    fun str(name: String) =
                        c.getColumnIndex(name)
                            .takeIf { it >= 0 }
                            ?.let { runCatching { c.getString(it) }.getOrNull() }

                    fun flag(name: String) = str(name)?.toIntOrNull() == 1

                    while (c.moveToNext()) {
                        out +=
                            SystemToy(
                                packageName = str("package_name").orEmpty(),
                                serviceName = str("service_name").orEmpty(),
                                isActive = flag("is_active"),
                                isAod = flag("is_aod"),
                                isAodActive = flag("is_aod_active"),
                                hasLongpress = flag("has_longpress"),
                                order = str("toy_order")?.toIntOrNull() ?: 0,
                            )
                    }
                }
            out.sortedBy { it.order }
        } catch (e: Exception) {
            Log.d(TAG, "provider query failed", e)
            emptyList()
        }

    /** The toy currently selected for always-on display, if any. */
    fun activeAodToy(): String? = queryProvider(AOD_TOY_URI).firstOrNull()

    /**
     * True when this app owns the visible Glyph layer — selected as the
     * active toy or configured as the Always-on toy. Nothing arbitrates the
     * matrix: only the owner's frames reach the lights.
     */
    fun ownsMatrix(): Boolean =
        runCatching {
            listSystemToys().any {
                it.packageName == context.packageName && (it.isActive || it.isAodActive)
            }
        }.getOrDefault(false)

    // ── Launchers into the system app ─────────────────────────────────────────

    /** Open the system Glyph Toys manager (toy carousel, enable/disable). */
    fun openToysManager(): Boolean = openSystemActivity(MANAGER_COMPONENTS)

    /** Open the always-on Glyph toy picker. */
    fun openAodToyPicker(): Boolean = openSystemActivity(AOD_PICKER_COMPONENTS)

    /** Open the toy idle-timeout settings. */
    fun openTimeoutSettings(): Boolean = openSystemActivity(TIMEOUT_COMPONENTS)

    private fun openSystemActivity(candidates: List<String>): Boolean {
        for (cls in candidates) {
            val intent =
                Intent().apply {
                    component = ComponentName(SYSTEM_PACKAGE, cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            if (runCatching { context.startActivity(intent) }.isSuccess) return true
        }
        // Fall back to the documented deep link, then a plain package launch.
        val deepLink =
            Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(deepLink) }.isSuccess) return true
        val launch =
            context.packageManager
                .getLaunchIntentForPackage(SYSTEM_PACKAGE)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return runCatching { context.startActivity(launch) }.isSuccess
    }

    /** Single-column text rows from a glyph provider URI, best-effort. */
    private fun queryProvider(uri: String): List<String> =
        try {
            val out = mutableListOf<String>()
            context.contentResolver.query(Uri.parse(uri), null, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    (0 until cursor.columnCount)
                        .mapNotNull { runCatching { cursor.getString(it) }.getOrNull() }
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                        .takeIf { it.isNotBlank() }
                        ?.let(out::add)
                }
            }
            out
        } catch (e: Exception) {
            Log.d(TAG, "provider query failed for $uri", e)
            emptyList()
        }

    companion object {
        private const val TAG = "GlyphToysBridge"

        const val SYSTEM_PACKAGE = "com.nothing.thirdparty"
        const val TOY_ACTION = "com.nothing.glyph.TOY"
        const val META_TOY_NAME = "com.nothing.glyph.toy.name"
        const val META_TOY_IMAGE = "com.nothing.glyph.toy.image"

        private const val TOY_PROVIDER_URI = "content://com.nothing.glyphtoyprovider/glyph_toy"
        private const val AOD_TOY_URI = "content://com.nothing.glyphtoyprovider/active_aod_toy_name"
        private const val DEEP_LINK = "glyphtoy://com.nothing.thirdparty/toys"

        // Verified on Nothing OS 4.1 (B4.1): these are the only toy
        // activities in com.nothing.thirdparty. The download activity is
        // the deep-link target for the toys manager.
        private val MANAGER_COMPONENTS =
            listOf(
                "$SYSTEM_PACKAGE.matrix.toys.download.ToysTransparentActivity",
                "$SYSTEM_PACKAGE.matrix.toys.preview.ToysPreviewActivity",
            )

        // No dedicated AOD-picker or timeout activities exist on 4.1 —
        // both fall through to the glyphtoy:// deep link below.
        private val AOD_PICKER_COMPONENTS = emptyList<String>()
        private val TIMEOUT_COMPONENTS = emptyList<String>()
    }
}
