@file:OptIn(ExperimentalMaterial3Api::class)

package com.tdvorak.nothingmodes.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.engine.model.DayOfWeek
import com.tdvorak.nothingmodes.ui.theme.Doto
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingDivider
import com.tdvorak.nothingmodes.ui.theme.NothingGhostButton
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ponytail: ceiling shared field-row pattern, upgrade path = extract NothingFieldRow primitive

// ── Time Field ───────────────────────────────────────────────────────────────

@Composable
fun NothingTimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }

    val (h, m) = remember(value) {
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23)
        val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59)
        (hour to minute)
    }

    val now = remember { java.time.LocalTime.now() }
    val displayHour = h ?: now.hour
    val displayMinute = m ?: now.minute
    val display = "%02d:%02d".format(displayHour, displayMinute)

    FieldRow(
        label = label,
        modifier = modifier,
        onClick = { open = true },
    ) {
        Box(
            modifier = Modifier
                .clip(NothingShapes.compact)
                .border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.compact)
                .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = display,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Doto,
                )
                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                Text(
                    text = ">",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
            }
        }
    }

    if (open) {
        NothingTimePickerDialog(
            initialHour = displayHour,
            initialMinute = displayMinute,
            onDismiss = { open = false },
            onConfirm = { hour, minute ->
                onValueChange("%02d:%02d".format(hour, minute))
                open = false
            },
        )
    }
}

// ── Time Picker Dialog ────────────────────────────────────────────────────────

private val WHEEL_ITEM_HEIGHT = 48.dp
private const val WHEEL_VISIBLE_COUNT = 5
private val WHEEL_CENTER_OFFSET = WHEEL_VISIBLE_COUNT / 2

@Composable
fun NothingTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    var selectedHour by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        DialogSurface {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SET TIME",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.md))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    WheelColumn(
                        count = 24,
                        initial = selectedHour,
                        onSelected = { selectedHour = it },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = Doto,
                        modifier = Modifier.padding(horizontal = NothingSpacing.sm),
                    )
                    WheelColumn(
                        count = 60,
                        initial = selectedMinute,
                        onSelected = { selectedMinute = it },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(NothingSpacing.lg))
                NothingDivider()
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                DialogActions(
                    onCancel = onDismiss,
                    onConfirm = { onConfirm(selectedHour, selectedMinute) },
                )
            }
        }
    }
}

@Composable
private fun WheelColumn(
    count: Int,
    initial: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Padding items so the first real value can be centered.
    val padding = WHEEL_CENTER_OFFSET

    LaunchedEffect(initial, count) {
        val target = initial.coerceIn(0, count - 1)
        state.scrollToItem(target, scrollOffset = 0)
    }

    // Snap to nearest on scroll stop. ponytail: ceiling fling snap, upgrade path = custom SnapFlingBehavior.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .filter { !it }
            .distinctUntilChanged()
            .collect {
                val layout = state.layoutInfo
                val viewportCenter = (WHEEL_ITEM_HEIGHT.value * WHEEL_VISIBLE_COUNT) / 2f
                val nearest = layout?.visibleItemsInfo?.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2f
                    kotlin.math.abs(itemCenter - viewportCenter)
                } ?: return@collect
                val resolved = (nearest.index - padding).coerceIn(0, count - 1)
                if (resolved + padding != nearest.index) {
                    state.scrollToItem(resolved + padding)
                }
                onSelected(resolved)
            }
    }

    val currentCenter by remember(state) {
        derivedStateOf {
            val layout = state.layoutInfo ?: return@derivedStateOf -1
            val viewportCenter = (WHEEL_ITEM_HEIGHT.value * WHEEL_VISIBLE_COUNT) / 2f
            val nearest = layout.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2f
                kotlin.math.abs(itemCenter - viewportCenter)
            }
            (nearest?.index ?: -1) - padding
        }
    }

    Box(
        modifier = modifier
            .height(WHEEL_ITEM_HEIGHT * WHEEL_VISIBLE_COUNT)
            .clip(NothingShapes.compact)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(padding) { WheelSpacer() }
            items(count) { index ->
                val isSelected = index == currentCenter
                Box(
                    modifier = Modifier
                        .height(WHEEL_ITEM_HEIGHT)
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { state.scrollToItem(index + padding) }
                            onSelected(index)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "%02d".format(index),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = SpaceMono,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
            items(padding) { WheelSpacer() }
        }
    }
}

