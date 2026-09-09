package com.tdvorak.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tdvorak.nothingmodes.data.entities.NotificationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Insert
    suspend fun insert(entity: NotificationLogEntity): Long

    @Query("SELECT * FROM notification_log ORDER BY postTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 100): List<NotificationLogEntity>

    @Query("SELECT * FROM notification_log ORDER BY postTime DESC LIMIT :limit")
    fun getRecentAsFlow(limit: Int = 100): Flow<List<NotificationLogEntity>>
}
