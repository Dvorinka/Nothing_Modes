package com.tdvorak.nothingmodes.nothing

/**
 * Renders live audio bands to a Glyph Matrix equalizer frame.
 *
 * The output is a row-major [IntArray] of size [size] x [size] where each
 * element is 0..4095 brightness. Values below 0 are treated as ARGB by the
 * matrix provider, so this renderer already emits 0..255 and lets
 * [NothingGlyphMatrixProvider.setFrame] scale them.
 */
object MusicGlyphRenderer {

    /**
     * @param bands energy per column, length should match [size].
     * @param size 13 or 25, matching the target Glyph Matrix.
     * @param brightness master brightness scalar 0..255.
     * @param simulated when true, the equalizer is generated, not live.
     * @return a full square IntArray ready for [NothingGlyphMatrixProvider.setFrame].
     */
    fun render(
        bands: FloatArray,
        size: Int,
        brightness: Int = 255,
        simulated: Boolean = false,
    ): IntArray {
        require(size == bands.size || size == 25 || size == 13) {
            "bands size ${bands.size} must match matrix size $size"
        }

        val output = IntArray(size * size) { 0 }
        val maxBright = brightness.coerceIn(0, 255)

        for (col in 0 until size) {
            val band = if (col < bands.size) bands[col] else 0f
            val height = (band * size).toInt().coerceIn(0, size)

            // Fill the column from the bottom up.
            for (row in 0 until height) {
                val visualRow = size - 1 - row
                val idx = visualRow * size + col

                // Dim the top of the bar for a smoother look.
                val dim = if (row == height - 1 && height > 1) 0.7f else 1.0f
                // Simulated mode is slightly softer so users can tell it's a
                // fallback when permission is missing.
                val simDim = if (simulated) 0.75f else 1.0f

                val value = (maxBright * dim * simDim * band.coerceIn(0f, 1f)).toInt().coerceIn(0, 255)
                if (value > 0) output[idx] = value
            }
        }

        return output
    }
}