@Composable
private fun WheelSpacer() {
    Box(modifier = Modifier.height(WHEEL_ITEM_HEIGHT).fillMaxWidth())
}

// ── Date Field ────────────────────────────────────────────────────────────────

@Composable
fun NothingDateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH) }
    val display = remember(date) { date.format(formatter) }

    FieldRow(
        label = label,
        modifier = modifier,
        onClick = { open = true },
    ) {
        Box(
            modifier = Modifier
                .clip(NothingShapes.compact)
                .border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.compact)
                .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = display,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = Doto,
                )
                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                Text(
                    text = ">",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
            }
        }
    }

    if (open) {
        NothingDatePickerDialog(
            initial = date,
            onDismiss = { open = false },
            onConfirm = {
                onDateChange(it)
                open = false
            },
        )
    }
}

// ── Date Picker Dialog ────────────────────────────────────────────────────────

@Composable
fun NothingDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    var month by remember { mutableStateOf(YearMonth.from(initial)) }
    var selected by remember { mutableStateOf(initial) }
    val today = remember { LocalDate.now() }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        DialogSurface {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MonthNavButton(text = "<") { month = month.minusMonths(1) }
                    Text(
                        text = "%s %d".format(
                            month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                            month.year,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    MonthNavButton(text = ">") { month = month.plusMonths(1) }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.md))

                // Weekday header — Monday first.
                val weekdayLabels = remember {
                    listOf("M", "T", "W", "T", "F", "S", "S")
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdayLabels.forEach { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = SpaceMono,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.xs))

                // Grid — 7 columns, up to 6 rows.
                val firstDay = remember(month) { month.atDay(1) }
                val daysInMonth = remember(month) { month.lengthOfMonth() }
                // Monday=0 .. Sunday=6
                val leadingBlanks = remember(month) {
                    (firstDay.dayOfWeek.value - 1) % 7
                }
                val totalCells = remember(month) {
                    val used = leadingBlanks + daysInMonth
                    val rows = (used + 6) / 7
                    rows * 7
                }

                val rows = totalCells / 7
                for (rowIdx in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIndex = rowIdx * 7 + col
                            val dayNumber = cellIndex - leadingBlanks + 1
                            val cellDate = if (dayNumber in 1..daysInMonth) {
                                month.atDay(dayNumber)
                            } else null
                            DayCell(
                                date = cellDate,
                                isSelected = cellDate == selected,
                                isToday = cellDate == today,
                                modifier = Modifier.weight(1f),
                                onClick = { cellDate?.let { selected = it } },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.md))
                NothingDivider()
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                DialogActions(
                    onCancel = onDismiss,
                    onConfirm = { onConfirm(selected) },
                )
            }
        }
    }
}

@Composable
private fun MonthNavButton(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(NothingShapes.compact)
            .border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.compact)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = SpaceMono,
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        date != null -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    Box(
        modifier = modifier
            .height(44.dp)
            .padding(2.dp)
            .clip(NothingShapes.compact)
            .background(bg)
            .clickable(enabled = date != null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (date != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "%02d".format(date.dayOfMonth),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceMono,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
                if (isToday && !isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(NothingColors.accent),
                    )
                }
            }
        }
    }
}

// ── Time Zone Field ───────────────────────────────────────────────────────────

private val COMMON_ZONES: List<String> = listOf(
    "UTC",
    "Europe/London", "Europe/Prague", "Europe/Berlin", "Europe/Paris", "Europe/Madrid",
    "Europe/Rome", "Europe/Amsterdam", "Europe/Warsaw", "Europe/Vienna", "Europe/Stockholm",
    "Europe/Athens", "Europe/Istanbul", "Europe/Moscow",
    "Africa/Cairo", "Africa/Johannesburg",
    "Asia/Dubai", "Asia/Karachi", "Asia/Kolkata", "Asia/Bangkok", "Asia/Singapore",
    "Asia/Shanghai", "Asia/Hong_Kong", "Asia/Tokyo", "Asia/Seoul",
    "Australia/Sydney", "Australia/Perth", "Pacific/Auckland",
    "America/Sao_Paulo", "America/New_York", "America/Chicago", "America/Denver",
    "America/Los_Angeles", "America/Toronto", "America/Mexico_City",
)

