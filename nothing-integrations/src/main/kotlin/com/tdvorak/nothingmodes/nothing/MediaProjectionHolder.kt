package com.tdvorak.nothingmodes.nothing

import android.media.projection.MediaProjection

/**
 * Holds a user-granted [MediaProjection] for [AudioAnalyzer] and other
 * features that need to capture screen or playback audio.
 *
 * The projection must be obtained from an Activity (e.g.
 * [com.tdvorak.nothingmodes.ui.screens.MediaProjectionRequestActivity]) and is
 * valid until the user revokes it.
 */
object MediaProjectionHolder {
    private var projection: MediaProjection? = null
    private var callback: MediaProjection.Callback? = null

    fun get(): MediaProjection? = projection

    fun set(projection: MediaProjection?) {
        clear()
        this.projection = projection
        if (projection != null) {
            val cb =
                object : MediaProjection.Callback() {
                    override fun onStop() {
                        if (MediaProjectionHolder.projection === projection) {
                            MediaProjectionHolder.projection = null
                            callback = null
                        }
                    }
                }
            callback = cb
            projection.registerCallback(cb, null)
        }
    }

    fun clear() {
        val p = projection
        val cb = callback
        if (p != null && cb != null) {
            p.unregisterCallback(cb)
        }
        p?.stop()
        projection = null
        callback = null
    }
}
