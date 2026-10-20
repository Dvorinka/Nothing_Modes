package com.tdvorak.nothingmodes.automation.lifecycle

import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Android 15 forbids BOOT_COMPLETED receivers from starting restricted
 * foreground service types — both of our services are `specialUse`, so a
 * direct startForegroundService there throws and the boot reschedule is
 * lost. An expedited job is exempt from the background FGS-start
 * restriction, so the receiver enqueues this worker and the same two
 * service starts happen inside the job instead.
 */
class BootWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        applicationContext.startForegroundServiceSafely(
            Intent(applicationContext, AutomationService::class.java)
                .setAction(AutomationService.ACTION_BOOT),
        )
        applicationContext.startForegroundServiceSafely(
            Intent(applicationContext, PersistentMonitorService::class.java),
        )
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "boot_reschedule"
    }
}
