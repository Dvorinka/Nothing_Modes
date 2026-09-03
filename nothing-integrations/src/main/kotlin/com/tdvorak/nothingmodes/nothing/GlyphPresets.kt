package com.tdvorak.nothingmodes.nothing

import android.graphics.Color

/**
 * Pre-built Glyph visual patterns for common automation events.
 * Works on both light stripe and matrix devices.
 *
 * Each preset returns a GlyphVisual that the providers can render.
 */
object GlyphPresets {

    /** Visual specification that works across device types. */
    sealed interface GlyphVisual {
        /** Light stripe visual: channels + animation params. */
        data class Stripe(
            val channels: List<Int>? = null, // null = all
            val zone: String? = null, // A/B/C/D/E — alternative to channels
            val periodMs: Int = 0,
            val cycles: Int = 0,
            val intervalMs: Int = 0,
            val progress: Int? = null, // 0-100, uses displayProgress
        ) : GlyphVisual

        /** Matrix visual: color frame or structured content. */
        data class Matrix(
            val color: Int? = null, // solid fill
            val text: String? = null, // text display
            val scrollingText: String? = null, // marquee
            val percentFill: Int? = null, // 0-100 bottom fill
            val number: Int? = null, // number display
            val fillPercent: Int? = null,
            val fillColor: Int = Color.WHITE,
        ) : GlyphVisual

        /** Turn off all glyphs. */
        data object Off : GlyphVisual
    }

    // ── Mode activation visuals ──

    /** Sleep mode: slow breathing on all channels, dim. */
    val sleepMode = GlyphVisual.Stripe(periodMs = 5000, cycles = 0, intervalMs = 100)

    /** Morning: bright pulse on zone A. */
    val morning = GlyphVisual.Stripe(zone = "A", periodMs = 1000, cycles = 5, intervalMs = 50)

    /** Work focus: steady glow on zone C (the dot strip). */
    val workFocus = GlyphVisual.Stripe(zone = "C", periodMs = 0, cycles = 0, intervalMs = 0)

    /** DND active: red fill on matrix / zone D on stripe. */
    val dndActive = GlyphVisual.Stripe(zone = "D", periodMs = 0, cycles = 0, intervalMs = 0)
    val dndActiveMatrix = GlyphVisual.Matrix(color = Color.RED)

    /** DND off: brief flash then off. */
    val dndOff = GlyphVisual.Stripe(periodMs = 200, cycles = 1, intervalMs = 0)

    // ── Event visuals ──

    /** Automation fired: quick pulse on all channels. */
    val automationFired = GlyphVisual.Stripe(periodMs = 300, cycles = 2, intervalMs = 50)

    /** Error: red blink. */
    val error = GlyphVisual.Stripe(periodMs = 100, cycles = 5, intervalMs = 100)
    val errorMatrix = GlyphVisual.Matrix(color = Color.RED)

    /** Success: green pulse. */
    val success = GlyphVisual.Stripe(periodMs = 500, cycles = 1, intervalMs = 0)

    // ── Notification visuals ──

    /** Charging started: battery fill animation. */
    val chargingStart = GlyphVisual.Stripe(progress = 0)
    val chargingComplete = GlyphVisual.Stripe(progress = 100)

    /** Incoming call: rapid pulse on all channels. */
    val incomingCall = GlyphVisual.Stripe(periodMs = 200, cycles = 20, intervalMs = 100)

    /** SMS received: brief double flash. */
    val smsReceived = GlyphVisual.Stripe(periodMs = 100, cycles = 2, intervalMs = 200)

    /** Timer fired: escalating pulse. */
    val timerFired = GlyphVisual.Stripe(periodMs = 500, cycles = 10, intervalMs = 50)

    // ── Matrix-specific visuals ──

    /** Battery level display on matrix. */
    fun batteryLevel(percent: Int) = GlyphVisual.Matrix(percentFill = percent, fillColor = if (percent < 20) Color.RED else Color.WHITE)

    /** Notification count on matrix. */
    fun notificationCount(count: Int) = GlyphVisual.Matrix(number = count)

    /** Now playing on matrix (scrolling text). */
    fun nowPlaying(artist: String, title: String) = GlyphVisual.Matrix(scrollingText = "$artist - $title")

    /** Mode name on matrix (scrolling text). */
    fun modeName(name: String) = GlyphVisual.Matrix(scrollingText = name)

    /** Turn everything off. */
    val off = GlyphVisual.Off
}
