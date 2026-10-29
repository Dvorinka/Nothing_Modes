package com.tdvorak.nothingmodes.shizuku

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Surfaces Shizuku loss to the user. Privileged actions fail into the audit
 * log when the binder dies (Shizuku stopped, an OTA killed it, permission
 * was revoked) — nothing told the user to go start it. One throttled
 * heads-up fixes that silence; the tap opens Shizuku so the fix is one
 * more tap. Never nags users who never authorized Shizuku.
 */
object ShizukuStatusNotifier {
    private const val CHANNEL_ID = "shizuku_status"
    private const val NOTIFICATION_ID = 4214
    private const val PREFS = "shizuku_status"
    private const val KEY_LAST = "last_notified_ms"
    private const val KEY_EVER_AUTHORIZED = "ever_authorized"
    private const val THROTTLE_MS = 60 * 60 * 1000L
    private const val MANAGER_PACKAGE = "moe.shizuku.privileged.api"

    /** Remember that privileged access once worked — only then is losing it worth nagging about. */
    fun markAuthorized(context: Context) {
        prefs(context).edit().putBoolean(KEY_EVER_AUTHORIZED, true).apply()
    }

    fun wasAuthorized(context: Context): Boolean = prefs(context).getBoolean(KEY_EVER_AUTHORIZED, false)

    /** Clear the reminder once access is back. */
    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    /** Throttled heads-up posted at most once per [THROTTLE_MS]. */
    fun notifyUnavailable(context: Context) {
        if (!wasAuthorized(context)) return
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST, 0) < THROTTLE_MS) return
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
                CHANNEL_ID,
                "Shizuku status",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when privileged automations can't run because Shizuku access was lost"
            },
        )
        val launch =
            context.packageManager.getLaunchIntentForPackage(MANAGER_PACKAGE)
                ?: context.packageManager.getLaunchIntentForPackage(context.packageName)
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
                .Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Shizuku access lost")
                .setContentText(
                    "Automations that need Shizuku are failing — open Shizuku, start it, " +
                        "and check Nothing Modes is granted.",
                ).setStyle(NotificationCompat.BigTextStyle())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .apply { pending?.let { setContentIntent(it) } }
                .build()
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
