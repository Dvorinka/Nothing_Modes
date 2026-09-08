package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.engine.runtime.ActiveNotifications

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
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE, "") ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT, "") ?: ""
        val category = notification.category ?: ""
        val sender = extras.getString(Notification.EXTRA_SUB_TEXT, "") ?: ""

        Log.d(TAG, "Notification posted from $pkg")

        // Dispatch to AutomationService
        val intent =
            Intent(this, AutomationService::class.java).apply {
                action = AutomationService.ACTION_NOTIFICATION
                putExtra(EXTRA_PACKAGE, pkg)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_TEXT, text)
                putExtra(EXTRA_CATEGORY, category)
                putExtra(EXTRA_SENDER, sender)
            }
        ContextCompat.startForegroundService(this, intent)
        refreshActiveNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        refreshActiveNotifications()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refreshActiveNotifications()
    }

    private fun refreshActiveNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val notifications = runCatching { activeNotifications?.toList() ?: emptyList() }.getOrDefault(emptyList())
        ActiveNotifications.update(notifications.map { it.toSnapshot() })
    }

    private fun StatusBarNotification.toSnapshot(): ActiveNotifications.NotificationSnapshot {
        val notification = this.notification ?: return ActiveNotifications.NotificationSnapshot(packageName ?: "", "", "")
        val extras = notification.extras
        val title = extras?.getString(Notification.EXTRA_TITLE, "") ?: ""
        val text = extras?.getString(Notification.EXTRA_TEXT, "") ?: ""
        return ActiveNotifications.NotificationSnapshot(packageName ?: "", title, text)
    }

    companion object {
        private const val TAG = "AutoNotificationListener"
        const val EXTRA_PACKAGE = "notification_pkg"
        const val EXTRA_TITLE = "notification_title"
        const val EXTRA_TEXT = "notification_text"
        const val EXTRA_CATEGORY = "notification_category"
        const val EXTRA_SENDER = "notification_sender"
    }
}
