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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
        context.contentResolver.query(
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
                out += DeviceCalendar(
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
        context.contentResolver.query(
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

fun formatEventTime(millis: Long): String {
    val zdt = java.time.ZonedDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(millis),
        java.time.ZoneId.systemDefault(),
    )
    return zdt.format(
        java.time.format.DateTimeFormatter.ofPattern("EEE MMM d HH:mm"),
    )
}

@Composable
fun CalendarEventPickerDialog(
    initialCalendarId: String?,
    onSelect: (UpcomingEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var calendarFilter by remember { mutableStateOf<String?>(initialCalendarId) }
    var calendars by remember { mutableStateOf(emptyList<DeviceCalendar>()) }
    var rawEvents by remember { mutableStateOf(emptyList<UpcomingEvent>()) }
    var limit by remember { mutableStateOf(PAGE_SIZE) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(query, calendarFilter, limit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            if (calendars.isEmpty()) calendars = loadCalendars(context)
            rawEvents = loadUpcomingEvents(context, calendarFilter, query, limit)
        }
        isLoading = false
    }

    // Show one row per distinct title; the earliest occurrence wins.
    val deduped = remember(rawEvents) {
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

                com.tdvorak.nothingmodes.ui.theme.NothingDivider()

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

                LazyRow(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    item {
                        CalendarFilterChip(
                            name = "All calendars",
                            selected = calendarFilter == null,
                            onClick = {
                                calendarFilter = null
                                limit = PAGE_SIZE
                            },
                        )
                    }
                    items(calendars, key = { it.id }) { cal ->
                        CalendarFilterChip(
                            name = cal.name,
                            selected = calendarFilter == cal.id,
                            onClick = {
                                calendarFilter = if (calendarFilter == cal.id) null else cal.id
                                limit = PAGE_SIZE
                            },
                        )
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
                    modifier = Modifier.fillMaxSize(),
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
                            NothingListRow(
                                title = event.title,
                                subtitle = "${formatEventTime(event.startMillis)} · ${event.calendarName}",
                                onClick = { onSelect(event) },
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
            }
        }
    }
}

@Composable
private fun CalendarFilterChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val classic = com.tdvorak.nothingmodes.ui.theme.LocalUiStyle.current == com.tdvorak.nothingmodes.ui.theme.ThemeManager.UiStyle.CLASSIC
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        color = bg,
        shape = NothingShapes.pill,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = if (classic) name else name.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontFamily = NothingFonts.mono(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
