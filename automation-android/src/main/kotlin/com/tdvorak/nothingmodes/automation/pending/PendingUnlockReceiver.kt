package com.tdvorak.nothingmodes.automation.pending

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tdvorak.nothingmodes.capabilities.PendingUnlockStore
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Tap target of the pending-unlock notification. A notification tap grants a
 * short activity-launch window, so queued UI actions (settings panels,
 * launch-app, open-URL) can finally run — no screen of our own needed.
 *
 * Plain receiver + EntryPointAccessors: @AndroidEntryPoint injection needs a
 * super.onReceive call that Kotlin can't make on an abstract BroadcastReceiver.
 */
class PendingUnlockReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PendingUnlockEntryPoint {
        fun actionExecutor(): ActionExecutor

        fun pendingUnlockStore(): PendingUnlockStore
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != ACTION) return
        val entryPoint = EntryPointAccessors.fromApplication(context, PendingUnlockEntryPoint::class.java)
        val pendingStore = entryPoint.pendingUnlockStore()
        val km = context.getSystemService(KeyguardManager::class.java)
        if (km?.isDeviceLocked == true) {
            // Tapped while still locked — keep the alert alive; USER_PRESENT
            // or a later tap drains for real.
            UnlockNotifier.notifyPending(context, null, pendingStore.pending().size)
            return
        }
        if (!pendingStore.hasPending()) return
        val executor = entryPoint.actionExecutor()
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                PendingUnlockDrain.run(pendingStore, executor)
            } finally {
                UnlockNotifier.cancel(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION = "com.tdvorak.nothingmodes.automation.DRAIN_PENDING_UNLOCK"
    }
}
