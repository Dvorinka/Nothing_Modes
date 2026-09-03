package com.tdvorak.nothingmodes.data

import com.tdvorak.nothingmodes.data.dao.ModeActivationDao
import com.tdvorak.nothingmodes.data.entities.ModeActivationEntity
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationSink

class RoomModeActivationSink(
    private val dao: ModeActivationDao,
) : ModeActivationSink {
    override suspend fun activate(automationId: AutomationId, atMillis: Long) {
        dao.deactivate(automationId.value, atMillis)
        dao.insert(ModeActivationEntity(
            modeId = automationId.value,
            activatedAtMillis = atMillis,
            status = "ACTIVE",
        ))
    }

    override suspend fun deactivate(automationId: AutomationId, atMillis: Long) {
        dao.deactivate(automationId.value, atMillis)
    }
}
