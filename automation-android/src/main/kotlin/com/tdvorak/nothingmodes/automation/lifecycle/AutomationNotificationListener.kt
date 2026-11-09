package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.tdvorak.nothingmodes.data.LOG_RETENTION_MILLIS
import com.tdvorak.nothingmodes.data.NothingModesDatabase
import com.tdvorak.nothingmodes.data.entities.NotificationLogEntity
import com.tdvorak.nothingmodes.data.guardedAppend
import com.tdvorak.nothingmodes.engine.runtime.ActiveNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger

/**
 * NotificationListenerService that dispatches notification trigger events.
 *
 * Requires BIND_NOTIFICATION_LISTENER_SERVICE permission (granted by user in Settings).
 * Manifest:
 * <service android:name=".automation.lifecycle.AutomationNotificationListener"
 *     android:exported="false"
 *     android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
 *     <intent-filter>
 *         <action android:name="android.service.notification.NotificationListenerService"/>
 *     </intent-filter>
 * </service>
 */
class AutomationNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshGeneration = AtomicInteger()
    private val refreshMutex = Mutex()
    private var database: NothingModesDatabase? = null

    override fun onCreate() {
        super.onCreate()
        database = NothingModesDatabase.build(this)
    }

    override fun onDestroy() {
        scope.cancel()
        database?.close()
        database = null
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val category = notification.category ?: ""
        val (title, text, sender) = notification.readExtras(pkg)

        Log.d(TAG, "Notification posted from $pkg")

        // Persist to notification log on a background thread.
        database?.let { db ->
            scope.launch {
                val dao = db.notificationLogDao()
                guardedAppend(
                    prune = {
                        dao.pruneBefore(System.currentTimeMillis() - LOG_RETENTION_MILLIS)
                    },
                ) {
                    dao.insert(
                        NotificationLogEntity(
                            packageName = pkg,
                            title = title,
                            text = text,
                            postTime = sbn.postTime,
                        ),
                    )
                }
            }
        }

        // Dispatch to AutomationService
        val intent =
            Intent(this, AutomationService::class.java).apply {
                action = AutomationService.ACTION_NOTIFICATION
                putExtra(EXTRA_PACKAGE, pkg)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_SENDER, sender)
                putExtra(EXTRA_IS_GROUP, sbn.isGroup)
                // groupKey is the stable per-conversation identifier Android
                // gives the listener (no public conversation-id extra exists).
                sbn.groupKey?.let { putExtra(EXTRA_CONVERSATION_ID, it) }
            }
        startForegroundServiceSafely(intent)
        // getActiveNotifications() is a binder call. Running it here holds
        // the main looper, so AutomationService cannot reach startForeground()
        // inside the FGS deadline (DidNotStartInTime on Android 15 / Xiaomi).
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refreshActiveNotifications()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // The only route that can cancel other apps' notifications; installed
        // so Action.ClearNotifications can reach it from another module.
        ActiveNotifications.clearAll = ::cancelAllNotifications
        refreshActiveNotifications()
    }

    override fun onListenerDisconnected() {
        ActiveNotifications.clearAll = null
        super.onListenerDisconnected()
    }

    private fun refreshActiveNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val generation = refreshGeneration.incrementAndGet()
        scope.launch {
            // Debounce bursts: every post/remove schedules a refresh, and each
            // one unparcels every active notification's extras. A chatty
            // stream would otherwise keep the heap churning toward OOM;
            // superseded generations still bail inside the mutex.
            delay(REFRESH_DEBOUNCE_MS)
            // Serialize refreshes: each one unparcels every active
            // notification's extras, and concurrent runs would multiply the
            // heap spike that OOMed on oversized parcels.
            refreshMutex.withLock {
                // A superseded generation must not even pay the binder call
                // and unparcel; re-check after the fetch so one overtaken
                // mid-flight can't overwrite fresher snapshots.
                if (!isActive || generation != refreshGeneration.get()) return@withLock
                val notifications =
                    runCatching { activeNotifications?.toList() ?: emptyList() }
                        .getOrDefault(emptyList())
                if (!isActive || generation != refreshGeneration.get()) return@withLock
                ActiveNotifications.update(notifications.map { it.toSnapshot() })
            }
        }
    }

    private fun StatusBarNotification.toSnapshot(): ActiveNotifications.NotificationSnapshot {
        val pkg = packageName ?: ""
        val notification = notification
            ?: return ActiveNotifications.NotificationSnapshot(pkg, "", "")
        val (title, text) = notification.readExtras(pkg)
        return ActiveNotifications.NotificationSnapshot(pkg, title, text)
    }

    /**
     * First extras access unparcels the whole bundle — a notification
     * carrying an oversized parcelled payload (chat history, embedded
     * images) can OOM the process mid-read. Title/text/sender are
     * best-effort: pkg, category, group state and timestamps still
     * dispatch without them.
     */
    private fun Notification.readExtras(pkg: String): Triple<String, String, String> {
        val bundle = extras ?: return Triple("", "", "")
        return runCatching {
            Triple(
                bundle.getString(Notification.EXTRA_TITLE, "") ?: "",
                bundle.getString(Notification.EXTRA_TEXT, "") ?: "",
                bundle.getString(Notification.EXTRA_SUB_TEXT, "") ?: "",
            )
        }.getOrElse {
            Log.w(TAG, "Notification extras unreadable for $pkg (${it.javaClass.simpleName})")
            Triple("", "", "")
        }
    }

    companion object {
        private const val TAG = "AutoNotificationListener"
        const val EXTRA_PACKAGE = "notification_pkg"
        const val EXTRA_TITLE = "notification_title"
        const val EXTRA_TEXT = "notification_text"
        const val EXTRA_CATEGORY = "notification_category"
        const val EXTRA_SENDER = "notification_sender"
        const val EXTRA_IS_GROUP = "notification_is_group"
        const val EXTRA_CONVERSATION_ID = "notification_conversation_id"
        private const val REFRESH_DEBOUNCE_MS = 500L
    }
}
