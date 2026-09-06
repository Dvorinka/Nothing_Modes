package com.tdvorak.nothingmodes.nothing

import kotlin.math.abs

/**
 * Renders a live audio waveform to a Glyph Matrix frame.
 *
 * The output is a row-major [IntArray] of size [size] x [size] where each
 * element is 0..255 brightness (scaled to 0..4095 by the matrix provider).
 *
 * The waveform is drawn as a line centred on the middle row. Positive samples
 * move toward the top, negative samples toward the bottom. When the audio
 * value is zero the pixel lands on the middle row, so the wave appears to
 * animate from the middle line. If no audio is active the provider sends a
 * blank frame, leaving the middle line empty.
 */
object MusicGlyphRenderer {

    /**
     * @param samples signed waveform samples, length must match [size].
     * @param size 13 or 25, matching the target Glyph Matrix.
     * @param brightness master brightness scalar 0..255.
     * @return a full square IntArray ready for [NothingGlyphMatrixProvider.setFrame].
     */
    fun render(
        samples: FloatArray,
        size: Int,
        brightness: Int = 255,
    ): IntArray {
        require(size == samples.size) {
            "samples size ${samples.size} must match matrix size $size"
        }

        val output = IntArray(size * size) { 0 }
        val maxBright = brightness.coerceIn(0, 255)
        val centerY = size / 2
        val halfHeight = size / 2

        var prevY = centerY

        for (col in 0 until size) {
            val sample = samples[col].coerceIn(-1f, 1f)
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
}
