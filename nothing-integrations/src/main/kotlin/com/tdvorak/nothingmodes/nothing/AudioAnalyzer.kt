package com.tdvorak.nothingmodes.nothing

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Minimal real-time audio capture for the Glyph Matrix equalizer.
 *
 * Uses [Visualizer] on session 0 (global output mix) when [RECORD_AUDIO] is
 * granted. Falls back to a simulated waveform driven by [AudioManager.isMusicActive].
 */
class AudioAnalyzer(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var visualizer: Visualizer? = null
    private var captureSize = 0
    private var fftBuffer = ByteArray(0)
    private var enabled = false

    /** Whether the caller is seeing simulated data, not live FFT. */
    var isSimulated = true
        private set

    /** True when any media stream is currently active. */
    val isMusicActive: Boolean
        get() = try {
            audioManager.isMusicActive
        } catch (_: Exception) {
            false
        }

    private var simPhase = 0.0

    /**
     * Try to initialise the [Visualizer]. Returns whether it succeeded.
     * Requires [android.Manifest.permission.RECORD_AUDIO].
     */
    fun init(): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "RECORD_AUDIO not granted — music visualizer will simulate")
            isSimulated = true
            return false
        }

        release()

        try {
            val range = Visualizer.getCaptureSizeRange()
            val requested = range?.getOrNull(1)?.takeIf { it > 0 }?.coerceAtMost(1024) ?: 1024

            val vis = Visualizer(0).apply {
                enabled = false
                captureSize = requested
                enabled = true
            }

            // Some devices report a zero range but still allow capture.
            // If the real captureSize stayed 0, the Visualizer is unusable.
            if (vis.captureSize <= 0) {
                vis.release()
                throw IllegalStateException("Visualizer capture size is 0")
            }

            captureSize = vis.captureSize
            visualizer = vis
            fftBuffer = ByteArray(captureSize / 2 + 1)
            enabled = true
            isSimulated = false
            Log.i(TAG, "Visualizer initialised with captureSize=$captureSize")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Visualizer init failed: ${e.message}")
            isSimulated = true
            return false
        }
    }

    /**
     * Returns an equalizer-style energy band for each of [bandCount] columns.
     * Values are in 0..1. When no music is playing all values are 0.
     */
    fun getBands(bandCount: Int): FloatArray {
        val out = FloatArray(bandCount) { 0f }
        if (!isMusicActive) {
            isSimulated = visualizer == null
            return out
        }

        if (!enabled || visualizer == null) {
            isSimulated = true
        }

        val vis = visualizer
        if (!isSimulated && vis != null) {
            try {
                vis.getFft(fftBuffer)
                binFft(fftBuffer, out)
            } catch (e: Exception) {
                Log.w(TAG, "getFft failed: ${e.message}")
                isSimulated = true
            }
        }

        if (isSimulated) {
            simulateBands(out)
        }

        return out
    }

    /**
     * Release the [Visualizer] and all native resources.
     */
    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        }
        visualizer = null
        enabled = false
        isSimulated = true
    }

    private fun binFft(fft: ByteArray, out: FloatArray) {
        val bands = out.size
        if (bands <= 0) return

        // The FFT buffer at index 0 is unused; meaningful bins start at 1.
        val usable = fft.size - 1
        if (usable <= 0) return

        for (band in 0 until bands) {
            val start = 1 + (band * usable / bands)
            val end = 1 + ((band + 1) * usable / bands)
            var sum = 0f
            var count = 0
            for (i in start until end.coerceAtMost(fft.size)) {
                val mag = (fft[i].toInt() and 0xFF) / 256f
                sum += mag
                count++
            }
            out[band] = if (count > 0) sum / count else 0f
        }

        // Gentle temporal smoothing across the whole frame is left to the
        // renderer; the raw band values are returned here.
    }

    private fun simulateBands(out: FloatArray) {
        val now = SystemClock.elapsedRealtime() / 1000.0
        val beat = (now * 4.0) % (Math.PI * 2)
        val pulse = ((kotlin.math.sin(beat) + 1.0) / 2.0).toFloat() * 0.6f + 0.2f

        for (i in out.indices) {
            val offset = i * 0.4
            val wave = kotlin.math.sin(beat + offset)
            val value = (pulse + 0.3f * wave.toFloat()).coerceIn(0f, 1f)
            out[i] = if (isMusicActive) value else 0f
        }
        simPhase += 0.1
    }

    companion object {
        private const val TAG = "AudioAnalyzer"
    }
}
