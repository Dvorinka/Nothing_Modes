package com.tdvorak.nothingmodes.automation.glyph

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import com.nothing.ketchum.GlyphToy
import com.tdvorak.nothingmodes.nothing.NothingDeviceDetector
import com.tdvorak.nothingmodes.nothing.GlyphHardware

/**
 * Glyph Toy Service for Nothing Phones with Glyph Matrix (Phone 3, Phone 4a Pro).
 *
 * Registers as a Glyph Toy that appears in the system's Glyph Toys manager.
 * Handles:
 * - Short press: cycle through mode visualizations
 * - Long press (EVENT_CHANGE): toggle current mode on/off
 * - AOD (EVENT_AOD): update always-on display every minute
 * - action_down / action_up: touch-down/up events
 *
 * Manifest registration required:
 * <service android:name=".automation.glyph.NothingModesToyService"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="com.nothing.glyph.TOY"/>
 *     </intent-filter>
 *     <meta-data android:name="com.nothing.glyph.toy.name" android:resource="@string/toy_name"/>
 *     <meta-data android:name="com.nothing.glyph.toy.image" android:resource="@drawable/ic_toy_preview"/>
 *     <meta-data android:name="com.nothing.glyph.toy.summary" android:resource="@string/toy_summary"/>
 *     <meta-data android:name="com.nothing.glyph.toy.longpress" android:value="1"/>
 *     <meta-data android:name="com.nothing.glyph.toy.aod_support" android:value="1"/>
 * </service>
 */
class NothingModesToyService : Service() {

    private var manager: GlyphMatrixManager? = null
    private var connected = false
    private val detector by lazy { NothingDeviceDetector(this) }
    private var currentModeIndex = 0

    private val modes = listOf(
        "Sleep" to "ZZ",
        "Morning" to "AM",
        "Work" to "WK",
        "DND" to "DN",
        "Movie" to "MV",
        "Off" to "--",
    )

    private val serviceHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            GlyphToy.MSG_GLYPH_TOY -> {
                val bundle = msg.data
                val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                Log.i(TAG, "Glyph Toy event: $event")
                when (event) {
                    GlyphToy.EVENT_CHANGE -> onLongPress()
                    GlyphToy.EVENT_AOD -> onAodTick()
                    "action_down" -> onTouchDown()
                    "action_up" -> onTouchUp()
                    else -> Log.d(TAG, "Unknown event: $event")
                }
                true
            }
            else -> false
        }
    }

    private val serviceMessenger = Messenger(serviceHandler)

    override fun onBind(intent: Intent?): IBinder? {
        initGlyph()
        return serviceMessenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return false
    }

    private fun initGlyph() {
        val hardware = detector.detectGlyphHardware()
        if (!hardware.isMatrix) {
            Log.w(TAG, "No Glyph Matrix on this device")
            return
        }
        manager = GlyphMatrixManager.getInstance(this)
        manager?.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(componentName: android.content.ComponentName) {
                connected = true
                val device = when (hardware) {
                    GlyphHardware.MATRIX_25 -> com.nothing.ketchum.Glyph.DEVICE_23112
                    GlyphHardware.MATRIX_13 -> com.nothing.ketchum.Glyph.DEVICE_25111p
                    else -> return
                }
                try {
                    manager?.register(device)
                } catch (e: Exception) {
                    Log.e(TAG, "register failed", e)
                }
                displayCurrentMode()
            }

            override fun onServiceDisconnected(componentName: android.content.ComponentName) {
                connected = false
            }
        })
    }

    private fun cleanup() {
        try {
            manager?.turnOff()
            manager?.unInit()
        } catch (e: Exception) {
            Log.e(TAG, "cleanup failed", e)
        }
        manager = null
        connected = false
    }

    // ── Event handlers ──

    /** Long press: toggle current mode. */
    private fun onLongPress() {
        currentModeIndex = (currentModeIndex + 1) % modes.size
        displayCurrentMode()
    }

    /** AOD tick: refresh display every minute. */
    private fun onAodTick() {
        displayCurrentMode()
    }

    private fun onTouchDown() {
        // Could trigger a visual effect on touch-down
    }

    private fun onTouchUp() {
        // Could finalize visual effect on touch-up
    }

    // ── Display ──

    private fun displayCurrentMode() {
        if (!connected) return
        val (name, abbrev) = modes[currentModeIndex]
        try {
            val obj = GlyphMatrixObject.Builder()
                .setText(abbrev)
                .build()
            val frame = GlyphMatrixFrame.Builder()
                .addTop(obj)
                .build(this)
            manager?.setMatrixFrame(frame)
        } catch (e: Exception) {
            Log.e(TAG, "displayCurrentMode failed", e)
        }
    }

    companion object {
        private const val TAG = "NothingModesToyService"
    }
}
