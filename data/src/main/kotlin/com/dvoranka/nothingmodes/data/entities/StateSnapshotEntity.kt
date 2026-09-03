package com.dvoranka.nothingmodes.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** State snapshot for Mode deactivation restoration. */
@Entity(tableName = "state_snapshots")
data class StateSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val automationId: String,
    val settingKey: String,
    val previousValue: String,
    val capturedAtMillis: Long,
)
