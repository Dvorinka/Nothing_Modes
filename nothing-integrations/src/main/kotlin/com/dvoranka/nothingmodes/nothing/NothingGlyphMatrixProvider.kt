package com.dvoranka.nothingmodes.nothing

import android.content.Context
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.Common

/**
 * Wraps the Nothing Glyph Matrix SDK for matrix devices (Phone 3, Phone 4a Pro).
 *
 * Uses setAppMatrixFrame (not setMatrixFrame) for third-party app control.
 * Requires system version 20250801 or later.
 */
class NothingGlyphMatrixProvider(private val context: Context) {

    private var manager: GlyphMatrixManager? = null
    private var connected = false
    private val detector = NothingDeviceDetector(context)

    fun isAvailable(): Boolean = detector.detectGlyphHardware().isMatrix

    fun matrixSize(): Int = detector.detectGlyphHardware().matrixSize

    fun init(onConnected: () -> Unit = {}, onDisconnected: () -> Unit = {}) {
        if (!isAvailable()) return
        try {
            manager = GlyphMatrixManager.getInstance(context)
            manager?.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(componentName: android.content.ComponentName) {
                    connected = true
                    val device = when (detector.detectGlyphHardware()) {
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
            })
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
        }
    }

    fun unInit() {
        try {
            manager?.unInit()
        } catch (e: Exception) {
            Log.e(TAG, "unInit failed", e)
        }
        connected = false
        manager = null
    }

    /** Display a raw color frame. colors must be exactly matrixSize*matrixSize entries. */
    fun setFrame(colors: IntArray): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        val expected = matrixSize() * matrixSize()
        if (colors.size != expected) return GlyphResult.Failure("Expected $expected colors, got ${colors.size}")
        return try {
            manager?.setAppMatrixFrame(colors)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "setFrame failed")
        }
    }

    /** Turn off app matrix display. */
    fun closeFrame(): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            manager?.closeAppMatrix()
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "closeFrame failed")
        }
    }

    /** Turn off all glyphs. */
    fun turnOff(): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            manager?.turnOff()
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "turnOff failed")
        }
    }

    companion object {
        private const val TAG = "NothingGlyphMatrix"
    }
}
