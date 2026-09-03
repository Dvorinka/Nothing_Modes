package com.dvoranka.nothingmodes.data

import com.dvoranka.nothingmodes.data.dao.AutomationDao
import com.dvoranka.nothingmodes.data.entities.AutomationEntity
import com.dvoranka.nothingmodes.engine.model.Automation
import com.dvoranka.nothingmodes.engine.model.AutomationId
import com.dvoranka.nothingmodes.engine.model.AutomationStatus
import com.dvoranka.nothingmodes.engine.model.EngineJson
import com.dvoranka.nothingmodes.engine.runtime.AutomationStore

class RoomAutomationStore(private val dao: AutomationDao) : AutomationStore {

    override fun get(id: AutomationId): Automation? = dao.get(id.value)?.toDomain()

    override fun armed(): List<Automation> = dao.armed().map { it.toDomain() }

    override fun save(automation: Automation) {
        dao.upsert(automation.toEntity())
    }

    override fun delete(id: AutomationId) {
        dao.delete(id.value)
    }

    override fun all(): List<Automation> = dao.armed().map { it.toDomain() }
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
