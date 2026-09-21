package com.tdvorak.nothingmodes.widget

import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Singleton-graph access for system surfaces (widgets, tiles, notifications). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun automationStore(): AutomationStore

    fun modeActivationProvider(): ModeActivationProvider
}
