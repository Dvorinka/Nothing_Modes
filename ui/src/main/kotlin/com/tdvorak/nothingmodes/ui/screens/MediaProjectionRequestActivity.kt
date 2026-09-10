package com.tdvorak.nothingmodes.ui.screens

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.tdvorak.nothingmodes.nothing.MediaProjectionHolder

/**
 * Transparent Activity that requests a [MediaProjection] from the user and
 * stores it in [MediaProjectionHolder].
 *
 * This is needed for [AudioPlaybackCapture], which can capture audio played by
 * other apps regardless of whether it is routed to headphones, Bluetooth, or
 * the built-in speaker.
 */
class MediaProjectionRequestActivity : ComponentActivity() {
    private val projectionManager by lazy { getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val projection = projectionManager.getMediaProjection(result.resultCode, data)
                if (projection != null) {
                    MediaProjectionHolder.set(projection)
                    Log.i(TAG, "MediaProjection granted")
                }
            } else {
                Log.d(TAG, "MediaProjection denied")
            }
            finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val intent = projectionManager.createScreenCaptureIntent()
            launcher.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create screen capture intent", e)
            finish()
        }
    }

    companion object {
        private const val TAG = "MediaProjectionRequest"

        fun intent(context: Context): Intent =
            Intent(context, MediaProjectionRequestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

        fun isGranted(): Boolean = MediaProjectionHolder.get() != null
    }
}
