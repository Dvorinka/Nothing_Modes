package com.dvoranka.nothingmodes.nothing

import android.content.Context
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphManager
import com.nothing.ketchum.Common

/**
 * Wraps the Nothing Glyph SDK for light stripe devices (Phone 1/2/2a/3a/4a/4b).
 *
 * The SDK requires foreground context. All operations are synchronous and
 * must be called from the main thread or after init() callback.
 */
class NothingGlyphProvider(private val context: Context) {

    private var manager: GlyphManager? = null
    private var connected = false
    private val detector = NothingDeviceDetector(context)

    fun isAvailable(): Boolean = detector.detectGlyphHardware().isLightStripe

    fun init(onConnected: () -> Unit = {}, onDisconnected: () -> Unit = {}) {
        if (!isAvailable()) return
        try {
            manager = GlyphManager.getInstance(context)
            manager?.init(object : GlyphManager.Callback {
                override fun onServiceConnected(componentName: android.content.ComponentName) {
                    connected = true
                    try {
                        manager?.register()
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

    fun toggle(channels: List<Int>? = null): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            val builder = manager?.glyphFrameBuilder ?: return GlyphResult.ServiceUnavailable
            if (channels != null) {
                for (ch in channels) builder.buildChannel(ch)
            } else {
                builder.buildChannelA()
            }
            val frame = builder.build()
            manager?.toggle(frame)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "toggle failed")
        }
    }

    fun turnOff(): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            manager?.turnOff()
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "turnOff failed")
        }
    }

    fun animate(channels: List<Int>, periodMs: Int, cycles: Int, intervalMs: Int): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        return try {
            val builder = manager?.glyphFrameBuilder ?: return GlyphResult.ServiceUnavailable
            for (ch in channels) builder.buildChannel(ch)
            builder.buildPeriod(periodMs)
            builder.buildCycles(cycles)
            builder.buildInterval(intervalMs)
            val frame = builder.build()
            manager?.animate(frame)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "animate failed")
        }
    }

    companion object {
        private const val TAG = "NothingGlyphProvider"
    }
}
