package com.tdvorak.nothingmodes.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.nothing.GlyphIconLibrary
import com.tdvorak.nothingmodes.nothing.GlyphRasterizer
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val GRID = 25
private const val GRID_PIXELS = GRID * GRID
private val LED_OFF = Color(0xFF1B1B1B)
private val LED_ON = Color(0xFFEDEDED)
private val PREVIEW_BG = Color(0xFF060606)
private const val LED_BRIGHT_MAX = 4095

/**
 * A WYSIWYG preview of a glyph action rendered on a 25x25 LED grid like the
 * real Glyph Matrix — the frames come from the app's own rasterizer and icon
 * library, so the preview is what the hardware will show.
 */
@Composable
fun GlyphMatrixPreview(
    action: Action,
    modifier: Modifier = Modifier,
) {
    val animated = action is Action.GlyphMusic || action is Action.GlyphAnimate
    val phase by
        rememberInfiniteTransition(label = "glyph").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "glyph-phase",
        )
    // Static frames are rasterized once per action; animated ones are drawn per frame.
    val staticFrame = remember(action) { if (animated) null else matrixFrame(action, 0f) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MATRIX PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
            if (animated) {
                Text(
                    text = "  • LIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingColors.accent,
                    fontFamily = NothingFonts.mono(),
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = NothingSpacing.xs)
                    .aspectRatio(1f)
                    .clip(NothingShapes.input)
                    .background(PREVIEW_BG)
                    .border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.input),
        ) {
            Canvas(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(NothingSpacing.lg),
            ) {
                val frame = staticFrame ?: matrixFrame(action, phase)
                val cell = min(size.width, size.height) / GRID
                val center = size.minDimension / 2f
                val radius = size.minDimension / 2f
                for (y in 0 until GRID) {
                    for (x in 0 until GRID) {
                        val cx = (x + 0.5f) * cell
                        val cy = (y + 0.5f) * cell
                        // The matrix is a disc — skip dots outside it.
                        if (sqrt((cx - center) * (cx - center) + (cy - center) * (cy - center)) > radius) continue
                        val argb = frame[y * GRID + x]
                        val color = if (argb == 0) LED_OFF else Color(argb)
                        drawCircle(color = color, radius = cell * 0.34f, center = Offset(cx, cy))
                    }
                }
            }
        }
    }
}

/** Builds the 25x25 ARGB frame for [action]; [phase] is 0..1 for animated actions. */
private fun matrixFrame(
    action: Action,
    phase: Float,
): IntArray =
    when (action) {
        is Action.SetGlyphMatrix -> {
            val colors = action.colors
            if (action.restore || colors.isNullOrEmpty()) {
                IntArray(GRID_PIXELS)
            } else {
                IntArray(GRID_PIXELS) { i ->
                    val v = colors.getOrElse(i) { 0 }
                    when {
                        v <= 0 -> 0
                        v <= LED_BRIGHT_MAX ->
                            // Brightness values 1..4095 → white LED scaled.
                            (0xFF000000.toInt() or scaleWhite(v / LED_BRIGHT_MAX.toFloat()))
                        else -> 0xFF000000.toInt() or (v and 0xFFFFFF)
                    }
                }
            }
        }
        is Action.GlyphText -> rasterized(action.text.take(8), 0.5f)
        is Action.GlyphScrollingText -> rasterized(action.text.take(5), 0.6f)
        is Action.GlyphIcon -> iconFrame(action.name)
        is Action.GlyphNumber -> rasterized(action.number.coerceIn(0, 99).toString(), 0.62f)
        is Action.GlyphCountdown -> {
            val s = action.seconds.coerceIn(1, 599)
            rasterized(String.format("%d:%02d", s / 60, s % 60), 0.5f)
        }
        is Action.GlyphPreset ->
            GlyphIconLibrary.frameFor(action.preset)?.toArgb()
                ?: rasterized(action.preset.take(1).uppercase(), 0.6f)
        is Action.GlyphProgress -> progressFrame(action.progress, action.reverse)
        is Action.GlyphMusic -> musicFrame(phase)
        is Action.GlyphAnimate -> breatheFrame(phase)
        is Action.SetGlyph -> stripeFrame(action)
        is Action.GlyphTurnOff -> IntArray(GRID_PIXELS)
        else -> IntArray(GRID_PIXELS)
    }

private fun rasterized(
    text: String,
    fontScale: Float,
): IntArray {
    val t = text.ifBlank { "·" }
    return runCatching { GlyphRasterizer.rasterize(t, GRID, fontScale).toArgb() }
        .getOrElse { IntArray(GRID_PIXELS) }
}

private fun iconFrame(name: String): IntArray =
    GlyphIconLibrary.frameFor(name)?.toArgb()
        ?: rasterized(name.take(1).uppercase(), 0.6f)

/** Rasterizer output (0/4095) → ARGB white LED. */
private fun IntArray.toArgb(): IntArray =
    IntArray(size) { i -> if (this[i] > 0) 0xFFEDEDED.toInt() else 0 }

private fun scaleWhite(intensity: Float): Int {
    val v = (intensity * 255).coerceIn(0f, 255f).toInt()
    return (v shl 16) or (v shl 8) or v
}

/** Nothing's progress mode fills the matrix bottom-up like a gauge. */
private fun progressFrame(
    progress: Int,
    reverse: Boolean,
): IntArray {
    val litRows = ((GRID * progress.coerceIn(0, 100)) / 100f)
    return IntArray(GRID_PIXELS) { i ->
        val row = i / GRID
        val lit = if (reverse) row < litRows else row >= GRID - litRows
        if (lit) 0xFFEDEDED.toInt() else 0
    }
}

/** Animated equalizer bars across the grid, driven by [phase]. */
private fun musicFrame(phase: Float): IntArray {
    val out = IntArray(GRID_PIXELS)
    val bars = 9
    val band = GRID / bars
    for (b in 0 until bars) {
        val h = 4 + (sin(phase * 2 * Math.PI + b * 1.25).toFloat() * 0.5f + 0.5f) * (GRID - 10)
        for (x in b * band + band / 4 until (b + 1) * band - band / 4) {
            for (y in 0 until GRID) {
                if (GRID - 1 - y < h) out[y * GRID + x] = 0xFFEDEDED.toInt()
            }
        }
    }
    return out
}

/** Breathing pulse over the whole matrix. */
private fun breatheFrame(phase: Float): IntArray {
    val level = 0.25f + 0.75f * abs(sin(phase * 2 * Math.PI)).toFloat()
    val argb = 0xFF000000.toInt() or scaleWhite(level)
    return IntArray(GRID_PIXELS) { argb }
}

/** The light stripe (Phone 1/2) is not the matrix — render it as lit segments. */
private fun stripeFrame(action: Action.SetGlyph): IntArray {
    if (!action.on) return IntArray(GRID_PIXELS)
    val out = IntArray(GRID_PIXELS)
    // Five horizontal "zones" like the physical stripe segments.
    for (z in 0 until 5) {
        val y = 3 + z * 5
        for (x in 6 until GRID - 6) {
            out[y * GRID + x] = 0xFFEDEDED.toInt()
            out[(y + 1) * GRID + x] = 0xFFEDEDED.toInt()
        }
    }
    return out
}
