package com.tdvorak.nothingmodes.engine.phone

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

/**
 * Phone number helper backed by libphonenumber.
 *
 * All stored phone numbers in the engine are kept in E.164 so call/SMS
 * triggers can be matched against the carrier-formatted numbers delivered
 * by the system.
 */
object PhoneNumberFormatter {
    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    data class CountryCode(
        val regionCode: String,
        val name: String,
        val dialCode: String,
        val flag: String,
    )

    private const val MANUAL_REGION = "ZZ"
    private const val MANUAL_NAME = "Other / manual"
    private const val MANUAL_DIAL = "+"
    private const val MANUAL_FLAG = "\uD83C\uDF0D" // 🌍

    /**
     * Returns the list of supported country codes, sorted by name, with an
     * "Other / manual" entry at the end for users who want to type the full
     * E.164 number themselves.
     */
    fun supportedCountryCodes(): List<CountryCode> {
        val locales =
            util.supportedRegions
                .map { region ->
                    val cc = util.getCountryCodeForRegion(region)
                    if (cc <= 0) {
                        null
                    } else {
                        CountryCode(
                            regionCode = region,
                            name = Locale("en", region).getDisplayCountry(Locale.ENGLISH),
                            dialCode = "+$cc",
                            flag = regionToFlag(region),
                        )
                    }
                }.filterNotNull()
                .sortedBy { it.name }

        return locales + CountryCode(MANUAL_REGION, MANUAL_NAME, MANUAL_DIAL, MANUAL_FLAG)
    }

    /**
     * Parses [input] using [regionCode] and returns the E.164 representation.
     * Returns `null` if the number cannot be parsed or is not valid.
     *
     * If [input] already starts with `+`, the region is only used as a fallback.
     */
    fun formatToE164(
        input: String,
        regionCode: String,
    ): String? {
        val normalized = input.trim()
        if (normalized.isEmpty()) return null

        return try {
            val number = util.parse(normalized, regionCode)
            if (util.isValidNumber(number)) util.format(number, PhoneNumberUtil.PhoneNumberFormat.E164) else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses an E.164 number and returns the national number and its detected
     * region code, or `null` if it cannot be parsed.
     */
    fun parseE164(e164: String): Pair<String, String>? {
        return try {
            val number = util.parse(e164, null)
            if (!util.isValidNumber(number)) return null
            val region = util.getRegionCodeForNumber(number)
            val national = util.getNationalSignificantNumber(number)
            national to region
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns the country calling code for a region, e.g. `CZ` -> `+<dialCode>`.
     */
    fun dialCodeForRegion(regionCode: String): String {
        val cc = util.getCountryCodeForRegion(regionCode.uppercase())
        return if (cc > 0) "+$cc" else "+"
    }

    /**
     * Converts a two-letter region code to a flag emoji using Unicode regional
     * indicator symbols.
     */
    fun regionToFlag(regionCode: String): String {
        val upper = regionCode.uppercase()
        if (upper.length != 2) return MANUAL_FLAG
        return buildString {
            for (ch in upper) {
                appendCodePoint(0x1F1E6 + (ch.code - 'A'.code))
            }
        }
    }
}
