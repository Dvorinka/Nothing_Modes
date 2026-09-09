package com.tdvorak.nothingmodes.nothing

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Renders live audio data to a Glyph Matrix frame.
 *
 * The output is a row-major [IntArray] of size [size] x [size] where each
 * element is 0..255 brightness (scaled to 0..4095 by the matrix provider).
 */
object MusicGlyphRenderer {
    private const val STYLE_WAVEFORM = "waveform"
    private const val STYLE_BARS = "bars"
    private const val STYLE_MIRROR = "mirror"
    private const val STYLE_PULSE = "pulse"
    private const val STYLE_VINYL = "vinyl"

    private val ALL_STYLES = setOf(STYLE_WAVEFORM, STYLE_BARS, STYLE_MIRROR, STYLE_PULSE, STYLE_VINYL)

    private fun normalizeStyle(style: String?): String = style?.takeIf { it in ALL_STYLES } ?: STYLE_WAVEFORM

    /**
     * @param wave signed waveform samples, length must match [size].
     * @param bands energy per band, length must match [size].
     * @param tick monotonically increasing frame counter for animations.
     * @param brightness master brightness scalar 0..255.
     * @return a full square IntArray ready for [NothingGlyphMatrixProvider.setFrame].
     */
    fun render(
        style: String,
        wave: FloatArray,
        bands: FloatArray,
        size: Int,
        tick: Int,
        brightness: Int = 255,
    ): IntArray {
        require(size == wave.size) { "wave size ${wave.size} must match matrix size $size" }
        require(size == bands.size) { "bands size ${bands.size} must match matrix size $size" }

        return when (normalizeStyle(style)) {
            STYLE_BARS -> renderBars(wave, bands, size, brightness)
            STYLE_MIRROR -> renderMirror(wave, size, brightness)
            STYLE_PULSE -> renderPulse(wave, bands, size, brightness)
            STYLE_VINYL -> renderVinyl(wave, bands, size, tick, brightness)
            else -> renderWaveform(wave, size, brightness)
        }
    }

    private fun renderWaveform(
        wave: FloatArray,
        size: Int,
        brightness: Int,
    ): IntArray {
        val output = IntArray(size * size) { 0 }
        val maxBright = brightness.coerceIn(0, 255)
        val centerY = size / 2
        val halfHeight = size / 2

        var prevY = centerY

        for (col in 0 until size) {
            val sample = wave[col].coerceIn(-1f, 1f)
            // Positive -> up (smaller row index), negative -> down (larger row index).
            val y = (centerY - (sample * halfHeight)).toInt().coerceIn(0, size - 1)

            // Draw the point and a short vertical segment to the previous point
            // so the wave does not look like disconnected dots.
            val y0 = minOf(prevY, y)
            val y1 = maxOf(prevY, y)
            for (row in y0..y1) {
                val dist = abs(row - centerY).toFloat() / halfHeight.coerceAtLeast(1)
                val bright = (maxBright * (1f - dist * 0.4f)).toInt().coerceIn(0, 255)
                output[row * size + col] = bright
            }

            prevY = y
        }

        return output
    }

    /** Frequency-band bars rising from the bottom, like a classic equalizer. */
    private fun renderBarsInternal(
        energyBands: FloatArray,
        size: Int,
        brightness: Int,
    ): IntArray {
        val output = IntArray(size * size) { 0 }
        val maxBright = brightness.coerceIn(0, 255)

        for (col in 0 until size) {
            val energy = energyBands[col].coerceIn(0f, 1f)
            val barHeight = (energy * (size - 1)).toInt().coerceIn(0, size - 1)
            val bright = (maxBright * 0.2f + energy * maxBright * 0.8f).toInt().coerceIn(0, 255)

            for (row in 0..barHeight) {
                val dist = 1f - row.toFloat() / (size - 1).coerceAtLeast(1)
                output[(size - 1 - row) * size + col] = (bright * (0.4f + 0.6f * dist)).toInt()
            }
        }

        return output
    }

