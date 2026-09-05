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
class GlyphToysBridge(private val context: Context) {

    data class ToyInfo(
        val packageName: String,
        val serviceName: String,
        val label: String,
        val isOurs: Boolean,
    )

    /** Whether the Nothing glyph system app exists on this device. */
    fun isGlyphSystemInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SYSTEM_PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** All services that registered the `com.nothing.glyph.TOY` intent filter. */
    fun listRegisteredToys(): List<ToyInfo> = try {
        val intent = Intent(TOY_ACTION)
        context.packageManager
            .queryIntentServices(intent, PackageManager.GET_META_DATA or PackageManager.MATCH_ALL)
            .map { resolve ->
                val info = resolve.serviceInfo
                val label = info.metaData
                    ?.let { md ->
                        // Prefer the declared toy.name resource; fall back to the app label.
                        val nameRes = md.getInt(META_TOY_NAME, 0)
                        if (nameRes != 0) {
                            runCatching {
                                context.packageManager.getResourcesForApplication(info.packageName)
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
     * Toys known to the system provider (`content://…/glyph_toy`).
     * Column names are undocumented, so this reads defensively and returns
     * whatever rows it can; callers should treat the list as best-effort.
     */
    fun listSystemToys(): List<String> = queryProvider(TOY_PROVIDER_URI)

    /** The toy currently selected for always-on display, if any. */
    fun activeAodToy(): String? = queryProvider(AOD_TOY_URI).firstOrNull()

    // ── Launchers into the system app ─────────────────────────────────────────

    /** Open the system Glyph Toys manager (toy carousel, enable/disable). */
    fun openToysManager(): Boolean = openSystemActivity(MANAGER_COMPONENTS)

    /** Open the always-on Glyph toy picker. */
    fun openAodToyPicker(): Boolean = openSystemActivity(AOD_PICKER_COMPONENTS)

    /** Open the toy idle-timeout settings. */
    fun openTimeoutSettings(): Boolean = openSystemActivity(TIMEOUT_COMPONENTS)

    private fun openSystemActivity(candidates: List<String>): Boolean {
        for (cls in candidates) {
            val intent = Intent().apply {
                component = ComponentName(SYSTEM_PACKAGE, cls)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(intent) }.isSuccess) return true
        }
        // Fall back to the documented deep link, then a plain package launch.
        val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse(DEEP_LINK))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(deepLink) }.isSuccess) return true
        val launch = context.packageManager.getLaunchIntentForPackage(SYSTEM_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return false
        return runCatching { context.startActivity(launch) }.isSuccess
    }

    private fun queryProvider(uri: String): List<String> = try {
        val out = mutableListOf<String>()
        context.contentResolver.query(Uri.parse(uri), null, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                // Concatenate all string columns; the schema is undocumented.
                val row = (0 until cursor.columnCount)
                    .mapNotNull { runCatching { cursor.getString(it) }.getOrNull() }
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                if (row.isNotBlank()) out.add(row)
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

        // Activity names from the analysed system build; several may move
        // between packages, so try a few shapes.
        private val MANAGER_COMPONENTS = listOf(
            "$SYSTEM_PACKAGE.matrix.toys.manager.ToysManagerActivity",
            "$SYSTEM_PACKAGE.matrix.toys.ToysManagerActivity",
            "$SYSTEM_PACKAGE.matrix.toys.manager.ToysTransparentActivity",
        )
        private val AOD_PICKER_COMPONENTS = listOf(
            "$SYSTEM_PACKAGE.matrix.toys.manager.AodToySelectActivity",
            "$SYSTEM_PACKAGE.matrix.toys.AodToySelectActivity",
        )
        private val TIMEOUT_COMPONENTS = listOf(
            "$SYSTEM_PACKAGE.matrix.toys.manager.ToyTimeoutSettingsActivity",
            "$SYSTEM_PACKAGE.matrix.toys.settings.ToyTimeoutSettingsActivity",
            "$SYSTEM_PACKAGE.matrix.toys.ToyTimeoutSettingsActivity",
        )
    }
}
