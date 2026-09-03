package com.tdvorak.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tdvorak.nothingmodes.data.entities.StateSnapshotEntity

@Dao
interface StateSnapshotDao {

    @Insert
    suspend fun insert(entity: StateSnapshotEntity): Long

    @Query("SELECT * FROM state_snapshots WHERE automationId = :automationId ORDER BY capturedAtMillis DESC")
    suspend fun forAutomation(automationId: String): List<StateSnapshotEntity>

    @Query("DELETE FROM state_snapshots WHERE automationId = :automationId")
    suspend fun deleteForAutomation(automationId: String)

    @Query("DELETE FROM state_snapshots WHERE capturedAtMillis < :beforeMillis")
    suspend fun pruneBefore(beforeMillis: Long): Int
}