    private fun renderBars(
        wave: FloatArray,
        bands: FloatArray,
        size: Int,
        brightness: Int,
    ): IntArray {
        // Prefer energy from bands when available, fall back to wave amplitude.
        val energy = if (bands.average() > 0.01) bands else FloatArray(size) { abs(wave[it]) }
        return renderBarsInternal(energy, size, brightness)
    }

    /** Frequency-band bars mirrored from the centre line, expanding up and down. */
    private fun renderMirror(
        wave: FloatArray,
        size: Int,
        brightness: Int,
    ): IntArray {
        val output = IntArray(size * size) { 0 }
        val maxBright = brightness.coerceIn(0, 255)
        val centerY = size / 2

        for (col in 0 until size) {
            val energy = abs(wave[col]).coerceIn(0f, 1f)
            val halfHeight = (energy * centerY).toInt().coerceIn(0, centerY)

            for (offset in 0..halfHeight) {
                val bright = (maxBright * (1f - offset.toFloat() / centerY.coerceAtLeast(1))).toInt()
                val top = (centerY - offset).coerceIn(0, size - 1)
                val bottom = (centerY + offset).coerceIn(0, size - 1)
                output[top * size + col] = bright
                if (bottom != top) output[bottom * size + col] = bright
            }
        }

        return output
    }

    /** A pulsing circle that grows with the overall audio energy. */
    private fun renderPulse(
        wave: FloatArray,
        bands: FloatArray,
        size: Int,
        brightness: Int,
    ): IntArray {
        val output = IntArray(size * size) { 0 }
        val maxBright = brightness.coerceIn(0, 255)
        val center = size / 2
        val maxRadius = (size / 2) - 1

        val waveEnergy = wave.map { it * it }.average().toFloat()
        val bandEnergy = bands.average().toFloat()
        val energy = (0.6f * bandEnergy + 0.4f * waveEnergy).coerceIn(0f, 1f)

        val radius = (energy * maxRadius).toInt().coerceIn(0, maxRadius)

        // Draw filled circle with brightness fall-off from centre.
        for (row in 0 until size) {
            for (col in 0 until size) {
                val d = hypot((col - center).toFloat(), (row - center).toFloat())
                if (d <= radius) {
                    val falloff = 1f - d / (radius.coerceAtLeast(1)).toFloat()
                    output[row * size + col] = (maxBright * falloff).toInt().coerceIn(0, 255)
                }
            }
        }

        // Bright centre dot on strong beats.
        if (energy > 0.5) {
            output[center * size + center] = maxBright
        }

        return output
    }

    /** A spinning ring with a small dot orbiting the centre; ring size reacts to bass. */
    private fun renderVinyl(
        wave: FloatArray,
        bands: FloatArray,
        size: Int,
        tick: Int,
        brightness: Int,
    ): IntArray {
        val output = IntArray(size * size) { 0 }
        val maxBright = brightness.coerceIn(0, 255)
        val center = size / 2
        val maxRadius = (size / 2) - 1

        // Use lower bands for the ring pulse, overall wave for the dot brightness.
        val bass = bands.take((size / 2).coerceAtLeast(1)).average().toFloat()
        val amplitude = abs(wave.average().toFloat()).coerceIn(0f, 1f)

        val baseRadius = (maxRadius * 0.6f).toInt()
        val pulseRadius = baseRadius + (bass * (maxRadius - baseRadius)).toInt()

        // Draw ring.
        for (row in 0 until size) {
            for (col in 0 until size) {
                val d = hypot((col - center).toFloat(), (row - center).toFloat())
                val ringWidth = 1.2f
                if (d in (pulseRadius - ringWidth)..(pulseRadius + ringWidth)) {
                    val edgeFade = 1f - abs(d - pulseRadius) / ringWidth
                    output[row * size + col] = (maxBright * edgeFade).toInt().coerceIn(0, 255)
                }
            }
        }

        // Spinning dot at the edge of the ring.
        val angle = tick * 0.2f
        val dotX = (center + pulseRadius * cos(angle)).toInt().coerceIn(0, size - 1)
        val dotY = (center + pulseRadius * sin(angle)).toInt().coerceIn(0, size - 1)
        val dotBright = (maxBright * (0.3f + 0.7f * amplitude)).toInt()
        output[dotY * size + dotX] = dotBright

        return output
    }
}
