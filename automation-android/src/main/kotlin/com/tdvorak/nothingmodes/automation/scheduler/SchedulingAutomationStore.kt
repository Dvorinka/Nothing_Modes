package com.tdvorak.nothingmodes.automation.scheduler

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore

/**
 * AutomationStore decorator that keeps the platform-side scheduling
 * (alarms, geofences) in sync with store mutations.
 *
 * Without this, saving or deleting an automation only touched the database:
 * new time triggers were never armed, deleted automations kept their alarms
 * and geofences, and disabling an automation left stale registrations.
 */
class SchedulingAutomationStore(
    private val context: Context,
    private val delegate: AutomationStore,
    private val scheduler: AutomationScheduler,
) : AutomationStore {

    override suspend fun get(id: AutomationId): Automation? = delegate.get(id)
    override suspend fun armed(): List<Automation> = delegate.armed()
    override suspend fun all(): List<Automation> = delegate.all()

    override suspend fun save(automation: Automation) {
        delegate.save(automation)
        runCatching {
            scheduler.cancel(automation.id)
            if (automation.enabled && automation.status == AutomationStatus.ARMED) {
                scheduler.schedule(automation)
                if (automation.trigger is Trigger.Immediate) {
                    dispatchRegistered(automation.id)
                }
            }
        }.onFailure { Log.w(TAG, "Failed to reschedule ${automation.id.value}", it) }
    }

    override suspend fun delete(id: AutomationId) {
        delegate.delete(id)
        runCatching { scheduler.cancel(id) }
            .onFailure { Log.w(TAG, "Failed to cancel schedules for ${id.value}", it) }
    }

    private fun dispatchRegistered(id: AutomationId) {
        val intent = Intent(context, AutomationService::class.java).apply {
            action = AutomationService.ACTION_REGISTERED
            putExtra(AutomationAlarmReceiver.EXTRA_AUTOMATION_ID, id.value)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private companion object {
        const val TAG = "SchedulingStore"
    }
}
