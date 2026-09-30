package com.tdvorak.nothingmodes.engine

import com.tdvorak.nothingmodes.engine.model.Action
import com.tdvorak.nothingmodes.engine.model.Automation
import com.tdvorak.nothingmodes.engine.model.AutomationId
import com.tdvorak.nothingmodes.engine.model.AutomationStatus
import com.tdvorak.nothingmodes.engine.model.AutomationType
import com.tdvorak.nothingmodes.engine.model.CreatedBy
import com.tdvorak.nothingmodes.engine.model.DndMode
import com.tdvorak.nothingmodes.engine.model.GateDirection
import com.tdvorak.nothingmodes.engine.model.GeofenceEndMode
import com.tdvorak.nothingmodes.engine.model.Transition
import com.tdvorak.nothingmodes.engine.model.Trigger
import com.tdvorak.nothingmodes.engine.runtime.AuditEvent
import com.tdvorak.nothingmodes.engine.runtime.AuditKind
import com.tdvorak.nothingmodes.engine.runtime.AuditSink
import com.tdvorak.nothingmodes.engine.runtime.Engine
import com.tdvorak.nothingmodes.engine.runtime.GeofenceGate
import com.tdvorak.nothingmodes.engine.runtime.InMemoryAutomationStore
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationProvider
import com.tdvorak.nothingmodes.engine.runtime.ModeActivationSink
import com.tdvorak.nothingmodes.engine.runtime.NoopActionExecutor
import com.tdvorak.nothingmodes.engine.runtime.StableExecutionIdFactory
import com.tdvorak.nothingmodes.engine.runtime.TriggerEnvelope
import com.tdvorak.nothingmodes.engine.runtime.TriggerEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Geofence deactivation policies: ENTER activates; the end is governed by
 * [GeofenceEndMode] — MANUAL ignores the inverse edge, ON_EXIT ends on it,
 * GATED defers until the position gate holds.
 */
class GeofenceLifecycleTest {
    private fun geofenceMode(
        endMode: GeofenceEndMode,
        gate: GateDirection = GateDirection.OUTSIDE,
    ) = Automation(
        id = AutomationId("mode-gym"),
        name = "Gym",
        type = AutomationType.MODE,
        createdBy = CreatedBy.USER,
        status = AutomationStatus.ARMED,
        trigger =
            Trigger.Geofence(
                lat = 50.0,
                lng = 14.4,
                radiusM = 150.0,
                transition = Transition.ENTER,
                endMode = endMode,
                gate = gate,
                snoozeMinutes = 15,
            ),
        actions = listOf(Action.SetDnd(DndMode.PRIORITY)),
    )

    private fun edge(transition: Transition) =
        TriggerEnvelope(
            id = "geo-$transition",
            event =
                TriggerEvent.GeofenceTriggered(
                    eventId = "geo-$transition",
                    lat = 50.0,
                    lng = 14.4,
                    transition = transition,
                    geofenceId = "mode-gym",
                ),
            receivedAtMillis = 0L,
        )

    private fun recheck() =
        TriggerEnvelope(
            id = "geo-recheck",
            event =
                TriggerEvent.GeofenceRecheck(
                    eventId = "geo-recheck",
                    automationId = AutomationId("mode-gym"),
                ),
            receivedAtMillis = 0L,
        )

    private fun engine(
        store: InMemoryAutomationStore,
        audit: RecordingAuditSink,
        sink: RecordingActivationSink,
        active: Boolean = false,
        inside: Boolean? = null,
    ): Engine =
        Engine(
            store = store,
            executor = NoopActionExecutor,
            audit = audit,
            modeActivationSink = sink,
            modeActivationProvider = ModeActivationProvider {
                if (active) listOf("mode-gym") else emptyList()
            },
            geofenceGate = GeofenceGate { _, _, _ -> inside },
            executionIds = StableExecutionIdFactory,
        )

