package com.tdvorak.nothingmodes.quicksettings

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.IntentCompat
import com.tdvorak.nothingmodes.MainActivity
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleActions
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleSpec
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleSpecEditorScreen
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleTilePrefs

/**
 * Editor for one tile's cycle spec — a custom Quick Settings slot or a
 * built-in tile override. Opened from the Quick settings screen, when the
 * user taps an unconfigured custom tile, or via a tile long-press
 * (QS_TILE_PREFERENCES carries the tile's ComponentName in the extras).
 */
class TileConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (routeNonCycleTile()) return
        val key = intent.getStringExtra(EXTRA_KEY) ?: keyForTileComponent()
        if (key.isNullOrBlank()) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            CycleSpecEditorScreen(
                title = intent.getStringExtra(EXTRA_TITLE) ?: titleFor(key),
                initial = CycleTilePrefs.load(this, key) ?: defaultSpecFor(key),
                onSave = { spec ->
                    CycleTilePrefs.save(this, key, spec)
                    finish()
                },
                onBack = { finish() },
            )
        }
    }

    /** Tiles outside the cycle system forward to their own settings screen. */
    private fun routeNonCycleTile(): Boolean {
        if (intent.getStringExtra(EXTRA_KEY) != null) return false
        val target =
            when (tileComponent()?.className?.substringAfterLast('.')) {
                "UltraDimTileService" -> Intent(this, DimAdjustActivity::class.java)
                "NothingModesTileService" -> Intent(this, MainActivity::class.java)
                else -> return false
            }
        startActivity(target)
        finish()
        return true
    }

    /** The ComponentName the system attaches to QS_TILE_PREFERENCES. */
    private fun tileComponent(): ComponentName? =
        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)

    /** Maps a tile service component to its cycle-spec prefs key. */
    private fun keyForTileComponent(): String? =
        when (tileComponent()?.className?.substringAfterLast('.')) {
            "ScreenTimeoutTileService" -> CycleTilePrefs.builtinKey(CycleActions.screenTimeout.id)
            "BrightnessTileService" -> CycleTilePrefs.builtinKey(CycleActions.brightness.id)
            else ->
                Regex("""CustomCycleTileService(\d+)""")
                    .matchEntire(tileComponent()?.className?.substringAfterLast('.') ?: "")
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull()
                    ?.takeIf { it in 1..CycleTilePrefs.SLOT_COUNT }
                    ?.let { CycleTilePrefs.tileKey(it) }
        }

    private fun titleFor(key: String): String =
        CycleActions
            .all
            .firstOrNull { CycleTilePrefs.builtinKey(it.id) == key }
            ?.label
            ?: Regex("""tile_slot_(\d+)""")
                .matchEntire(key)
                ?.let { "Custom tile ${it.groupValues[1]}" }
            ?: "Custom tile"

    /** Built-in slots fall back to their shipping defaults in the editor. */
    private fun defaultSpecFor(key: String) =
        CycleActions
            .all
            .firstOrNull { CycleTilePrefs.builtinKey(it.id) == key }
            ?.let { CycleSpec(it.id, it.defaultSteps) }

    companion object {
        const val EXTRA_KEY = "key"
        const val EXTRA_TITLE = "title"
    }
}
