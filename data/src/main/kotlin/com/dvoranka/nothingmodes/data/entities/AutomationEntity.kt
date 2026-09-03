package com.dvoranka.nothingmodes.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dvoranka.nothingmodes.engine.model.AutomationStatus
import com.dvoranka.nothingmodes.engine.model.AutomationType

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: AutomationType,
    val status: AutomationStatus,
    val enabled: Boolean,
    val priority: Int,
    val cooldownMs: Long,
    val schemaVersion: Int,
    val lastFiredAt: Long? = null,
    val json: String,
)
