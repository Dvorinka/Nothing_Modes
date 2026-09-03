package com.tdvorak.nothingmodes.automation

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.runtime.ActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.ActionResult
import com.tdvorak.nothingmodes.engine.runtime.FireContext

/**
 * Stub executor: all actions return Success.
 * Will be replaced by real capability controllers in Phase 3.
 */
object StubActionExecutor : ActionExecutor {
    override suspend fun execute(action: Action, context: FireContext): ActionResult = ActionResult.Success
}
