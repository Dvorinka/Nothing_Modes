package com.dvoranka.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dvoranka.nothingmodes.data.entities.AuditEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditDao {

    @Insert
    suspend fun insert(entity: AuditEntity): Long

    @Query("SELECT * FROM audit_log ORDER BY atMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<AuditEntity>>

    @Query("SELECT * FROM audit_log WHERE automationId = :id ORDER BY atMillis DESC LIMIT :limit")
    suspend fun forAutomation(id: String, limit: Int = 100): List<AuditEntity>

    @Query("SELECT * FROM audit_log WHERE executionId = :executionId ORDER BY atMillis ASC")
    suspend fun forExecution(executionId: String): List<AuditEntity>

    @Query("DELETE FROM audit_log WHERE atMillis < :beforeMillis")
    suspend fun pruneBefore(beforeMillis: Long): Int
}
