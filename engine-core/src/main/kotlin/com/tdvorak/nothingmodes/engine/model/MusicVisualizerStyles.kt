package com.tdvorak.nothingmodes.engine.model

/** Supported music visualizer styles shared by model, renderer and UI. */
object MusicVisualizerStyles {
    const val WAVEFORM = "waveform"
    const val BARS = "bars"
    const val MIRROR = "mirror"
    const val PULSE = "pulse"
    const val VINYL = "vinyl"

    /** The canonical style list exposed to the user. */
    val ALL = listOf(WAVEFORM, BARS, MIRROR, PULSE, VINYL)

    /** Default style for existing automations and for malformed inputs. */
    fun default(): String = WAVEFORM

    /** Coerce [style] to a known style or fall back to the default. */
    fun normalize(style: String?): String = style?.takeIf { it in ALL } ?: default()
}
