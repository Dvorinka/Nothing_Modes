package com.dvoranka.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dvoranka.nothingmodes.data.entities.PendingDraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PendingDraftEntity)

    @Query("SELECT * FROM pending_drafts ORDER BY updatedAtMillis DESC")
    fun observeAll(): Flow<List<PendingDraftEntity>>

    @Query("SELECT * FROM pending_drafts WHERE id = :id")
    suspend fun get(id: String): PendingDraftEntity?

    @Query("SELECT * FROM pending_drafts WHERE automationId = :automationId")
    suspend fun forAutomation(automationId: String): PendingDraftEntity?

    @Query("DELETE FROM pending_drafts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM pending_drafts WHERE updatedAtMillis < :beforeMillis AND quarantineCode IS NOT NULL")
    suspend fun pruneQuarantinedBefore(beforeMillis: Long): Int
}
