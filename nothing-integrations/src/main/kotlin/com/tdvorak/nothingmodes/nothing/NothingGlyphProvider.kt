package com.tdvorak.nothingmodes.nothing

import android.content.Context
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import com.nothing.ketchum.Common

/**
 * Wraps the Nothing Glyph SDK for light stripe devices (Phone 1/2/2a/3a/4a/4b).
 *
 * Full lifecycle: init → register → openSession → operations → closeSession → unInit
 * Supports: toggle, animate, displayProgress, turnOff, zone presets
 */
class NothingGlyphProvider(private val context: Context) {

    private var manager: GlyphManager? = null
    private var connected = false
    private var sessionOpen = false
    private val detector = NothingDeviceDetector(context)
    private var deviceChannels: DeviceChannels? = null

    fun isAvailable(): Boolean = detector.detectGlyphHardware().isLightStripe

    fun init(onConnected: () -> Unit = {}, onDisconnected: () -> Unit = {}) {
        if (!isAvailable()) return
        try {
            manager = GlyphManager.getInstance(context)
            manager?.init(object : GlyphManager.Callback {
                override fun onServiceConnected(componentName: android.content.ComponentName) {
                    connected = true
                    val model = detector.detectModel()
                    try {
                        val targetDevice = mapToDevice(model)
                        if (targetDevice != null) {
                            manager?.register(targetDevice)
                        } else {
                            manager?.register()
                        }
                        manager?.openSession()
                        sessionOpen = true
                        deviceChannels = model?.let { GlyphChannels.forDevice(it) }
                    } catch (e: GlyphException) {
                        Log.e(TAG, "register/session failed", e)
                    } catch (e: Exception) {
                        Log.e(TAG, "register failed", e)
                    }
                    onConnected()
                }

                override fun onServiceDisconnected(componentName: android.content.ComponentName) {
                    connected = false
                    sessionOpen = false
                    onDisconnected()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "init failed", e)
        }
    }

    fun unInit() {
        try {
            if (sessionOpen) manager?.closeSession()
        } catch (_: Exception) {}
        try {
            manager?.unInit()
        } catch (e: Exception) {
            Log.e(TAG, "unInit failed", e)
        }
        connected = false
        sessionOpen = false
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
            manager?.toggle(builder.build())
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "toggle failed")
        }
    }

    /** Toggle a named zone (A/B/C/D/E). */
    fun toggleZone(zone: String): GlyphResult {
        val channels = deviceChannels?.zones?.get(zone.uppercase()) ?: return GlyphResult.Failure("Unknown zone: $zone")
        return toggle(channels)
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
            manager?.animate(builder.build())
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "animate failed")
        }
    }

    /** Animate a named zone with breathing effect. */
    fun animateZone(zone: String, periodMs: Int = 3000, cycles: Int = 3, intervalMs: Int = 10): GlyphResult {
        val channels = deviceChannels?.zones?.get(zone.uppercase()) ?: return GlyphResult.Failure("Unknown zone: $zone")
        return animate(channels, periodMs, cycles, intervalMs)
    }

    /**
     * Display a progress value (0-100) on the progress channel (C1 or D1).
     * Only works on devices with a progress-capable zone.
     */
    fun displayProgress(progress: Int, reverse: Boolean = false): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        val progressChannels = deviceChannels?.progress ?: return GlyphResult.Failure("No progress zone on this device")
        return try {
            val builder = manager?.glyphFrameBuilder ?: return GlyphResult.ServiceUnavailable
            for (ch in progressChannels) builder.buildChannel(ch)
            manager?.displayProgress(builder.build(), progress.coerceIn(0, 100), reverse)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "displayProgress failed")
        }
    }

    /**
     * Toggle all channels except progress zone, and display progress on progress zone.
     */
    fun displayProgressAndToggle(progress: Int, reverse: Boolean = false): GlyphResult {
        if (!connected) return GlyphResult.ServiceUnavailable
        val progressChannels = deviceChannels?.progress ?: return GlyphResult.Failure("No progress zone")
        val otherChannels = deviceChannels?.all?.minus(progressChannels.toSet()) ?: emptyList()
        return try {
            val builder = manager?.glyphFrameBuilder ?: return GlyphResult.ServiceUnavailable
            for (ch in otherChannels) builder.buildChannel(ch)
            manager?.displayProgressAndToggle(builder.build(), progress.coerceIn(0, 100), reverse)
            GlyphResult.Success
        } catch (e: Exception) {
            GlyphResult.Failure(e.message ?: "displayProgressAndToggle failed")
        }
    }

    /** Get available zones for this device. */
    fun availableZones(): Set<String> = deviceChannels?.zones?.keys ?: emptySet()

    private fun mapToDevice(model: String?): String? = when (model) {
        NothingDeviceIds.PHONE_1 -> Glyph.DEVICE_20111
        NothingDeviceIds.PHONE_2 -> Glyph.DEVICE_22111
        NothingDeviceIds.PHONE_2A -> Glyph.DEVICE_23111
        NothingDeviceIds.PHONE_2A_PLUS -> Glyph.DEVICE_23113
        NothingDeviceIds.PHONE_3A -> Glyph.DEVICE_24111
        NothingDeviceIds.PHONE_4A -> Glyph.DEVICE_25111
        NothingDeviceIds.PHONE_4B -> Glyph.DEVICE_25111
        else -> null
    }

    companion object {
        private const val TAG = "NothingGlyphProvider"
    }
}
