package com.tdvorak.nothingmodes.automation.di

import android.content.Context
import com.tdvorak.nothingmodes.automation.scheduler.AutomationScheduler
import com.tdvorak.nothingmodes.capabilities.controllers.RealActionExecutor
import com.tdvorak.nothingmodes.data.NothingModesDatabase
import com.tdvorak.nothingmodes.data.RoomAuditSink
import com.tdvorak.nothingmodes.data.RoomAutomationStore
import com.tdvorak.nothingmodes.data.RoomExecutionJournal
import com.tdvorak.nothingmodes.data.RoomStateSnapshotStore
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.AuditSink
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.ExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.SettingReader
import com.tdvorak.nothingmodes.engine.runtime.StateProvider
import com.tdvorak.nothingmodes.engine.runtime.StateSnapshotStore
import com.tdvorak.nothingmodes.engine.runtime.StableExecutionIdFactory
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.nothing.NothingGlyphProvider
import com.tdvorak.nothingmodes.shizuku.PrivilegedShellFactory
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AutomationModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NothingModesDatabase =
        NothingModesDatabase.build(context)

    @Provides
    @Singleton
    fun provideAutomationStore(db: NothingModesDatabase): AutomationStore =
        RoomAutomationStore(db.automationDao())

    @Provides
    @Singleton
    fun provideAuditSink(db: NothingModesDatabase): AuditSink =
        RoomAuditSink(db.auditDao())

    @Provides
    @Singleton
    fun provideExecutionJournal(db: NothingModesDatabase): ExecutionJournal =
        RoomExecutionJournal(db.executionJournalDao(), db.fireClaimDao())

    @Provides
    @Singleton
    fun provideShizukuGateway(@ApplicationContext context: Context): ShizukuGateway =
        ShizukuGateway(context)

    @Provides
    @Singleton
    fun providePrivilegedShellFactory(
        @ApplicationContext context: Context,
        gateway: ShizukuGateway,
    ): PrivilegedShellFactory = PrivilegedShellFactory(context, gateway)

    @Provides
    @Singleton
    fun provideGlyphProvider(@ApplicationContext context: Context): NothingGlyphProvider =
        NothingGlyphProvider(context).also { it.init() }

    @Provides
    @Singleton
    fun provideGlyphMatrixProvider(@ApplicationContext context: Context): NothingGlyphMatrixProvider =
        NothingGlyphMatrixProvider(context).also { it.init() }

    @Provides
    @Singleton
    fun provideActionExecutor(
        @ApplicationContext context: Context,
        shellFactory: PrivilegedShellFactory,
        glyphProvider: NothingGlyphProvider,
        glyphMatrixProvider: NothingGlyphMatrixProvider,
    ): ActionExecutor = RealActionExecutor.create(
        context = context,
        shellFactory = shellFactory,
        glyphProvider = glyphProvider,
        glyphMatrixProvider = glyphMatrixProvider,
    )

    @Provides
    @Singleton
    fun provideModeActivationProvider(db: NothingModesDatabase): com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider =
        com.tdvorak.nothingmodes.data.RoomModeActivationProvider(db.modeActivationDao())

    @Provides
    @Singleton
    fun provideModeActivationSink(db: NothingModesDatabase): com.tdvorak.nothingmodes.engine.runtime.ModeActivationSink =
        com.tdvorak.nothingmodes.data.RoomModeActivationSink(db.modeActivationDao(), db)

    @Provides
    @Singleton
    fun provideStateProvider(
        @ApplicationContext context: Context,
        modeActivationProvider: com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider,
    ): StateProvider =
        com.tdvorak.nothingmodes.capabilities.controllers.AndroidStateProvider(context, modeActivationProvider)

    @Provides
    @Singleton
    fun provideSettingReader(@ApplicationContext context: Context): SettingReader =
        com.tdvorak.nothingmodes.capabilities.controllers.AndroidSettingReader(context)

    @Provides
    @Singleton
    fun provideSnapshotStore(db: NothingModesDatabase): StateSnapshotStore =
        RoomStateSnapshotStore(db.stateSnapshotDao())

    @Provides
    @Singleton
    fun provideEngine(
        store: AutomationStore,
        executor: ActionExecutor,
        audit: AuditSink,
        journal: ExecutionJournal,
        stateProvider: StateProvider,
        snapshotStore: StateSnapshotStore,
        settingReader: SettingReader,
        modeActivationSink: com.tdvorak.nothingmodes.engine.runtime.ModeActivationSink,
    ): Engine = Engine(
        store = store,
        executor = executor,
        audit = audit,
        journal = journal,
        stateProvider = stateProvider,
        snapshotStore = snapshotStore,
        settingReader = settingReader,
        modeActivationSink = modeActivationSink,
        executionIds = StableExecutionIdFactory,
    )

    @Provides
    @Singleton
    fun provideScheduler(@ApplicationContext context: Context): AutomationScheduler =
        AutomationScheduler(context)
}
