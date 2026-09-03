package com.dvoranka.nothingmodes.nothing

import android.content.Context
import android.os.Build

/** Detects Nothing device model and Glyph hardware at runtime. */
class NothingDeviceDetector(private val context: Context) {

    /** Detects the Nothing device model using Build.MODEL. */
    fun detectModel(): String? {
        if (!isNothingDevice()) return null
        val model = Build.MODEL ?: return null
        return when {
            model.startsWith("A063") && !model.contains("P") -> NothingDeviceIds.PHONE_1
            model.startsWith("A065") -> NothingDeviceIds.PHONE_2
            model.startsWith("A142") && !model.contains("P") -> NothingDeviceIds.PHONE_2A
            model.startsWith("A142P") -> NothingDeviceIds.PHONE_2A_PLUS
            model.startsWith("A059") && !model.contains("P") -> NothingDeviceIds.PHONE_3A
            model.startsWith("A059P") -> NothingDeviceIds.PHONE_3A
            model.startsWith("A001") -> NothingDeviceIds.PHONE_3
            model.startsWith("A063P") -> NothingDeviceIds.PHONE_4A_PRO
            model.startsWith("A172") && !model.contains("P") -> NothingDeviceIds.PHONE_4A
            model.startsWith("A172P") -> NothingDeviceIds.PHONE_4B
            else -> null
        }
    }

    /** Detects Glyph hardware type from device model. */
    fun detectGlyphHardware(): GlyphHardware {
        val model = detectModel() ?: return GlyphHardware.NONE
        return when (model) {
            NothingDeviceIds.PHONE_3 -> GlyphHardware.MATRIX_25
            NothingDeviceIds.PHONE_4A_PRO -> GlyphHardware.MATRIX_13
            NothingDeviceIds.PHONE_1,
            NothingDeviceIds.PHONE_2,
            NothingDeviceIds.PHONE_2A,
            NothingDeviceIds.PHONE_2A_PLUS,
            NothingDeviceIds.PHONE_3A,
            NothingDeviceIds.PHONE_4A,
            NothingDeviceIds.PHONE_4B,
            -> GlyphHardware.LIGHT_STRIPE
            else -> GlyphHardware.NONE
        }
    }

    /** Whether this device has Glyph Touch (only Phone 3). */
    fun hasGlyphTouch(): Boolean = detectModel() == NothingDeviceIds.PHONE_3

    /** Whether this is a Nothing device at all. */
    fun isNothingDevice(): Boolean =
        Build.MANUFACTURER?.equals("nothing", ignoreCase = true) == true

    companion object {
        @JvmStatic
        val NONE = GlyphHardware.NONE
    }
}
