package com.tdvorak.nothingmodes.automation.di

import android.content.Context
import com.tdvorak.nothingmodes.automation.scheduler.AutomationScheduler
import com.tdvorak.nothingmodes.capabilities.controllers.RealActionExecutor
import com.tdvorak.nothingmodes.data.NothingModesDatabase
import com.tdvorak.nothingmodes.data.RoomAuditSink
import com.tdvorak.nothingmodes.data.RoomAutomationStore
import com.tdvorak.nothingmodes.data.RoomExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.AuditSink
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.ExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.StateProvider
import com.tdvorak.nothingmodes.engine.runtime.StableExecutionIdFactory
import com.tdvorak.nothingmodes.nothing.NothingGlyphMatrixProvider
import com.tdvorak.nothingmodes.nothing.NothingGlyphProvider
import com.tdvorak.nothingmodes.shizuku.PrivilegedShell
import com.tdvorak.nothingmodes.shizuku.ShizukuGateway
import com.tdvorak.nothingmodes.shizuku.ShizukuGatewayStatus
import com.tdvorak.nothingmodes.shizuku.ShizukuPrivilegedShell
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
        RoomExecutionJournal(db.executionJournalDao())

    @Provides
    @Singleton
    fun provideShizukuGateway(@ApplicationContext context: Context): ShizukuGateway =
        ShizukuGateway(context)

    @Provides
    @Singleton
    fun providePrivilegedShell(
        @ApplicationContext context: Context,
        gateway: ShizukuGateway,
    ): PrivilegedShell? = if (gateway.status() == ShizukuGatewayStatus.AUTHORIZED) {
        ShizukuPrivilegedShell(context, gateway, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    } else null

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
        shell: PrivilegedShell?,
        glyphProvider: NothingGlyphProvider,
        glyphMatrixProvider: NothingGlyphMatrixProvider,
    ): ActionExecutor = RealActionExecutor.create(
        context = context,
        shell = shell,
        glyphProvider = glyphProvider,
        glyphMatrixProvider = glyphMatrixProvider,
    )

    @Provides
    @Singleton
    fun provideStateProvider(@ApplicationContext context: Context): StateProvider =
        com.tdvorak.nothingmodes.capabilities.controllers.AndroidStateProvider(context)

    @Provides
    @Singleton
    fun provideEngine(
        store: AutomationStore,
        executor: ActionExecutor,
        audit: AuditSink,
        journal: ExecutionJournal,
        stateProvider: StateProvider,
    ): Engine = Engine(
        store = store,
        executor = executor,
        audit = audit,
        journal = journal,
        stateProvider = stateProvider,
        executionIds = StableExecutionIdFactory,
    )

    @Provides
    @Singleton
    fun provideScheduler(@ApplicationContext context: Context): AutomationScheduler =
        AutomationScheduler(context)
}
