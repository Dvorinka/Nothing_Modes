package com.tdvorak.nothingmodes.nothing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixFrameWithMarquee
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphMatrixUtils

/**
 * Wraps the Nothing Glyph Matrix SDK for matrix devices (Phone 3: 25x25, Phone 4a Pro: 13x13).
 *
 * Supports:
 * - Raw color frames (setAppMatrixFrame)
 * - Structured frames with GlyphMatrixObject (image, text, position, rotation, scale, brightness)
 * - Layer composition (top/mid/low, max 3 objects)
 * - Scrolling text marquee (buildWithMarquee)
 * - Visual presets (fill, percent fill, number)
 */
class NothingGlyphMatrixProvider(
    private val context: Context,
) {
    private var manager: GlyphMatrixManager? = null
    private var connected = false
    private val detector = NothingDeviceDetector(context)
    private var marquee: GlyphMatrixFrameWithMarquee? = null
    private var marqueeHandler: Handler? = null

    @Volatile
    private var marqueeRunning = false

    fun isAvailable(): Boolean = detector.detectGlyphHardware().isMatrix

    fun matrixSize(): Int = detector.detectGlyphHardware().matrixSize

    fun init(
        onConnected: () -> Unit = {},
        onDisconnected: () -> Unit = {},
    ) {
        if (!isAvailable()) return
        try {
            manager = GlyphMatrixManager.getInstance(context)
            manager?.init(
                object : GlyphMatrixManager.Callback {
                    override fun onServiceConnected(componentName: android.content.ComponentName) {
                        connected = true
                        val device =
                            when (detector.detectGlyphHardware()) {
                                GlyphHardware.MATRIX_25 -> Glyph.DEVICE_23112
                                GlyphHardware.MATRIX_13 -> Glyph.DEVICE_25111p
                                else -> return
                            }
                        try {
                            manager?.register(device)
                        } catch (e: Exception) {
                            Log.e(TAG, "register failed", e)
                        }
                        onConnected()
                    }

                    override fun onServiceDisconnected(componentName: android.content.ComponentName) {
                        connected = false
                        onDisconnected()
                    }
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
        }
    }

    fun unInit() {
        stopMarquee()
        try {
            manager?.unInit()
        } catch (e: Exception) {
            Log.e(TAG, "unInit failed", e)
        }
        connected = false
        manager = null
    }

    // ── Raw color frame ──

    fun setFrame(colors: IntArray): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        val expected = matrixSize() * matrixSize()
        if (colors.size != expected) return GlyphResult.Failure("Expected $expected colors, got ${colors.size}")
        return try {
            // The service expects per-dot brightness 0..4095, NOT ARGB.
            // ARGB (e.g. 0xFFFFFFFF) or a 0..255 value renders near-black —
            // the threshold path snaps anything < 1024 to off.
            manager?.setAppMatrixFrame(IntArray(colors.size) { i -> toDotBrightness(colors[i]) })
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "setFrame failed")
        }
    }

    /**
     * Normalize any caller value to the matrix's 0..4095 brightness scale.
     * 0..255 -> treated as 8-bit brightness and scaled up; anything with an
     * alpha byte (ARGB) -> luminance first, then scaled.
     */
    private fun toDotBrightness(value: Int): Int {
        val eightBit =
            when {
                value in 0..255 -> value
                value < 0 -> { // ARGB int
                    val r = (value shr 16) and 0xFF
                    val g = (value shr 8) and 0xFF
                    val b = value and 0xFF
                    (r * 299 + g * 587 + b * 114) / 1000
                }
                else -> return value.coerceIn(0, 4095) // already wide-range
            }
        return (eightBit * 4095 / 255).coerceIn(0, 4095)
    }

    fun closeFrame(): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            manager?.closeAppMatrix()
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "closeFrame failed")
        }
    }

    fun turnOff(): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            stopMarquee()
            manager?.turnOff()
            // Push a real black frame — turnOff() alone leaves the last
            // frame on the app layer — then release the layer entirely.
            val size = matrixSize()
            if (size > 0) manager?.setAppMatrixFrame(IntArray(size * size))
            manager?.closeAppMatrix()
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "turnOff failed")
        }
    }

    // ── Structured frames (GlyphMatrixObject) ──

    fun displayText(
        text: String,
        x: Int = -1,
        y: Int = -1,
        scale: Int = 100,
        brightness: Int = 255,
    ): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            // (0,0) is the grid's top-left corner — outside the round matrix.
            // Center: each glyph ~5 dots wide +1 spacing, ~7 dots tall.
            val size = matrixSize()
            val px = if (x >= 0) x else ((size - text.length * 6) / 2).coerceIn(0, size)
            val py = if (y >= 0) y else (size - 7) / 2
            val obj =
                GlyphMatrixObject
                    .Builder()
                    .setText(text)
                    .setPosition(px, py)
                    .setScale(scale)
                    .setBrightness(brightness)
                    .build()
            val frame =
                GlyphMatrixFrame
                    .Builder()
                    .addTop(obj)
                    .build(context)
            manager?.setAppMatrixFrame(frame)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "displayText failed")
        }
    }

    fun displayImage(
        bitmap: Bitmap,
        x: Int = 0,
        y: Int = 0,
        scale: Int = 100,
        brightness: Int = 255,
    ): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            val obj =
                GlyphMatrixObject
                    .Builder()
                    .setImageSource(bitmap)
                    .setPosition(x, y)
                    .setScale(scale)
                    .setBrightness(brightness)
                    .build()
            val frame =
                GlyphMatrixFrame
                    .Builder()
                    .addTop(obj)
                    .build(context)
            manager?.setAppMatrixFrame(frame)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "displayImage failed")
        }
    }

    fun displayLayers(
        top: GlyphMatrixObject? = null,
        mid: GlyphMatrixObject? = null,
        low: GlyphMatrixObject? = null,
    ): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            val builder = GlyphMatrixFrame.Builder()
            if (low != null) builder.addLow(low)
            if (mid != null) builder.addMid(mid)
            if (top != null) builder.addTop(top)
            val frame = builder.build(context)
            manager?.setAppMatrixFrame(frame)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "displayLayers failed")
        }
    }

    // ── Scrolling text (Marquee) ──

    /**
     * Display scrolling text on the Glyph Matrix.
     * Uses buildWithMarquee — the text scrolls horizontally.
     * @param text Text to scroll
     * @param intervalMs Delay between marquee ticks (lower = faster)
     * @param stepPx Matrix dots shifted per tick
     */
    fun displayScrollingText(
        text: String,
        intervalMs: Int = 100,
        stepPx: Int = 1,
    ): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            stopMarquee()
            // Marquee text is ~7 dots tall; center it vertically on the grid.
            val y = (matrixSize() - 7) / 2
            val obj =
                GlyphMatrixObject
                    .Builder()
                    .setText(text, GlyphMatrixObject.TYPE_MARQUEE_FORCE)
                    .setPosition(0, y)
                    .build()
            val builder = GlyphMatrixFrame.Builder().addTop(obj)
            val handler = Handler(Looper.getMainLooper())
            // Each tick hands us a rendered frame — we must push it ourselves.
            // The SDK reposts its tick without checking the running flag, so
            // the listener gates on marqueeRunning to survive the stop race.
            val marqueeFrame =
                builder.buildWithMarquee(
                    context,
                    handler,
                    intervalMs,
                    stepPx,
                ) { frame ->
                    if (marqueeRunning) manager?.setAppMatrixFrame(frame)
                }
            marquee = marqueeFrame
            marqueeHandler = handler
            marqueeRunning = true
            manager?.setAppMatrixFrame(marqueeFrame)
            marqueeFrame.startMarquee()
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "displayScrollingText failed")
        }
    }

    /** Stop any active marquee animation. */
    fun stopMarquee() {
        marqueeRunning = false
        val frame = marquee
        val handler = marqueeHandler
        marquee = null
        marqueeHandler = null
        if (frame != null) {
            // Post onto the marquee's own looper so the stop serializes
            // against an in-flight tick — a running tick reposts the next
            // one, and removing the pending callback is then effective.
            try {
                (handler ?: Handler(Looper.getMainLooper())).post { frame.stopMarquee() }
            } catch (_: Exception) {
            }
        }
    }

    // ── Visual presets ──

    fun fillMatrix(color: Int): GlyphResult {
        val size = matrixSize()
        if (size == 0) return GlyphResult.Unsupported
        val colors = IntArray(size * size) { color }
        return setFrame(colors)
    }

    fun displayPercentFill(
        percent: Int,
        color: Int = Color.WHITE,
    ): GlyphResult {
        val size = matrixSize()
        if (size == 0) return GlyphResult.Unsupported
        val clamped = percent.coerceIn(0, 100)
        val fillRows = (size * clamped) / 100
        val colors =
            IntArray(size * size) { index ->
                val row = index / size
                val fromBottom = size - 1 - row
                if (fromBottom < fillRows) color else 0
            }
        return setFrame(colors)
    }

    fun displayNumber(number: Int): GlyphResult {
        val size = matrixSize()
        if (size == 0) return GlyphResult.Unsupported
        val clamped = number.coerceIn(0, 99)
        return displayText(clamped.toString(), x = size / 4, y = size / 4, scale = 100)
    }

    fun drawableToBitmap(
        context: Context,
        drawableRes: Int,
    ): Bitmap? =
        try {
            GlyphMatrixUtils.drawableToBitmap(context.getDrawable(drawableRes))
        } catch (e: Exception) {
            Log.e(TAG, "drawableToBitmap failed", e)
            null
        }

    companion object {
        private const val TAG = "NothingGlyphMatrix"
    }
}
