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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.ui.components.NothingDateField
import com.tdvorak.nothingmodes.ui.components.NothingTimeField
import com.tdvorak.nothingmodes.ui.components.NothingTimeZoneField
import com.tdvorak.nothingmodes.ui.screens.triggerDescription
import com.tdvorak.nothingmodes.ui.theme.NothingEnumSelector
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingLabel
import com.tdvorak.nothingmodes.ui.theme.NothingSectionHeader
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import com.tdvorak.nothingmodes.ui.theme.SpaceMono
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private enum class Recurrence {
    ONCE, DAILY, WEEKDAYS, WEEKENDS, WEEKLY, MONTHLY, YEARLY;

    val label: String
        get() = when (this) {
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
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val dayOfMonth: Int = 1,
    val month: Int = 1,
    val year: Int = LocalDate.now().year,
)

private fun parseTrigger(trigger: Trigger.Time): TimeSchedule? {
    val zone = runCatching { ZoneId.of(trigger.tz) }.getOrNull() ?: ZoneId.systemDefault()

    trigger.at?.let {
        val at = runCatching { ZonedDateTime.parse(it) }.getOrNull()
            ?: runCatching { ZonedDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }.getOrNull()
        if (at != null) {
            return TimeSchedule(
                recurrence = Recurrence.ONCE,
                hour = at.hour,
                minute = at.minute,
                year = at.year,
                month = at.monthValue,
                dayOfMonth = at.dayOfMonth,
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
        day == "*" && month == "*" && dow.toIntOrNull() != null -> {
            val javaDow = when (dow.toInt() % 7) {
                0 -> DayOfWeek.SUNDAY
                1 -> DayOfWeek.MONDAY
                2 -> DayOfWeek.TUESDAY
                3 -> DayOfWeek.WEDNESDAY
                4 -> DayOfWeek.THURSDAY
                5 -> DayOfWeek.FRIDAY
                else -> DayOfWeek.SATURDAY
            }
            schedule.copy(recurrence = Recurrence.WEEKLY, dayOfWeek = javaDow)
        }
        day != "*" && month == "*" && dow == "*" -> schedule.copy(recurrence = Recurrence.MONTHLY, dayOfMonth = day.toIntOrNull() ?: 1)
        day != "*" && month != "*" && dow == "*" -> schedule.copy(
            recurrence = Recurrence.YEARLY,
            dayOfMonth = day.toIntOrNull() ?: 1,
            month = month.toIntOrNull() ?: 1,
        )
        else -> schedule.copy(recurrence = Recurrence.DAILY)
    }
}

private fun TimeSchedule.toTrigger(tz: String): Trigger.Time {
    return when (recurrence) {
        Recurrence.ONCE -> {
            val zone = runCatching { ZoneId.of(tz) }.getOrDefault(ZoneId.systemDefault())
            val at = try {
                ZonedDateTime.of(year, month, dayOfMonth, hour, minute, 0, 0, zone).toString()
            } catch (e: Exception) {
                ZonedDateTime.now(zone).withHour(hour).withMinute(minute).toString()
            }
            Trigger.Time(cron = null, at = at, tz = tz)
        }
        Recurrence.DAILY -> Trigger.Time(cron = "$minute $hour * * *", tz = tz)
        Recurrence.WEEKDAYS -> Trigger.Time(cron = "$minute $hour * * 1-5", tz = tz)
        Recurrence.WEEKENDS -> Trigger.Time(cron = "$minute $hour * * 0,6", tz = tz)
        Recurrence.WEEKLY -> {
            val cronDow = dayOfWeek.value % 7
            Trigger.Time(cron = "$minute $hour * * $cronDow", tz = tz)
        }
        Recurrence.MONTHLY -> Trigger.Time(cron = "$minute $hour $dayOfMonth * *", tz = tz)
        Recurrence.YEARLY -> Trigger.Time(cron = "$minute $hour $dayOfMonth $month *", tz = tz)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomTimePicker(
    trigger: Trigger.Time,
    onUpdate: (Trigger.Time) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initial = remember(trigger.cron, trigger.at, trigger.tz) {
        parseTrigger(trigger) ?: TimeSchedule(Recurrence.DAILY, 12, 0)
    }
    var schedule by remember { mutableStateOf(initial) }

    fun update(updater: TimeSchedule.() -> TimeSchedule) {
        schedule = schedule.updater()
        onUpdate(schedule.toTrigger(trigger.tz))
    }

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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DayOfWeek.entries.forEach { day ->
                DayChip(
                    label = day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                    selected = day == schedule.dayOfWeek,
                    onClick = { update { copy(dayOfWeek = day) } },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    if (schedule.recurrence == Recurrence.ONCE) {
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        NothingDateField(
            label = "Date",
            date = LocalDate.of(
                schedule.year.coerceIn(1970, 2100),
                schedule.month.coerceIn(1, 12),
                schedule.dayOfMonth.coerceIn(1, 28),
            ),
            onDateChange = { d ->
                update { copy(year = d.year, month = d.monthValue, dayOfMonth = d.dayOfMonth) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    } else if (schedule.recurrence == Recurrence.MONTHLY || schedule.recurrence == Recurrence.YEARLY) {
        Spacer(modifier = Modifier.height(NothingSpacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
        ) {
            NumberField(
                value = schedule.dayOfMonth,
                onValueChange = { update { copy(dayOfMonth = it.coerceIn(1, 31)) } },
                label = "Day",
                range = 1..31,
                modifier = Modifier.weight(1f),
            )
            if (schedule.recurrence == Recurrence.YEARLY) {
                NumberField(
                    value = schedule.month,
                    onValueChange = { update { copy(month = it.coerceIn(1, 12)) } },
                    label = "Month",
                    range = 1..12,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(NothingSpacing.md))
    NothingTimeZoneField(
        value = trigger.tz,
        onValueChange = { onUpdate(trigger.copy(tz = it)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(modifier = Modifier.height(NothingSpacing.sm))
    val preview = triggerDescription(schedule.toTrigger(trigger.tz))
    Text(
        text = preview,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = SpaceMono,
    )
}

@Composable
private fun NumberField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    range: IntRange,
    modifier: Modifier = Modifier,
) {
    NothingInput(
        value = value.toString().padStart(2, '0'),
        onValueChange = { text ->
            val intVal = text.toIntOrNull() ?: 0
            onValueChange(intVal.coerceIn(range))
        },
        label = label,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
        ),
        modifier = modifier,
    )
}

@Composable
private fun DayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(NothingShapes.compact)
            .background(bg)
            .border(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                NothingShapes.compact,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.uppercase().take(3),
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontFamily = SpaceMono,
            textAlign = TextAlign.Center,
        )
    }
}
