package com.tdvorak.nothingmodes.widget

import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun automationStore(): AutomationStore
}
