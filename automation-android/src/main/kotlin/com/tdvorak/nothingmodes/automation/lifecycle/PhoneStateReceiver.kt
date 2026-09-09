package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receives SMS and dispatches SMS trigger events to AutomationService.
 *
 * Call state is tracked via TelephonyCallback / PhoneStateListener in
 * PersistentMonitorService because the ACTION_PHONE_STATE broadcast is not
 * reliably delivered on modern Android.
 *
 * Requires RECEIVE_SMS permission.
 */
class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        when (intent.action) {
            SMS_RECEIVED -> handleSms(context, intent)
        }
    }

    private fun handleSms(
        context: Context,
        intent: Intent,
    ) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "SMS_RECEIVED with no PDUs, ignoring")
            return
        }
        val sender = messages.firstOrNull()?.displayOriginatingAddress ?: ""
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }

        Log.d(TAG, "SMS received")

        val serviceIntent =
            Intent(context, AutomationService::class.java).apply {
                action = AutomationService.ACTION_SMS
                putExtra(EXTRA_SMS_SENDER, sender)
                putExtra(EXTRA_SMS_BODY, body)
            }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        private const val TAG = "PhoneStateReceiver"
        private const val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
        const val EXTRA_PHONE_STATE = "phone_state"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_SMS_SENDER = "sms_sender"
        const val EXTRA_SMS_BODY = "sms_body"
    }
}
