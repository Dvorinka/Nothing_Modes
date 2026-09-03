package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.json.Json

/** Single source of truth for polymorphic serialization of engine types. */
object EngineJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        prettyPrint = false
    }
}
