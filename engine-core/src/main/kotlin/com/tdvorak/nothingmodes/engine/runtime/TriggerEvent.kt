package com.tdvorak.nothingmodes.engine.runtime

import com.tdvorak.nothingmodes.engine.model.Trigger

/** Event delivered to the engine when a trigger fires. */
sealed interface TriggerEvent {
    val eventId: String

    data class Registered(
        override val eventId: String,
        val automationId: com.tdvorak.nothingmodes.engine.model.AutomationId,
        val approvalFingerprint: String? = null,
    ) : TriggerEvent

    data class TimeFired(
        override val eventId: String,
        val automationId: com.tdvorak.nothingmodes.engine.model.AutomationId,
        val atMillis: Long,
    ) : TriggerEvent

    data class ModeWindowStart(
        override val eventId: String,
        val automationId: com.tdvorak.nothingmodes.engine.model.AutomationId,
        val atMillis: Long,
    ) : TriggerEvent

    data class ModeWindowEnd(
        override val eventId: String,
        val automationId: com.tdvorak.nothingmodes.engine.model.AutomationId,
        val atMillis: Long,
    ) : TriggerEvent

    data class NotificationPosted(
        override val eventId: String,
        val pkg: String,
        val title: String?,
        val text: String?,
        val sender: String?,
    ) : TriggerEvent

    data class PhoneStateChanged(
        override val eventId: String,
        val event: com.tdvorak.nothingmodes.engine.model.PhoneEvent,
        val number: String?,
        val smsText: String?,
    ) : TriggerEvent

    data class ConnectivityChanged(
        override val eventId: String,
        val medium: com.tdvorak.nothingmodes.engine.model.ConnMedium,
        val state: com.tdvorak.nothingmodes.engine.model.ConnState,
        val match: String?,
    ) : TriggerEvent

    data class BootCompleted(override val eventId: String) : TriggerEvent

    data class BatteryLevelChanged(
        override val eventId: String,
        val level: Int,
        val isCharging: Boolean,
    ) : TriggerEvent

    data class ScreenStateChanged(
        override val eventId: String,
        val state: com.tdvorak.nothingmodes.engine.model.ScreenState,
    ) : TriggerEvent

    data class AppForegroundChanged(
        override val eventId: String,
        val pkg: String,
        val inForeground: Boolean,
    ) : TriggerEvent
}

/** Envelope wrapping a trigger event with metadata. */
data class TriggerEnvelope(
    val id: String,
    val event: TriggerEvent,
    val receivedAtMillis: Long,
)