    @Test
    fun `ENTER edge activates an ON_EXIT geofence mode`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.ON_EXIT)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink).onTrigger(edge(Transition.ENTER))

            assertEquals(1, outcomes.size)
            assertEquals(AuditKind.MODE_ACTIVATED, audit.events.single().kind)
            assertEquals(listOf("mode-gym"), sink.activated)
        }

    @Test
    fun `EXIT edge ends an active ON_EXIT mode`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.ON_EXIT)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = true).onTrigger(edge(Transition.EXIT))

            assertEquals(1, outcomes.size)
            assertTrue(outcomes.single().isDeactivation)
            assertEquals(AuditKind.MODE_DEACTIVATED, audit.events.single().kind)
            assertEquals(listOf("mode-gym"), sink.deactivated)
        }

    @Test
    fun `EXIT edge does not end a MANUAL geofence mode`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.MANUAL)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = true).onTrigger(edge(Transition.EXIT))

            assertTrue(outcomes.isEmpty())
            assertTrue(audit.events.isEmpty())
            assertTrue(sink.deactivated.isEmpty())
        }

    @Test
    fun `GATED end defers while position is still inside`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.GATED)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = true, inside = true).onTrigger(edge(Transition.EXIT))

            assertEquals(1, outcomes.size)
            assertTrue(outcomes.single().endDeferred)
            assertFalse(outcomes.single().isDeactivation)
            assertEquals(AuditKind.END_DEFERRED, audit.events.single().kind)
            assertTrue(sink.deactivated.isEmpty())
        }

    @Test
    fun `GATED end proceeds once position is outside`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.GATED)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = true, inside = false).onTrigger(edge(Transition.EXIT))

            assertEquals(1, outcomes.size)
            assertTrue(outcomes.single().isDeactivation)
            assertEquals(AuditKind.MODE_DEACTIVATED, audit.events.single().kind)
        }

    @Test
    fun `GATED end defers when position is unknown`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.GATED)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = true, inside = null).onTrigger(edge(Transition.EXIT))

            assertEquals(1, outcomes.size)
            assertTrue(outcomes.single().endDeferred)
            assertTrue(sink.deactivated.isEmpty())
        }

    @Test
    fun `snooze recheck ends the mode once the gate holds`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.GATED)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = true, inside = false).onTrigger(recheck())

            assertEquals(1, outcomes.size)
            assertTrue(outcomes.single().isDeactivation)
            assertEquals(listOf("mode-gym"), sink.deactivated)
        }

    @Test
    fun `snooze recheck defers again while the gate is unmet`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.GATED)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = true, inside = true).onTrigger(recheck())

            assertEquals(1, outcomes.size)
            assertTrue(outcomes.single().endDeferred)
            assertEquals(AuditKind.END_DEFERRED, audit.events.single().kind)
        }

    @Test
    fun `stale recheck does not end a mode re-armed as MANUAL`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.MANUAL)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = true, inside = false).onTrigger(recheck())

            assertTrue(outcomes.isEmpty())
            assertTrue(sink.deactivated.isEmpty())
        }

    @Test
    fun `recheck on an inactive mode is a no-op`() =
        runTest {
            val store = InMemoryAutomationStore().also { it.save(geofenceMode(GeofenceEndMode.GATED)) }
            val audit = RecordingAuditSink()
            val sink = RecordingActivationSink()
            val outcomes = engine(store, audit, sink, active = false, inside = false).onTrigger(recheck())

            assertTrue(outcomes.isEmpty())
            assertTrue(audit.events.isEmpty())
        }

    private class RecordingAuditSink : AuditSink {
        val events = mutableListOf<AuditEvent>()

        override suspend fun record(event: AuditEvent) {
            events.add(event)
        }
    }

    private class RecordingActivationSink : ModeActivationSink {
        val activated = mutableListOf<String>()
        val deactivated = mutableListOf<String>()

        override suspend fun activate(
            automationId: AutomationId,
            atMillis: Long,
        ) {
            activated.add(automationId.value)
        }

        override suspend fun deactivate(
            automationId: AutomationId,
            atMillis: Long,
        ) {
            deactivated.add(automationId.value)
        }
    }
}
