package com.tdvorak.nothingmodes.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Presentation-layer conversion: SI goes in, localized strings come out (and back). */
class UnitsTest {

    private val metric = DisplayUnits(fahrenheit = false, miles = false, clock12h = false)
    private val us = DisplayUnits(fahrenheit = true, miles = true, clock12h = true)

    // ── Temperature ──────────────────────────────────────────────────────────

    @Test
    fun `temperature formats celsius metric`() {
        assertEquals("40°C", metric.formatTemperature(40.0))
        assertEquals("5°C", metric.formatTemperature(5.0))
    }

    @Test
    fun `temperature formats fahrenheit imperial`() {
        assertEquals("104°F", us.formatTemperature(40.0))
        assertEquals("41°F", us.formatTemperature(5.0))
        assertEquals("32°F", us.formatTemperature(0.0))
    }

    @Test
    fun `temp input roundtrips`() {
        assertEquals("104", us.tempToInput(40.0))
        assertEquals(40.0, us.inputToCelsius("104")!!, 0.5)
        assertEquals("40", metric.tempToInput(40.0))
        assertEquals(40.0, metric.inputToCelsius("40")!!, 0.001)
    }

    // ── Distance ─────────────────────────────────────────────────────────────

    @Test
    fun `distance formats meters metric`() {
        assertEquals("150 m", metric.formatDistance(150.0))
        assertEquals("1.2 km", metric.formatDistance(1200.0))
    }

    @Test
    fun `distance formats feet and miles imperial`() {
        assertEquals("328 ft", us.formatDistance(100.0))
        assertEquals("0.6 mi", us.formatDistance(1000.0))
    }

    @Test
    fun `distance input roundtrips`() {
        // 330 ft ≈ 100.6 m
        assertEquals(100.0, us.inputToDistance("328")!!, 1.0)
        assertEquals(100.0, metric.inputToDistance("100")!!, 0.001)
    }

    // ── Clock ────────────────────────────────────────────────────────────────

    @Test
    fun `local time formats 24h metric`() {
        assertEquals("21:30", metric.formatLocalTime("21:30"))
        assertEquals("07:05", metric.formatLocalTime("07:05"))
    }

    @Test
    fun `local time formats 12h imperial`() {
        assertEquals("9:30 PM", us.formatLocalTime("21:30"))
        assertEquals("12:00 AM", us.formatLocalTime("00:00"))
        assertEquals("12:00 PM", us.formatLocalTime("12:00"))
        assertEquals("7:05 AM", us.formatLocalTime("07:05"))
    }

    @Test
    fun `iso minute formats both`() {
        assertEquals("2024-06-15 21:30", metric.formatIsoMinute("2024-06-15T21:30"))
        assertEquals("6/15/2024 9:30 PM", us.formatIsoMinute("2024-06-15T21:30"))
    }

    @Test
    fun `invalid input passes through`() {
        assertEquals("garbage", us.formatLocalTime("garbage"))
        assertEquals("garbage", us.formatIsoMinute("garbage"))
    }
}
