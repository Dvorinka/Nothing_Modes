package com.tdvorak.nothingmodes.nothing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Pre-flight check for glyph actions, run at fire time so a broken setup
 * never fails silently. When the matrix/stripe can't be driven we tell the
 * user once (throttled) how to fix it instead of just logging a failure.
 */
object GlyphPreflight {

    enum class Problem {
        NO_GLYPH_HARDWARE,
        TOY_NOT_SELECTED,
    }

    /**
     * @return null when glyph output can proceed, otherwise the blocking
     * problem. Devices without the Nothing glyph system app cannot arbitrate
     * ownership, so the check passes there and the provider reports its own
     * result.
     */
    fun check(context: Context): Problem? {
        val hw = NothingDeviceDetector(context).detectGlyphHardware()
        if (hw == GlyphHardware.NONE) return Problem.NO_GLYPH_HARDWARE
        val bridge = GlyphToysBridge(context)
        if (!bridge.isGlyphSystemInstalled()) return null
        if (!bridge.ownsMatrix()) return Problem.TOY_NOT_SELECTED
        return null
    }

    fun message(problem: Problem): String =
        when (problem) {
            Problem.NO_GLYPH_HARDWARE -> "this device has no Glyph lights"
            Problem.TOY_NOT_SELECTED ->
                "another Glyph toy owns the lights — pick Nothing Modes in Glyph Toys"
        }

    /**
     * Post a heads-up reminder. Fires at most once per [THROTTLE_MS] and only
     * while the problem persists — never spams on repeated automation runs.
     */
    fun notifyIfNeeded(
        context: Context,
        problem: Problem,
    ) {
        if (problem != Problem.TOY_NOT_SELECTED) return // only the fixable one nags
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST, 0)
        val now = System.currentTimeMillis()
        if (now - last < THROTTLE_MS) return
        prefs.edit().putLong(KEY_LAST, now).apply()

        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL,
                "Glyph setup",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        val launch =
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending =
            launch?.let {
                PendingIntent.getActivity(
                    context,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Glyph action skipped")
                .setContentText("Pick Nothing Modes in Glyph Toys to light the matrix.")
                .setAutoCancel(true)
                .apply { pending?.let { setContentIntent(it) } }
                .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private const val CHANNEL = "glyph_setup"
    private const val PREFS = "glyph_preflight"
    private const val KEY_LAST = "last_notified_ms"
    private const val NOTIFICATION_ID = 4213
    private const val THROTTLE_MS = 30 * 60 * 1000L
}
