package com.tdvorak.nothingmodes.quicksettings.cycle

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tdvorak.nothingmodes.capabilities.controllers.ControllerResult
import com.tdvorak.nothingmodes.quicksettings.TileConfigActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base for tap-to-cycle Quick Settings tiles. Each tap applies the next step
 * of a [CycleSpec] — e.g. screen timeout 30s → 1m → 5m → back to 30s.
 * Built-ins pin a spec; the Custom* services read theirs from [CycleTilePrefs]
 * so the user can rebind a slot to any registered [CycleAction].
 */
abstract class CycleTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** The spec this tile runs. Null = unconfigured custom slot. */
    protected abstract fun spec(): CycleSpec?

    /** Which custom slot (1..N) this service is, 0 for built-ins. */
    protected open val slot: Int = 0

    override fun onStartListening() {
        super.onStartListening()
        scope.launch { updateTile() }
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val spec = spec()
            if (spec == null) {
                // Unconfigured slot — drop the user straight into the editor.
                launchAndCollapse(
                    Intent(this@CycleTileService, TileConfigActivity::class.java)
                        .putExtra(TileConfigActivity.EXTRA_SLOT, slot),
                )
                return@launch
            }
            val action = CycleActions.byId(spec.actionId) ?: return@launch
            action.permissionFix(this@CycleTileService)?.let { fix ->
                launchAndCollapse(fix)
                return@launch
            }
            val result =
                withContext(Dispatchers.IO) {
                    CycleEngine.advance(this@CycleTileService, spec)
                }
            lastError =
                when (result) {
                    is ControllerResult.Failure -> result.reason
                    is ControllerResult.PermissionRequired -> "Needs permission"
                    else -> null
                }
            updateTile()
        }
    }

    @Volatile
    private var lastError: String? = null

    private suspend fun updateTile() {
        val tile = qsTile ?: return
        val spec = spec()
        val action = spec?.let { CycleActions.byId(it.actionId) }
        if (spec == null || action == null) {
            tile.label = "Custom tile $slot"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = "Tap to set up"
            tile.state = Tile.STATE_INACTIVE
            tile.updateTile()
            return
        }

        tile.label = action.label
        tile.icon = Icon.createWithResource(this, action.iconRes)
        val missingPermission = action.permissionFix(this) != null
        val current =
            if (missingPermission) {
                null
            } else {
                withContext(Dispatchers.IO) { action.current(this@CycleTileService) }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle =
                when {
                    missingPermission -> "Needs permission"
                    lastError != null -> "Failed"
                    current != null -> action.format(current)
                    else -> spec.steps.joinToString(" › ") { action.format(it) }
                }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            tile.stateDescription = tile.subtitle
        }
        tile.state =
            when {
                missingPermission || current == null -> Tile.STATE_INACTIVE
                action.isOn(current) -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun launchAndCollapse(intent: Intent) {
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
    }
}

/** Screen timeout cycler: 30s → 1m → 5m → wrap. Needs Write Settings. */
class ScreenTimeoutTileService : CycleTileService() {
    override fun spec(): CycleSpec =
        CycleSpec(CycleActions.screenTimeout.id, CycleActions.screenTimeout.defaultSteps)
}

/** Brightness cycler: auto → 25% → 50% → 100% → wrap. Needs Write Settings. */
class BrightnessTileService : CycleTileService() {
    override fun spec(): CycleSpec =
        CycleSpec(CycleActions.brightness.id, CycleActions.brightness.defaultSteps)
}

/** User-configured cycle slot 1. */
class CustomCycleTileService1 : CycleTileService() {
    override val slot = 1
    override fun spec(): CycleSpec? = CycleTilePrefs.load(this, CycleTilePrefs.tileKey(1))
}

/** User-configured cycle slot 2. */
class CustomCycleTileService2 : CycleTileService() {
    override val slot = 2
    override fun spec(): CycleSpec? = CycleTilePrefs.load(this, CycleTilePrefs.tileKey(2))
}

/** User-configured cycle slot 3. */
class CustomCycleTileService3 : CycleTileService() {
    override val slot = 3
    override fun spec(): CycleSpec? = CycleTilePrefs.load(this, CycleTilePrefs.tileKey(3))
}
