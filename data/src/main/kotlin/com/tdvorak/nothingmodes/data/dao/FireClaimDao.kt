package com.tdvorak.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tdvorak.nothingmodes.data.entities.FireClaimEntity

@Dao
interface FireClaimDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun tryClaim(entity: FireClaimEntity): Long

    @Query("SELECT * FROM fire_claims WHERE automationId = :automationId AND eventIdHash = :eventIdHash")
    suspend fun get(automationId: String, eventIdHash: String): FireClaimEntity?

    @Query("SELECT * FROM fire_claims WHERE executionId = :executionId")
    suspend fun byExecution(executionId: String): FireClaimEntity?

    @Query("UPDATE fire_claims SET status = :status, completedAtMillis = :atMillis, succeededCount = :succeeded, failedCount = :failed WHERE executionId = :executionId")
    suspend fun complete(executionId: String, status: String, atMillis: Long, succeeded: Int, failed: Int)

    @Query("UPDATE fire_claims SET status = 'INTERRUPTED' WHERE status = 'RUNNING' AND claimedAtMillis < :beforeMillis")
    suspend fun markStaleAsInterrupted(beforeMillis: Long): Int

    @Query("DELETE FROM fire_claims WHERE claimedAtMillis < :beforeMillis AND status != 'RUNNING'")
    suspend fun pruneBefore(beforeMillis: Long): Int
}
