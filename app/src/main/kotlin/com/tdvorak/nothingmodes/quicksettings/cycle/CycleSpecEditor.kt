package com.tdvorak.nothingmodes.quicksettings.cycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingCheckbox
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingGhostButton
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingModesThemeDynamic
import com.tdvorak.nothingmodes.ui.theme.NothingPrimaryButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.NothingTopBar

/**
 * Shared editor for a tap-to-cycle spec: pick an action, then its ordered
 * steps. Enum actions render checkboxes; numeric actions take a
 * comma-separated shorthand list. Used by [TileConfigActivity] for custom
 * Quick Settings slots and by the cycle-widget config activity.
 */
@Composable
fun CycleSpecEditorScreen(
    title: String,
    initial: CycleSpec?,
    onSave: (CycleSpec?) -> Unit,
    onBack: () -> Unit,
) {
    var actionId by remember { mutableStateOf(initial?.actionId ?: CycleActions.screenTimeout.id) }
    val action = CycleActions.byId(actionId) ?: CycleActions.screenTimeout
    var selected by remember { mutableStateOf(initial?.steps?.toSet() ?: action.defaultSteps.toSet()) }
    var stepsText by remember {
        mutableStateOf(initial?.steps?.joinToString(", ") { action.format(it) } ?: "")
    }
    var error by remember { mutableStateOf<String?>(null) }

    NothingModesThemeDynamic {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NothingTopBar(title = title, onBack = onBack)
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(NothingSpacing.md),
                ) {
                    NothingEnumSelector(
                        label = "Action",
                        value = action.label,
                        options = CycleActions.all.map { it.label },
                        onSelect = { label ->
                            CycleActions.all.firstOrNull { it.label == label }?.let { picked ->
                                actionId = picked.id
                                selected = picked.defaultSteps.toSet()
                                stepsText = ""
                                error = null
                            }
                        },
                        infoText = "Each tap on the tile or widget applies the next step, then wraps around.",
                    )
                    Spacer(modifier = Modifier.height(NothingSpacing.md))

                    NothingCard {
                        val allowed = action.allowedValues
                        if (allowed != null) {
                            NothingLabel(text = "STEPS (IN ORDER)")
                            Spacer(modifier = Modifier.height(NothingSpacing.xs))
                            allowed.forEachIndexed { index, value ->
                                if (index > 0) NothingDivider()
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = NothingSpacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = action.format(value),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = NothingFonts.mono(),
                                    )
                                    NothingCheckbox(
                                        checked = value in selected,
                                        onCheckedChange = { on ->
                                            selected =
                                                if (on) selected + value else selected - value
                                        },
                                    )
                                }
                            }
                        } else {
                            NothingInput(
                                value = stepsText,
                                onValueChange = {
                                    stepsText = it
                                    error = null
                                },
                                label = "STEPS",
                                placeholder = action.stepsHint,
                                infoText = "Comma-separated. Each tap moves to the next value.",
                            )
                        }
                    }

                    val resolvedSteps =
                        action.allowedValues?.filter { it in selected }
                            ?: stepsText.takeIf { it.isNotBlank() }?.let { action.parseSteps(it) }

                    Spacer(modifier = Modifier.height(NothingSpacing.sm))
                    Text(
                        text =
                            if (resolvedSteps.isNullOrEmpty()) {
                                "Add at least one step"
                            } else {
                                "Cycles: " + resolvedSteps.joinToString(" → ") { action.format(it) }
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            if (resolvedSteps.isNullOrEmpty()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        fontFamily = NothingFonts.mono(),
                    )
                    error?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontFamily = NothingFonts.mono(),
                        )
                    }

                    Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        NothingPrimaryButton(
                            text = "Save",
                            onClick = {
                                val steps = resolvedSteps
                                if (steps.isNullOrEmpty()) {
                                    error = "Could not parse the steps — check the format"
                                    return@NothingPrimaryButton
                                }
                                onSave(CycleSpec(actionId, steps))
                            },
                        )
                        if (initial != null) {
                            NothingGhostButton(text = "Clear", onClick = { onSave(null) })
                        }
                    }
                }
            }
        }
    }
}
