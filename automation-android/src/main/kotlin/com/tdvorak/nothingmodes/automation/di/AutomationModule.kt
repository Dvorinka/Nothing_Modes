package com.tdvorak.nothingmodes.automation.di

import android.content.Context
import com.tdvorak.nothingmodes.automation.lifecycle.AutomationService
import com.tdvorak.nothingmodes.automation.scheduler.AutomationScheduler
import com.tdvorak.nothingmodes.capabilities.controllers.RealActionExecutor
import com.tdvorak.nothingmodes.data.RoomAutomationStore
import com.tdvorak.nothingmodes.data.NothingModesDatabase
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.AuditSink
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.ExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.NoopAuditSink
import com.tdvorak.nothingmodes.engine.runtime.NoopExecutionJournal
import com.tdvorak.nothingmodes.engine.runtime.StableExecutionIdFactory
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
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
    fun provideActionExecutor(@ApplicationContext context: Context): ActionExecutor =
        RealActionExecutor.create(context)

    @Provides
    @Singleton
    fun provideAuditSink(): AuditSink = NoopAuditSink

    @Provides
    @Singleton
    fun provideExecutionJournal(): ExecutionJournal = NoopExecutionJournal

    @Provides
    @Singleton
    fun provideEngine(
        store: AutomationStore,
        executor: ActionExecutor,
        audit: AuditSink,
        journal: ExecutionJournal,
    ): Engine = Engine(
        store = store,
        executor = executor,
        audit = audit,
        journal = journal,
        executionIds = StableExecutionIdFactory,
    )

    @Provides
    @Singleton
    fun provideScheduler(@ApplicationContext context: Context): AutomationScheduler =
        AutomationScheduler(context)
}
