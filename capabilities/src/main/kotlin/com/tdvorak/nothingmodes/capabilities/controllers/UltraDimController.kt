package com.tdvorak.nothingmodes.capabilities.controllers

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager

/**
 * Ultra-dim overlay: a black, untouchable window over the whole screen so the
 * display can go darker than the hardware minimum.
 *
 * Two window hosts, picked per attach:
 *  - [DimOverlayAccessibilityService] (preferred): TYPE_ACCESSIBILITY_OVERLAY
 *    is a trusted overlay — touches pass through unflagged, so secure UIs
 *    (sign-in sheets, permission dialogs, account pickers) keep working at any
 *    intensity. Also immune to the API-31 cutoff that drops ALL touches once
 *    an untrusted overlay exceeds ~80% opacity.
 *  - Fallback: TYPE_APPLICATION_OVERLAY — needs "display over other apps".
 *    Touches pass through but stay flagged partially-obscured (some secure UIs
 *    ignore them) and above ~80% opacity Android swallows every touch.
 *
 * The window is sized to the real display bounds (not MATCH_PARENT, which can
 * stop short of nav-bar/cutout regions) and re-sized on display rotation.
 * Window ops must run on a thread with a Looper — everything is marshalled to
 * main. [percent] reflects the last requested level so callers stay sync.
 */
// StaticFieldLeak: views are only ever built on applicationContext or the
// bound accessibility service — applyShow is invoked with those exclusively.
@SuppressLint("StaticFieldLeak")
object UltraDimController {
    private const val TAG = "UltraDim"
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Percent 0 = off, 1-100 = overlay opacity. */
    @Volatile
    var percent: Int = 0
        private set

    /**
     * The level set by the last deliberate show() (mode action, QS tile).
     * [adjustTo] changes [percent] without touching this, so a temporary dip
     * can snap back to the mode-configured level.
     */
    @Volatile
    var baselinePercent: Int = 0
        private set

    val isActive: Boolean
        get() = percent > 0

    /**
     * True while the overlay rides the accessibility host — touches pass
     * through completely unaffected. False on the SAW fallback, where secure
     * UIs may ignore flagged touches.
     */
    val isTouchSafe: Boolean
        get() = trustedHost != null

    /** Fired on the main thread after every effective intensity change (0 = off). */
    @Volatile
    var onChanged: ((Int) -> Unit)? = null

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** True when any window path is usable right now. */
    fun canDim(context: Context): Boolean = canDrawOverlays(context) || trustedHost != null

