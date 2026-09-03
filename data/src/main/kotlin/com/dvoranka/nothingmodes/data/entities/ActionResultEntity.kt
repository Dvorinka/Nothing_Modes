package com.dvoranka.nothingmodes.data.entities

import androidx.room.Entity

@Entity(
    tableName = "action_results",
    primaryKeys = ["executionId", "actionIndex"],
)
data class ActionResultEntity(
    val executionId: String,
    val actionIndex: Int,
    val actionType: String,
    val outcome: String,
    val atMillis: Long,
    val errorCode: String? = null,
)
