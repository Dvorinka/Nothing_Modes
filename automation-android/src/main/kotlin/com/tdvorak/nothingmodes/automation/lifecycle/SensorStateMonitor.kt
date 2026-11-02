package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.sqrt
import com.tdvorak.nothingmodes.engine.model.DeviceStateKeys

/**
 * Watches the motion sensors that have no broadcast equivalent:
 *
 *  - Proximity (covered/uncovered) — emitted through the device-state
 *    [DeviceStateMonitor.emitExternal] channel so Proximity works like
 *    every other Device-state trigger.
 *  - Shake — a wrist-flip spike pattern on the accelerometer, dispatched
 *    as a dedicated one-shot event to AutomationService.
 *
 * Listeners run on a background looper; deregistered as usual by [stop].
 */
class SensorStateMonitor(
    private val context: Context,
    private val deviceStateMonitor: DeviceStateMonitor,
) {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private val proximityListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // One value: 0 uncovered/far, max covered/near (5-10cm for
                // most sensors). Anything above zero counts as covered.
                val near = event.values.firstOrNull() ?: 0f
                deviceStateMonitor.emitExternal(
                    DeviceStateKeys.PROXIMITY,
                    if (near > 0f) "near" else "far",
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

    private val shakeListener =
        object : SensorEventListener {
            private var lastSample = 0f
            private var spikeCount = 0
            private var lastSpikeAt = 0L
            private var lastFired = 0L

            override fun onSensorChanged(event: SensorEvent) {
                val magnitude = sqrt(event.values.take(3).sumOf { (it * it).toDouble() }).toFloat()
                val delta = magnitude - lastSample
                lastSample = magnitude
                if (delta < SPIKE_THRESHOLD || delta < 0f) return
                val now = System.currentTimeMillis()
                if (now - lastFired < REFIRE_COOLDOWN_MS) return
                if (now - lastSpikeAt > SPIKE_WINDOW_MS) {
                    spikeCount = 0
                }
                lastSpikeAt = now
                spikeCount++
                if (spikeCount >= SPIKE_COUNT) {
                    spikeCount = 0
                    lastFired = now
                    context.startForegroundServiceSafely(
                        Intent(context, AutomationService::class.java).apply {
                            action = AutomationService.ACTION_SHAKE
                        },
                    )
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

    fun start() {
        val manager = sensorManager ?: return
        runCatching {
            manager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let { sensor ->
                manager.registerListener(proximityListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
            }
            manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
                manager.registerListener(shakeListener, sensor, SensorManager.SENSOR_DELAY_NORMAL, handler)
            }
        }.onFailure { Log.w(TAG, "Sensor registration failed", it) }
    }

    fun stop() {
        sensorManager?.runCatching {
            unregisterListener(proximityListener)
            unregisterListener(shakeListener)
        }
    }

    private companion object {
        const val TAG = "SensorStateMonitor"
        /** m/s² over the previous sample that counts as a spike (gravity is ~9.8). */
        const val SPIKE_THRESHOLD = 14f
        /** Spikes inside this window accumulate toward a shake. */
        const val SPIKE_WINDOW_MS = 300L
        const val SPIKE_COUNT = 3
        const val REFIRE_COOLDOWN_MS = 2000L
    }
}