@Composable
fun NothingTimeZoneField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        FieldRow(
            label = "Timezone",
            onClick = { open = true },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceMono,
                )
                Spacer(modifier = Modifier.width(NothingSpacing.sm))
                Text(
                    text = ">",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
            }
        }

        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        Box(
            modifier = Modifier
                .clip(NothingShapes.technical)
                .clickable { advanced = !advanced }
                .padding(vertical = NothingSpacing.xxs),
        ) {
            Text(
                text = if (advanced) "ADVANCED  –" else "ADVANCED  +",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = SpaceMono,
            )
        }

        if (advanced) {
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            NothingInput(
                value = value,
                onValueChange = onValueChange,
                label = "Raw zone id",
                placeholder = "e.g. Europe/Prague",
                keyboardOptions = KeyboardOptions.Default,
            )
        }
    }

    if (open) {
        TimeZonePickerDialog(
            current = value,
            onDismiss = { open = false },
            onSelect = {
                onValueChange(it)
                open = false
            },
        )
    }
}

@Composable
private fun TimeZonePickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val deviceDefault = remember { ZoneId.systemDefault().id }

    val results: List<String> = remember(query) {
        if (query.isBlank()) COMMON_ZONES
        else {
            val q = query.trim().lowercase(Locale.ENGLISH)
            ZoneId.getAvailableZoneIds()
                .asSequence()
                .filter { it.lowercase(Locale.ENGLISH).contains(q) }
                .sorted()
                .take(200)
                .toList()
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        DialogSurface {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "TIME ZONE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                )
                Spacer(modifier = Modifier.height(NothingSpacing.sm))

                NothingInput(
                    value = query,
                    onValueChange = { query = it },
                    label = "Search time zones",
                    placeholder = "Search",
                )

                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                NothingDivider()
                Spacer(modifier = Modifier.height(NothingSpacing.xs))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                ) {
                    if (query.isBlank()) {
                        item {
                            ZoneRow(
                                label = "Device default — $deviceDefault",
                                isSelected = current == deviceDefault,
                                onClick = { onSelect(deviceDefault) },
                            )
                        }
                        item { NothingDivider() }
                    }
                    items(results.size) { index ->
                        val id = results[index]
                        ZoneRow(
                            label = id,
                            isSelected = current == id,
                            onClick = { onSelect(id) },
                        )
                        if (index < results.lastIndex) {
                            NothingDivider()
                        }
                    }
                }

                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                NothingDivider()
                Spacer(modifier = Modifier.height(NothingSpacing.sm))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    NothingGhostButton(text = "Close", onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun ZoneRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.md),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            fontFamily = SpaceMono,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ── Day Selector ──────────────────────────────────────────────────────────────

@Composable
fun NothingDaySelector(
    selected: Set<DayOfWeek>,
    onChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day in selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .then(
                        if (isSelected) Modifier
                        else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    )
                    .clickable {
                        onChange(
                            if (isSelected) selected - day else selected + day
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day.name.take(3).uppercase(Locale.ENGLISH),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = SpaceMono,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Shared internals ──────────────────────────────────────────────────────────

@Composable
private fun FieldRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(NothingShapes.compact)
            .clickable(onClick = onClick)
            .padding(vertical = NothingSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        NothingLabel(text = label)
        trailing()
    }
}

@Composable
private fun DialogSurface(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = NothingShapes.dialog,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(NothingSpacing.lg)) {
            content()
        }
    }
}

@Composable
private fun DialogActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NothingGhostButton(text = "Cancel", onClick = onCancel)
        Spacer(modifier = Modifier.width(NothingSpacing.sm))
        NothingPillButton(text = "Set", onClick = onConfirm)
    }
}
