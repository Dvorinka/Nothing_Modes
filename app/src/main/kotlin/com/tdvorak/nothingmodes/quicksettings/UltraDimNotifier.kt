package com.tdvorak.nothingmodes.quicksettings

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tdvorak.nothingmodes.capabilities.controllers.UltraDimController
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.widget.WidgetEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Persistent, low-priority notification for the ultra-dim overlay.
 *
 * While the overlay is on it carries -10/+10/off actions and opens
 * [DimAdjustActivity]. While it is off but a still-active mode contains a
 * SetUltraDim action, a quiet "Turn on" entry stays posted — the mode keeps
 * its system-level handle even when the user temporarily removes the dim.
 * The notification shade is a trusted system surface, so these actions keep
 * working even on the fallback overlay path at high intensity where Android
 * drops touches to app windows.
 */
object UltraDimNotifier {
    private const val CHANNEL_ID = "ultra_dim"
    private const val NOTIFICATION_ID = 4100

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    /** Sync notification state with the current overlay intensity. */
    fun sync(context: Context) = sync(context, allowRecheck = true)

    private fun sync(
        context: Context,
        allowRecheck: Boolean,
    ) {
        val appContext = context.applicationContext
        val nm = appContext.getSystemService(NotificationManager::class.java) ?: return
        if (UltraDimController.isActive) {
            if (!NotificationManagerCompat.from(appContext).areNotificationsEnabled()) return
            ensureChannel(nm)
            nm.notify(NOTIFICATION_ID, buildOn(appContext))
            return
        }
        // Overlay off. A live mode may still own the dimmer — check before
        // dropping the notification so the user keeps a way to re-enable it.
        scope.launch {
            val engaged = engagedPercent(appContext)
            if (engaged == null) {
                nm.cancel(NOTIFICATION_ID)
                return@launch
            }
            if (NotificationManagerCompat.from(appContext).areNotificationsEnabled()) {
                ensureChannel(nm)
                nm.notify(NOTIFICATION_ID, buildOff(appContext, engaged))
            }
            // Mode-end restores hide the overlay a beat before the activation
            // row flips to DEACTIVATED — re-verify once so a stale "off" entry
            // does not linger after the mode actually ended.
            if (allowRecheck) {
                handler.postDelayed({ sync(appContext, allowRecheck = false) }, 1_500)
            }
        }
    }

    /**
     * The dim level a currently active mode wants, or null when no active
     * mode contains a SetUltraDim action. Drives both the off-state
     * notification and "Turn on".
     */
    internal suspend fun engagedPercent(context: Context): Int? =
        withContext(Dispatchers.IO) {
            val entryPoint =
                runCatching {
                    EntryPoints.get(context.applicationContext, WidgetEntryPoint::class.java)
                }.getOrNull() ?: return@withContext null
            val ids =
                runCatching { entryPoint.modeActivationProvider().activeModeIds() }
                    .getOrDefault(emptyList())
            for (id in ids) {
                val automation =
                    runCatching { entryPoint.automationStore().get(AutomationId(id)) }
                        .getOrNull() ?: continue
                automation.actions
                    .filterIsInstance<Action.SetUltraDim>()
                    .firstOrNull { it.percent > 0 }
                    ?.let { return@withContext it.percent }
            }
            null
        }

    private fun pendingAction(
        context: Context,
        name: String,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            name.hashCode(),
            Intent(context, UltraDimControlReceiver::class.java).setAction(name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun adjustIntent(
        context: Context,
        suggestedPercent: Int,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, DimAdjustActivity::class.java)
                .putExtra(DimAdjustActivity.EXTRA_SUGGESTED_PERCENT, suggestedPercent),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun baseBuilder(context: Context) =
        NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(com.tdvorak.nothingmodes.automation.R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)

    private fun buildOn(context: Context): Notification {
        val percent = UltraDimController.percent
        return baseBuilder(context)
            .setContentTitle("Ultra dim · $percent%")
            .setContentText("Tap to adjust")
            .setContentIntent(adjustIntent(context, percent))
            .addAction(0, "-10%", pendingAction(context, UltraDimControlReceiver.ACTION_DIM_LESS))
            .addAction(0, "+10%", pendingAction(context, UltraDimControlReceiver.ACTION_DIM_MORE))
            .addAction(0, "Turn off", pendingAction(context, UltraDimControlReceiver.ACTION_DIM_OFF))
            .setOngoing(true)
            .build()
    }

    /** Off-but-engaged entry: a quiet handle that re-arms the mode's dim level. */
    private fun buildOff(
        context: Context,
        engagedPercent: Int,
    ): Notification =
        baseBuilder(context)
            .setContentTitle("Ultra dim · Off")
            .setContentText("Mode still active. Tap to adjust or turn it back on.")
            .setContentIntent(adjustIntent(context, engagedPercent))
            .addAction(0, "Turn on · $engagedPercent%", pendingAction(context, UltraDimControlReceiver.ACTION_DIM_ON))
            // Swipeable on purpose — if the mode ended in the same instant the
            // recheck missed, the user can still dismiss a stale entry.
            .setOngoing(false)
            .setAutoCancel(false)
            .build()

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
