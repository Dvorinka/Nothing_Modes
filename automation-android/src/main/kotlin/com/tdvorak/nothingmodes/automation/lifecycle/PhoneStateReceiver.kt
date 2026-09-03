package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receives phone state changes (incoming call, offhook, idle) and SMS.
 * Dispatches PhoneState and SMS trigger events to AutomationService.
 *
 * Manifest:
 * <receiver android:name=".automation.lifecycle.PhoneStateReceiver" android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.intent.action.PHONE_STATE"/>
 *         <action android:name="android.provider.Telephony.SMS_RECEIVED"/>
 *     </intent-filter>
 * </receiver>
 *
 * Requires READ_PHONE_STATE and RECEIVE_SMS permissions.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PHONE_STATE -> handlePhoneState(context, intent)
            SMS_RECEIVED -> handleSms(context, intent)
        }
    }

    private fun handlePhoneState(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

        Log.d(TAG, "Phone state: $state number=$incomingNumber")

        val phoneEvent = when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> "ringing"
            TelephonyManager.EXTRA_STATE_OFFHOOK -> "offhook"
            TelephonyManager.EXTRA_STATE_IDLE -> "idle"
            else -> return
        }

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_PHONE_STATE
            putExtra(EXTRA_PHONE_STATE, phoneEvent)
            putExtra(EXTRA_PHONE_NUMBER, incomingNumber)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun handleSms(context: Context, intent: Intent) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            Log.w(TAG, "SMS_RECEIVED with no PDUs, ignoring")
            return
        }
        val sender = messages.firstOrNull()?.displayOriginatingAddress ?: ""
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }

        Log.d(TAG, "SMS received: from=$sender body=${body.take(80)}")

        val serviceIntent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_SMS
            putExtra(EXTRA_SMS_SENDER, sender)
            putExtra(EXTRA_SMS_BODY, body)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    companion object {
        private const val TAG = "PhoneStateReceiver"
        private const val SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED"
        private const val ACTION_PHONE_STATE = "android.intent.action.PHONE_STATE"
        const val EXTRA_PHONE_STATE = "phone_state"
        const val EXTRA_PHONE_NUMBER = "phone_number"
        const val EXTRA_SMS_SENDER = "sms_sender"
        const val EXTRA_SMS_BODY = "sms_body"
    }
}
