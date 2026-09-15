package com.tdvorak.nothingmodes.engine

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The server's canonicalJson is JSON.stringify with sorted keys — every
 * case here is verified against the ECMAScript Number/String output.
 */
class CanonicalJsonTest {
    private fun canon(json: String): String = Json.parseToJsonElement(json).canonicalJson()

    @Test
    fun `object keys are sorted`() {
        assertEquals("""{"a":1,"b":2}""", canon("""{"b":2,"a":1}"""))
    }

    @Test
    fun `nested keys sorted recursively`() {
        assertEquals("""{"a":{"y":1,"z":2}}""", canon("""{"a":{"z":2,"y":1}}"""))
    }

    @Test
    fun `integral double serializes like JS`() {
        // JSON.stringify(500.0) → "500"
        assertEquals("""{"v":500}""", canon("""{"v":500.0}"""))
    }

    @Test
    fun `fractional double stays decimal`() {
        assertEquals("""{"v":0.5}""", canon("""{"v":0.5}"""))
    }

    @Test
    fun `large integral double stays decimal below 1e21`() {
        // JSON.stringify(1e19) → "10000000000000000000"
        assertEquals("""{"v":10000000000000000000}""", canon("""{"v":1e19}"""))
    }

    @Test
    fun `double at 1e21 uses exponential`() {
        // JSON.stringify(1e21) → "1e+21"
        assertEquals("""{"v":1e+21}""", canon("""{"v":1e21}"""))
    }

    @Test
    fun `tiny double below 1e-6 uses exponential`() {
        // JSON.stringify(1e-7) → "1e-7"
        assertEquals("""{"v":1e-7}""", canon("""{"v":1e-7}"""))
    }

    @Test
    fun `negative zero stringifies as 0`() {
        // JSON.stringify(-0) → "0"
        assertEquals("""{"v":0}""", canon("""{"v":-0.0}"""))
    }

    @Test
    fun `form feed uses shorthand escape`() {
        // JSON.stringify of a form-feed char produces the two-char escape \f.
        assertEquals("{\"v\":\"\\f\"}", canon("""{"v":"\f"}"""))
    }

    @Test
    fun `control chars use unicode escape`() {
        assertEquals("{\"v\":\"\\u0001\"}", canon("""{"v":"\u0001"}"""))
    }

    @Test
    fun `non-ascii passes through raw`() {
        assertEquals("""{"v":"čau ☺"}""", canon("""{"v":"čau ☺"}"""))
    }
}
