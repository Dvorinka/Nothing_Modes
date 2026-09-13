package com.tdvorak.nothingmodes.ui.components

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingListRow
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class DeviceCalendar(
    val id: String,
    val name: String,
    val account: String?,
)

/** Snapshot of a single future calendar instance exposed to callers. */
data class UpcomingEvent(
    val eventId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val calendarId: String,
    val calendarName: String,
)

private const val PAGE_SIZE = 25

private fun loadCalendars(context: Context): List<DeviceCalendar> {
    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return emptyList()
    return runCatching {
        val out = mutableListOf<DeviceCalendar>()
        context.contentResolver
            .query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(
                    CalendarContract.Calendars._ID,
                    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    CalendarContract.Calendars.ACCOUNT_NAME,
                ),
                null,
                null,
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    out +=
                        DeviceCalendar(
                            id = c.getString(0) ?: continue,
                            name = c.getString(1) ?: "Calendar",
                            account = c.getString(2),
                        )
                }
            }
        out
    }.getOrDefault(emptyList())
}

internal fun loadUpcomingEvents(
    context: Context,
    calendarId: String? = null,
    query: String = "",
    limit: Int = PAGE_SIZE,
): List<UpcomingEvent> {
    val granted = context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    if (!granted) return emptyList()

    val now = System.currentTimeMillis()
    val windowEnd = now + 90L * 24 * 60 * 60 * 1000

    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, now)
    ContentUris.appendId(builder, windowEnd)

    val projection =
        arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
        )

    val selection = calendarId?.let { "${CalendarContract.Instances.CALENDAR_ID} = ?" }
    val selectionArgs = calendarId?.let { arrayOf(it) }

    return runCatching {
        val out = mutableListOf<UpcomingEvent>()
        context.contentResolver
            .query(
                builder.build(),
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext() && out.size < limit) {
                    val id = c.getLong(0)
                    val title = c.getString(1) ?: "(no title)"
                    if (query.isNotBlank() && !title.contains(query, ignoreCase = true)) continue
                    val begin = c.getLong(2)
                    val end = c.getLong(3)
                    val calId = c.getString(4) ?: "0"
                    val calName = c.getString(5) ?: "Calendar"
                    out += UpcomingEvent(id, title, begin, end, calId, calName)
                }
            }
        out
    }.getOrDefault(emptyList())
}

/** Count of upcoming events per calendar over the same 90-day window. */
private fun loadCalendarEventCounts(context: Context): Map<String, Int> {
    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return emptyMap()
    val now = System.currentTimeMillis()
    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, now)
    ContentUris.appendId(builder, now + 90L * 24 * 60 * 60 * 1000)
    return runCatching {
        val out = mutableMapOf<String, Int>()
        context.contentResolver
            .query(
                builder.build(),
                arrayOf(CalendarContract.Instances.CALENDAR_ID),
                null,
                null,
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: "0"
                    out[id] = (out[id] ?: 0) + 1
                }
            }
        out
    }.getOrDefault(emptyMap())
}

fun formatEventTime(millis: Long): String {
    val zdt =
        java.time.ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(millis),
            java.time.ZoneId.systemDefault(),
        )
    return zdt.format(
        java.time.format.DateTimeFormatter
            .ofPattern("EEE MMM d HH:mm"),
    )
}

