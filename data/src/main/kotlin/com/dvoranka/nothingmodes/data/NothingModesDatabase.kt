package com.dvoranka.nothingmodes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dvoranka.nothingmodes.data.dao.AuditDao
import com.dvoranka.nothingmodes.data.dao.AutomationDao
import com.dvoranka.nothingmodes.data.dao.DraftDao
import com.dvoranka.nothingmodes.data.dao.ExecutionJournalDao
import com.dvoranka.nothingmodes.data.dao.FireClaimDao
import com.dvoranka.nothingmodes.data.dao.ModeActivationDao
import com.dvoranka.nothingmodes.data.dao.ScheduledTimeAlarmDao
import com.dvoranka.nothingmodes.data.dao.StateSnapshotDao
import com.dvoranka.nothingmodes.data.entities.ActionResultEntity
import com.dvoranka.nothingmodes.data.entities.AuditEntity
import com.dvoranka.nothingmodes.data.entities.AutomationEntity
import com.dvoranka.nothingmodes.data.entities.FireClaimEntity
import com.dvoranka.nothingmodes.data.entities.ModeActivationEntity
import com.dvoranka.nothingmodes.data.entities.PendingDraftEntity
import com.dvoranka.nothingmodes.data.entities.ScheduledTimeAlarmEntity
import com.dvoranka.nothingmodes.data.entities.StateSnapshotEntity

@Database(
    entities = [
        AutomationEntity::class,
        AuditEntity::class,
        FireClaimEntity::class,
        PendingDraftEntity::class,
        ActionResultEntity::class,
        ScheduledTimeAlarmEntity::class,
        StateSnapshotEntity::class,
        ModeActivationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NothingModesDatabase : RoomDatabase() {
    abstract fun automationDao(): AutomationDao
    abstract fun auditDao(): AuditDao
    abstract fun draftDao(): DraftDao
    abstract fun executionJournalDao(): ExecutionJournalDao
    abstract fun fireClaimDao(): FireClaimDao
    abstract fun scheduledTimeAlarmDao(): ScheduledTimeAlarmDao
    abstract fun stateSnapshotDao(): StateSnapshotDao
    abstract fun modeActivationDao(): ModeActivationDao

    companion object {
        fun build(context: Context, name: String = "nothing_modes.db"): NothingModesDatabase =
            Room.databaseBuilder(context, NothingModesDatabase::class.java, name)
                .build()

        fun inMemory(context: Context): NothingModesDatabase =
            Room.inMemoryDatabaseBuilder(context, NothingModesDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
