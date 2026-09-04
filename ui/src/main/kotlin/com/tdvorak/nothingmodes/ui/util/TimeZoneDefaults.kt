package com.tdvorak.nothingmodes.ui.util

import java.time.ZoneId

/**
 * Returns the device's current system default time-zone ID.
 * Used for all new triggers and conditions so the app never assumes a fixed zone.
 */
fun defaultTimeZone(): String = ZoneId.systemDefault().id
