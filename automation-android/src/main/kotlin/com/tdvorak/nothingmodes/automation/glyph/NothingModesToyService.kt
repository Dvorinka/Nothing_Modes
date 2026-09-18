package com.tdvorak.nothingmodes.automation.glyph

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Messenger
import android.util.Log
import com.nothing.ketchum.GlyphToy
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider
import com.tdvorak.nothingmodes.nothing.GlyphToyState
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Glyph Toy Service for Nothing Phones with Glyph Matrix (Phone 3, Phone 4a Pro).
 *
 * Registers as a Glyph Toy that appears in the system's Glyph Toys manager.
 * Handles:
 * - STATUS_START/END: marks interactive ownership in [GlyphToyState] so
 *   automation actions know whether their frames can reach the lights.
 * - Long press (EVENT_CHANGE): cycles through currently active modes.
 * - AOD (EVENT_AOD): shows real mode state — the active mode's initials,
 *   or "ZZ" when nothing is running (the idle/sleep glyph).
 */
@AndroidEntryPoint
class NothingModesToyService : Service() {
    @Inject lateinit var store: AutomationStore

    @Inject lateinit var modeActivationProvider: ModeActivationProvider

    private lateinit var provider: NothingGlyphMatrixProvider
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var cycleIndex = 0

    private val serviceHandler =
        Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                GlyphToy.MSG_GLYPH_TOY -> {
                    val bundle = msg.data
                    val event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA)
                    Log.i(TAG, "Glyph Toy event: $event")
                    when (event) {
                        // Lifecycle statuses sent through the same channel.
                        GlyphToy.STATUS_PREPARE -> { /* system warming up the toy */ }
                        GlyphToy.STATUS_START -> {
                            GlyphToyState.interactiveActive = true
                            displayCurrentState()
                        }
                        GlyphToy.STATUS_END -> {
                            GlyphToyState.interactiveActive = false
                            provider.turnOff()
                        }
                        GlyphToy.EVENT_CHANGE -> {
                            cycleIndex++
                            displayCurrentState()
                        }
                        GlyphToy.EVENT_AOD -> displayCurrentState()
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
            onConnected = { displayCurrentState() },
            onDisconnected = { Log.w(TAG, "Glyph Matrix disconnected") },
        )
    }

    override fun onBind(intent: Intent?): IBinder? = serviceMessenger.binder

    override fun onUnbind(intent: Intent?): Boolean = false

    override fun onDestroy() {
        GlyphToyState.interactiveActive = false
        scope.cancel()
        provider.turnOff()
        provider.unInit()
        super.onDestroy()
    }

    // -- Display --

    /**
     * Renders the real mode state. If the provider isn't connected yet —
     * e.g. an AOD tick lands before init finished — kick init and render
     * from its callback instead of silently showing nothing.
     */
    private fun displayCurrentState() {
        if (!provider.isConnected()) {
            provider.init(onConnected = { renderState() })
            return
        }
        renderState()
    }

    private fun renderState() {
        scope.launch {
            val names =
                runCatching {
                    modeActivationProvider
                        .activeModeIds()
                        .mapNotNull { store.get(AutomationId(it))?.name }
                        .filter { it.isNotBlank() }
                }.getOrDefault(emptyList())

            if (names.isEmpty()) {
                provider.displayText(IDLE_GLYPH)
            } else {
                provider.displayText(abbrev(names[cycleIndex % names.size]))
            }
        }
    }

    private fun abbrev(name: String): String = name.take(2).uppercase()

    companion object {
        private const val TAG = "NothingModesToyService"
        private const val IDLE_GLYPH = "ZZ"
    }
}
