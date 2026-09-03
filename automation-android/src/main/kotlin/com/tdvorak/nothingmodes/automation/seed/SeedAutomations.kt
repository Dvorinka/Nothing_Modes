package com.tdvorak.nothingmodes.automation.seed

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.NightMode
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AutomationStore

/**
 * Seeds default Sleep and Morning automations on first launch.
 * Idempotent: only inserts if the store is empty.
 */
object SeedAutomations {

    suspend fun seedIfEmpty(store: AutomationStore) {
        if (store.all().isNotEmpty()) return
        store.save(sleepMode())
        store.save(morningRoutine())
    }

    private fun sleepMode() = Automation(
        id = AutomationId("mode-sleep"),
        name = "Sleep",
        type = AutomationType.MODE,
        createdBy = CreatedBy.USER,
        status = AutomationStatus.ARMED,
        trigger = Trigger.Time(
            cron = "30 22 * * *",
            tz = "Europe/Prague",
        ),
        actions = listOf(
            Action.SetDnd(DndMode.PRIORITY),
            Action.SetDarkMode(NightMode.ON),
            Action.SetExtraDim(on = true, restore = true),
            Action.SetBrightness(level = 26, restore = true),
            Action.SetGlyph(on = false),
        ),
        priority = 10,
    )

    private fun morningRoutine() = Automation(
        id = AutomationId("routine-morning"),
        name = "Morning",
        type = AutomationType.ROUTINE,
        createdBy = CreatedBy.USER,
        status = AutomationStatus.ARMED,
        trigger = Trigger.Time(
            cron = "0 7 * * *",
            tz = "Europe/Prague",
        ),
        actions = listOf(
            Action.SetDnd(DndMode.OFF),
            Action.SetDarkMode(NightMode.OFF),
            Action.SetExtraDim(on = false, restore = true),
            Action.SetBrightness(level = 128, restore = true),
            Action.SetGlyph(on = true, restore = true),
        ),
        priority = 5,
    )
}
