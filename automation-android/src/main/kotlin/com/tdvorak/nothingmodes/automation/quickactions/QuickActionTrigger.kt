package com.tdvorak.nothingmodes.automation.quickactions

import android.content.Context
import android.content.Intent
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.automation.lifecycle.startForegroundServiceSafely

/**
 * Triggers a single automation manually from widget / Quick Settings tile surfaces.
 */
object QuickActionTrigger {
    fun run(
        context: Context,
        automationId: String,
    ) {
        val intent =
            Intent(context, AutomationService::class.java).apply {
                action = AutomationService.ACTION_MANUAL
                putExtra(AutomationService.EXTRA_MANUAL_ID, automationId)
            }
        context.startForegroundServiceSafely(intent)
    }
}
