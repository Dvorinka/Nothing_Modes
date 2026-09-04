@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.tdvorak.nothingmodes.engine.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@JvmInline @Serializable value class AutomationId(val value: String)
enum class CreatedBy { LLM, USER, IMPORT }
enum class AutomationStatus { PENDING_APPROVAL, ARMED, DISABLED, NEEDS_REVIEW }
enum class AutomationType { MODE, ROUTINE }

const val AUTOMATION_SCHEMA_VERSION_V1 = 1

object AutomationSchema {
    val supportedVersions: Set<Int> = setOf(AUTOMATION_SCHEMA_VERSION_V1)

    fun isSupportedVersion(version: Int): Boolean = version in supportedVersions
}

@Serializable
data class Automation(
    val id: AutomationId,
    val name: String,
    val type: AutomationType,
    val createdBy: CreatedBy,
    val status: AutomationStatus,
    val trigger: Trigger,
    val actions: List<Action>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val conditions: Condition? = null,
    val enabled: Boolean = true,
    /** Allow this automation to appear in quick-action surfaces (widget, Quick Settings tile). */
    val quickAction: Boolean = true,
    /** Higher priority wins in conflict. Same priority: last-activated wins. */
    val priority: Int = 0,
    val cooldownMs: Long = 0,
    val schemaVersion: Int = AUTOMATION_SCHEMA_VERSION_V1,
    val approvalFingerprint: String? = null,
    /** Display icon name from the Material icon set. */
    val icon: String = "",
    /** Background color as a hex string. */
    val iconBackground: String = "",
)

@Serializable
data class AutomationDraft(
    val name: String,
    val type: AutomationType,
    val trigger: Trigger,
    val actions: List<Action>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val conditions: Condition? = null,
    val rationale: String = "",
    val cooldownMs: Long = 0,
)
