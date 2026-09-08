package com.tdvorak.nothingmodes.ui.screens

import android.content.pm.ResolveInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

private data class InstalledApp(
    val label: String,
    val pkg: String,
    val resolveInfo: ResolveInfo,
)

@Composable
fun AppPicker(
    currentPackage: String,
    onPkgChange: (String) -> Unit,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showList by remember { mutableStateOf(false) }

    val installedApps =
        remember {
            runCatching {
                val pm = context.packageManager
                val mainIntent =
                    android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    }
                pm
                    .queryIntentActivities(mainIntent, 0)
                    .map { ri ->
                        InstalledApp(
                            label = ri.loadLabel(pm).toString(),
                            pkg = ri.activityInfo.packageName,
                            resolveInfo = ri,
                        )
                    }.sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
        }

    val filteredApps =
        remember(searchQuery, installedApps) {
            if (searchQuery.isBlank()) {
                installedApps
            } else {
                installedApps.filter {
                    it.label.contains(searchQuery, ignoreCase = true) ||
                        it.pkg.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    val selectedLabel = installedApps.find { it.pkg == currentPackage }?.label ?: currentPackage

    Column {
        Text(
            text = "Selected app",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontFamily = NothingFonts.mono(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = NothingShapes.input,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { showList = !showList },
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(NothingSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = selectedLabel.ifBlank { "Tap to select an app" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = NothingFonts.mono(),
                )
                Text(
                    text = if (showList) "[CLOSE]" else "[OPEN]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
        }

        if (showList) {
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            NothingInput(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Search",
                placeholder = "Search apps...",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
            ) {
                items(filteredApps, key = { it.pkg }) { app ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = NothingShapes.input,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPkgChange(app.pkg)
                                    showList = false
                                    searchQuery = ""
                                },
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(NothingSpacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (app.pkg == currentPackage) {
                                Box(
                                    modifier =
                                        Modifier
                                            .width(2.dp)
                                            .height(20.dp)
                                            .background(NothingColors.accent),
                                )
                                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                            }
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MultiAppPicker(
    currentPackages: List<String>,
    onChange: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val selected = remember(currentPackages) { currentPackages.toSortedSet() }

    val installedApps =
        remember {
            runCatching {
                val pm = context.packageManager
                val mainIntent =
                    android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                        addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                    }
                pm
                    .queryIntentActivities(mainIntent, 0)
                    .map { ri ->
                        InstalledApp(
                            label = ri.loadLabel(pm).toString(),
                            pkg = ri.activityInfo.packageName,
                            resolveInfo = ri,
                        )
                    }.sortedBy { it.label.lowercase() }
            }.getOrDefault(emptyList())
        }

    val filteredApps =
        remember(searchQuery, installedApps) {
            if (searchQuery.isBlank()) {
                installedApps
            } else {
                installedApps.filter {
                    it.label.contains(searchQuery, ignoreCase = true) ||
                        it.pkg.contains(searchQuery, ignoreCase = true)
                }
            }
        }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Selected apps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = NothingFonts.mono(),
            )
            if (selected.isNotEmpty()) {
                NothingPillButton(
                    text = "Clear",
                    onClick = { onChange(emptyList()) },
                )
            }
        }
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = NothingShapes.input,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(NothingSpacing.md),
        ) {
            Text(
                text =
                    if (selected.isEmpty()) {
                        "Tap below to select apps"
                    } else {
                        selected.size.toString() + " app" + if (selected.size > 1) "s" else ""
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = NothingFonts.mono(),
                modifier = Modifier.padding(NothingSpacing.md),
            )
        }
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        NothingInput(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = "Search",
            placeholder = "Search apps...",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            items(filteredApps, key = { it.pkg }) { app ->
                val isSelected = app.pkg in selected
                val icon =
                    remember(app.pkg) {
                        runCatching {
                            app.resolveInfo.loadIcon(context.packageManager)
                                ?.toBitmap(96, 96)
                                ?.asImageBitmap()
                        }.getOrNull()
                    }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = NothingShapes.input,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next =
                                    if (isSelected) {
                                        selected - app.pkg
                                    } else {
                                        selected + app.pkg
                                    }
                                onChange(next.toList())
                            },
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(NothingSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null,
                        )
                        if (icon != null) {
                            Image(
                                bitmap = icon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                            )
                        } else {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = NothingFonts.mono(),
                            )
                            Text(
                                text = app.pkg,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                    }
                }
            }
        }
    }
}
