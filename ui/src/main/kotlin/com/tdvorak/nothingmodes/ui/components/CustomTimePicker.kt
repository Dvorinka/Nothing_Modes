package com.tdvorak.nothingmodes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.ui.screens.triggerDescription
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import com.tdvorak.nothingmodes.engine.model.DayOfWeek as EngineDayOfWeek
import com.tdvorak.nothingmodes.ui.util.defaultTimeZone
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class Recurrence {
    ONCE,
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ;

    val label: String
        get() =
            when (this) {
                ONCE -> "Once"
                DAILY -> "Daily"
                WEEKDAYS -> "Weekdays"
                WEEKENDS -> "Weekends"
                WEEKLY -> "Weekly"
                MONTHLY -> "Monthly"
                YEARLY -> "Yearly"
            }
}

private data class TimeSchedule(
    val recurrence: Recurrence,
    val hour: Int,
    val minute: Int,
    val daysOfWeek: Set<EngineDayOfWeek> = setOf(EngineDayOfWeek.MONDAY),
    val daysOfMonth: Set<Int> = setOf(1),
    val months: Set<Int> = setOf(1),
    val year: Int = LocalDate.now().year,
)

private fun parseTrigger(trigger: Trigger.Time): TimeSchedule? {
    val zone = ZoneId.systemDefault()

    trigger.at?.let {
        val at =
            runCatching { ZonedDateTime.parse(it) }.getOrNull()
                ?: runCatching { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }.getOrNull()
        if (at != null) {
            val local = at.withZoneSameInstant(zone)
            return TimeSchedule(
                recurrence = Recurrence.ONCE,
                hour = local.hour,
                minute = local.minute,
                year = local.year,
                months = setOf(local.monthValue),
                daysOfMonth = setOf(local.dayOfMonth),
            )
        }
    }

    val cron = trigger.cron ?: return null
    val parts = cron.trim().split(Regex("\\s+"))
    if (parts.size != 5) return null

    val minute = parts[0].toIntOrNull() ?: 0
    val hour = parts[1].toIntOrNull() ?: 0
    val day = parts[2]
    val month = parts[3]
    val dow = parts[4]

    val schedule = TimeSchedule(recurrence = Recurrence.DAILY, hour = hour, minute = minute)

    return when {
        day == "*" && month == "*" && dow == "*" -> schedule.copy(recurrence = Recurrence.DAILY)
        day == "*" && month == "*" && dow == "1-5" -> schedule.copy(recurrence = Recurrence.WEEKDAYS)
        day == "*" && month == "*" && (dow == "0,6" || dow == "6,0") -> schedule.copy(recurrence = Recurrence.WEEKENDS)
        day == "*" && month == "*" && dow.isNotBlank() -> {
            val engineDays =
                dow
                    .split(",")
                    .mapNotNull { it.toIntOrNull() }
                    .mapNotNull { javaDowValueToEngine(it) }
                    .toSet()
            schedule.copy(recurrence = Recurrence.WEEKLY, daysOfWeek = engineDays)
        }
        day != "*" && month == "*" && dow == "*" -> {
            val days = day.split(",").mapNotNull { if (it == "L") -1 else it.toIntOrNull() }.toSet()
            schedule.copy(recurrence = Recurrence.MONTHLY, daysOfMonth = days)
        }
        day != "*" && month != "*" && dow == "*" -> {
            val days = day.split(",").mapNotNull { if (it == "L") -1 else it.toIntOrNull() }.toSet()
            val months = month.split(",").mapNotNull { it.toIntOrNull() }.toSet()
            schedule.copy(recurrence = Recurrence.YEARLY, daysOfMonth = days, months = months)
        }
        else -> schedule.copy(recurrence = Recurrence.DAILY)
    }
}

