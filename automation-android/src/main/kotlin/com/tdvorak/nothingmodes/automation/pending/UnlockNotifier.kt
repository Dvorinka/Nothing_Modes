package com.tdvorak.nothingmodes.automation.pending

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tdvorak.nothingmodes.automation.R

/**
 * Alert for actions queued by [PendingUnlockStore]: lights the screen and
 * posts a high-priority heads-up notification. The lockscreen stays under
 * the user's control — tapping the notification fires
 * [PendingUnlockReceiver], which drains the queue inside the brief
 * activity-launch window a tap grants.
 */
object UnlockNotifier {
    private const val TAG = "UnlockNotifier"
    private const val CHANNEL_ID = "pending_unlock"
    private const val NOTIFICATION_ID = 3001

    /** Wake the screen and post the pending-actions alert. */
    fun notifyPending(
        context: Context,
        automationName: String?,
        pendingCount: Int,
    ) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled — cannot surface pending unlock actions")
            wakeScreen(context)
            return
        }
        wakeScreen(context)
        ensureChannel(context)
        post(context, automationName, pendingCount)
    }

    private fun post(
        context: Context,
        automationName: String?,
        pendingCount: Int,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        val drainIntent =
            Intent(PendingUnlockReceiver.ACTION)
                .setPackage(context.packageName)
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                0,
                drainIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val title =
            if (automationName != null) {
                "Finish $automationName"
            } else {
                "Finish your mode"
            }
        val text = "$pendingCount action${if (pendingCount == 1) "" else "s"} waiting — unlock to run"

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    /**
     * Light the display so the lockscreen (and this alert) is actually seen.
     * ACQUIRE_CAUSES_WAKEUP is deprecated but remains the only way to turn the
     * screen on without a visible activity.
     */
    @SuppressLint("WakelockTimeout")
    @Suppress("DEPRECATION")
    private fun wakeScreen(context: Context) {
        runCatching {
            val pm = context.getSystemService(PowerManager::class.java) ?: return
            pm
                .newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                    "nothingmodes:pending_unlock",
                ).acquire(10_000)
        }.onFailure { Log.w(TAG, "Could not wake screen", it) }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                "Pending actions",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Actions waiting for the device to be unlocked"
            }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}
