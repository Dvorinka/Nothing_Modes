package com.tdvorak.nothingmodes.automation.capture

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.automation.R
import com.tdvorak.nothingmodes.nothing.MediaProjectionHolder

/**
 * Foreground service that owns a user-granted [MediaProjection].
 *
 * Since Android 14, [MediaProjectionManager.getMediaProjection] throws
 * SecurityException unless a foreground service of type `mediaProjection`
 * is already running — so the consent result is handed here, we go
 * foreground first, and only then take the projection.
 *
 * The service stays up while the projection is held so AudioPlaybackCapture
 * keeps working (headphones, Bluetooth, speaker) and dies when the
 * projection stops or the user taps Stop on the persistent notification.
 */
class MediaProjectionCaptureService : Service() {
    private var stopCallback: MediaProjection.Callback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                MediaProjectionHolder.clear()
                stopSelf()
            }
            ACTION_GRANT -> handleGrant(intent)
            else -> stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun handleGrant(intent: Intent) {
        startForegroundWithNotification()

        val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
        val data = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "grant intent missing result data")
            stopSelf()
            return
        }

        val projection =
            runCatching {
                getSystemService(MediaProjectionManager::class.java)
                    .getMediaProjection(resultCode, data)
            }.getOrElse { e ->
                Log.e(TAG, "getMediaProjection failed", e)
                stopSelf()
                return
            }
        if (projection == null) {
            Log.e(TAG, "getMediaProjection returned null")
            stopSelf()
            return
        }

        stopCallback =
            object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.i(TAG, "projection stopped by system/user")
                    stopSelf()
                }
            }.also { projection.registerCallback(it, null) }

        MediaProjectionHolder.set(projection)
        Log.i(TAG, "MediaProjection acquired")
    }

    override fun onDestroy() {
        MediaProjectionHolder.get()?.let { p ->
            stopCallback?.let { runCatching { p.unregisterCallback(it) } }
        }
        stopCallback = null
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Audio capture",
                NotificationManager.IMPORTANCE_MIN,
            ).apply { description = "Active while playback capture is available" },
        )

        val stopIntent =
            PendingIntent.getService(
                this,
                0,
                Intent(this, MediaProjectionCaptureService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Audio capture active")
                .setContentText("Used for the Glyph music visualizer.")
                .setOngoing(true)
                .addAction(0, "Stop", stopIntent)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val TAG = "MediaProjectionCapture"
        private const val CHANNEL_ID = "audio_capture"
        private const val NOTIFICATION_ID = 4242
        private const val ACTION_GRANT = "com.tdvorak.nothingmodes.CAPTURE_GRANT"
        private const val ACTION_STOP = "com.tdvorak.nothingmodes.CAPTURE_STOP"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_RESULT_DATA = "result_data"

        /** Hand the consent result to the service — it owns the projection. */
        fun grantIntent(
            context: Context,
            resultCode: Int,
            data: Intent,
        ): Intent =
            Intent(context, MediaProjectionCaptureService::class.java).apply {
                action = ACTION_GRANT
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }

        fun startWithGrant(
            context: Context,
            resultCode: Int,
            data: Intent,
        ) {
            ContextCompat.startForegroundService(context, grantIntent(context, resultCode, data))
        }
    }
}
