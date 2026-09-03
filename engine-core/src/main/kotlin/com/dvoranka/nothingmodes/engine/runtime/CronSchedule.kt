package com.dvoranka.nothingmodes.engine.runtime

import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Parses 5-field cron expressions and computes the next fire time. */
class CronSchedule(val expression: String, val zone: ZoneId) {

    private val fields: List<Set<Int>> = parse(expression)

    /** Next fire time after [after], exclusive. */
    fun nextFire(after: ZonedDateTime): ZonedDateTime? {
        var candidate = after.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1)
        val limit = after.plusYears(1)
        while (candidate.isBefore(limit)) {
            if (matches(candidate)) return candidate
            candidate = candidate.plusMinutes(1)
        }
        return null
    }

    fun matches(dt: ZonedDateTime): Boolean {
        val local = dt.withZoneSameInstant(zone)
        return local.minute in fields[0] &&
            local.hour in fields[1] &&
            local.dayOfMonth in fields[2] &&
            local.monthValue in fields[3] &&
            local.dayOfWeek.value % 7 in fields[4]
    }

    companion object {
        private val MAX_FIELD = intArrayOf(59, 23, 31, 12, 7)

        fun parse(expr: String): List<Set<Int>> {
            val parts = expr.trim().split(Regex("\\s+"))
            require(parts.size == 5) { "Cron expression must have 5 fields: $expr" }
            return parts.mapIndexed { i, field -> parseField(field, i) }
        }

        private fun parseField(field: String, index: Int): Set<Int> {
            val max = MAX_FIELD[index]
            val min = if (index == 2) 1 else if (index == 3) 1 else 0
            val result = mutableSetOf<Int>()

            for (part in field.split(",")) {
                when {
                    part == "*" -> result.addAll(min..max)
                    part.startsWith("*/") -> {
                        val step = part.substring(2).toIntOrNull()
                            ?: throw IllegalArgumentException("Invalid step: $part")
                        require(step > 0) { "Step must be positive: $part" }
                        var v = min
                        while (v <= max) { result.add(v); v += step }
                    }
                    part.contains("-") -> {
                        val range = part.split("-")
                        require(range.size == 2) { "Invalid range: $part" }
                        val start = range[0].toInt()
                        val end = range[1].toInt()
                        require(start in min..max && end in min..max && start <= end) {
                            "Range out of bounds: $part"
                        }
                        result.addAll(start..end)
                    }
                    else -> {
                        val v = part.toIntOrNull()
                            ?: throw IllegalArgumentException("Invalid value: $part")
                        require(v in min..max) { "Value out of bounds: $part" }
                        result.add(v)
                    }
                }
            }
            return result
        }
    }
}
