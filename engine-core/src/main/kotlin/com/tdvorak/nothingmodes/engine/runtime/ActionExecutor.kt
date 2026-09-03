package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Action

/** Result of a single action execution. */
sealed interface ActionResult {
    data object Success : ActionResult
    data class Failure(val reason: String) : ActionResult
    data object Unsupported : ActionResult
    data object PermissionRequired : ActionResult
    data object ShizukuRequired : ActionResult
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
    suspend fun execute(action: Action, context: FireContext): ActionResult
}

/** No-op executor for testing. */
object NoopActionExecutor : ActionExecutor {
    override suspend fun execute(action: Action, context: FireContext): ActionResult = ActionResult.Success
}
