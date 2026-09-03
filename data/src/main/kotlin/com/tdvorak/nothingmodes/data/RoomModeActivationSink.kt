package com.tdvorak.nothingmodes.data

import androidx.room.withTransaction
import com.tdvorak.nothingmodes.data.dao.ModeActivationDao
import com.tdvorak.nothingmodes.data.entities.ModeActivationEntity
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationSink

class RoomModeActivationSink(
    private val dao: ModeActivationDao,
    private val db: androidx.room.RoomDatabase,
) : ModeActivationSink {
    override suspend fun activate(automationId: AutomationId, atMillis: Long) {
        db.withTransaction {
            dao.deactivate(automationId.value, atMillis)
            dao.insert(ModeActivationEntity(
                modeId = automationId.value,
                activatedAtMillis = atMillis,
                status = "ACTIVE",
            ))
        }
    }

    override suspend fun deactivate(automationId: AutomationId, atMillis: Long) {
        dao.deactivate(automationId.value, atMillis)
    }
}
