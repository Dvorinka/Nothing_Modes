package com.tdvorak.nothingmodes.ui.theme

import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages theme preference (dark/light/system) using SharedPreferences.
 * Singleton — call [ThemeManager.init] once at app startup, then use [instance].
 */
class ThemeManager private constructor(
    private val context: Context,
) {
    enum class ThemeMode { SYSTEM, DARK, LIGHT }

    /** Visual language. AUTO resolves to NOTHING on Nothing devices, CLASSIC elsewhere. */
    enum class UiStyle { AUTO, NOTHING, CLASSIC }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(loadMode())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    private val _uiStyle = MutableStateFlow(loadUiStyle())
    val uiStyle: StateFlow<UiStyle> = _uiStyle.asStateFlow()

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _mode.value = mode
    }

    fun setUiStyle(style: UiStyle) {
        prefs.edit().putString(KEY_UI_STYLE, style.name).apply()
        _uiStyle.value = style
    }

    /** Effective style after AUTO resolution. NOTHING on Nothing devices, CLASSIC elsewhere. */
    fun resolvedUiStyle(): UiStyle =
        when (_uiStyle.value) {
            UiStyle.AUTO ->
                if (Build.MANUFACTURER.equals("nothing", ignoreCase = true)) {
                    UiStyle.NOTHING
                } else {
                    UiStyle.CLASSIC
                }
            else -> _uiStyle.value
        }

    private fun loadMode(): ThemeMode =
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!)
        }.getOrDefault(ThemeMode.SYSTEM)

    private fun loadUiStyle(): UiStyle =
        runCatching {
            UiStyle.valueOf(prefs.getString(KEY_UI_STYLE, UiStyle.AUTO.name)!!)
        }.getOrDefault(UiStyle.AUTO)

    companion object {
        private const val PREFS_NAME = "nothing_modes_prefs"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_UI_STYLE = "ui_style"

        @Volatile
        private var _instance: ThemeManager? = null

        fun init(context: Context): ThemeManager =
            _instance ?: synchronized(this) {
                _instance ?: ThemeManager(context.applicationContext).also { _instance = it }
            }

        val instance: ThemeManager
            get() = _instance ?: error("ThemeManager.init() must be called first")
    }
}
