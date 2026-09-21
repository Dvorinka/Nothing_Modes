package com.tdvorak.nothingmodes.quicksettings

import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.tdvorak.nothingmodes.R
import com.tdvorak.nothingmodes.quicksettings.cycle.BrightnessTileService
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleActions
import com.tdvorak.nothingmodes.quicksettings.cycle.CycleTilePrefs
import com.tdvorak.nothingmodes.quicksettings.cycle.ScreenTimeoutTileService
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingCompactPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingModesThemeDynamic
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar
import com.tdvorak.nothingmodes.widget.CycleWidgetConfigActivity
import com.tdvorak.nothingmodes.widget.CycleWidgetReceiver
import java.util.concurrent.Executor

private data class TileInfo(
    val label: String,
    val description: String,
    val service: Class<*>,
    val iconRes: Int,
    /** Prefs key when the tile's steps are user-editable. */
    val editKey: String? = null,
)

/** Catalog of every surface the app offers in Quick Settings and the launcher. */
@Composable
fun QuickSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val tiles =
        listOf(
            TileInfo(
                "Ultra dim",
                "Dims below the hardware minimum. Taps cycle 25% → 50% → 75% → off. A persistent notification follows while a mode owns it.",
                UltraDimTileService::class.java,
                R.drawable.ic_tile_ultra_dim,
            ),
            TileInfo(
                "Screen timeout",
                "Cycles 30s → 1m → 5m. Tap to change the steps. Needs Write settings.",
                ScreenTimeoutTileService::class.java,
                R.drawable.ic_tile_timeout,
                editKey = CycleTilePrefs.builtinKey(CycleActions.screenTimeout.id),
            ),
            TileInfo(
                "Brightness",
                "Cycles 25% → 50% → 100%. Tap to change the steps. Needs Write settings.",
                BrightnessTileService::class.java,
                R.drawable.ic_tile_brightness,
                editKey = CycleTilePrefs.builtinKey(CycleActions.brightness.id),
            ),
        )

    val widgetIds =
        runCatching {
            context
                .getSystemService(AppWidgetManager::class.java)
                ?.getAppWidgetIds(ComponentName(context, CycleWidgetReceiver::class.java))
                ?.toList()
        }.getOrNull().orEmpty()

    NothingModesThemeDynamic {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { NothingTopBar(title = "Quick settings", onBack = onBack) },
        ) { padding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = NothingSpacing.md),
            ) {
                Text(
                    text = "QUICK SETTINGS & WIDGETS",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = NothingFonts.doto(),
                )
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Text(
                    text =
                        "Tiles live in the system shade: pull down twice, tap the edit " +
                            "(pencil) icon and drag a tile up. Each tap on a cycle tile " +
                            "applies the next step, then wraps around.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.lg))

                NothingSectionHeader(text = "Tiles")
                NothingCard {
                    tiles.forEachIndexed { index, tile ->
                        if (index > 0) NothingDivider()
                        NothingListRow(
                            title = tile.label,
                            subtitle = tile.description,
                            onClick =
                                tile.editKey?.let { key ->
                                    { openTileEditor(context, key, tile.label) }
                                },
                            trailing = { AddTileButton(context, tile) },
                        )
                    }
                }

                NothingSectionHeader(text = "Custom tiles")
                NothingCard {
                    for (slot in 1..CycleTilePrefs.SLOT_COUNT) {
                        if (slot > 1) NothingDivider()
                        val spec = CycleTilePrefs.load(context, CycleTilePrefs.tileKey(slot))
                        val action = spec?.let { CycleActions.byId(it.actionId) }
                        NothingListRow(
                            title = "Custom tile $slot",
                            subtitle =
                                when {
                                    spec == null || action == null ->
                                        "Not set. Tap to pick an action and steps."
                                    action.usesDynamicSteps -> action.label
                                    else ->
                                        action.label +
                                            " · " +
                                            spec.steps.joinToString(" → ") { action.format(it) }
                                },
                            onClick = {
                                openTileEditor(
                                    context,
                                    CycleTilePrefs.tileKey(slot),
                                    "Custom tile $slot",
                                )
                            },
                            trailing = {
                                AddTileButton(
                                    context,
                                    TileInfo(
                                        "Custom tile $slot",
                                        "",
                                        CycleTilePrefs.customTileClasses()[slot - 1],
                                        R.drawable.ic_tile_cycle,
                                    ),
                                )
                            },
                        )
                    }
                }

                NothingSectionHeader(text = "Home screen")
                NothingCard {
                    NothingListRow(
                        title = "Cycle widget",
                        subtitle =
                            "Same tap-to-cycle engine on your home screen. Pin it, " +
                                "then pick the action and steps.",
                        trailing = { PinWidgetButton(context) },
                        onClick = { pinCycleWidget(context) },
                    )
                    widgetIds.forEachIndexed { index, id ->
                        NothingDivider()
                        NothingListRow(
                            title = "Cycle widget ${index + 1}",
                            subtitle =
                                CycleTilePrefs
                                    .load(context, CycleTilePrefs.widgetKey(id))
                                    ?.let { spec ->
                                        CycleActions.byId(spec.actionId)?.label ?: "Custom"
                                    } ?: "Not set. Tap to configure.",
                            onClick = {
                                context.startActivity(
                                    Intent(context, CycleWidgetConfigActivity::class.java)
                                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id),
                                )
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(NothingSpacing.lg))
            }
        }
    }
}

private fun openTileEditor(
    context: Context,
    key: String,
    title: String,
) {
    context.startActivity(
        Intent(context, TileConfigActivity::class.java)
            .putExtra(TileConfigActivity.EXTRA_KEY, key)
            .putExtra(TileConfigActivity.EXTRA_TITLE, title),
    )
}

@Composable
private fun AddTileButton(
    context: Context,
    tile: TileInfo,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    NothingCompactPillButton(
        text = "Add",
        onClick = {
            val sbm = context.getSystemService(StatusBarManager::class.java) ?: return@NothingCompactPillButton
            runCatching {
                sbm.requestAddTileService(
                    ComponentName(context, tile.service),
                    tile.label,
                    Icon.createWithResource(context, tile.iconRes),
                    Executor { it.run() },
                ) { /* result codes ignored — the system prompt speaks for itself */ }
            }
        },
    )
}

@Composable
private fun PinWidgetButton(context: Context) {
    NothingCompactPillButton(text = "Pin", onClick = { pinCycleWidget(context) })
}

private fun pinCycleWidget(context: Context) {
    val awm = context.getSystemService(AppWidgetManager::class.java) ?: return
    if (!awm.isRequestPinAppWidgetSupported) return
    runCatching {
        awm.requestPinAppWidget(
            ComponentName(context, CycleWidgetReceiver::class.java),
            null,
            null,
        )
    }
}
