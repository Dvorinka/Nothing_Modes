package com.tdvorak.nothingmodes.engine.model

/** Vocabolario CHIUSO delle chiavi di DeviceState (spec §5 rev 3): il compile non può inventare
 *  chiavi (sono nel manifest) e il DraftValidator rifiuta StateEquals su chiavi fuori registry. */
object StateKeys {
    const val RINGER = "ringer"
    const val WIFI = "wifi"
    const val BLUETOOTH = "bluetooth"
    const val DND = "dnd"
    const val BATTERY = "battery"
    const val CHARGING = "charging"
    const val AIRPLANE = "airplane"
    const val SCREEN = "screen"

    // Boolean state keys (Condition.BooleanState)
    const val DEVICE_LOCKED = "device_locked"
    const val WIFI_RADIO = "wifi_radio"
    const val BLUETOOTH_RADIO = "bluetooth_radio"
    const val MOBILE_DATA = "mobile_data"
    const val AOD_ENABLED = "aod_enabled"

    // DND active boolean state (Condition.BooleanState)
    const val DND_ACTIVE = "dnd_active"

    // Numeric state keys (Condition.NumericState)
    const val BRIGHTNESS = "brightness"
    const val REFRESH_RATE = "refresh_rate"
    const val SCREEN_TIMEOUT = "screen_timeout"

    /** chiave -> valori ammessi (usato nel render del manifest e in doc) */
    val ALL: Map<String, String> =
        mapOf(
            RINGER to "normal|vibrate|silent",
            WIFI to "on|off",
            BLUETOOTH to "on|off",
            DND to "off|priority|total",
            BATTERY to "0-100",
            CHARGING to "true|false",
            AIRPLANE to "on|off",
            SCREEN to "on|off",
            DEVICE_LOCKED to "true|false",
            WIFI_RADIO to "true|false",
            BLUETOOTH_RADIO to "true|false",
            MOBILE_DATA to "true|false",
            AOD_ENABLED to "true|false",
            DND_ACTIVE to "true|false",
            BRIGHTNESS to "0-255",
            REFRESH_RATE to "0-144",
            SCREEN_TIMEOUT to "0-",
        )
}
