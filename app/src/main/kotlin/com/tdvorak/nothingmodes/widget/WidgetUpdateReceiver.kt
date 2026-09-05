package com.tdvorak.nothingmodes.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "com.tdvorak.nothingmodes.UPDATE_WIDGET") return
        scope.launch {
            NothingModesWidget().updateAll(context)
            SingleAutomationWidget().updateAll(context)
        }
    }
}
