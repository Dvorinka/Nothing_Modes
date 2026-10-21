package com.tdvorak.nothingmodes.engine.model

/**
 * Catalog of Nothing OS feature flags — live settings keys observed on
 * Nothing OS 5.0 (`C5.0-260924-2115`). Every entry is a real boolean key
 * that accepts `settings put` and holds its value; the executor writes each
 * listed namespace and verifies the first one back.
 *
 * Note the vendor's own typos (`enalbe`, `ebable`) — the key strings are
 * reproduced verbatim because that's what the OS reads.
 */
object NothingFeatures {
    data class Spec(
        /** The literal settings key — also the serialized feature id. */
        val key: String,
        /** Namespaces to write, in order. The first is the canonical table. */
        val namespaces: List<String>,
        val label: String,
        val summary: String,
    )

    private fun secure(key: String, label: String, summary: String) =
        Spec(key, listOf("secure"), label, summary)

    private fun global(key: String, label: String, summary: String) =
        Spec(key, listOf("global"), label, summary)

    val ALL: List<Spec> =
        listOf(
            // --- Nothing OS 5.0 system features (secure) ---
            secure(
                "nt_generative_notification_enable",
                "AI notification summaries",
                "Nothing's generative notification summaries.",
            ),
            Spec(
                "nt_circle_to_search_support",
                listOf("secure", "global"),
                "Circle to Search",
                "Hold-to-search gesture support.",
            ),
            secure(
                "nt_mistouch_prevention_enable",
                "Mistouch prevention",
                "Edge mistouch rejection.",
            ),
            secure(
                "nt_block_essential_key",
                "Block Essential Key",
                "Disables the Essential Key hardware button.",
            ),
            secure(
                "nt_power_off_verify",
                "Verify on power off",
                "Require authentication before powering off.",
            ),
            secure(
                "nt_generative_ringtone_enable",
                "Generative ringtone",
                "AI-generated ringtones (SIM 1).",
            ),
            Spec(
                "nt_face_recognition_unlock_with_mask",
                listOf("secure", "system"),
                "Face unlock with mask",
                "Allow face unlock while wearing a mask.",
            ),
            secure(
                "nt_private_space_auto_leaving_space",
                "Private Space auto-exit",
                "Leave Private Space automatically.",
            ),
            secure(
                "nt_essential_voice_action_hold",
                "Essential Key hold → voice",
                "Essential Key long-press triggers voice input.",
            ),
            secure(
                "nt_essential_voice_action_double_click",
                "Essential Key double-tap → voice",
                "Essential Key double-press triggers voice input.",
            ),
            secure(
                "nt_game_mode_mistouch_prevention",
                "Game mistouch block",
                "Mistouch prevention inside Game Mode.",
            ),
            secure(
                "nt_game_mode_notification_display_mode",
                "Game mode notifications",
                "Notification presentation during games.",
            ),
            // --- global/system ---
            Spec(
                "nt_data_collection",
                listOf("system"),
                "Nothing data collection",
                "Nothing telemetry/usage collection flag.",
            ),
            global(
                "enable_cellular_on_boot",
                "Mobile data at boot",
                "Re-enable mobile data automatically on boot.",
            ),
            // --- Glyph light-stripe effects (global) ---
            global(
                "led_effect_music_enalbe",
                "Glyph music visualizer",
                "Glyph reacts to playing music.",
            ),
            global(
                "led_effect_call_enalbe",
                "Glyph call effect",
                "Glyph pattern on incoming calls.",
            ),
            global(
                "led_effect_charging_enable",
                "Glyph charging effect",
                "Glyph progress while charging.",
            ),
            global(
                "led_effect_wlr_charging_enable",
                "Glyph wireless-charge effect",
                "Glyph pattern on wireless charging.",
            ),
            global(
                "led_effect_talk_reactive_ebable",
                "Glyph talk feedback",
                "Glyph reacts while you speak.",
            ),
            global(
                "led_effect_volume_indicator_enable",
                "Glyph volume indicator",
                "Glyph shows volume changes.",
            ),
            global(
                "led_effect_schedule_time_ebable",
                "Glyph schedule",
                "Time-windowed glyph effects.",
            ),
            global(
                "led_effect_gestures_flip_ebable",
                "Glyph flip gesture",
                "Flip-to-glyph gesture.",
            ),
            global(
                "led_effect_google_assistant_enalbe",
                "Glyph assistant effect",
                "Glyph reacts to the voice assistant.",
            ),
            global(
                "led_auto_brightness_enable",
                "Glyph auto brightness",
                "Automatic glyph light brightness.",
            ),
        )

    fun byKey(key: String): Spec? = ALL.firstOrNull { it.key == key }
}
