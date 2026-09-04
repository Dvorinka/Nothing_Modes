package com.tdvorak.nothingmodes.automation.widget

import android.content.Context
import android.content.Intent

/**
 * Requests that all placed Nothing Modes app widgets refresh their contents.
 * The receiving BroadcastReceiver lives in the app module.
 */
object WidgetRefreshHelper {
    private const val ACTION = "com.tdvorak.nothingmodes.UPDATE_WIDGET"

    fun refresh(context: Context) {
        val intent = Intent(ACTION).setPackage(context.packageName)
        context.sendBroadcast(intent)
    }
}
