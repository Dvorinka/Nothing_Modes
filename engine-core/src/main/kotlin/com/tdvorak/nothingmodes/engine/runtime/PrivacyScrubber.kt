package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.Condition
import com.tdvorak.nothingmodes.engine.model.Trigger

/**
 * Strips private device identifiers and personal content from an automation
 * before it leaves the device as a shareable template.
 *
 * Scrubbed fields become null/empty/zero — the structure survives intact so
 * the template still shows exactly what it does, but the importer must fill
 * the blanks before it can run. [scrub] returns the readable names of the
 * blanked fields so the importer can be told what to configure.
 *
 * Kept deliberately conservative: app package names, timezone ids, and
 * user-authored template content (notification text, glyph text) are
 * structural, not private. Device-bound identifiers, coordinates, message
 * content, and match strings are private.
 */
object PrivacyScrubber {
    /** @return scrubbed automation plus the names of fields the importer must configure. */
    fun scrub(automation: Automation): Pair<Automation, List<String>> {
        val missing = mutableListOf<String>()
        val scrubbed =
            automation.copy(
                trigger = scrubTrigger(automation.trigger, missing),
                conditions = automation.conditions?.let { scrubCondition(it, missing) },
                actions = automation.actions.map { scrubAction(it, missing) },
                endActions = automation.endActions.map { scrubAction(it, missing) },
            )
        return scrubbed to missing.distinct()
    }

    /**
     * Names of fields that would be scrubbed — used to badge nodes that need
     * setup on an imported/scrubbed automation. Does not modify the input.
     */
    fun missingFields(trigger: Trigger): List<String> =
        mutableListOf<String>().also { scrubTrigger(trigger, it) }

    fun missingFields(condition: Condition): List<String> =
        mutableListOf<String>().also { scrubCondition(condition, it) }

    fun missingFields(action: Action): List<String> =
        mutableListOf<String>().also { scrubAction(action, it) }

    private fun scrubTrigger(
        trigger: Trigger,
        missing: MutableList<String>,
    ): Trigger =
        when (trigger) {
            is Trigger.BluetoothDevice -> {
                if (trigger.deviceName != null || trigger.deviceAddress != null) {
                    missing += "Bluetooth device"
                }
                trigger.copy(deviceName = null, deviceAddress = null)
            }
            is Trigger.WifiConnected -> {
                if (trigger.ssid != null) missing += "Wi-Fi network"
                trigger.copy(ssid = null)
            }
            is Trigger.Connectivity -> {
                if (trigger.match != null) missing += "connection detail"
                trigger.copy(match = null)
            }
            is Trigger.Notification -> {
                if (trigger.conversationId != null ||
                    trigger.sender != null ||
                    trigger.titleMatch != null ||
                    trigger.textMatch != null
                ) {
                    missing += "notification filter"
                }
                trigger.copy(
                    conversationId = null,
                    sender = null,
                    titleMatch = null,
                    textMatch = null,
                )
            }
            is Trigger.PhoneState -> {
                if (trigger.number != null) missing += "phone number"
                if (trigger.textMatch != null) missing += "message text"
                trigger.copy(number = null, textMatch = null)
            }
            is Trigger.CalendarEvent -> {
                if (trigger.calendarId != null || trigger.titleMatch != null || trigger.events.isNotEmpty()) {
                    missing += "calendar event"
                }
                trigger.copy(calendarId = null, titleMatch = null, events = emptyList())
            }
            is Trigger.Geofence -> {
                if (trigger.lat != 0.0 || trigger.lng != 0.0) missing += "location"
                trigger.copy(lat = 0.0, lng = 0.0)
            }
            else -> trigger
        }

    private fun scrubCondition(
        condition: Condition,
        missing: MutableList<String>,
    ): Condition =
        when (condition) {
            is Condition.WifiConnected -> {
                if (condition.ssid != null) missing += "Wi-Fi network"
                condition.copy(ssid = null)
            }
            is Condition.BluetoothConnected -> {
                if (condition.deviceName != null) missing += "Bluetooth device"
                condition.copy(deviceName = null)
            }
            is Condition.AtLocation -> {
                if (condition.lat != 0.0 || condition.lng != 0.0) missing += "location"
                condition.copy(lat = 0.0, lng = 0.0)
            }
            is Condition.EventActive -> {
                if (condition.titleMatch.isNotBlank()) missing += "calendar event title"
                condition.copy(titleMatch = "")
            }
            is Condition.NotificationPresent -> {
                if (condition.titleMatch.isNotBlank()) missing += "notification text"
                condition.copy(titleMatch = "")
            }
            is Condition.AlarmRinging -> {
                if (condition.titleMatch != null) missing += "alarm title"
                condition.copy(titleMatch = null)
            }
            is Condition.CurrentModeActive -> {
                // References a local mode id that won't exist after import.
                missing += "linked mode"
                condition
            }
            is Condition.And -> condition.copy(all = condition.all.map { scrubCondition(it, missing) })
            is Condition.Or -> condition.copy(any = condition.any.map { scrubCondition(it, missing) })
            is Condition.Not -> condition.copy(cond = scrubCondition(condition.cond, missing))
            else -> condition
        }

    private fun scrubAction(
        action: Action,
        missing: MutableList<String>,
    ): Action =
        when (action) {
            is Action.SendSms -> {
                if (action.number.isNotBlank() || action.text.isNotBlank()) missing += "SMS recipient and text"
                action.copy(number = "", text = "")
            }
            is Action.CopyText -> {
                if (action.text.isNotBlank()) missing += "clipboard text"
                action.copy(text = "")
            }
            is Action.OpenUrl -> {
                if (action.url.isNotBlank()) missing += "URL"
                action.copy(url = "")
            }
            is Action.SetWallpaper -> {
                if (action.uri.isNotBlank()) missing += "wallpaper image"
                action.copy(uri = "")
            }
            is Action.Group -> action.copy(actions = action.actions.map { scrubAction(it, missing) })
            else -> action
        }
}