private fun TimeSchedule.toTrigger(): Trigger.Time {
    val tz = defaultTimeZone()
    val zone = ZoneId.systemDefault()
    return when (recurrence) {
        Recurrence.ONCE -> {
            val month = months.firstOrNull() ?: 1
            val dayOfMonth = daysOfMonth.firstOrNull() ?: 1
            val at =
                try {
                    ZonedDateTime.of(year, month, dayOfMonth, hour, minute, 0, 0, zone).toString()
                } catch (e: Exception) {
                    ZonedDateTime
                        .now(zone)
                        .withHour(hour)
                        .withMinute(minute)
                        .toString()
                }
            Trigger.Time(cron = null, at = at, tz = tz)
        }
        Recurrence.DAILY -> Trigger.Time(cron = "$minute $hour * * *", tz = tz)
        Recurrence.WEEKDAYS -> Trigger.Time(cron = "$minute $hour * * 1-5", tz = tz)
        Recurrence.WEEKENDS -> Trigger.Time(cron = "$minute $hour * * 0,6", tz = tz)
        Recurrence.WEEKLY -> {
            val cronDow = daysOfWeek.map { engineDowToCronValue(it) }.sorted().joinToString(",")
            Trigger.Time(cron = "$minute $hour * * $cronDow", tz = tz)
        }
        Recurrence.MONTHLY -> {
            val cronDay = formatCronDays(daysOfMonth)
            Trigger.Time(cron = "$minute $hour $cronDay * *", tz = tz)
        }
        Recurrence.YEARLY -> {
            val cronDay = formatCronDays(daysOfMonth)
            val cronMonth = months.sorted().joinToString(",")
            Trigger.Time(cron = "$minute $hour $cronDay $cronMonth *", tz = tz)
        }
    }
}