    /** True when the user enabled the dim accessibility service in system settings. */
    fun accessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(AccessibilityManager::class.java) ?: return false
        val expected = ComponentName(context, DimOverlayAccessibilityService::class.java)
        return runCatching {
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
                val info = it.resolveInfo.serviceInfo
                info.packageName == expected.packageName && info.name == expected.className
            }
        }.getOrDefault(false)
    }

    /** Returns true when the window was actually requested. */
    fun show(
        context: Context,
        percent: Int,
    ): Boolean {
        val clamped = percent.coerceIn(1, 100)
        baselinePercent = clamped
        return apply(context, clamped)
    }

    /**
     * Temporary intensity change (notification buttons, adjust panel). Does
     * not move [baselinePercent]; 0 hides the overlay.
     */
    fun adjustTo(
        context: Context,
        percent: Int,
    ) {
        val clamped = percent.coerceIn(0, 100)
        if (clamped <= 0) {
            hide(context)
            return
        }
        apply(context, clamped)
    }

    /** Snap back to the last deliberate level after a temporary [adjustTo]. */
    fun resetToBaseline(context: Context) {
        val baseline = baselinePercent
        if (baseline > 0) apply(context, baseline) else hide(context)
    }

    private fun apply(
        context: Context,
        percent: Int,
    ): Boolean {
        this.percent = percent
        val appContext = context.applicationContext
        mainHandler.post { applyShow(appContext) }
        return true
    }

    fun hide(context: Context) {
        percent = 0
        baselinePercent = 0
        val appContext = context.applicationContext
        mainHandler.post {
            removeView(appContext)
            notifyChanged()
        }
    }

    /** Called by [DimOverlayAccessibilityService] when it binds. */
    fun attachTrustedHost(service: DimOverlayAccessibilityService) {
        val wm = service.getSystemService(WindowManager::class.java) ?: return
        mainHandler.post {
            trustedHost = Host(service, wm, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
            // Migrate a live fallback window onto the trusted host.
            if (percent > 0) applyShow(service.applicationContext)
        }
    }

    /** Called when the accessibility service unbinds or dies. */
    fun detachTrustedHost(service: DimOverlayAccessibilityService) {
        mainHandler.post {
            if (trustedHost?.context !== service) return@post
            trustedHost = null
            if (percent > 0) {
                // The system already tore the accessibility window down; drop
                // our refs and re-add through the fallback path when possible.
                removeView(service.applicationContext)
                if (canDrawOverlays(service)) {
                    applyShow(service.applicationContext)
                } else {
                    percent = 0
                    baselinePercent = 0
                }
            }
            notifyChanged()
        }
    }

    /** Opens the system page where the user grants the overlay permission. */
    fun permissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens system accessibility settings so the user can enable the trusted host. */
    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** A live window target: context for the view plus its WindowManager. */
    private class Host(
        val context: Context,
        val windowManager: WindowManager,
        val windowType: Int,
    )

    @Volatile
    private var trustedHost: Host? = null

    @Volatile
    private var overlayView: View? = null

    private var attachedHost: Host? = null
    private var displayListener: DisplayManager.DisplayListener? = null
    private var displayManager: DisplayManager? = null

    private fun fallbackHost(context: Context): Host? {
        if (!canDrawOverlays(context)) return null
        val wm = context.getSystemService(WindowManager::class.java) ?: return null
        return Host(
            context.applicationContext,
            wm,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        )
    }

    private fun applyShow(context: Context) {
        // A newer hide() may have raced ahead — only draw when still wanted.
        if (percent <= 0) return
        val host =
            trustedHost
                ?: fallbackHost(context)
                ?: run {
                    Log.w(TAG, "no overlay path: accessibility host down, SAW not granted")
                    return
                }
        val view = overlayView
        if (view != null && attachedHost?.windowType == host.windowType) {
            view.alpha = percent / 100f
            val lp = layoutParams(host)
            view.layoutParams = lp
            runCatching { host.windowManager.updateViewLayout(view, lp) }
                .onFailure { Log.w(TAG, "updateViewLayout failed", it) }
        } else {
            removeView(context)
            val newView =
                View(host.context).apply {
                    setBackgroundColor(Color.BLACK)
                    alpha = percent / 100f
                }
            runCatching { host.windowManager.addView(newView, layoutParams(host)) }
                .onSuccess {
                    overlayView = newView
                    attachedHost = host
                    watchDisplay(host)
                }
                .onFailure { Log.w(TAG, "addView failed", it) }
        }
        notifyChanged()
    }

    private fun removeView(context: Context) {
        unwatchDisplay()
        overlayView?.let { view ->
            overlayView = null
            val wm = attachedHost?.windowManager ?: context.getSystemService(WindowManager::class.java)
            attachedHost = null
            runCatching { wm?.removeView(view) }
                .onFailure { Log.w(TAG, "removeView failed", it) }
        }
        attachedHost = null
    }

    /** Re-size the window when the display rotates or its real bounds change. */
    private fun watchDisplay(host: Host) {
        val dm = host.context.getSystemService(DisplayManager::class.java) ?: return
        val listener =
            object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) = Unit

                override fun onDisplayRemoved(displayId: Int) = Unit

                override fun onDisplayChanged(displayId: Int) {
                    if (displayId != Display.DEFAULT_DISPLAY) return
                    overlayView?.let { view ->
                        val lp = layoutParams(host)
                        view.layoutParams = lp
                        runCatching { host.windowManager.updateViewLayout(view, lp) }
                            .onFailure { Log.w(TAG, "rotation relayout failed", it) }
                    }
                }
            }
        dm.registerDisplayListener(listener, mainHandler)
        displayListener = listener
        displayManager = dm
    }

    private fun unwatchDisplay() {
        displayListener?.let { runCatching { displayManager?.unregisterDisplayListener(it) } }
        displayListener = null
        displayManager = null
    }

    private fun displayBounds(host: Host): Rect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            host.windowManager.currentWindowMetrics.bounds
        } else {
            // API 28-29: real size includes status/nav bars — MATCH_PARENT did not.
            val point = Point()
            @Suppress("DEPRECATION")
            host.context
                .getSystemService(DisplayManager::class.java)
                ?.getDisplay(Display.DEFAULT_DISPLAY)
                ?.getRealSize(point)
            Rect(0, 0, point.x, point.y)
        }

    private fun layoutParams(host: Host): WindowManager.LayoutParams {
        val bounds = displayBounds(host)
        return WindowManager.LayoutParams(
            bounds.width(),
            bounds.height(),
            host.windowType,
            // Not touchable, not focusable — pure dimming layer.
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            alpha = percent / 100f
            // jarvis: SHORT_EDGES is the best available below API 30 — a
            // landscape (long-edge) cutout can stay undimmed there; upgrade if
            // minSdk rises to 30.
            layoutInDisplayCutoutMode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                } else {
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
        }
    }

    private fun notifyChanged() {
        runCatching { onChanged?.invoke(percent) }
            .onFailure { Log.w(TAG, "onChanged failed", it) }
    }
}
