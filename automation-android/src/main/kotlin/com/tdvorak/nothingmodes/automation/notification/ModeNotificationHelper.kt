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
import com.tdvorak.nothingmodes.engine.model.actionDescription
import com.tdvorak.nothingmodes.engine.model.supportsRestore
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.failureLabel

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
    fun effectiveRules(automation: Automation): List<NotifyRule> = if (automation.notifyRules.isNotEmpty()) automation.notifyRules else prefs.getDefaultRules()

    /** Call after a mode fires and its actions have run. */
    fun postOnTrigger(
        automation: Automation,
        results: List<ActionResult>,
    ) {
        if (!canPost()) return
        if (!effectiveRules(automation).contains(NotifyRule.OnTrigger)) return

        val applied =
            automation.actions
                .zip(results)
                .filter { (_, result) -> result is ActionResult.Success || result is ActionResult.NeedsUserAction }
                .map { (action, _) -> actionDescription(action) }
        val failed = results.size - applied.size
        if (failed > 0) {
            postFailure(automation, results)
            return
        }
        val text =
            buildString {
                append("just ran")
                if (applied.isNotEmpty()) {
                    append(" — ")
                    append(applied.take(4).joinToString(" · "))
                    if (applied.size > 4) append(" +${applied.size - 4} more")
                }
                if (failed > 0) append(if (applied.isEmpty()) " — $failed failed" else ", $failed failed")
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
    fun postBefore(
        automation: Automation,
        minutes: Int,
    ) {
        if (!canPost()) return
        val hasBefore = effectiveRules(automation).any { it is NotifyRule.Before && it.minutes == minutes }
        if (!hasBefore) return

        val summary = actionSummary(automation)
        val text = "fires in $minutes min$summary"
        post(automation, "Mode fires soon", text)
    }

    /** High-priority heads-up naming the concrete reason(s) an action failed. */
    private fun postFailure(
        automation: Automation,
        results: List<ActionResult>,
    ) {
        val reasons =
            automation.actions
                .zip(results)
                .mapNotNull { (a, r) -> r.failureLabel(a) }
                .distinct()

        val detail =
            if (reasons.isEmpty()) {
                "Action failed"
            } else {
                reasons.take(2).joinToString(" · ") +
                    if (reasons.size > 2) " · +${reasons.size - 2} more" else ""
            }

        val text = "$detail — tap to open the mode"
        val launch =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_AUTOMATION_ID, automation.id.value)
            }
        val contentIntent =
            launch?.let {
                PendingIntent.getActivity(
                    context,
                    (automation.id.value.hashCode() and 0x7FFFFFFF),
                    it,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

        val notification =
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle("Couldn't run · ${automation.name.ifBlank { "Untitled" }}")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(appIcon())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .build()

        notificationManager.notify(notificationId(automation) + 1, notification)
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
        val title = "$titlePrefix · ${automation.name.ifBlank { "Untitled" }}"
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
            NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
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
        const val EXTRA_AUTOMATION_ID = "com.tdvorak.nothingmodes.OPEN_AUTOMATION_ID"
    }
}
