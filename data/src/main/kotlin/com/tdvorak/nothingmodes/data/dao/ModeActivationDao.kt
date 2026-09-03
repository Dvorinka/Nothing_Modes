package com.tdvorak.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tdvorak.nothingmodes.data.entities.ModeActivationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeActivationDao {

    @Insert
    suspend fun insert(entity: ModeActivationEntity): Long

    @Query("SELECT * FROM mode_activations WHERE modeId = :modeId AND status = 'ACTIVE' ORDER BY activatedAtMillis DESC LIMIT 1")
    suspend fun activeForMode(modeId: String): ModeActivationEntity?

    @Query("SELECT DISTINCT modeId FROM mode_activations WHERE status = 'ACTIVE'")
    suspend fun activeModeIds(): List<String>

    @Query("UPDATE mode_activations SET status = 'DEACTIVATED', deactivatedAtMillis = :atMillis WHERE modeId = :modeId AND status = 'ACTIVE'")
    suspend fun deactivate(modeId: String, atMillis: Long): Int

    @Query("SELECT * FROM mode_activations ORDER BY activatedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<ModeActivationEntity>>

    @Query("DELETE FROM mode_activations WHERE activatedAtMillis < :beforeMillis AND status != 'ACTIVE'")
    suspend fun pruneBefore(beforeMillis: Long): Int
}
