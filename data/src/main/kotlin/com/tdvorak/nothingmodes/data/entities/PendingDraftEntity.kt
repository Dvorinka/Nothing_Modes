package com.tdvorak.nothingmodes.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_drafts")
data class PendingDraftEntity(
    @PrimaryKey val id: String,
    val automationId: String,
    val name: String,
    val revision: Int,
    val fingerprint: String,
    val createdBy: String,
    val priority: Int,
    val schemaVersion: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val quarantineCode: String? = null,
    val draftJson: String,
)
