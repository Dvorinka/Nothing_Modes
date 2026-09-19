package com.tdvorak.nothingmodes.quicksettings

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tdvorak.nothingmodes.capabilities.controllers.UltraDimController

/**
 * Persistent, low-priority notification shown while the ultra-dim overlay is
 * active. Actions step the intensity without touching the mode that set it;
 * tapping the body opens [DimAdjustActivity] for slider control.
 *
 * Lives entirely off [UltraDimController.onChanged] — every caller (mode
 * engine, QS tile, these actions, the adjust panel) drives it implicitly.
 * The notification shade is a trusted system surface, so these actions keep
 * working even on the fallback overlay path at high intensity where Android
 * drops touches to app windows.
 */
object UltraDimNotifier {
    private const val CHANNEL_ID = "ultra_dim"
    private const val NOTIFICATION_ID = 4100

    /** Sync notification state with the current overlay intensity. */
    fun sync(context: Context) {
        val appContext = context.applicationContext
        val nm = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (!UltraDimController.isActive) {
            nm.cancel(NOTIFICATION_ID)
            return
        }
        if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return
        ensureChannel(nm)
        nm.notify(NOTIFICATION_ID, build(appContext))
    }

    private fun build(context: Context): Notification {
        val percent = UltraDimController.percent
        val action =
            { name: String ->
                PendingIntent.getBroadcast(
                    context,
                    name.hashCode(),
                    Intent(context, UltraDimControlReceiver::class.java).setAction(name),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(com.tdvorak.nothingmodes.automation.R.drawable.ic_notification)
            .setContentTitle("Ultra dim · $percent%")
            .setContentText("Tap to adjust")
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, DimAdjustActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            ).addAction(0, "-10%", action(UltraDimControlReceiver.ACTION_DIM_LESS))
            .addAction(0, "+10%", action(UltraDimControlReceiver.ACTION_DIM_MORE))
            .addAction(0, "Turn off", action(UltraDimControlReceiver.ACTION_DIM_OFF))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
    }

    private fun ensureChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Ultra dim",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Live intensity control for the ultra-dim overlay" },
        )
    }
}