@Composable
fun CalendarEventPickerDialog(
    initialCalendarId: String?,
    initialSelectedIds: Set<Long> = emptySet(),
    onSelect: (List<UpcomingEvent>) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var calendarFilter by remember { mutableStateOf<String?>(initialCalendarId) }
    var calendars by remember { mutableStateOf(emptyList<DeviceCalendar>()) }
    var rawEvents by remember { mutableStateOf(emptyList<UpcomingEvent>()) }
    var limit by remember { mutableStateOf(PAGE_SIZE) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(initialSelectedIds) }
    // Every event ever loaded, so ids selected then filtered away keep their data.
    val knownEvents = remember { mutableStateMapOf<Long, UpcomingEvent>() }
    var eventCounts by remember { mutableStateOf(emptyMap<String, Int>()) }
    var calendarListOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        eventCounts = withContext(Dispatchers.IO) { loadCalendarEventCounts(context) }
    }

    LaunchedEffect(query, calendarFilter, limit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            if (calendars.isEmpty()) calendars = loadCalendars(context)
            rawEvents = loadUpcomingEvents(context, calendarFilter, query, limit)
        }
        rawEvents.forEach { knownEvents[it.eventId] = it }
        isLoading = false
    }

    // Show one row per distinct title; the earliest occurrence wins.
    val deduped =
        remember(rawEvents) {
            val seen = linkedSetOf<String>()
            rawEvents.filter { seen.add(it.title.lowercase()) }
        }
    val hasMore = deduped.size >= limit

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding(),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "UPCOMING EVENTS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = NothingFonts.mono(),
                    )
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "CLOSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingColors.accent,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                }

                com.tdvorak.nothingmodes.ui.theme
                    .NothingDivider()

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.sm),
                ) {
                    NothingInput(
                        value = query,
                        onValueChange = {
                            query = it
                            limit = PAGE_SIZE
                        },
                        label = "Search events",
                        placeholder = "Filter by title...",
                    )
                }

                // Calendar filter — expandable list, search always spans all calendars.
                // Calendars with no upcoming events are dimmed and not selectable.
                val selectedCalendar = calendars.firstOrNull { it.id == calendarFilter }
                val totalEvents = eventCounts.values.sum()
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.xs),
                ) {
                    com.tdvorak.nothingmodes.ui.theme
                        .NothingLabel(text = "Calendar")
                    Spacer(modifier = Modifier.height(NothingSpacing.xs))
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        shape = NothingShapes.input,
                        border =
                            BorderStroke(
                                1.dp,
                                if (calendarListOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { calendarListOpen = !calendarListOpen },
                    ) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = (selectedCalendar?.name ?: "All calendars").uppercase(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = NothingFonts.mono(),
                            )
                            Text(
                                text = if (calendarListOpen) "[CLOSE]" else "[OPEN]",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = NothingFonts.mono(),
                            )
                        }
                    }

                    if (calendarListOpen) {
                        Spacer(modifier = Modifier.height(NothingSpacing.xs))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = NothingShapes.input,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                CalendarRow(
                                    name = "All calendars",
                                    detail = "$totalEvents upcoming events",
                                    count = null,
                                    selected = calendarFilter == null,
                                    enabled = true,
                                    onClick = {
                                        calendarFilter = null
                                        limit = PAGE_SIZE
                                        calendarListOpen = false
                                    },
                                )
                                val sorted =
                                    calendars.sortedWith(
                                        compareByDescending<DeviceCalendar> { eventCounts[it.id] ?: 0 }
                                            .thenBy { it.name.lowercase() },
                                    )
                                sorted.forEach { cal ->
                                    com.tdvorak.nothingmodes.ui.theme
                                        .NothingDivider()
                                    val count = eventCounts[cal.id] ?: 0
                                    CalendarRow(
                                        name = cal.name,
                                        detail = cal.account ?: "",
                                        count = count,
                                        selected = calendarFilter == cal.id,
                                        enabled = count > 0,
                                        onClick = {
                                            calendarFilter = cal.id
                                            limit = PAGE_SIZE
                                            calendarListOpen = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                if (calendars.isEmpty() && !isLoading) {
                    Text(
                        text = "No calendars found. Grant READ_CALENDAR permission.",
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

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
                ) {
                    if (deduped.isEmpty()) {
                        item {
                            Text(
                                text =
                                    if (query.isBlank() && calendarFilter == null) {
                                        "No upcoming events in the next 90 days."
                                    } else {
                                        "No events match the current filter."
                                    },
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
                    } else {
                        items(deduped, key = { it.eventId }) { event ->
                            val isSelected = event.eventId in selectedIds
                            NothingListRow(
                                title = event.title,
                                subtitle = "${formatEventTime(event.startMillis)} · ${event.calendarName}",
                                selected = isSelected,
                                onClick = {
                                    selectedIds =
                                        if (isSelected) selectedIds - event.eventId else selectedIds + event.eventId
                                },
                                trailing = {
                                    if (isSelected) {
                                        com.tdvorak.nothingmodes.ui.theme.NothingRedDot()
                                    }
                                },
                                modifier = Modifier.padding(horizontal = NothingSpacing.md),
                            )
                        }
                        if (hasMore) {
                            item {
                                NothingPillButton(
                                    text = "Load more",
                                    onClick = { limit += PAGE_SIZE },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.md),
                                )
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(NothingSpacing.lg))
                    }
                }

                com.tdvorak.nothingmodes.ui.theme
                    .NothingDivider()
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(NothingSpacing.md),
                ) {
                    NothingPillButton(
                        text = if (selectedIds.isEmpty()) "Done" else "Done · ${selectedIds.size} selected",
                        onClick = {
                            onSelect(selectedIds.mapNotNull { knownEvents[it] })
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarRow(
    name: String,
    detail: String,
    count: Int?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val fg =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.bodyMedium,
                color = fg,
                fontFamily = NothingFonts.mono(),
            )
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                    fontFamily = NothingFonts.mono(),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            if (count != null) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f),
                    fontFamily = NothingFonts.mono(),
                )
            }
            if (selected) {
                com.tdvorak.nothingmodes.ui.theme.NothingRedDot(size = 6f)
            }
        }
    }
}
