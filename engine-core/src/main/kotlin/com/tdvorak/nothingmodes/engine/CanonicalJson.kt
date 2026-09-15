package com.tdvorak.nothingmodes.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Canonical JSON string with sorted object keys.
 *
 * This lets the client compute the same SHA-256 hash that the server stored
 * even when a DB/transport layer (e.g. Postgres jsonb) reorders keys.
 */
fun JsonElement.canonicalJson(): String =
    when (this) {
        is JsonNull -> "null"
        is JsonPrimitive -> canonicalPrimitive()
        is JsonArray -> joinToString(",", "[", "]") { it.canonicalJson() }
        is JsonObject ->
            entries
                .toList()
                .sortedBy { it.key }
                .joinToString(",", "{", "}") { "\"${escape(it.key)}\":${it.value.canonicalJson()}" }
    }

private fun JsonPrimitive.canonicalPrimitive(): String {
    if (isString) return "\"${escape(content)}\""
    booleanOrNull?.let { return it.toString() }
    // Preserve the original textual form for integers.
    intOrNull?.let { return it.toString() }
    longOrNull?.let { return it.toString() }
    doubleOrNull?.let { return jsNumber(it) }
    return content
}

/**
 * Reproduces ECMAScript Number→String, which the server's canonicalJson
 * gets from JSON.stringify: decimal for 1e-6 ≤ |d| < 1e21 (integral or not),
 * "me±X" exponential outside that, "0" for negative zero, "null" for
 * non-finite values. Kotlin's toString differs — it switches to E-notation
 * at 1e7 and saturates toLong past Long range, so format explicitly.
 */
private fun jsNumber(d: Double): String {
    if (!d.isFinite()) return "null" // JS stringifies NaN/Infinity as null
    if (d == 0.0) return "0" // JSON.stringify(-0) → "0"
    val abs = kotlin.math.abs(d)
    return if (abs in 1e-6..<1e21) {
        // BigDecimal on the shortest-round-trip repr expands exactly like JS.
        java.math.BigDecimal(d.toString()).stripTrailingZeros().toPlainString()
    } else {
        val s = d.toString() // e.g. "1.5E21", "1.0E-7"
        val ei = s.indexOf('E')
        if (ei < 0) return s
        val mantissa = s.substring(0, ei).removeSuffix(".0")
        val exp = s.substring(ei + 1).toInt()
        mantissa + "e" + (if (exp >= 0) "+$exp" else "$exp")
    }
}

private fun escape(s: String): String =
    buildString {
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f") // JS uses the \f shorthand, not \u000c
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                in '\u0000'..'\u001f' ->
                    append("\\u%04x".format(c.code))
                else -> append(c)
            }
        }
    }

/**
 * Convenience: canonical JSON and SHA-256 hash of a raw JSON string.
 */
fun jsonSha256(json: String): String = sha256Hex(Json.parseToJsonElement(json).canonicalJson())
