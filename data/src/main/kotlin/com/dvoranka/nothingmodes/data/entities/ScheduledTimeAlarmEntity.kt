package com.dvoranka.nothingmodes.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scheduled_time_alarms")
data class ScheduledTimeAlarmEntity(
    @PrimaryKey val automationId: String,
    val approvalFingerprint: String,
    val eventAtMillis: Long,
    val wakeAtMillis: Long,
    val requestedPrecision: String,
    val scheduledMode: String,
    val updatedAtMillis: Long,
)
