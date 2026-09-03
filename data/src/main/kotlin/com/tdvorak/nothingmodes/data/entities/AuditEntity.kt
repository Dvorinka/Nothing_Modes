package com.tdvorak.nothingmodes.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_log")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val automationId: String,
    val kind: String,
    val atMillis: Long,
    val detail: String = "",
    val eventIdHash: String? = null,
    val eventId: String = "",
    val executionId: String? = null,
)
