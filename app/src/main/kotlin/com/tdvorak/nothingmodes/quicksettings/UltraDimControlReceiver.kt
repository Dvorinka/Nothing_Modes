package com.tdvorak.nothingmodes.quicksettings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tdvorak.nothingmodes.capabilities.controllers.UltraDimController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Notification action target for the ultra-dim overlay. Steps are temporary
 * adjustments — they move the live intensity, never the mode's stored value.
 * Non-exported: only our own notification PendingIntents may fire it.
 */
class UltraDimControlReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_DIM_LESS -> UltraDimController.adjustTo(context, UltraDimController.percent - STEP)
            ACTION_DIM_MORE -> UltraDimController.adjustTo(context, UltraDimController.percent + STEP)
            ACTION_DIM_OFF -> UltraDimController.hide(context)
            ACTION_DIM_ON -> {
                // Re-arm at the level the still-active mode configured.
                val pending = goAsync()
                scope.launch {
                    try {
                        val percent = UltraDimNotifier.engagedPercent(context) ?: DEFAULT_ON
                        UltraDimController.show(context, percent)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_DIM_LESS = "com.tdvorak.nothingmodes.ULTRA_DIM_LESS"
        const val ACTION_DIM_MORE = "com.tdvorak.nothingmodes.ULTRA_DIM_MORE"
        const val ACTION_DIM_OFF = "com.tdvorak.nothingmodes.ULTRA_DIM_OFF"
        const val ACTION_DIM_ON = "com.tdvorak.nothingmodes.ULTRA_DIM_ON"
        private const val STEP = 10
        private const val DEFAULT_ON = 50
    }
}
