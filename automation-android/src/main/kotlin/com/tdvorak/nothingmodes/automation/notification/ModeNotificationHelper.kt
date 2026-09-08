package com.tdvorak.nothingmodes.automation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tdvorak.nothingmodes.automation.R
import com.tdvorak.nothingmodes.data.prefs.NotificationPreferences
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.NotifyRule
import com.tdvorak.nothingmodes.engine.model.supportsRestore
import com.tdvorak.nothingmodes.engine.runtime.ActionResult

/**
 * Posts per-mode heads-up notifications.
 * OFF by default; explicit per-mode rules win over the global default.
 */
class ModeNotificationHelper(
    context: Context,
) {
    private val context = context.applicationContext
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val prefs = NotificationPreferences(context)

    init {
        createChannel()
    }

    /** Rules that apply for this automation. */
    fun effectiveRules(automation: Automation): List<NotifyRule> =
        if (automation.notifyRules.isNotEmpty()) automation.notifyRules else prefs.getDefaultRules()

    /** Call after a mode fires and its actions have run. */
    fun postOnTrigger(automation: Automation, results: List<ActionResult>) {
        if (!canPost()) return
        if (!effectiveRules(automation).contains(NotifyRule.OnTrigger)) return

        val applied = results.count { it is ActionResult.Success || it is ActionResult.NeedsUserAction }
        val failed = results.size - applied
        val text = buildString {
            append("just ran — $applied applied")
            if (failed > 0) append(", $failed failed")
        }
        post(automation, "Mode ran", text)
    }

    /** Call when a windowed mode ends. */
    fun postOnEnd(automation: Automation) {
        if (!canPost()) return
        if (!effectiveRules(automation).contains(NotifyRule.OnEnd)) return

        val restored = automation.actions.count { it.supportsRestore }
        val text = if (restored > 0) "mode ended — $restored settings restored" else "mode ended"
        post(automation, "Mode ended", text)
    }

    /** Call for a BEFORE rule. */
    fun postBefore(automation: Automation, minutes: Int) {
        if (!canPost()) return
        val hasBefore = effectiveRules(automation).any { it is NotifyRule.Before && it.minutes == minutes }
        if (!hasBefore) return

        val summary = actionSummary(automation)
        val text = "fires in $minutes min$summary"
        post(automation, "Mode fires soon", text)
    }

    private fun actionSummary(automation: Automation): String {
        val count = automation.actions.size
        return if (count == 0) "" else " — $count action${if (count > 1) "s" else ""}"
    }

    private fun post(
        automation: Automation,
        titlePrefix: String,
        text: String,
    ) {
        val title = "$titlePrefix · ${automation.name}"
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent =
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                PendingIntent.getActivity(
                    context,
                    automation.id.value.hashCode(),
                    launch,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            } else {
                null
            }

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(appIcon())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()

        notificationManager.notify(notificationId(automation), notification)
    }

    private fun appIcon(): Bitmap? {
        val id = context.resources.getIdentifier("ic_launcher", "mipmap", context.packageName)
        if (id == 0) return null
        return runCatching { BitmapFactory.decodeResource(context.resources, id) }.getOrNull()
    }

    private fun notificationId(automation: Automation): Int {
        // Keep different IDs for the same mode by hashing the name, stable enough.
        return (automation.id.value.hashCode() and 0x7FFFFFFF) + 2000
    }

    private fun canPost(): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID) ?: return true
            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }
        return true
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "Mode notifications",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Heads-ups for your modes" }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "mode_notifications"
    }
}
