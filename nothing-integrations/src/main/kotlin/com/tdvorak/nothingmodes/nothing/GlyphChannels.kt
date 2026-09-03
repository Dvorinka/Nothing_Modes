package com.tdvorak.nothingmodes.nothing

/**
 * Glyph channel constants for each Nothing phone model.
 *
 * The Glyph Interface is indexed differently per device. This object provides
 * typed channel constants and zone presets for each supported phone.
 *
 * Source: https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit
 */
object GlyphChannels {

    // ── Phone (1) — DEVICE_20111 ──
    object Phone1 {
        const val A1 = 0
        const val B1 = 1
        const val C1 = 2; const val C2 = 3; const val C3 = 4; const val C4 = 5
        const val E1 = 6
        const val D1_1 = 7; const val D1_2 = 8; const val D1_3 = 9; const val D1_4 = 10
        const val D1_5 = 11; const val D1_6 = 12; const val D1_7 = 13; const val D1_8 = 14

        val ZONE_A = listOf(A1)
        val ZONE_B = listOf(B1)
        val ZONE_C = listOf(C1, C2, C3, C4)
        val ZONE_D = listOf(D1_1, D1_2, D1_3, D1_4, D1_5, D1_6, D1_7, D1_8)
        val ZONE_E = listOf(E1)
        val ALL = ZONE_A + ZONE_B + ZONE_C + ZONE_D + ZONE_E
        val PROGRESS = ZONE_D // D1 used for progress display
    }

    // ── Phone (2) — DEVICE_22111 ──
    object Phone2 {
        const val A1 = 0; const val A2 = 1
        const val B1 = 2
        const val C1_1 = 3; const val C1_2 = 4; const val C1_3 = 5; const val C1_4 = 6
        const val C1_5 = 7; const val C1_6 = 8; const val C1_7 = 9; const val C1_8 = 10
        const val C1_9 = 11; const val C1_10 = 12; const val C1_11 = 13; const val C1_12 = 14
        const val C1_13 = 15; const val C1_14 = 16; const val C1_15 = 17; const val C1_16 = 18
        const val C2 = 19; const val C3 = 20; const val C4 = 21; const val C5 = 22; const val C6 = 23
        const val E1 = 24
        const val D1_1 = 25; const val D1_2 = 26; const val D1_3 = 27; const val D1_4 = 28
        const val D1_5 = 29; const val D1_6 = 30; const val D1_7 = 31; const val D1_8 = 32

        val ZONE_A = listOf(A1, A2)
        val ZONE_B = listOf(B1)
        val ZONE_C = listOf(C1_1, C1_2, C1_3, C1_4, C1_5, C1_6, C1_7, C1_8,
            C1_9, C1_10, C1_11, C1_12, C1_13, C1_14, C1_15, C1_16, C2, C3, C4, C5, C6)
        val ZONE_D = listOf(D1_1, D1_2, D1_3, D1_4, D1_5, D1_6, D1_7, D1_8)
        val ZONE_E = listOf(E1)
        val ALL = ZONE_A + ZONE_B + ZONE_C + ZONE_D + ZONE_E
        val PROGRESS = ZONE_D
    }

    // ── Phone (2a) / (2a) Plus — DEVICE_23111 / 23113 ──
    object Phone2a {
        const val C1 = 0; const val C2 = 1; const val C3 = 2; const val C4 = 3
        const val C5 = 4; const val C6 = 5; const val C7 = 6; const val C8 = 7
        const val C9 = 8; const val C10 = 9; const val C11 = 10; const val C12 = 11
        const val C13 = 12; const val C14 = 13; const val C15 = 14; const val C16 = 15
        const val C17 = 16; const val C18 = 17; const val C19 = 18; const val C20 = 19
        const val C21 = 20; const val C22 = 21; const val C23 = 22; const val C24 = 23
        const val B = 24; const val A = 25

        val ZONE_A = listOf(A)
        val ZONE_B = listOf(B)
        val ZONE_C = (C1..C24).toList()
        val ALL = ZONE_A + ZONE_B + ZONE_C
        val PROGRESS = ZONE_C
    }

