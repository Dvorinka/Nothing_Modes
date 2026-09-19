package com.tdvorak.nothingmodes.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tdvorak.nothingmodes.capabilities.controllers.UltraDimController

/**
 * Quick Settings tile for the ultra-dim overlay. Each tap cycles
 * 25% → 50% → 75% → off. Without "display over other apps" the tap opens
 * the system permission page instead.
 */
class UltraDimTileService : TileService() {
    private val steps = intArrayOf(25, 50, 75)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        if (!UltraDimController.canDim(this)) {
            val intent = UltraDimController.permissionIntent(this)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                    PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        val current = UltraDimController.percent
        val next = steps.firstOrNull { it > current } ?: 0
        if (next == 0) {
            UltraDimController.hide(this)
        } else {
            UltraDimController.show(this, next)
        }
        updateTile()
    }

    private fun updateTile() {
        qsTile?.let { tile ->
            tile.label = "Ultra dim"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle =
                    when {
                        !UltraDimController.canDim(this) -> "Needs overlay permission"
                        UltraDimController.isActive -> "${UltraDimController.percent}%"
                        else -> "Off"
                    }
            }
            tile.state =
                if (UltraDimController.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }
}
