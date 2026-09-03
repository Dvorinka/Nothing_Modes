package com.tdvorak.nothingmodes.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages theme preference (dark/light/system) using SharedPreferences.
 * Singleton — call [ThemeManager.init] once at app startup, then use [instance].
 */
class ThemeManager private constructor(private val context: Context) {

    enum class ThemeMode { SYSTEM, DARK, LIGHT }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _mode.value = mode
    }

    private fun loadMode(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
    }.getOrDefault(ThemeMode.SYSTEM)

    companion object {
        private const val PREFS_NAME = "nothing_modes_prefs"
        private const val KEY_THEME = "theme_mode"

        @Volatile
        private var _instance: ThemeManager? = null

        fun init(context: Context): ThemeManager {
            return _instance ?: synchronized(this) {
                _instance ?: ThemeManager(context.applicationContext).also { _instance = it }
            }
        }

        val instance: ThemeManager
            get() = _instance ?: error("ThemeManager.init() must be called first")
    }
}
