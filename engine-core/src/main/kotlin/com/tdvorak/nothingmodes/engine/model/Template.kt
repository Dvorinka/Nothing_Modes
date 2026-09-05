package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.Serializable

/**
 * Index of community templates. Served from templates/index.json in the repo
 * (raw.githubusercontent.com), decoded with ignoreUnknownKeys so older apps
 * tolerate newer index fields.
 */
@Serializable
data class TemplateIndex(
    val version: Int = 1,
    val templates: List<TemplateSummary> = emptyList(),
)

/** One row in the community template index. `file` resolves against the index directory. */
@Serializable
data class TemplateSummary(
    val id: String,
    val name: String,
    val description: String = "",
    val creator: String = "",
    val emoji: String = "",
    val tags: List<String> = emptyList(),
    /** Minimum export schema version needed to consume this template. */
    val minSchemaVersion: Int = AUTOMATION_SCHEMA_VERSION_V1,
    /** Relative path to the ExportBundle JSON, e.g. "night-owl.json". */
    val file: String,
)
