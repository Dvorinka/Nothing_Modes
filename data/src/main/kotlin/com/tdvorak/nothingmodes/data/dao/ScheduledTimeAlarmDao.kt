package com.tdvorak.nothingmodes.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tdvorak.nothingmodes.data.entities.ScheduledTimeAlarmEntity

@Dao
interface ScheduledTimeAlarmDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduledTimeAlarmEntity)

    @Query("SELECT * FROM scheduled_time_alarms WHERE automationId = :automationId")
    suspend fun get(automationId: String): ScheduledTimeAlarmEntity?

    @Query("SELECT * FROM scheduled_time_alarms WHERE wakeAtMillis <= :atMillis AND scheduledMode = :mode")
    suspend fun dueBefore(atMillis: Long, mode: String): List<ScheduledTimeAlarmEntity>

    @Query("DELETE FROM scheduled_time_alarms WHERE automationId = :automationId")
    suspend fun delete(automationId: String)

    @Query("DELETE FROM scheduled_time_alarms WHERE wakeAtMillis < :beforeMillis")
    suspend fun pruneBefore(beforeMillis: Long): Int
}
