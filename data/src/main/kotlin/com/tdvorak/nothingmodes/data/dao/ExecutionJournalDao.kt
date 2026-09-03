package com.tdvorak.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tdvorak.nothingmodes.data.entities.ActionResultEntity

@Dao
interface ExecutionJournalDao {

    @Insert
    suspend fun insert(entity: ActionResultEntity): Long

    @Query("SELECT * FROM action_results WHERE executionId = :executionId ORDER BY actionIndex ASC")
    suspend fun forExecution(executionId: String): List<ActionResultEntity>

    @Query("SELECT * FROM action_results WHERE executionId IN (SELECT executionId FROM fire_claims WHERE automationId = :automationId) ORDER BY atMillis DESC LIMIT :limit")
    suspend fun forAutomation(automationId: String, limit: Int = 500): List<ActionResultEntity>

    @Query("DELETE FROM action_results WHERE executionId IN (SELECT executionId FROM fire_claims WHERE claimedAtMillis < :beforeMillis)")
    suspend fun pruneBefore(beforeMillis: Long): Int
}
