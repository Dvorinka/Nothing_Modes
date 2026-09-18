package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Action

/** Result of a single action execution. */
sealed interface ActionResult {
    data object Success : ActionResult

    data class Failure(
        val reason: String,
    ) : ActionResult

    data object Unsupported : ActionResult

    data object PermissionRequired : ActionResult

    data object ShizukuRequired : ActionResult

    /** The system can't toggle silently — a panel/settings page was opened for the user. */
    data object NeedsUserAction : ActionResult

    /**
     * The action needs to show UI but the keyguard is locked, so it was queued
     * and will be re-executed the moment the user unlocks the device.
     */
    data object DeferredUntilUnlock : ActionResult
}

/**
 * Human-readable reason an action didn't complete — used for audit entries,
 * notifications, and detail screens. Null for successful results.
 */
fun ActionResult.failureLabel(action: Action): String? {
    val name = com.tdvorak.nothingmodes.engine.model.actionDescription(action)
    return when (this) {
        is ActionResult.Failure -> "$name: ${reason.ifBlank { "failed" }}"
        ActionResult.PermissionRequired -> "$name: missing permission"
        ActionResult.ShizukuRequired -> "$name: needs Shizuku"
        ActionResult.Unsupported -> "$name: not supported on this device"
        else -> null
    }
}

/** Context passed to action executors at fire time. */
data class FireContext(
    val eventId: String,
    val executionId: String,
    val automationId: com.tdvorak.nothingmodes.engine.model.AutomationId,
    val actionIndex: Int,
    val priority: Int,
)

/** Executes a single action. Implemented by the Android runtime. */
fun interface ActionExecutor {
    suspend fun execute(
        action: Action,
        context: FireContext,
    ): ActionResult
}

/** No-op executor for testing. */
object NoopActionExecutor : ActionExecutor {
    override suspend fun execute(
        action: Action,
        context: FireContext,
    ): ActionResult = ActionResult.Success
}
