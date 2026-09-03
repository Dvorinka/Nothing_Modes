package com.tdvorak.nothingmodes.data

import com.tdvorak.nothingmodes.data.dao.AuditDao
import com.tdvorak.nothingmodes.data.entities.AuditEntity
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.runtime.AuditEvent
import com.tdvorak.nothingmodes.engine.runtime.AuditKind
import com.tdvorak.nothingmodes.engine.runtime.AuditSink

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
