package com.tdvorak.nothingmodes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.tdvorak.nothingmodes.data.dao.AuditDao
import com.tdvorak.nothingmodes.data.dao.AutomationDao
import com.tdvorak.nothingmodes.data.dao.DraftDao
import com.tdvorak.nothingmodes.data.dao.ExecutionJournalDao
import com.tdvorak.nothingmodes.data.dao.FireClaimDao
import com.tdvorak.nothingmodes.data.dao.ModeActivationDao
import com.tdvorak.nothingmodes.data.dao.NotificationLogDao
import com.tdvorak.nothingmodes.data.dao.ScheduledTimeAlarmDao
import com.tdvorak.nothingmodes.data.dao.StateSnapshotDao
import com.tdvorak.nothingmodes.data.entities.ActionResultEntity
import com.tdvorak.nothingmodes.data.entities.AuditEntity
import com.tdvorak.nothingmodes.data.entities.AutomationEntity
import com.tdvorak.nothingmodes.data.entities.FireClaimEntity
import com.tdvorak.nothingmodes.data.entities.ModeActivationEntity
import com.tdvorak.nothingmodes.data.entities.NotificationLogEntity
import com.tdvorak.nothingmodes.data.entities.PendingDraftEntity
import com.tdvorak.nothingmodes.data.entities.ScheduledTimeAlarmEntity
import com.tdvorak.nothingmodes.data.entities.StateSnapshotEntity

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
        NotificationLogEntity::class,
    ],
    version = 3,
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

    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL("ALTER TABLE audit_log ADD COLUMN latencyMillis INTEGER NOT NULL DEFAULT 0")
                }
            }

        val MIGRATION_2_3: Migration =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS notification_log (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            packageName TEXT NOT NULL,
                            title TEXT NOT NULL,
                            text TEXT NOT NULL,
                            postTime INTEGER NOT NULL
                        )
                        """.trimIndent(),
                    )
                }
            }

        fun build(
            context: Context,
            name: String = "nothing_modes.db",
        ): NothingModesDatabase =
            Room
                .databaseBuilder(context, NothingModesDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()

        fun inMemory(context: Context): NothingModesDatabase =
            Room
                .inMemoryDatabaseBuilder(context, NothingModesDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}
