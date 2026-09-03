package com.dvoranka.nothingmodes.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Mode activation/deactivation lifecycle tracking. */
@Entity(tableName = "mode_activations")
data class ModeActivationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modeId: String,
    val activatedAtMillis: Long,
    val deactivatedAtMillis: Long? = null,
    val status: String, // ACTIVE, DEACTIVATED, EXPIRED
)
