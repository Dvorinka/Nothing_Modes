package com.tdvorak.nothingmodes

import android.app.Application
import com.tdvorak.nothingmodes.automation.seed.SeedAutomations
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.prefs.ThemeManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NothingModesApplication : Application() {

    @Inject lateinit var store: AutomationStore

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        ThemeManager.init(this)
        appScope.launch { SeedAutomations.seedIfEmpty(store) }
    }
}
