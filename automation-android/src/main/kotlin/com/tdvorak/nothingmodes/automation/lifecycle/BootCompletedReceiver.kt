package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager

/**
 * Reschedules all armed automations after device reboot.
 *
 * Android 15: BOOT_COMPLETED can no longer start `specialUse` foreground
 * services directly — hop through an expedited WorkManager job, which is
 * exempt from the restriction, and let [BootWorker] perform the starts.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        // jarvis: if expedited quota is exhausted the job degrades to regular
        // work and the FGS starts may be refused — services then come up on
        // next app launch; revisit only if that proves common on Android 15.
        val work =
            OneTimeWorkRequestBuilder<BootWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(BootWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, work)
    }
}
