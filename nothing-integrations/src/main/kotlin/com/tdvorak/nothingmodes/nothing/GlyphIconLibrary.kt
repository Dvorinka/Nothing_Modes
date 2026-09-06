package com.tdvorak.nothingmodes.nothing

/**
 * Glyph Matrix icon library backed by real emoji.
 *
 * Instead of hand-drawn pixel art (which read poorly on the 25x25 matrix),
 * icon names map to emoji rendered through [GlyphRasterizer] — the same
 * technique the GlyphMatrixEditor uses, and how Glyph Museum produces its
 * sharp dot-matrix art. Any emoji not in the table still works: unknown
 * names that are themselves emoji pass straight through.
 */
object GlyphIconLibrary {
    const val SIZE = 25

    private val nameToEmoji: Map<String, String> =
        linkedMapOf(
            "check" to "✔️",
            "x" to "❌",
            "heart" to "❤️",
            "music_note" to "🎵",
            "arrow_up" to "⬆️",
            "arrow_down" to "⬇️",
            "arrow_left" to "⬅️",
            "arrow_right" to "➡️",
            "battery" to "🔋",
            "phone" to "📱",
            "bell" to "🔔",
            "star" to "⭐",
            "smile" to "😊",
            "home" to "🏠",
            "zap" to "⚡",
            "mail" to "✉️",
            "wifi" to "📶",
            "clock" to "🕐",
            "alarm" to "⏰",
            "fire" to "🔥",
            "sun" to "☀️",
            "moon" to "🌙",
            "cloud" to "☁️",
            "rain" to "🌧️",
            "snow" to "❄️",
            "lock" to "🔒",
            "unlock" to "🔓",
            "car" to "🚗",
            "plane" to "✈️",
            "rocket" to "🚀",
            "trophy" to "🏆",
            "gift" to "🎁",
            "coffee" to "☕",
            "pizza" to "🍕",
            "dog" to "🐶",
            "cat" to "🐱",
            "flower" to "🌸",
            "tree" to "🌲",
            "flag" to "🚩",
            "warning" to "⚠️",
            "question" to "❓",
            "info" to "ℹ️",
            "muscle" to "💪",
            "thumbs_up" to "👍",
            "calendar" to "📅",
            "camera" to "📷",
            "flashlight" to "🔦",
            "money" to "💰",
            "game" to "🎮",
            "key" to "🔑",
            "shield" to "🛡️",
            "target" to "🎯",
            "pin" to "📍",
            "play" to "▶️",
            "pause" to "⏸️",
            "stop" to "⏹️",
            "repeat" to "🔁",
        )

    /** Canonical icon names — used by the catalog/config UI and debug parse. */
    val names: List<String> = nameToEmoji.keys.toList()

    /** The emoji rendered for [name], or null if the name is not a known icon. */
    fun emojiFor(name: String): String? = nameToEmoji[name.lowercase()]

    /**
     * @return the 25x25 frame for [name], or null when the name is neither a
     * known icon nor a usable emoji.
     */
    fun frameFor(name: String): IntArray? {
        val emoji = nameToEmoji[name.lowercase()]
        if (emoji != null) {
            return GlyphRasterizer.rasterize(emoji, SIZE)
        }
        // Passthrough: a raw emoji (any non-ASCII string) renders directly.
        if (name.isNotBlank() && name.any { it.code > 0x7F }) {
            return GlyphRasterizer.rasterize(name, SIZE)
        }
        return null
    }
}
