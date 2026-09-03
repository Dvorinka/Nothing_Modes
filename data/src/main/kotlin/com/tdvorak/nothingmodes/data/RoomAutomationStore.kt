package com.tdvorak.nothingmodes.data

import com.tdvorak.nothingmodes.data.dao.AutomationDao
import com.tdvorak.nothingmodes.data.entities.AutomationEntity
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.EngineJson
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore

class RoomAutomationStore(private val dao: AutomationDao) : AutomationStore {

    override suspend fun get(id: AutomationId): Automation? = dao.get(id.value)?.let { runCatching { it.toDomain() }.getOrNull() }

    override suspend fun armed(): List<Automation> = dao.armed().mapNotNull { runCatching { it.toDomain() }.getOrNull() }

    override suspend fun save(automation: Automation) {
        dao.upsert(automation.toEntity())
    }

    override suspend fun delete(id: AutomationId) {
        dao.delete(id.value)
    }

    override suspend fun all(): List<Automation> = dao.all().mapNotNull { runCatching { it.toDomain() }.getOrNull() }
}

private fun AutomationEntity.toDomain(): Automation =
    EngineJson.json.decodeFromString(Automation.serializer(), json)

private fun Automation.toEntity(): AutomationEntity = AutomationEntity(
    id = id.value,
    name = name,
    type = type,
    status = status,
    enabled = enabled,
    priority = priority,
    cooldownMs = cooldownMs,
    schemaVersion = schemaVersion,
    lastFiredAt = null,
    json = EngineJson.json.encodeToString(Automation.serializer(), this),
)
