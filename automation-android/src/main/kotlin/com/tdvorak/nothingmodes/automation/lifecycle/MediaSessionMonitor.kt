package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.engine.runtime.ActiveMedia

/**
 * Watches active media sessions and dispatches MediaPlaybackChanged events.
 *
 * Uses the notification listener component to query sessions; this works
 * without the privileged MEDIA_CONTENT_CONTROL permission because the app
 * exposes an enabled NotificationListenerService.
 */
class MediaSessionMonitor(
    private val context: Context,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    private val listenerComponent = ComponentName(context, AutomationNotificationListener::class.java)
    private val activeControllers = mutableListOf<MediaController>()
    private var lastPlaying: Boolean? = null
    private var lastPackage: String? = null

    private val sessionChangeListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateControllers(controllers ?: emptyList())
        }

    private val controllerCallback =
        object : MediaController.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                evaluateAndDispatch()
            }

            override fun onMetadataChanged(metadata: MediaMetadata?) {
                evaluateAndDispatch()
            }
        }

    fun start() {
        if (sessionManager == null) {
            Log.w(TAG, "MediaSessionManager not available")
            return
        }
        try {
            sessionManager.addOnActiveSessionsChangedListener(sessionChangeListener, listenerComponent, handler)
            updateControllers(sessionManager.getActiveSessions(listenerComponent) ?: emptyList())
            Log.i(TAG, "Media session monitoring started")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot listen to media sessions: ${e.message}")
        }
    }

    fun stop() {
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionChangeListener)
        } catch (_: Exception) {
            // ignore
        }
        activeControllers.forEach { it.unregisterCallback(controllerCallback) }
        activeControllers.clear()
    }

    private fun updateControllers(controllers: List<MediaController>) {
        activeControllers.forEach { it.unregisterCallback(controllerCallback) }
        activeControllers.clear()
        activeControllers.addAll(controllers)
        activeControllers.forEach { it.registerCallback(controllerCallback, handler) }
        evaluateAndDispatch()
    }

    private fun evaluateAndDispatch() {
        val top = activeControllers.firstOrNull()
        val state = top?.playbackState?.state ?: PlaybackState.STATE_NONE
        val playing =
            state == PlaybackState.STATE_PLAYING ||
                state == PlaybackState.STATE_BUFFERING

        val packageName = top?.packageName

        if (playing == lastPlaying && packageName == lastPackage) return
        lastPlaying = playing
        lastPackage = packageName

        val metadata = top?.metadata
        val artist =
            metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)

        ActiveMedia.update(packageName, artist, title)

        val intent =
            Intent(context, AutomationService::class.java).apply {
                action = AutomationService.ACTION_MEDIA_PLAYBACK
                putExtra(AutomationService.EXTRA_MEDIA_PLAYING, playing)
                putExtra(AutomationService.EXTRA_MEDIA_PACKAGE, packageName)
                putExtra(AutomationService.EXTRA_MEDIA_ARTIST, artist)
                putExtra(AutomationService.EXTRA_MEDIA_TITLE, title)
            }
        ContextCompat.startForegroundService(context, intent)
        Log.d(TAG, "Media playback: playing=$playing pkg=$packageName artist=$artist title=$title")
    }

    companion object {
        private const val TAG = "MediaSessionMonitor"
    }
}
