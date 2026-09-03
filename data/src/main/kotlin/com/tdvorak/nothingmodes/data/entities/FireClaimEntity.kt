package com.tdvorak.nothingmodes.data.entities

import androidx.room.Entity

@Entity(
    tableName = "fire_claims",
    primaryKeys = ["automationId", "eventIdHash"],
)
data class FireClaimEntity(
    val automationId: String,
    val eventIdHash: String,
    val executionId: String,
    val claimedAtMillis: Long,
    val status: String = "RUNNING",
    val completedAtMillis: Long? = null,
    val succeededCount: Int = 0,
    val failedCount: Int = 0,
)
