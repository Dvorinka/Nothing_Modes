package com.tdvorak.nothingmodes.nothing

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Captures audio from other apps using the Android 10+ AudioPlaybackCapture API.
 *
 * Unlike [Visualizer] session 0, this captures the media stream before it is
 * routed to a specific output device, so it works for headphones, Bluetooth
 * A2DP, and the built-in speaker. It still requires the playing app to allow
 * playback capture; if it blocks, the buffer stays silent and the caller should
 * fall back to [Visualizer] or simulation.
 */
class PlaybackAudioCapture(
    context: Context,
) {
    private val appContext = context.applicationContext
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var mediaProjection: MediaProjection? = null

    private var captureBuffer = ShortArray(0)
    private var writeIndex = 0
    private var samplesFilled = 0
    private val bufferLock = Any()

    private var lastRms = 0f
    private var silenceStart = 0L

    /**
     * Set or update the [MediaProjection] to use. Call before [init].
     * The projection is normally obtained from [MediaProjectionManager] and
     * held for the lifetime of the visualizer.
     */
    fun setMediaProjection(projection: MediaProjection?) {
        this.mediaProjection = projection
    }

    /**
     * Try to initialise AudioPlaybackCapture. Returns true if a capture stream
     * is actively running.
     */
    @SuppressLint("MissingPermission")
    fun init(): Boolean {
        release()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.d(TAG, "AudioPlaybackCapture requires Android 10+")
            return false
        }
        if (mediaProjection == null) {
            Log.d(TAG, "No MediaProjection provided")
            return false
        }
        if (!hasRecordAudio()) {
            Log.w(TAG, "RECORD_AUDIO not granted")
            return false
        }

        try {
            val config =
                AudioPlaybackCaptureConfiguration
                    .Builder(mediaProjection!!)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_GAME)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

            val minBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
            val bufferBytes = minBytes.coerceAtLeast(SAMPLE_RATE / 25 * BYTES_PER_SAMPLE)

            val record =
                AudioRecord
                    .Builder()
                    .setAudioFormat(
                        AudioFormat
                            .Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(ENCODING)
                            .setChannelMask(CHANNEL_CONFIG)
                            .build(),
                    ).setBufferSizeInBytes(bufferBytes)
                    .setAudioPlaybackCaptureConfig(config)
                    .build()

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return false
            }

            synchronized(bufferLock) {
                captureBuffer = ShortArray(SAMPLE_RATE)
                writeIndex = 0
                samplesFilled = 0
            }

            record.startRecording()
            audioRecord = record

            captureJob =
                scope.launch {
                    val readBuffer = ShortArray(bufferBytes / BYTES_PER_SAMPLE)
                    while (isActive) {
                        val read = record.read(readBuffer, 0, readBuffer.size)
                        if (read > 0) {
                            appendSamples(readBuffer, read)
                            updateRms(readBuffer, read)
                        }
                    }
                }

            return true
        } catch (e: Exception) {
            Log.w(TAG, "AudioPlaybackCapture init failed", e)
            return false
        }
    }

    /**
     * Release the AudioRecord, MediaProjection is not released here because it
     * may be reused for screenshot or other capture.
     */
    fun release() {
        captureJob?.cancel()
        captureJob = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
        synchronized(bufferLock) {
            captureBuffer = ShortArray(0)
            writeIndex = 0
            samplesFilled = 0
        }
        lastRms = 0f
        silenceStart = 0L
    }

    /** True when the AudioRecord is running. */
    fun isCapturing(): Boolean = audioRecord != null

    /**
     * True when the captured stream has been near-silent for longer than
     * [SILENCE_MS]. This usually means the playing app is blocking capture.
     */
    fun isSilent(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (lastRms > SILENCE_THRESHOLD) {
            silenceStart = 0L
            return false
        }
        if (silenceStart == 0L) silenceStart = now
        return now - silenceStart > SILENCE_MS
    }

    /** Returns the most recent [size] samples resampled to -1..1. */
    fun getWaveform(size: Int): FloatArray {
        val out = FloatArray(size) { 0f }
        if (size <= 0) return out

        val snapshot = synchronized(bufferLock) { latestSamples(WINDOW_SAMPLES) }
        if (snapshot.isEmpty()) return out

        val step = snapshot.size.toFloat() / size
        for (i in 0 until size) {
            val center = (i * step + step / 2).toInt().coerceIn(0, snapshot.size - 1)
            out[i] = (snapshot[center] / MAX_16BIT).coerceIn(-1f, 1f)
        }
        return out
    }

    /** Returns [bandCount] frequency-band energy values in 0..1. */
    fun getBands(bandCount: Int): FloatArray {
        val out = FloatArray(bandCount) { 0f }
        if (bandCount <= 0) return out
        if (isSilent()) return out

        val samples = synchronized(bufferLock) { latestSamples(FREQUENCY_WINDOW_SAMPLES) }
        val n = samples.size
        if (n < MIN_SAMPLES) return out

        for (band in 0 until bandCount) {
            val freq = bandFrequency(band, bandCount)
            out[band] = sqrt(goertzelPower(samples, n, freq)).toFloat().coerceIn(0f, 1f)
        }
        return out
    }

    private fun appendSamples(
        buffer: ShortArray,
        count: Int,
    ) {
        synchronized(bufferLock) {
            for (i in 0 until count) {
                captureBuffer[writeIndex] = buffer[i]
                writeIndex = (writeIndex + 1) % captureBuffer.size
                if (samplesFilled < captureBuffer.size) samplesFilled++
            }
        }
    }

    private fun latestSamples(count: Int): ShortArray {
        val n = count.coerceIn(0, samplesFilled)
        val out = ShortArray(n)
        val start = (writeIndex - n + captureBuffer.size) % captureBuffer.size
        for (i in 0 until n) {
            out[i] = captureBuffer[(start + i) % captureBuffer.size]
        }
        return out
    }

    private fun updateRms(
        buffer: ShortArray,
        count: Int,
    ) {
        var sum = 0L
        for (i in 0 until count) {
            val v = buffer[i].toInt()
            sum += v * v
        }
        lastRms = sqrt(sum.toFloat() / count) / MAX_16BIT
    }

    private fun bandFrequency(
        band: Int,
        bands: Int,
    ): Double {
        if (bands <= 1) return (MIN_FREQ + MAX_FREQ) / 2.0
        val t = band / (bands - 1).toDouble()
        return MIN_FREQ * (MAX_FREQ / MIN_FREQ).pow(t)
    }

    private fun goertzelPower(
        samples: ShortArray,
        n: Int,
        freq: Double,
    ): Double {
        val k = (0.5 + n * freq / SAMPLE_RATE).toInt().toDouble()
        val omega = 2.0 * PI * k / n
        val coeff = 2.0 * cos(omega)
        var s0 = 0.0
        var s1 = 0.0
        var s2 = 0.0
        for (i in 0 until n) {
            s0 = samples[i] / MAX_16BIT_DOUBLE + coeff * s1 - s2
            s2 = s1
            s1 = s0
        }
        return (s1 * s1 + s2 * s2 - coeff * s1 * s2) / n
    }

    private fun hasRecordAudio(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext,
            android.Manifest.permission.RECORD_AUDIO,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "PlaybackAudioCapture"
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val BYTES_PER_SAMPLE = 2
        private const val WINDOW_SAMPLES = 2048
        private const val FREQUENCY_WINDOW_SAMPLES = 2048
        private const val MIN_SAMPLES = 64
        private const val SILENCE_THRESHOLD = 0.005f
        private const val SILENCE_MS = 500L
        private const val MIN_FREQ = 60.0
        private const val MAX_FREQ = 8000.0
        private const val MAX_16BIT = 32768f
        private const val MAX_16BIT_DOUBLE = 32768.0
    }
}
