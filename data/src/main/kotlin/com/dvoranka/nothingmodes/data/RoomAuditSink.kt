package com.dvoranka.nothingmodes.data

import com.dvoranka.nothingmodes.data.dao.AuditDao
import com.dvoranka.nothingmodes.data.entities.AuditEntity
import com.dvoranka.nothingmodes.engine.model.AutomationId
import com.dvoranka.nothingmodes.engine.runtime.AuditEvent
import com.dvoranka.nothingmodes.engine.runtime.AuditKind
import com.dvoranka.nothingmodes.engine.runtime.AuditSink

class RoomAuditSink(private val dao: AuditDao) : AuditSink {

    override suspend fun record(event: AuditEvent) {
        dao.insert(
            AuditEntity(
                automationId = event.automationId.value,
                kind = event.kind.name,
                atMillis = event.atMillis,
                detail = event.detail,
                eventId = event.eventId,
                executionId = event.executionId,
            ),
        )
    }
}
