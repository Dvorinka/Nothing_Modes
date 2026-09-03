package com.tdvorak.nothingmodes.automation.lifecycle

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

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

        Log.d(TAG, "Notification: pkg=$pkg title=$title cat=$category")

        // Dispatch to AutomationService
        val intent = Intent(this, AutomationService::class.java).apply {
            action = AutomationService.ACTION_NOTIFICATION
            putExtra(EXTRA_PACKAGE, pkg)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_TEXT, text)
            putExtra(EXTRA_CATEGORY, category)
        }
        startService(intent)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Could dispatch a "notification dismissed" event if needed
    }

    companion object {
        private const val TAG = "AutoNotificationListener"
        const val EXTRA_PACKAGE = "notification_pkg"
        const val EXTRA_TITLE = "notification_title"
        const val EXTRA_TEXT = "notification_text"
        const val EXTRA_CATEGORY = "notification_category"
    }
}
