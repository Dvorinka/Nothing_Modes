# Nothing Modes — Architecture Decisions

## ADR-001: Multi-module Gradle architecture
**Date:** 2026-09-03
**Status:** Accepted
**Decision:** 9 Gradle modules — engine-core (pure Kotlin), data (Room), capabilities (controllers), core-shizuku, device-tools, nothing-integrations, automation-android, ui, app.
**Rationale:** Separation of concerns. engine-core is testable without Android. Nothing SDK isolated in nothing-integrations. Shizuku isolated in core-shizuku.

## ADR-002: kotlinx.serialization for automation schema
**Date:** 2026-09-03
**Status:** Accepted
**Decision:** All model types use @Serializable with stable @SerialName discriminators. Schema versioned (v1).
**Rationale:** Stable wire format for persistence and import/export. No reflection-based JSON.

## ADR-003: Hilt for DI
**Date:** 2026-09-03
**Status:** Accepted
**Decision:** Hilt across all Android modules. engine-core remains DI-free (constructor injection).
**Rationale:** Standard Android DI. engine-core stays pure Kotlin for testing.

## ADR-004: Room for persistence
**Date:** 2026-09-03
**Status:** Accepted
**Decision:** 8 entities, 8 DAOs, schema export enabled.
**Rationale:** Type-safe persistence, migration support, observable queries.

## ADR-005: Shizuku as optional capability provider
**Date:** 2026-09-03
**Status:** Accepted
**Decision:** Shizuku provides privileged shell access for settings that require it (WiFi, BT, mobile data, dark mode, extra dim, write setting). App degrades gracefully without Shizuku.
**Rationale:** Many system settings cannot be modified via public APIs on modern Android. Shizuku provides a safe, user-authorized path without root.

## ADR-006: Capability-based device support
**Date:** 2026-09-03
**Status:** Accepted
**Decision:** CapabilityResolver checks 51 capability IDs against DeviceCapabilities. Actions report Unsupported/PermissionRequired/ShizukuRequired when capabilities are missing.
**Rationale:** Nothing devices vary in Glyph hardware. Android versions vary in API access. Capability system provides deterministic behavior.

## ADR-007: Proxmox as build worker
**Date:** 2026-09-03
**Status:** Accepted
**Decision:** Heavy Gradle builds run on Proxmox (19GB RAM, 8 cores). Git remains source of truth.
**Rationale:** Local machine has limited RAM. Gradle Android builds are RAM-intensive. Proxmox provides headroom.

## ADR-008: Glyph SDK isolation
**Date:** 2026-09-03
**Status:** Accepted
**Decision:** All Nothing Glyph SDK calls isolated in nothing-integrations module. Other modules interact via provider interfaces.
**Rationale:** SDK is vendor-specific. Isolation allows mocking in tests and clean degradation on non-Nothing devices.
