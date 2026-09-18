package com.tdvorak.nothingmodes.ui.util

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Preferred measurement convention. Stored values stay canonical
 *  (meters, Celsius, 24-hour "HH:mm") — this only affects display and input. */
enum class UnitSystem {
    SYSTEM,
    METRIC,
    IMPERIAL,
    ;

    val label: String
        get() =
            when (this) {
                SYSTEM -> "System default"
                METRIC -> "Metric — m, °C, 24h"
                IMPERIAL -> "Freedom — mi, °F, AM/PM"
            }
}

/** Concrete display units after [UnitSystem.SYSTEM] resolves against the
 *  device locale and clock preference. */
data class DisplayUnits(
    val clock12h: Boolean,
    val miles: Boolean,
    val fahrenheit: Boolean,
) {
    val distanceUnit: String get() = if (miles) "feet" else "meters"
    val distanceUnitShort: String get() = if (miles) "ft" else "m"
    val tempUnit: String get() = if (fahrenheit) "Fahrenheit" else "Celsius"

    /** "100 m" / "1.2 km" / "328 ft" / "3.1 mi". */
    fun formatDistance(meters: Double): String =
        when {
            !miles && meters < 1000 -> "${meters.toInt()} m"
            !miles -> "${"%.1f".format(meters / 1000).trimEnd('0').trimEnd('.')} km"
            meters < MILE_METERS / 4 -> "${(meters / FOOT_METERS).toInt()} ft"
            else -> "${"%.1f".format(meters / MILE_METERS).trimEnd('0').trimEnd('.')} mi"
        }

    /** "30.5°C" / "87°F". */
    fun formatTemperature(celsius: Double): String =
        if (fahrenheit) "${(celsius * 9 / 5 + 32).toInt()}°F" else "${fmt(celsius)}°C"

    /** "22:30" / "10:30 PM". Non "HH:mm" input passes through unchanged. */
    fun formatLocalTime(text: String): String {
        if (!clock12h) return text
        val parts = text.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return text
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return text
        val ampm = if (h < 12) "AM" else "PM"
        return "${((h + 11) % 12) + 1}:%02d $ampm".format(m)
    }

    /** "2026-09-16 14:30" → "9/16/2026 2:30 PM" in 12-hour mode. */
    fun formatIsoMinute(iso: String): String {
        val ldt =
            runCatching { java.time.LocalDateTime.parse(iso) }.getOrNull()
                ?: runCatching { java.time.ZonedDateTime.parse(iso).toLocalDateTime() }
                    .getOrNull()
                ?: return iso.take(16).replace("T", " ")
        return ldt.format(
            java.time.format.DateTimeFormatter.ofPattern(
                if (clock12h) "M/d/yyyy h:mm a" else "yyyy-MM-dd HH:mm",
            ),
        )
    }

    fun formatTimestamp(millis: Long): String =
        SimpleDateFormat(
            if (clock12h) "h:mm:ss a M/d" else "HH:mm:ss dd/MM",
            Locale.getDefault(),
        ).format(Date(millis))

    // ── Input-field conversions (display value ↔ stored SI value) ───────────

    fun distanceToInput(meters: Double): String =
        if (miles) (meters / FOOT_METERS).toInt().toString() else fmt(meters)

    fun inputToDistance(text: String): Double? =
        text.toDoubleOrNull()?.let { if (miles) it * FOOT_METERS else it }

    fun tempToInput(celsius: Double): String =
        if (fahrenheit) (celsius * 9 / 5 + 32).toInt().toString() else fmt(celsius)

    fun inputToCelsius(text: String): Double? =
        text.toDoubleOrNull()?.let { if (fahrenheit) (it - 32) * 5 / 9 else it }

    companion object {
        private const val FOOT_METERS = 0.3048
        private const val MILE_METERS = 1609.344

        /** "40" for 40.0, "36.5" for 36.5. */
        private fun fmt(v: Double): String =
            if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString()
    }
}

/** Units preference store — same singleton shape as [ThemeManager]. */
class UnitsManager private constructor(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _system = MutableStateFlow(load())
    val system: StateFlow<UnitSystem> = _system.asStateFlow()

    fun set(system: UnitSystem) {
        prefs.edit().putString(KEY_UNITS, system.name).apply()
        _system.value = system
    }

    fun resolved(context: Context): DisplayUnits =
        when (_system.value) {
            UnitSystem.METRIC -> DisplayUnits(clock12h = false, miles = false, fahrenheit = false)
            UnitSystem.IMPERIAL -> DisplayUnits(clock12h = true, miles = true, fahrenheit = true)
            UnitSystem.SYSTEM ->
                DisplayUnits(
                    clock12h = !DateFormat.is24HourFormat(context),
                    miles = Locale.getDefault().country in MILES_COUNTRIES,
                    fahrenheit = Locale.getDefault().country in FAHRENHEIT_COUNTRIES,
                )
        }

    private fun load(): UnitSystem =
        runCatching {
            UnitSystem.valueOf(prefs.getString(KEY_UNITS, UnitSystem.SYSTEM.name)!!)
        }.getOrDefault(UnitSystem.SYSTEM)

    companion object {
        private const val PREFS_NAME = "nothing_modes_prefs"
        private const val KEY_UNITS = "unit_system"

        // Countries off the metric system; GB drives in miles but uses Celsius.
        private val MILES_COUNTRIES = setOf("US", "LR", "MM", "GB")
        private val FAHRENHEIT_COUNTRIES = setOf("US", "LR", "MM")

        @Volatile
        private var _instance: UnitsManager? = null

        fun get(context: Context): UnitsManager =
            _instance ?: synchronized(this) {
                _instance ?: UnitsManager(context.applicationContext).also { _instance = it }
            }
    }
}

/** Resolved display units for the current composition — recomposes when the
 *  preference changes. */
@Composable
fun rememberUnits(): DisplayUnits {
    val context = LocalContext.current
    val manager = remember { UnitsManager.get(context) }
    val system by manager.system.collectAsState()
    return remember(system) { manager.resolved(context) }
}
