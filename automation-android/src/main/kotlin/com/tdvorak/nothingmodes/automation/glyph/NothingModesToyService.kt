package com.tdvorak.nothingmodes.automation.glyph

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.GlyphToy
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider

/**
 * Glyph Toy Service for Nothing Phones with Glyph Matrix (Phone 3, Phone 4a Pro).
 *
 * Registers as a Glyph Toy that appears in the system's Glyph Toys manager.
 * Handles:
 * - Short press (EVENT_ACTION_DOWN/UP): touch feedback
 * - Long press (EVENT_CHANGE): cycle through mode visualizations
 * - AOD (EVENT_AOD): update always-on display
 *
 * Manifest registration required:
 * <service android:name=".automation.glyph.NothingModesToyService"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="com.nothing.glyph.TOY"/>
 *     </intent-filter>
 *     <meta-data android:name="com.nothing.glyph.toy.name" android:value="Nothing Modes"/>
 *     <meta-data android:name="com.nothing.glyph.toy.summary" android:value="Display active automation modes on Glyph Matrix"/>
 *     <meta-data android:name="com.nothing.glyph.toy.longpress" android:value="1"/>
 *     <meta-data android:name="com.nothing.glyph.toy.aod_support" android:value="1"/>
 * </service>
 */
class NothingModesToyService : Service() {

    private lateinit var provider: NothingGlyphMatrixProvider
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
                    GlyphToy.EVENT_ACTION_DOWN -> onTouchDown()
                    GlyphToy.EVENT_ACTION_UP -> onTouchUp()
                    else -> Log.d(TAG, "Unknown event: $event")
                }
                true
            }
            else -> false
        }
    }

    private val serviceMessenger = Messenger(serviceHandler)

    override fun onCreate() {
        super.onCreate()
        provider = NothingGlyphMatrixProvider(this)
        provider.init(
            onConnected = { displayCurrentMode() },
            onDisconnected = { Log.w(TAG, "Glyph Matrix disconnected") },
        )
    }

    override fun onBind(intent: Intent?): IBinder? = serviceMessenger.binder

    override fun onUnbind(intent: Intent?): Boolean {
        return false
    }

    override fun onDestroy() {
        provider.turnOff()
        provider.unInit()
        super.onDestroy()
    }

    // -- Event handlers --

    /** Long press: cycle to next mode. */
    private fun onLongPress() {
        currentModeIndex = (currentModeIndex + 1) % modes.size
        displayCurrentMode()
    }

    /** AOD tick: refresh display. */
    private fun onAodTick() {
        displayCurrentMode()
    }

    private fun onTouchDown() {
        // Visual feedback on touch-down could be added here
    }

    private fun onTouchUp() {
        // Finalize touch feedback here
    }

    // -- Display --

    private fun displayCurrentMode() {
        val (_, abbrev) = modes[currentModeIndex]
        if (abbrev == "--") {
            provider.turnOff()
        } else {
            provider.displayText(abbrev)
        }
    }

    companion object {
        private const val TAG = "NothingModesToyService"
    }
}
