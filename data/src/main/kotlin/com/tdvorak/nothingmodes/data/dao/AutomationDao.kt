package com.tdvorak.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tdvorak.nothingmodes.data.entities.AutomationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {

    @Query("SELECT * FROM automations ORDER BY priority DESC, name ASC")
    fun observeAll(): Flow<List<AutomationEntity>>

    @Query("SELECT * FROM automations WHERE status = 'ARMED' AND enabled = 1 ORDER BY priority DESC, id ASC")
    suspend fun armed(): List<AutomationEntity>

    @Query("SELECT * FROM automations ORDER BY priority DESC, name ASC")
    suspend fun all(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun get(id: String): AutomationEntity?

    @Query("SELECT * FROM automations WHERE type = :type ORDER BY name ASC")
    suspend fun byType(type: String): List<AutomationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AutomationEntity)

    @Query("UPDATE automations SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("UPDATE automations SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("UPDATE automations SET lastFiredAt = :atMillis WHERE id = :id")
    suspend fun recordFired(id: String, atMillis: Long)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM automations WHERE status = 'ARMED' AND enabled = 1")
    suspend fun armedCount(): Int
}
