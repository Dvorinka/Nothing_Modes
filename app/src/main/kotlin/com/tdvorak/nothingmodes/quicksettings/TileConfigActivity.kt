package com.tdvorak.nothingmodes.quicksettings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleSpecEditorScreen
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleTilePrefs

/**
 * Editor for one custom Quick Settings tile slot. Opened from the Quick
 * settings screen, or directly when the user taps an unconfigured tile.
 */
class TileConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val slot = intent.getIntExtra(EXTRA_SLOT, 0)
        if (slot !in 1..CycleTilePrefs.SLOT_COUNT) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            CycleSpecEditorScreen(
                title = "Custom tile $slot",
                initial = CycleTilePrefs.load(this, CycleTilePrefs.tileKey(slot)),
                onSave = { spec ->
                    CycleTilePrefs.save(this, CycleTilePrefs.tileKey(slot), spec)
                    finish()
                },
                onBack = { finish() },
            )
        }
    }

    companion object {
        const val EXTRA_SLOT = "slot"
    }
}
