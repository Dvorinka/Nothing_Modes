package com.tdvorak.nothingmodes.quicksettings.cycle

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.service.quicksettings.TileService

/**
 * SharedPreferences store for user-defined cycle specs. Custom tile slots are
 * keyed "tile_slot_N", built-in overrides "tile_builtin_<actionId>", widget
 * instances "widget_<id>", and rotation cursors for stateless actions
 * "cursor_<key>".
 */
object CycleTilePrefs {
    private const val PREFS = "cycle_tile_specs"
    private const val CURSOR_PREFIX = "cursor_"

    const val SLOT_COUNT = 6

    fun tileKey(slot: Int): String = "tile_slot_$slot"

    /** Override slot for a built-in tile — empty/absent means the defaults run. */
    fun builtinKey(actionId: String): String = "tile_builtin_$actionId"

    fun widgetKey(appWidgetId: Int): String = "widget_$appWidgetId"

    fun load(
        context: Context,
        key: String,
    ): CycleSpec? = CycleSpec.decode(prefs(context).getString(key, null))

    fun save(
        context: Context,
        key: String,
        spec: CycleSpec?,
    ) {
        prefs(context)
            .edit()
            .apply {
                if (spec == null) remove(key) else putString(key, spec.encode())
            }.apply()
        refreshTiles(context)
    }

    /** Last applied step for stateless actions (the mode runner). */
    fun cursor(
        context: Context,
        key: String,
    ): String? = prefs(context).getString(CURSOR_PREFIX + key, null)

    fun saveCursor(
        context: Context,
        key: String,
        value: String,
    ) {
        prefs(context).edit().putString(CURSOR_PREFIX + key, value).apply()
    }

    /** Ask the system to re-query every tile so a fresh spec lands at once. */
    fun refreshTiles(context: Context) {
        (customTileClasses() + builtinTileClasses()).forEach { cls ->
            runCatching {
                TileService.requestListeningState(context, ComponentName(context, cls))
            }
        }
    }

    /** Manifest components for the configurable slots. */
    fun customTileClasses(): List<Class<*>> =
        listOf(
            CustomCycleTileService1::class.java,
            CustomCycleTileService2::class.java,
            CustomCycleTileService3::class.java,
            CustomCycleTileService4::class.java,
            CustomCycleTileService5::class.java,
            CustomCycleTileService6::class.java,
        )

    private fun builtinTileClasses(): List<Class<*>> =
        listOf(
            ScreenTimeoutTileService::class.java,
            BrightnessTileService::class.java,
        )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
