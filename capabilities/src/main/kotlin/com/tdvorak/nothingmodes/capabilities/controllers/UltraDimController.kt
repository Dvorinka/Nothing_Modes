package com.tdvorak.nothingmodes.capabilities.controllers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager

/**
 * Ultra-dim overlay: a black, untouchable window over the whole screen so the
 * display can go darker than the hardware minimum. Backed by
 * TYPE_APPLICATION_OVERLAY — needs "display over other apps", no service or
 * foreground work. The view lives as long as the process does.
 *
 * Window ops must run on a thread with a Looper — everything is marshalled to
 * main. [percent] reflects the last requested level so callers stay sync.
 */
// StaticFieldLeak: the view is only ever built on applicationContext —
// applyShow is invoked with appContext exclusively.
@SuppressLint("StaticFieldLeak")
object UltraDimController {
    private const val TAG = "UltraDim"
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Percent 0 = off, 1-100 = overlay opacity. */
    @Volatile
    var percent: Int = 0
        private set

    val isActive: Boolean
        get() = percent > 0

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Returns true when the window was actually attached. */
    fun show(
        context: Context,
        percent: Int,
    ): Boolean {
        val clamped = percent.coerceIn(1, 100)
        val appContext = context.applicationContext
        this.percent = clamped
        mainHandler.post { applyShow(appContext, clamped) }
        return true
    }

    fun hide(context: Context) {
        percent = 0
        val appContext = context.applicationContext
        mainHandler.post {
            overlayView?.let { view ->
                overlayView = null
                runCatching {
                    appContext.getSystemService(WindowManager::class.java)?.removeView(view)
                }.onFailure { Log.w(TAG, "removeView failed", it) }
            }
        }
    }

    /** Opens the system page where the user grants the overlay permission. */
    fun permissionIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    @Volatile
    private var overlayView: View? = null

    private fun applyShow(
        context: Context,
        percent: Int,
    ) {
        // A newer hide() may have raced ahead — only draw when still wanted.
        if (this.percent <= 0) return
        val wm = context.getSystemService(WindowManager::class.java) ?: return
        val view = overlayView
        if (view != null) {
            view.alpha = percent / 100f
            runCatching { wm.updateViewLayout(view, view.layoutParams) }
                .onFailure { Log.w(TAG, "updateViewLayout failed", it) }
        } else {
            val newView =
                View(context).apply {
                    setBackgroundColor(Color.BLACK)
                    alpha = percent / 100f
                }
            runCatching { wm.addView(newView, layoutParams(percent)) }
                .onSuccess { overlayView = newView }
                .onFailure { Log.w(TAG, "addView failed", it) }
        }
    }

    private fun layoutParams(percent: Int) =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Not touchable, not focusable — pure dimming layer.
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { alpha = percent / 100f }
}
