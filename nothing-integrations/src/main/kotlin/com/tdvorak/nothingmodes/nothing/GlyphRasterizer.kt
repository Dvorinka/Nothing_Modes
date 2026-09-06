package com.tdvorak.nothingmodes.nothing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

/**
 * Rasterizes emoji/text to a Glyph Matrix int[] frame — the same technique as
 * the GlyphMatrixEditor web tool (GPL-3.0, github.com/pauwma/GlyphMatrixEditor):
 * draw the glyph big on a canvas, read pixels, threshold luminance.
 *
 * The system emoji font already ships on Android, so no font asset is needed.
 * Output pixels are 0 or 4095 — the matrix's on/off range.
 */
object GlyphRasterizer {
    private const val ON = 4095
    private const val OFF = 0
    private const val THRESHOLD = 96 // alpha-weighted luminance cutoff (editor uses ~128)

    /**
     * Render [text] (an emoji or short string) centered in a size×size matrix.
     * [fontScale] is the fraction of the canvas the glyph should roughly fill —
     * emoji look best near 0.72, short text near 0.55.
     */
    fun rasterize(
        text: String,
        size: Int,
        fontScale: Float = 0.72f,
        typeface: Typeface = Typeface.DEFAULT,
    ): IntArray {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                this.typeface = typeface
            }
        // Fit text to the canvas: measure at a reference size, scale to target.
        paint.textSize = 100f
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val ref = maxOf(bounds.width(), bounds.height()).coerceAtLeast(1)
        paint.textSize = 100f * (size * fontScale) / ref
        // Vertically center via the font metrics, horizontally via CENTER align.
        val cy = size / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, size / 2f, cy, paint)

        val out = IntArray(size * size)
        val px = IntArray(size * size)
        bmp.getPixels(px, 0, size, 0, 0, size, size)
        for (i in px.indices) {
            val a = (px[i] ushr 24) and 0xFF
            if (a == 0) continue
            val r = (px[i] shr 16) and 0xFF
            val g = (px[i] shr 8) and 0xFF
            val b = px[i] and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            out[i] = if ((lum * a) / 255 > THRESHOLD) ON else OFF
        }
        return out
    }

    /**
     * Convert an arbitrary image to a size×size matrix frame — the editor's
     * image import. Scales the bitmap to the grid, keeps alpha-weighted
     * luminance, thresholds at [THRESHOLD]. Values are 0 or 4095.
     */
    fun bitmapToMatrix(bitmap: Bitmap, size: Int): IntArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val out = IntArray(size * size)
        val px = IntArray(size * size)
        scaled.getPixels(px, 0, size, 0, 0, size, size)
        for (i in px.indices) {
            val a = (px[i] ushr 24) and 0xFF
            if (a == 0) continue
            val r = (px[i] shr 16) and 0xFF
            val g = (px[i] shr 8) and 0xFF
            val b = px[i] and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            out[i] = if ((lum * a) / 255 > THRESHOLD) ON else OFF
        }
        return out
    }
}
