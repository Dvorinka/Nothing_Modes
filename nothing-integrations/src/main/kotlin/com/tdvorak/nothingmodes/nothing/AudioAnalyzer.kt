package com.tdvorak.nothingmodes.nothing

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import android.media.projection.MediaProjection
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Minimal real-time audio capture for the Glyph Matrix music visualizer.
 *
 * Uses three sources in order:
 * 1. [PlaybackAudioCapture] via [MediaProjection] (Android 10+) — captures
 *    app audio regardless of whether it is playing through the speaker,
 *    headphones, or Bluetooth.
 * 2. [Visualizer] on session 0 (global output mix) — works for the built-in
 *    speaker on most devices, but fails for many headphone/Bluetooth routes.
 * 3. Simulated waveform — runs when no real source is available so the glyph
 *    still animates to a generic beat pattern.
 *
 * The first source that returns live, non-silent data wins for each frame.
 */
class AudioAnalyzer(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val playbackCapture = PlaybackAudioCapture(appContext)
    private var visualizer: Visualizer? = null
    private var captureSize = 0
    private var fftBuffer = ByteArray(0)
    private var waveBuffer = ByteArray(0)
    private var enabled = false

    /** Whether the caller is seeing simulated data, not live audio. */
    var isSimulated = true
        private set

    /** Active capture source for diagnostics. */
    var source = Source.SIMULATION
        private set

    enum class Source {
        PLAYBACK,
        VISUALIZER,
        SIMULATION,
    }

    /** True when any media stream is currently active. */
    val isMusicActive: Boolean
        get() =
            try {
                audioManager.isMusicActive
            } catch (_: Exception) {
                false
            }

    private var simPhase = 0.0

    /**
     * Provide a [MediaProjection] for [PlaybackAudioCapture]. Call before [init]
     * if headphone/Bluetooth capture is needed. The projection is held and used
     * for as long as the analyzer is active.
     */
    fun setMediaProjection(projection: MediaProjection?) {
        playbackCapture.setMediaProjection(projection)
    }

    /**
     * Try to initialise real audio capture. Returns whether any live source is
     * running. Requires [android.Manifest.permission.RECORD_AUDIO].
     */
    fun init(): Boolean {
        if (!hasRecordAudio()) {
            Log.w(TAG, "RECORD_AUDIO not granted — music visualizer will simulate")
            isSimulated = true
            source = Source.SIMULATION
            return false
        }

        release()

        // Prefer AudioPlaybackCapture when a MediaProjection is available.
        try {
            if (playbackCapture.init()) {
                source = Source.PLAYBACK
                isSimulated = false
                Log.i(TAG, "Using PlaybackAudioCapture")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "PlaybackAudioCapture init failed: ${e.message}")
        }

        // Fall back to Visualizer(0).
        try {
            val range = Visualizer.getCaptureSizeRange()
            val requested = range?.getOrNull(1)?.takeIf { it > 0 }?.coerceAtMost(1024) ?: 1024

            val vis =
                Visualizer(0).apply {
                    enabled = false
                    captureSize = requested
                    enabled = true
                }

            if (vis.captureSize <= 0) {
                vis.release()
                throw IllegalStateException("Visualizer capture size is 0")
            }

            captureSize = vis.captureSize
            visualizer = vis
            fftBuffer = ByteArray(captureSize / 2 + 1)
            waveBuffer = ByteArray(captureSize)
            enabled = true
            isSimulated = false
            source = Source.VISUALIZER
            Log.i(TAG, "Visualizer initialised with captureSize=$captureSize")
            return true
        } catch (e: Exception) {
            Log.w(TAG, "Visualizer init failed: ${e.message}")
        }

        isSimulated = true
        source = Source.SIMULATION
        return false
    }

    /**
     * Returns an equalizer-style energy band for each of [bandCount] columns.
     * Values are in 0..1. When no music is playing all values are 0.
     */
    fun getBands(bandCount: Int): FloatArray = if (isMusicActive) getSpectrum(bandCount) else FloatArray(bandCount) { 0f }

    /**
     * Returns a single raw waveform sample for each column, centred on 0.
     * Values are in -1..1. When no music is playing all values are 0.
     */
    fun getWaveform(size: Int): FloatArray = if (isMusicActive) getWaveformSamples(size) else FloatArray(size) { 0f }

    private fun getSpectrum(bandCount: Int): FloatArray {
        val out = FloatArray(bandCount) { 0f }
        if (!isMusicActive) {
            return out
        }

        // 1. Playback capture (works with headphones/Bluetooth when allowed).
        if (playbackCapture.isCapturing() && !playbackCapture.isSilent()) {
            isSimulated = false
            source = Source.PLAYBACK
            try {
                return playbackCapture.getBands(bandCount)
            } catch (e: Exception) {
                Log.w(TAG, "PlaybackAudioCapture getBands failed: ${e.message}")
            }
        }

        // 2. Visualizer on global output mix.
        if (enabled && visualizer != null) {
            val vis = visualizer
            if (vis != null) {
                try {
                    vis.getFft(fftBuffer)
                    binFft(fftBuffer, out)
                    isSimulated = false
                    source = Source.VISUALIZER
                    return out
                } catch (e: Exception) {
                    Log.w(TAG, "getFft failed: ${e.message}")
                }
            }
        }

        // 3. Simulation.
        isSimulated = true
        source = Source.SIMULATION
        simulateBands(out)
        return out
    }

    private fun getWaveformSamples(size: Int): FloatArray {
        val out = FloatArray(size) { 0f }

        // 1. Playback capture.
        if (playbackCapture.isCapturing() && !playbackCapture.isSilent()) {
            isSimulated = false
            source = Source.PLAYBACK
            try {
                return playbackCapture.getWaveform(size)
            } catch (e: Exception) {
                Log.w(TAG, "PlaybackAudioCapture getWaveform failed: ${e.message}")
            }
        }

        // 2. Visualizer.
        if (enabled && visualizer != null) {
            val vis = visualizer
            if (vis != null) {
                try {
                    vis.getWaveForm(waveBuffer)
                    resampleWaveform(waveBuffer, out)
                    isSimulated = false
                    source = Source.VISUALIZER
                    return out
                } catch (e: Exception) {
                    Log.w(TAG, "getWaveForm failed: ${e.message}")
                }
            }
        }

        // 3. Simulation.
        isSimulated = true
        source = Source.SIMULATION
        simulateWaveform(out)
        return out
    }

    private fun resampleWaveform(
        wave: ByteArray,
        out: FloatArray,
    ) {
        val size = out.size
        if (size <= 0) return

        val step = wave.size / size
        for (i in 0 until size) {
            val center = (i * step + step / 2).coerceIn(0, wave.size - 1)
            val signed = (wave[center].toInt() and 0xFF) - 128
            out[i] = (signed / 128f).coerceIn(-1f, 1f)
        }
    }

    private fun simulateWaveform(out: FloatArray) {
        val now = SystemClock.elapsedRealtime() / 1000.0
        val t = now * 6.0
        for (i in out.indices) {
            val x = i / out.size.toDouble() * Math.PI * 2
            val value = kotlin.math.sin(t + x) * 0.4 + kotlin.math.sin(t * 1.3 + x * 2) * 0.2
            out[i] = value.toFloat().coerceIn(-1f, 1f)
        }
        simPhase += 0.1
    }

    /**
     * Release all capture resources.
     */
    fun release() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) {
        }
        visualizer = null
        enabled = false
        playbackCapture.release()
        isSimulated = true
        source = Source.SIMULATION
    }

    private fun binFft(
        fft: ByteArray,
        out: FloatArray,
    ) {
        val bands = out.size
        if (bands <= 0) return

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
    }

    private fun simulateBands(out: FloatArray) {
        val now = SystemClock.elapsedRealtime() / 1000.0
        val beat = (now * 4.0) % (Math.PI * 2)
        val pulse = ((kotlin.math.sin(beat) + 1.0) / 2.0).toFloat() * 0.6f + 0.2f

        for (i in out.indices) {
            val offset = i * 0.4
            val wave = kotlin.math.sin(beat + offset)
            val value = (pulse + 0.3f * wave.toFloat()).coerceIn(0f, 1f)
            out[i] = value
        }
        simPhase += 0.1
    }

    private fun hasRecordAudio(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "AudioAnalyzer"
    }
}
