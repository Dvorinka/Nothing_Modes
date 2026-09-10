package com.tdvorak.nothingmodes.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.ui.theme.NothingCard
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingIconCircle
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingRequirementBadge
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

/** A single catalog row: label, category grouping, icon, one-line description,
 *  and capability badges (e.g. "SHIZUKU", "LOC") shown when the entry cannot run. */
data class CatalogEntry(
    val label: String,
    val category: String,
    val icon: ImageVector,
    val description: String = "",
    val badges: List<String> = emptyList(),
    /** Red-accented icon circle — used for the Glyph group, the app's signature. */
    val accent: Boolean = false,
)

/** An extra caller-defined chip filter (e.g. "No Shizuku", "Needs setup").
 *  Selected by label so it survives recomposition and process death. */
data class CatalogFilter(
    val label: String,
    val matches: (CatalogEntry) -> Boolean,
)

/** Saver for the active category chip set. */
private val CatalogCategorySaver: Saver<Set<String>, String> =
    Saver(
        save = { it.joinToString(",") },
        restore = { if (it.isEmpty()) emptySet() else it.split(",").toSet() },
    )

/**
 * One unified picker list used by the trigger, condition, and action catalogs.
 * Renders a search field, category chips, and category-grouped cards of
 * [CatalogRow]s. The caller supplies the entries and the click handler, so each
 * kind (trigger/condition/action) limits what is shown while the look stays
 * identical.
 */
@Composable
fun CatalogPickerContent(
    entries: List<CatalogEntry>,
    onSelect: (CatalogEntry) -> Unit,
    modifier: Modifier = Modifier,
    searchPlaceholder: String = "Find an item",
    categoryOrder: List<String> = emptyList(),
    extraFilters: List<CatalogFilter> = emptyList(),
    isSelected: (CatalogEntry) -> Boolean = { false },
    selectedTray: (@Composable () -> Unit)? = null,
    horizontalPadding: androidx.compose.ui.unit.Dp = NothingSpacing.md,
    bottomPadding: androidx.compose.ui.unit.Dp = 160.dp,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var activeCategories by rememberSaveable(stateSaver = CatalogCategorySaver) { mutableStateOf(emptySet<String>()) }
    var activeFilterLabels by rememberSaveable(stateSaver = CatalogCategorySaver) { mutableStateOf(emptySet<String>()) }

    val categories = remember(entries) { entries.map { it.category }.distinct() }
    val activeExtraFilters = extraFilters.filter { it.label in activeFilterLabels }
    val filtered =
        remember(query, activeCategories, activeFilterLabels, entries, extraFilters) {
            entries.filter {
                val matchesQuery =
                    query.isBlank() ||
                        it.label.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
                val matchesCategory = activeCategories.isEmpty() || it.category in activeCategories
                val matchesExtras = activeExtraFilters.all { f -> f.matches(it) }
                matchesQuery && matchesCategory && matchesExtras
            }
        }
    val grouped = filtered.groupBy { it.category.uppercase() }
    val orderedCategories =
        remember(grouped, categoryOrder) {
            val preferred = categoryOrder.map { it.uppercase() }.filter { it in grouped.keys }
            if (preferred.isNotEmpty()) {
                preferred
            } else {
                grouped.keys.sorted()
            }
        }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = horizontalPadding)) {
        // Search + chips stay pinned while the list scrolls.
        Spacer(modifier = Modifier.height(NothingSpacing.lg))
        NothingInput(
            value = query,
            onValueChange = { query = it },
            label = "Search",
            placeholder = searchPlaceholder,
        )
        LazyRow(contentPadding = PaddingValues(vertical = NothingSpacing.sm)) {
            items(categories) { category ->
                val selected = category in activeCategories
                CatalogChip(
                    text = category,
                    selected = selected,
                    onClick = {
                        activeCategories =
                            if (selected) activeCategories - category else activeCategories + category
                    },
                )
            }
            items(extraFilters) { filter ->
                val selected = filter.label in activeFilterLabels
                CatalogChip(
                    text = filter.label,
                    selected = selected,
                    onClick = {
                        activeFilterLabels =
                            if (selected) activeFilterLabels - filter.label else activeFilterLabels + filter.label
                    },
                )
            }
            if (activeCategories.isNotEmpty() || activeFilterLabels.isNotEmpty() || query.isNotBlank()) {
                item {
                    TextButton(
                        onClick = {
                            activeCategories = emptySet()
                            activeFilterLabels = emptySet()
                            query = ""
                        },
                        modifier = Modifier.padding(start = NothingSpacing.sm),
                    ) {
                        Text("Clear", fontFamily = NothingFonts.mono())
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomPadding),
        ) {
            if (selectedTray != null) {
                item { selectedTray() }
            }

            if (filtered.isEmpty()) {
                item {
                    Text(
                        text = if (query.isBlank()) "Nothing here." else "Nothing matches \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = NothingFonts.mono(),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(NothingSpacing.md),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            orderedCategories.forEach { category ->
                val groupItems = grouped[category] ?: emptyList()
                item {
                    NothingSectionHeader(text = category)
                    NothingCard {
                        groupItems.forEachIndexed { index, entry ->
                            if (index > 0) NothingDivider()
                            CatalogRow(
                                entry = entry,
                                selected = isSelected(entry),
                                onClick = { onSelect(entry) },
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(NothingSpacing.lg)) }
        }
    }
}

/** Shared filter chip used for both category and extra filters. */
@Composable
private fun CatalogChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = text,
                fontFamily = NothingFonts.mono(),
                style = MaterialTheme.typography.labelSmall,
            )
        },
        shape = NothingShapes.pill,
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = NothingColors.accent,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
            ),
        border = FilterChipDefaults.filterChipBorder(false, selected),
        modifier = Modifier.padding(end = NothingSpacing.sm),
    )
}

/** One catalog row: icon chip, label, description, and requirement badges.
 *  `selected` draws a primary dot on the right for single-select pickers. */
@Composable
fun CatalogRow(
    entry: CatalogEntry,
    onClick: () -> Unit,
    selected: Boolean = false,
) {
    NothingListRow(
        title = entry.label,
        subtitle = entry.description,
        selected = selected,
        onClick = onClick,
        leading = {
            NothingIconCircle(size = 44f, accent = entry.accent) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = entry.label,
                    tint =
                        when {
                            selected -> MaterialTheme.colorScheme.primary
                            entry.accent -> NothingColors.accent
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        trailing =
            when {
                entry.badges.isNotEmpty() -> {
                    {
                        Row(horizontalArrangement = Arrangement.spacedBy(NothingSpacing.xs)) {
                            entry.badges.take(2).forEach {
                                NothingRequirementBadge(text = it)
                            }
                        }
                    }
                }
                selected -> {
                    {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                }
                else -> null
            },
    )
}
