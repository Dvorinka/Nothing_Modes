package com.tdvorak.nothingmodes.quicksettings

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tdvorak.nothingmodes.MainActivity
import com.tdvorak.nothingmodes.automation.quickactions.QuickActionTrigger
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore
import com.tdvorak.nothingmodes.widget.WidgetEntryPoint
import dagger.hilt.EntryPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NothingModesTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun store(): AutomationStore {
        return EntryPoints.get(applicationContext, WidgetEntryPoint::class.java).automationStore()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    override fun onClick() {
        super.onClick()
        scope.launch {
            val automations = runCatching {
                store().all().filter {
                    it.quickAction && it.enabled && it.status == AutomationStatus.ARMED && it.trigger is Trigger.Manual
                }
            }.getOrDefault(emptyList())

            if (automations.isNotEmpty()) {
                QuickActionTrigger.run(this@NothingModesTileService, automations.first().id.value)
            } else {
                openAppOrFallback()
            }

            qsTile?.let { tile ->
                tile.state = if (automations.isNotEmpty()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.updateTile()
            }
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openAppOrFallback() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pending = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            startActivityAndCollapse(pending)
        } else {
            startActivity(intent)
        }
    }

    private fun updateTile() {
        scope.launch {
            val automations = runCatching {
                store().all().filter {
                    it.quickAction && it.enabled && it.status == AutomationStatus.ARMED && it.trigger is Trigger.Manual
                }
            }.getOrDefault(emptyList())

            qsTile?.let { tile ->
                tile.label = if (automations.isNotEmpty()) automations.first().name else "Nothing Modes"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = if (automations.isNotEmpty()) "${automations.size} quick action(s)" else "No quick actions"
                }
                tile.state = if (automations.isNotEmpty()) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.updateTile()
            }
        }
    }
}
