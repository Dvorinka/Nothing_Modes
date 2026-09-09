package com.tdvorak.nothingmodes.engine.phone

import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhoneNumberFormatterTest {
    private val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    @Test
    fun `format national number to E164`() {
        val example = util.getExampleNumber("US")
        assertNotNull(example)
        val national = util.getNationalSignificantNumber(example)
        val e164 = util.format(example, PhoneNumberUtil.PhoneNumberFormat.E164)

        assertEquals(e164, PhoneNumberFormatter.formatToE164(national, "US"))
    }

    @Test
    fun `format international number to E164`() {
        val example = util.getExampleNumber("US")
        assertNotNull(example)
        val e164 = util.format(example, PhoneNumberUtil.PhoneNumberFormat.E164)

        assertEquals(e164, PhoneNumberFormatter.formatToE164(e164, "US"))
    }

    @Test
    fun `invalid number returns null`() {
        assertNull(PhoneNumberFormatter.formatToE164("000", "CZ"))
    }

    @Test
    fun `parse E164 returns national and region`() {
        val example = util.getExampleNumber("US")
        assertNotNull(example)
        val e164 = util.format(example, PhoneNumberUtil.PhoneNumberFormat.E164)

        val parsed = PhoneNumberFormatter.parseE164(e164)!!
        assertEquals("US", parsed.second)
        assertTrue(parsed.first.isNotEmpty())
    }

    @Test
    fun `supported country list covers all libphonenumber regions`() {
        val codes = PhoneNumberFormatter.supportedCountryCodes()
        assertTrue(codes.size > 200, "expected >200 countries, got ${codes.size}")
        assertTrue(codes.any { it.regionCode == "US" })
        assertTrue(codes.any { it.regionCode == "CZ" })
    }

    @Test
    fun `Czechia has a valid dial code and flag`() {
        val cz = PhoneNumberFormatter.supportedCountryCodes().first { it.regionCode == "CZ" }
        assertEquals("Czechia", cz.name)
        assertTrue(cz.dialCode.startsWith("+"))
        assertTrue(cz.dialCode.length > 1)
        assertTrue(cz.flag.isNotEmpty())
    }
}
