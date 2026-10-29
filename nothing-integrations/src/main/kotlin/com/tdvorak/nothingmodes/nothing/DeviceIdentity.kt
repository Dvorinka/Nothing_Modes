package com.tdvorak.nothingmodes.nothing

import android.os.Build

/**
 * Canonical "is this a Nothing-family device" check. Single source —
 * previously three call sites compared Build.MANUFACTURER to "nothing",
 * which misses devices reporting the brand field instead (and the CMF
 * sub-brand entirely).
 */
object DeviceIdentity {
    private val NOTHING_IDENTIFIERS = setOf("nothing", "cmf")

    fun isNothingFamily(): Boolean =
        NOTHING_IDENTIFIERS.any { id ->
            Build.MANUFACTURER.equals(id, ignoreCase = true) ||
                Build.BRAND.equals(id, ignoreCase = true)
        }
}
