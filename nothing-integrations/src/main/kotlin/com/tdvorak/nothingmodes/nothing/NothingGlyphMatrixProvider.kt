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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private val countdownScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var countdownJob: Job? = null
    private var designJob: Job? = null
    private var musicJob: Job? = null

    private val toysBridge by lazy { GlyphToysBridge(context) }
    private val audioAnalyzer by lazy { AudioAnalyzer(context) }

    fun isAvailable(): Boolean = detector.detectGlyphHardware().isMatrix

    fun isConnected(): Boolean = connected

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
        stopCountdown()
        stopDesign()
        stopMusicVisualizer()
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
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
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
            stopCountdown()
            stopDesign()
            stopMusicVisualizer()
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
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
        return try {
            // (0,0) is the grid's top-left corner — outside the round matrix.
            // NDot glyphs are ~5 dots wide with 1-dot spacing, ~7 dots tall.
            val size = matrixSize()
            val textWidth = text.length * 5 + (text.length - 1).coerceAtLeast(0)
            val px = if (x >= 0) x else ((size - textWidth) / 2).coerceIn(0, size)
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
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
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
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
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
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
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

    /**
     * Circular progress arc — the same renderer Nothing's battery/loading
     * toys use. `icon` is an optional small sprite (int[][]) stamped center.
     */
    fun displayProgressArc(
        percent: Int,
        brightness: Int = 4095,
        reverse: Boolean = false,
        icon: Array<IntArray>? = null,
        label: String? = null,
    ): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
        val size = matrixSize()
        if (size == 0) return GlyphResult.Unsupported
        return try {
            // Signature: (size, brightness, percent, reverse, icon) — arg2 is
            // the 0..100 value; passing brightness there renders a full ring.
            val colors =
                GlyphMatrixUtils.generateMatrixProgress(
                    size,
                    brightness,
                    percent.coerceIn(0, 100),
                    reverse,
                    icon,
                )
            if (label.isNullOrEmpty()) {
                manager?.setAppMatrixFrame(colors)
            } else {
                // Baked 5x7 pixel digits — the SDK's NDot table renders a
                // closed-loop "9" and only ~5px-tall glyphs. Contrast merge:
                // dark cutout where the arc is lit, lit where it is off.
                val mask = IntArray(size * size)
                if (!GlyphDigitFont.supports(label)) {
                    manager?.setAppMatrixFrame(colors)
                } else {
                    val w = GlyphDigitFont.measure(label)
                    val lx = ((size - w) / 2).coerceAtLeast(0)
                    val ly = ((size - GlyphDigitFont.GLYPH_H) / 2).coerceAtLeast(0)
                    GlyphDigitFont.draw(label, mask, size, lx, ly, 1)
                    val merged =
                        IntArray(colors.size) { i ->
                            if (mask[i] > 0) {
                                if (colors[i] > 0) 0 else brightness
                            } else {
                                colors[i]
                            }
                        }
                    if (Log.isLoggable(TAG, Log.DEBUG)) dumpFrame("arc+label", merged, size)
                    manager?.setAppMatrixFrame(merged)
                }
            }
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "displayProgressArc failed")
        }
    }

    /** ASCII-art dump of a frame for visual debugging without eyes on the device. */
    private fun dumpFrame(
        label: String,
        colors: IntArray,
        size: Int,
    ) {
        val sb = StringBuilder("$label ${size}x$size:\n")
        for (y in 0 until size) {
            for (x in 0 until size) {
                val v = colors.getOrElse(y * size + x) { 0 }
                sb.append(if (v > 0) '#' else '.')
            }
            sb.append('\n')
        }
        Log.d(TAG, sb.toString())
    }

    private fun centeredX(size: Int, text: String): Int {
        val w = text.length * 5 + (text.length - 1).coerceAtLeast(0)
        return ((size - w) / 2).coerceIn(0, size)
    }

    fun displayNumber(number: Int): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
        val size = matrixSize()
        if (size == 0) return GlyphResult.Unsupported
        val text = number.coerceIn(0, 99).toString()
        return try {
            val frame = IntArray(size * size)
            val x = ((size - GlyphDigitFont.measure(text)) / 2).coerceAtLeast(0)
            val y = ((size - GlyphDigitFont.GLYPH_H) / 2).coerceAtLeast(0)
            GlyphDigitFont.draw(text, frame, size, x, y, 4095)
            manager?.setAppMatrixFrame(frame)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "displayNumber failed")
        }
    }

    private val presets by lazy { GlyphMuseumPresets(context) }

    /**
     * Display an icon by name. Resolution order: saved custom design
     * ([CustomGlyphStore]) → bundled Glyph Museum presets
     * ([GlyphMuseumPresets]) → emoji icon table ([GlyphIconLibrary]) → raw
     * emoji passthrough. Animated designs loop until [stopDesign] or another
     * glyph replaces them; static designs remain lit.
     */
    fun displayIcon(name: String): GlyphResult {
        stopMusicVisualizer()
        CustomGlyphStore(context).design(name)?.let { return displayDesign(it, loop = true) }
        presets.design(name)?.let { return displayDesign(it, loop = true) }
        val frame = GlyphIconLibrary.frameFor(name)
            ?: return GlyphResult.Failure("Unknown icon: $name")
        if (Log.isLoggable(TAG, Log.DEBUG)) dumpFrame("icon:$name", frame, matrixSize())
        return setFrame(frame)
    }

    /**
     * Display a decoded open-format design. Static designs push one frame;
     * animations play each frame at its `d` duration (default 100ms).
     * When [loop] is true the animation repeats until cancelled; otherwise
     * it plays once and stops.
     */
    fun displayDesign(
        design: GlyphFrameCodec.Design,
        loop: Boolean = false,
    ): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
        val size = matrixSize()
        val target =
            if (design.gridSize == size) design else GlyphFrameCodec.rescaleDesign(design, size)
        if (target.frames.size == 1) {
            stopMusicVisualizer()
            return setFrame(target.frames[0].pixels)
        }
        stopMusicVisualizer()
        stopDesign()
        designJob =
            countdownScope.launch {
                while (isActive) {
                    for (frame in target.frames) {
                        if (!isActive) break
                        if (setFrame(frame.pixels) !is GlyphResult.Success) break
                        delay((frame.durationMs ?: 100).toLong().coerceIn(20, 60_000))
                    }
                    if (!loop) break
                }
            }
        return GlyphResult.Success
    }

    /** Stop any active design animation. Safe to call when none is running. */
    fun stopDesign() {
        designJob?.cancel()
        designJob = null
    }

    /**
     * Start a live music-reactive waveform on the matrix.
     *
     * Requires [android.Manifest.permission.RECORD_AUDIO] for real capture;
     * falls back to a gentle simulated waveform when denied or unsupported.
     * The visualizer runs until another glyph action, [turnOff], or
     * [stopMusicVisualizer] cancels it.
     */
    fun startMusicVisualizer(): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }

        stopMusicVisualizer()
        stopDesign()

        val ok = audioAnalyzer.init()
        if (!ok) {
            // Even without permission we still try to show a simulated
            // waveform while music is playing.
            Log.w(TAG, "Audio visualizer not available — waveform will simulate")
        }

        musicJob =
            countdownScope.launch {
                while (isActive) {
                    if (!audioAnalyzer.isMusicActive) {
                        // Draw nothing while music is paused.
                        setFrame(IntArray(matrixSize() * matrixSize()))
                        delay(200)
                        continue
                    }

                    val size = matrixSize()
                    val samples = audioAnalyzer.getWaveform(size)
                    val frame = MusicGlyphRenderer.render(
                        samples = samples,
                        size = size,
                    )
                    if (setFrame(frame) !is GlyphResult.Success) break
                    delay(30)
                }
            }
        return GlyphResult.Success
    }

    /** Stop the live music visualizer and release its audio resources. */
    fun stopMusicVisualizer() {
        musicJob?.cancel()
        musicJob = null
        audioAnalyzer.release()
    }

    /**
     * Countdown timer: render the remaining seconds on the matrix, ticking
     * once per second. Runs on a private [CoroutineScope] cancelled by
     * [stopCountdown] (also called from [turnOff] and [unInit]).
     *
     * @param seconds Total seconds, coerced to 1..599.
     * @param onTick Optional callback fired each second with the remaining value.
     */
    fun displayCountdown(
        seconds: Int,
        onTick: ((remaining: Int) -> Unit)? = null,
    ): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        if (!toysBridge.ownsMatrix()) {
            return GlyphResult.Failure("matrix owned by another toy")
        }
        stopCountdown()
        val total = seconds.coerceIn(1, 599)
        // Render the initial value immediately so the user sees feedback
        // before the first 1-second tick elapses.
        val initial = displayNumber(total)
        if (initial !is GlyphResult.Success) return initial
        countdownJob =
            countdownScope.launch {
                var remaining = total
                while (isActive && remaining > 0) {
                    delay(1000)
                    if (!isActive) break
                    remaining = (remaining - 1).coerceAtLeast(0)
                    val r = displayNumber(remaining)
                    if (r !is GlyphResult.Success) break
                    onTick?.invoke(remaining)
                }
            }
        return GlyphResult.Success
    }

    /** Stop any active countdown loop. Safe to call when none is running. */
    fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
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
