package com.tdvorak.nothingmodes.nothing

/** Result of a Glyph operation. */
sealed interface GlyphResult {
    data object Success : GlyphResult
    data class Failure(val reason: String) : GlyphResult
    data object Unsupported : GlyphResult
    data object PermissionRequired : GlyphResult
    data object ServiceUnavailable : GlyphResult
}

/** Detected Glyph hardware type. */
enum class GlyphHardware {
    NONE,
    LIGHT_STRIPE,
    MATRIX_25,
    MATRIX_13,
    ;

    val isMatrix: Boolean get() = this == MATRIX_25 || this == MATRIX_13
    val isLightStripe: Boolean get() = this == LIGHT_STRIPE
    val matrixSize: Int get() = when (this) {
        MATRIX_25 -> 25
        MATRIX_13 -> 13
        else -> 0
    }
}

/** Nothing device model identifiers. */
object NothingDeviceIds {
    const val PHONE_1 = "DEVICE_20111"
    const val PHONE_2 = "DEVICE_22111"
    const val PHONE_2A = "DEVICE_23111"
    const val PHONE_2A_PLUS = "DEVICE_23113"
    const val PHONE_3A = "DEVICE_24111"
    const val PHONE_3 = "DEVICE_23112"
    const val PHONE_4A = "DEVICE_25111"
    const val PHONE_4A_PRO = "DEVICE_25111p"
    const val PHONE_4B = "DEVICE_25131"
}
