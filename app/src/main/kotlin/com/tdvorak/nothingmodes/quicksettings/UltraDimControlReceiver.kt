package com.tdvorak.nothingmodes.quicksettings

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tdvorak.nothingmodes.capabilities.controllers.UltraDimController

/**
 * Notification action target for the ultra-dim overlay. Steps are temporary
 * adjustments — they move the live intensity, never the mode's stored value.
 * Non-exported: only our own notification PendingIntents may fire it.
 */
class UltraDimControlReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            ACTION_DIM_LESS -> UltraDimController.adjustTo(context, UltraDimController.percent - STEP)
            ACTION_DIM_MORE -> UltraDimController.adjustTo(context, UltraDimController.percent + STEP)
            ACTION_DIM_OFF -> UltraDimController.hide(context)
        }
    }

    companion object {
        const val ACTION_DIM_LESS = "com.tdvorak.nothingmodes.ULTRA_DIM_LESS"
        const val ACTION_DIM_MORE = "com.tdvorak.nothingmodes.ULTRA_DIM_MORE"
        const val ACTION_DIM_OFF = "com.tdvorak.nothingmodes.ULTRA_DIM_OFF"
        private const val STEP = 10
    }
}