    // ── Phone (3a) / (3a) Pro — DEVICE_24111 ──
    object Phone3a {
        const val C1 = 0; const val C2 = 1; const val C3 = 2; const val C4 = 3
        const val C5 = 4; const val C6 = 5; const val C7 = 6; const val C8 = 7
        const val C9 = 8; const val C10 = 9; const val C11 = 10; const val C12 = 11
        const val C13 = 12; const val C14 = 13; const val C15 = 14; const val C16 = 15
        const val C17 = 16; const val C18 = 17; const val C19 = 18; const val C20 = 19
        const val A1 = 20; const val A2 = 21; const val A3 = 22; const val A4 = 23
        const val A5 = 24; const val A6 = 25; const val A7 = 26; const val A8 = 27
        const val A9 = 28; const val A10 = 29; const val A11 = 30
        const val B1 = 31; const val B2 = 32; const val B3 = 33; const val B4 = 34; const val B5 = 35

        val ZONE_A = (A1..A11).toList()
        val ZONE_B = (B1..B5).toList()
        val ZONE_C = (C1..C20).toList()
        val ALL = ZONE_A + ZONE_B + ZONE_C
        val PROGRESS = ZONE_C
    }

    // ── Phone (4a) — DEVICE_25111 ──
    object Phone4a {
        const val A1 = 0; const val A2 = 1; const val A3 = 2
        const val A4 = 3; const val A5 = 4; const val A6 = 5

        val ZONE_A = listOf(A1, A2, A3, A4, A5, A6)
        val ALL = ZONE_A
        val PROGRESS = ZONE_A
    }

    // ── Phone (4b) — DEVICE_25131 ──
    object Phone4b {
        const val A1 = 0; const val A2 = 1; const val A3 = 2; const val A4 = 3

        val ZONE_A = listOf(A1, A2, A3, A4)
        val ALL = ZONE_A
        val PROGRESS = ZONE_A
    }

    /** Get channel set for a device model. */
    fun forDevice(modelId: String): DeviceChannels? = when (modelId) {
        NothingDeviceIds.PHONE_1 -> DeviceChannels(
            all = Phone1.ALL, progress = Phone1.PROGRESS,
            zones = mapOf("A" to Phone1.ZONE_A, "B" to Phone1.ZONE_B, "C" to Phone1.ZONE_C, "D" to Phone1.ZONE_D, "E" to Phone1.ZONE_E),
        )
        NothingDeviceIds.PHONE_2 -> DeviceChannels(
            all = Phone2.ALL, progress = Phone2.PROGRESS,
            zones = mapOf("A" to Phone2.ZONE_A, "B" to Phone2.ZONE_B, "C" to Phone2.ZONE_C, "D" to Phone2.ZONE_D, "E" to Phone2.ZONE_E),
        )
        NothingDeviceIds.PHONE_2A, NothingDeviceIds.PHONE_2A_PLUS -> DeviceChannels(
            all = Phone2a.ALL, progress = Phone2a.PROGRESS,
            zones = mapOf("A" to Phone2a.ZONE_A, "B" to Phone2a.ZONE_B, "C" to Phone2a.ZONE_C),
        )
        NothingDeviceIds.PHONE_3A -> DeviceChannels(
            all = Phone3a.ALL, progress = Phone3a.PROGRESS,
            zones = mapOf("A" to Phone3a.ZONE_A, "B" to Phone3a.ZONE_B, "C" to Phone3a.ZONE_C),
        )
        NothingDeviceIds.PHONE_4A -> DeviceChannels(
            all = Phone4a.ALL, progress = Phone4a.PROGRESS,
            zones = mapOf("A" to Phone4a.ZONE_A),
        )
        NothingDeviceIds.PHONE_4B -> DeviceChannels(
            all = Phone4b.ALL, progress = Phone4b.PROGRESS,
            zones = mapOf("A" to Phone4b.ZONE_A),
        )
        else -> null
    }
}

/** Channel layout for a specific device. */
data class DeviceChannels(
    val all: List<Int>,
    val progress: List<Int>,
    val zones: Map<String, List<Int>>,
)