private fun formatCronDays(daysOfMonth: Set<Int>): String {
    val parts = mutableListOf<String>()
    if (-1 in daysOfMonth) parts.add("L")
    parts.addAll(daysOfMonth.filter { it != -1 }.sorted().map { it.toString() })
    return parts.joinToString(",")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomTimePicker(
    trigger: Trigger.Time,
    onUpdate: (Trigger) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initial =
        remember(trigger.cron, trigger.at, trigger.tz) {
            parseTrigger(trigger) ?: TimeSchedule(Recurrence.DAILY, 12, 0)
        }
    var schedule by remember { mutableStateOf(initial) }

    fun update(updater: TimeSchedule.() -> TimeSchedule) {
        schedule = schedule.updater()
        onUpdate(schedule.toTrigger())
    }

    val sourceOptions = listOf("Clock", "Calendar event")
    NothingEnumSelector(
        label = "Source",
        value = "Clock",
        options = sourceOptions,
        onSelect = { if (it == "Calendar event") onUpdate(Trigger.CalendarEvent()) },
    )
    Spacer(modifier = Modifier.height(NothingSpacing.md))

    val options = Recurrence.entries.map { it.label }

    NothingSectionHeader(text = "Schedule")
    NothingEnumSelector(
        label = "Recurrence",
        value = schedule.recurrence.label,
        options = options,
        onSelect = { label ->
            val selected = Recurrence.entries.first { it.label == label }
            update { copy(recurrence = selected) }
        },
    )
    Spacer(modifier = Modifier.height(NothingSpacing.md))

    // Time — tappable wheel picker instead of raw text fields.
    NothingTimeField(
        label = "At",
        value = "%02d:%02d".format(schedule.hour, schedule.minute),
        onValueChange = { v ->
            val parts = v.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: schedule.hour
            val m = parts.getOrNull(1)?.toIntOrNull() ?: schedule.minute
            update { copy(hour = h.coerceIn(0, 23), minute = m.coerceIn(0, 59)) }
        },
        modifier = Modifier.fillMaxWidth(),
    )

    if (schedule.recurrence == Recurrence.WEEKLY) {
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        NothingLabel(text = "Day of week")
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        NothingDaySelector(
            selected = schedule.daysOfWeek,
            onChange = { update { copy(daysOfWeek = it) } },
        )
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            NothingPillButton(
                text = "Every day",
                onClick = { update { copy(daysOfWeek = EngineDayOfWeek.entries.toSet()) } },
                modifier = Modifier.weight(1f),
            )
            NothingPillButton(
                text = "Weekdays",
                onClick = { update { copy(daysOfWeek = setOf(EngineDayOfWeek.MONDAY, EngineDayOfWeek.TUESDAY, EngineDayOfWeek.WEDNESDAY, EngineDayOfWeek.THURSDAY, EngineDayOfWeek.FRIDAY)) } },
                modifier = Modifier.weight(1f),
            )
            NothingPillButton(
                text = "Weekend",
                onClick = { update { copy(daysOfWeek = setOf(EngineDayOfWeek.SATURDAY, EngineDayOfWeek.SUNDAY)) } },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (schedule.recurrence == Recurrence.ONCE) {
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        NothingDateField(
            label = "Date",
            date =
                LocalDate.of(
                    schedule.year.coerceIn(1970, 2100),
                    schedule.months.firstOrNull()?.coerceIn(1, 12) ?: 1,
                    schedule.daysOfMonth.firstOrNull()?.coerceIn(1, 28) ?: 1,
                ),
            onDateChange = { d ->
                update { copy(year = d.year, months = setOf(d.monthValue), daysOfMonth = setOf(d.dayOfMonth)) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (schedule.recurrence == Recurrence.MONTHLY || schedule.recurrence == Recurrence.YEARLY) {
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        if (schedule.recurrence == Recurrence.YEARLY) {
            NothingLabel(text = "Months")
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            MonthGrid(
                selected = schedule.months,
                onChange = { update { copy(months = it) } },
            )
            Spacer(modifier = Modifier.height(NothingSpacing.sm))
        }
        NothingLabel(text = "Day of month")
        Spacer(modifier = Modifier.height(NothingSpacing.xs))
        // 7-column grid, like a calendar — easier than a raw number field.
        val days = (1..31).toList()
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { day ->
                    DayChip(
                        label = day.toString(),
                        selected = day in schedule.daysOfMonth,
                        onClick = { update { copy(daysOfMonth = if (day in daysOfMonth) daysOfMonth - day else daysOfMonth + day) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(7 - week.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Spacer(modifier = Modifier.height(NothingSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            NothingPillButton(
                text = "1st of month",
                onClick = { update { copy(daysOfMonth = setOf(1)) } },
                modifier = Modifier.weight(1f),
            )
            NothingPillButton(
                text = "15th",
                onClick = { update { copy(daysOfMonth = if (15 in daysOfMonth) daysOfMonth - 15 else daysOfMonth + 15) } },
                modifier = Modifier.weight(1f),
            )
            NothingPillButton(
                text = "Last day",
                onClick = { update { copy(daysOfMonth = setOf(-1)) } },
                modifier = Modifier.weight(1f),
            )
        }
    }

    Spacer(modifier = Modifier.height(NothingSpacing.sm))
    val preview = triggerDescription(schedule.toTrigger())
    Text(
        text = preview,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = NothingFonts.mono(),
    )
}

private fun javaDowValueToEngine(javaValue: Int): EngineDayOfWeek? =
    EngineDayOfWeek.entries.firstOrNull { (it.ordinal + 1) % 7 == javaValue % 7 }

private fun engineDowToCronValue(day: EngineDayOfWeek): Int =
    (day.ordinal + 1) % 7

private fun monthName(month: Int): String =
    java.time.Month.of(month.coerceIn(1, 12)).getDisplayName(
        java.time.format.TextStyle.SHORT,
        Locale.getDefault(),
    )

@Composable
private fun MonthGrid(
    selected: Set<Int>,
    onChange: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        (1..12).chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { month ->
                    DayChip(
                        label = monthName(month),
                        selected = month in selected,
                        onClick = { onChange(if (month in selected) selected - month else selected + month) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier =
            modifier
                .height(48.dp)
                .clip(NothingShapes.input)
                .background(bg)
                .then(
                    if (selected) {
                        Modifier
                    } else {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.input)
                    },
                ).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase().take(3),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontFamily = NothingFonts.mono(),
            textAlign = TextAlign.Center,
        )
    }
}
