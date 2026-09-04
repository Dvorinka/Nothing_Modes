package com.tdvorak.nothingmodes.data

import com.tdvorak.nothingmodes.data.dao.StateSnapshotDao
import com.tdvorak.nothingmodes.data.entities.StateSnapshotEntity
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.runtime.StateSnapshot as EngineStateSnapshot
import com.tdvorak.nothingmodes.engine.runtime.StateSnapshotStore

class RoomStateSnapshotStore(
    private val dao: StateSnapshotDao,
) : StateSnapshotStore {

    override suspend fun save(snapshot: EngineStateSnapshot) {
        dao.insert(StateSnapshotEntity(
            automationId = snapshot.automationId.value,
            settingKey = snapshot.settingKey,
            previousValue = snapshot.previousValue,
            capturedAtMillis = snapshot.capturedAtMillis,
            namespace = snapshot.namespace,
        ))
    }

    override suspend fun forAutomation(id: AutomationId): List<EngineStateSnapshot> =
        dao.forAutomation(id.value).map { entity ->
            EngineStateSnapshot(
                automationId = AutomationId(entity.automationId),
                settingKey = entity.settingKey,
                previousValue = entity.previousValue,
                capturedAtMillis = entity.capturedAtMillis,
                namespace = entity.namespace,
            )
        }

    override suspend fun deleteForAutomation(id: AutomationId) {
        dao.deleteForAutomation(id.value)
    }
}
