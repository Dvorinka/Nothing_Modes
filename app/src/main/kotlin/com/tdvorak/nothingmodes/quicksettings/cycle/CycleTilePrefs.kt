package com.tdvorak.nothingmodes.quicksettings.cycle

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.service.quicksettings.TileService

/**
 * SharedPreferences store for user-defined cycle specs. Custom tile slots are
 * keyed "tile_slot_N"; home-screen widget instances are keyed "widget_<id>".
 */
object CycleTilePrefs {
    private const val PREFS = "cycle_tile_specs"

    const val SLOT_COUNT = 3

    fun tileKey(slot: Int): String = "tile_slot_$slot"

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
        prefs(context).edit().putString(key, spec?.encode()).apply()
        refreshTiles(context)
    }

    /** Ask the system to re-query every tile so a fresh spec lands at once. */
    fun refreshTiles(context: Context) {
        customTileClasses().forEach { cls ->
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
        )

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
