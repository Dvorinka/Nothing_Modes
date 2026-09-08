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
    // Preserve the original textual form for integers and doubles.
    intOrNull?.let { return it.toString() }
    longOrNull?.let { return it.toString() }
    doubleOrNull?.let { return it.toString() }
    return content
}

private fun escape(s: String): String =
    buildString {
        for (c in s) {
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\b' -> append("\\b")
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
