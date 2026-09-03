package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.AutomationId

/** Provides active mode IDs for DeviceState population. */
fun interface ModeActivationProvider {
    suspend fun activeModeIds(): List<String>
}

object NoopModeActivationProvider : ModeActivationProvider {
    override suspend fun activeModeIds(): List<String> = emptyList()
}

/** Records mode activation/deactivation lifecycle for state queries. */
interface ModeActivationSink {
    suspend fun activate(automationId: AutomationId, atMillis: Long)
    suspend fun deactivate(automationId: AutomationId, atMillis: Long)
}

object NoopModeActivationSink : ModeActivationSink {
    override suspend fun activate(automationId: AutomationId, atMillis: Long) = Unit
    override suspend fun deactivate(automationId: AutomationId, atMillis: Long) = Unit
}
