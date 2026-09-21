package com.tdvorak.nothingmodes.quicksettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleActions
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleSpec
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleSpecEditorScreen
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleTilePrefs

/**
 * Editor for one tile's cycle spec — a custom Quick Settings slot or a
 * built-in tile override. Opened from the Quick settings screen, or directly
 * when the user taps an unconfigured custom tile.
 */
class TileConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val key = intent.getStringExtra(EXTRA_KEY)
        if (key.isNullOrBlank()) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            CycleSpecEditorScreen(
                title = intent.getStringExtra(EXTRA_TITLE) ?: "Custom tile",
                initial = CycleTilePrefs.load(this, key) ?: defaultSpecFor(key),
                onSave = { spec ->
                    CycleTilePrefs.save(this, key, spec)
                    finish()
                },
                onBack = { finish() },
            )
        }
    }

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
