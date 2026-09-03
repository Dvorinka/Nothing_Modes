# Argus Integration

## Overview

Nothing Modes derives its architecture from [Argus](https://github.com/JackRushante/argus) (GPL-3.0), an LLM-compiled, deterministically-executed Android automation engine. We reuse the engine architecture, adapt the Android runtime, and replace the UI entirely with a Nothing OS design system.

## License

Argus is GPL-3.0. Nothing Modes is also GPL-3.0. This is compatible — derivatives of GPL-3.0 must stay GPL-3.0.

## Argus Module Map

### engine-core (REUSE — port directly)

**Package:** `dev.argus.engine` → rename to `com.tdvorak.nothingmodes.engine`

**What it is:** Pure JVM Kotlin, zero Android dependencies. Domain models, runtime, validation, scheduling, fingerprinting.

**Key files to port:**

| File | Lines | Role | Reuse? |
|---|---|---|---|
| `model/Action.kt` | 428 | Sealed interface with 25+ action types (SetWifi, SetBluetooth, SetDnd, SetVolume, SetDarkMode, LaunchApp, OpenUrl, etc.) | **Adapt** — keep core actions, add Nothing-specific (SetExtraDim, SetBrightness, SetGlyph, SetGlyphMatrix, SetScreenTimeout). Remove WhatsApp-specific (WhatsAppReply, InvokeLlm generative lane initially). |
| `model/Trigger.kt` | 112 | Sealed interface: Geofence, Time, Immediate, Notification, PhoneState, Connectivity, Sensor | **Reuse** — all triggers map to our MVP trigger list. Add TimeTrigger variants (weekdays, weekends, sunrise/sunset). |
| `model/Condition.kt` | 92 | Sealed interface: TimeWindow, StateEquals, StateCompare, AppInForeground, LocationIn, And, Or, Not, BooleanLiteral, VarCompare | **Reuse** — add BatteryCondition, ChargingCondition, WifiCondition, BluetoothCondition, ScreenStateCondition, CurrentModeCondition. |
| `model/Automation.kt` | 118 | Automation + AutomationDraft data classes, AutomationSchema, schema versioning | **Adapt** — add `type: Mode/Routine` field, add `schedule` for mode time windows (start/end), add state snapshot reference. |
| `model/ApprovalFingerprint.kt` | — | SHA-256 fingerprinting of executable data | **Reuse** — needed for import validation and AI draft approval. |
| `model/CapabilityRequirements.kt` | 196 | Derives required capabilities from trigger/actions/conditions | **Adapt** — extend with Nothing Glyph/Glyph Matrix capability requirements. |
| `model/StateQuery.kt` | 175 | Typed state query system (Builtin, Setting, SystemProperty, Sysfs, DumpsysField) | **Reuse** — needed for condition evaluation and state reading. |
| `model/Variables.kt` | 167 | P4 variable binding system (Literal, RandomInt, State, TriggerPayload) | **Defer** — P4 features (variables, control flow) are not MVP. Port later. |
| `runtime/Engine.kt` | 495 | Core engine: onTrigger → match → fire policy → conditions → execute → audit/journal | **Reuse** — the main orchestration loop. Adapt for Mode activation/deactivation lifecycle. |
| `runtime/ProgramInterpreter.kt` | 512 | P4 deterministic interpreter (if/while/capture) | **Defer** — P4 control flow not MVP. Port basic flat execution first. |
| `runtime/ConditionEvaluator.kt` | 228 | Evaluates Condition tree against DeviceState | **Reuse** — extend with new condition types. |
| `runtime/TriggerMatcher.kt` | — | Matches Trigger against TriggerEvent | **Reuse** — extend with new trigger types. |
| `runtime/CronSchedule.kt` | 132 | Cron expression parsing, DST handling | **Reuse** — needed for recurring time triggers. |
| `runtime/DeviceState.kt` | 135 | State container for condition evaluation | **Reuse** — extend with Nothing-specific state (glyph status, etc.). |
| `runtime/ExecutionJournal.kt` | 171 | Execution journaling interface | **Reuse** — needed for execution log. |
| `runtime/FirePolicy.kt` | 193 | Fire policy (cooldown, duplicate suppression) | **Reuse** — needed for conflict handling. |
| `safety/DraftValidator.kt` | 1264 | Validates automation drafts (closed vocabulary, bounds, security) | **Adapt** — keep security invariants, remove LLM-specific validation initially. Add Nothing action validation. |
| `safety/Approval.kt` | 191 | Approval lifecycle (PENDING → ARMED) | **Reuse** — needed for AI draft approval and import validation. |
| `safety/ConflictDetector.kt` | — | Detects conflicts between automations | **Reuse** — needed for conflict handling phase. |
| `brain/Brain.kt` | — | LLM brain interface | **Defer** — AI is Phase 14, optional. |

**Test files to port:** All engine-core tests (30+ test files). These are JVM-only, fast, and critical for correctness.

### automation-android (REUSE — adapt heavily)

**Package:** `dev.argus.automation` → rename to `com.tdvorak.nothingmodes.automation`

**Key components:**

| Component | Role | Reuse? |
|---|---|---|
| `AndroidTimeAlarmBackend.kt` | AlarmManager scheduling | **Reuse** — core scheduling mechanism. |
| `TimeAlarmCoordinator.kt` | Coordinates time trigger scheduling | **Reuse** — adapt for Mode schedule (start/end times). |
| `TimeAlarmReceivers.kt` | BroadcastReceiver for alarm firing | **Reuse**. |
| `connectivity/ConnectivityBroadcastReceivers.kt` | WiFi/BT/Power receivers | **Reuse** — connectivity triggers. |
| `connectivity/ConnectivityEventIngress.kt` | Connectivity event → engine | **Reuse**. |
| `phone/PhoneBroadcastReceivers.kt` | Call/SMS receivers | **Reuse** — phone triggers. |
| `notification/ArgusNotificationListenerService.kt` | Notification listener | **Reuse** — notification triggers. |
| `geofence/AndroidGeofenceBackend.kt` | Geofence via LocationManager | **Defer** — location is later. |
| `sensor/SensorTriggerRuntime.kt` | Sensor triggers | **Defer** — not MVP. |
| `base/AndroidBaseActionExecutor.kt` | Base action executor (WiFi, BT, volume, etc.) | **Adapt** — add Nothing-specific action execution. |
| `ShizukuActionExecutor.kt` | Shizuku-backed action execution | **Reuse** — for privileged actions. |
| `vm/*.kt` | ViewModels for all screens | **Replace** — new ViewModels for Nothing UI. |
| `ArgusRuntimeController.kt` | Runtime lifecycle controller | **Reuse** — adapt for Mode lifecycle. |
| `AndroidCapabilityProbe.kt` | Runtime capability detection | **Reuse** — extend with Nothing capabilities. |

### core-shizuku (REUSE — port directly)

**Package:** `dev.argus.shizuku` → rename to `com.tdvorak.nothingmodes.shizuku`

| Component | Role | Reuse? |
|---|---|---|
| `ShizukuGateway.kt` | Shizuku connection lifecycle (NOT_INSTALLED → AUTHORIZED) | **Reuse** — excellent state machine. |
| `PrivilegedShell.kt` | Shell execution interface | **Reuse**. |
| `ShizukuPrivilegedShell.kt` | Shizuku-backed shell | **Reuse**. |
| `PrivilegedShellUserService.kt` | UserService for Shizuku | **Reuse**. |

### device-tools (REUSE — adapt)

**Package:** `dev.argus.device` → rename to `com.tdvorak.nothingmodes.device`

| Component | Role | Reuse? |
|---|---|---|
| `DeviceTools.kt` | Typed device tools (toggles, screen, settings) | **Adapt** — add Nothing-specific tools. |
| `StateReader.kt` | State reading interface | **Reuse**. |
| `ParametricStateReader.kt` | Parametric state reader (settings, properties) | **Reuse**. |

### data (REUSE — adapt schema)

**Package:** `dev.argus.data` → rename to `com.tdvorak.nothingmodes.data`

| Component | Role | Reuse? |
|---|---|---|
| `ArgusDatabase.kt` | Room database, migrations | **Adapt** — new schema for Nothing Modes (add Mode/Routine type, state snapshots, remove WhatsApp-specific tables). Start at version 1. |
| `entities/AutomationEntity.kt` | Automation row (flat columns + JSON blob) | **Adapt** — add `type` column (MODE/Routine), add `scheduleStart`/`scheduleEnd` for modes. |
| `entities/AuditEntity.kt` | Audit log | **Reuse**. |
| `entities/FireClaimEntity.kt` | Fire claim (dedup, cooldown) | **Reuse**. |
| `entities/ActionResultEntity.kt` | Action result journal | **Reuse**. |
| `entities/PendingDraftEntity.kt` | Pending draft (AI/import) | **Reuse**. |
| `entities/ScheduledTimeAlarmEntity.kt` | Scheduled alarm tracking | **Reuse**. |
| `dao/*.kt` | All DAOs | **Adapt** — add Mode-specific queries. |
| `Converters.kt` | Room type converters | **Adapt** — for new types. |
| `RoomAutomationStore.kt` | Automation store implementation | **Reuse** — adapt for new entity. |
| `RoomAuditSink.kt` | Audit sink implementation | **Reuse**. |
| `RoomExecutionJournal.kt` | Execution journal implementation | **Reuse**. |

**Tables to REMOVE (not needed for Nothing Modes):**
- `whitelisted_contacts` (WhatsApp-specific)
- `observed_conversations` (WhatsApp-specific)
- `deferred_replies` (WhatsApp-specific)
- `usage_events` (LLM usage tracking — defer to AI phase)

**Tables to ADD:**
- `state_snapshots` — for state restoration (setting, previousValue, automationId, timestamp)
- `mode_activations` — for mode lifecycle tracking (modeId, activatedAt, deactivatedAt, status)

### brain-android (DEFER — Phase 14)

**Package:** `dev.argus.brain`

LLM transport layer. Not needed for MVP. Port in Phase 14 (AI rule generation).

### ui (REPLACE entirely)

**Package:** `dev.argus.ui`

Argus UI is Material 3, Italian language, 6 stateless screens. We replace it completely with a Nothing OS design system.

**What we keep from Argus UI:**
- `presentation/RuleRenderMapper.kt` — rendering rules from types (not LLM paraphrase). **Reuse concept, rewrite implementation.**
- `model/UiContracts.kt` — UI state contracts. **Reuse concept, rewrite.**
- `preview/Fixtures.kt` — Compose preview fixtures. **Reuse concept, rewrite.**

**What we replace:**
- All screens (Chat, AutomationList, AutomationDetail, ExecutionLog, Onboarding, Settings)
- All components (Badges, Banners, RuleCard, dialogs)
- All theme (Color, Theme, Type, SemanticColors)
- All presentation (BudgetFormat, RenderLanguage)

### app (REUSE — adapt)

**Package:** `dev.argus` → rename to `com.tdvorak.nothingmodes`

| Component | Role | Reuse? |
|---|---|---|
| `ArgusApplication.kt` | Hilt application | **Adapt** — rename, adjust module imports. |
| `MainActivity.kt` | Single activity | **Adapt** — new Compose navigation. |
| `nav/ArgusNavHost.kt` | Navigation graph | **Replace** — new screens. |
| `di/ArgusModule.kt` (in automation-android) | Hilt DI module | **Adapt** — add Nothing providers, capability controllers. |

## Build System

### Version Catalog (libs.versions.toml)

Argus uses:
- AGP 8.13.2, Kotlin 2.1.0
- Compose BOM 2025.05.01
- Room 2.6.1, KSP 2.1.0-1.0.29
- Shizuku 13.1.5
- Hilt 2.57.1
- Kotlin Serialization 1.7.3
- Kotlin Coroutines 1.9.0
- JUnit5 5.11.3, Robolectric 4.14.1

**Nothing Modes adjustments:**
- Same versions (proven to work)
- Add: Nothing Glyph Matrix SDK AAR (local libs/)
- Add: DataStore preferences
- Add: Glance (for widget) — later phase
- Add: WorkManager (for deferrable background work)

### Gradle Properties

Argus: `-Xmx3g` for Gradle daemon. We'll use the same but route builds to Proxmox (14 GiB RAM).

### Module Structure

Argus: 8 modules (engine-core, automation-android, brain-android, data, ui, device-tools, core-shizuku, app)

Nothing Modes: 12 modules (same 8 + capabilities, nothing-integrations, import-export, widget, quicksettings)

## Deviations from Upstream

| Deviation | Reason |
|---|---|
| Add `capabilities` module | Argus has device-tools but no formal capability layer. Nothing Modes needs capability detection for graceful degradation across Nothing devices. |
| Add `nothing-integrations` module | Argus has no Nothing-specific code. All Glyph/Glyph Matrix SDK calls must be isolated. |
| Add `import-export` module | Argus has no import/export. Nothing Modes needs portable JSON format. |
| Add `widget` + `quicksettings` modules | Argus has no widgets or QS tiles. |
| Replace `ui` module entirely | Nothing OS design language, not Material 3. |
| Add Mode/Routine distinction | Argus has only "automations". Nothing Modes has Modes (persistent state) and Routines (event-based). |
| Add state snapshot system | Argus has no state restoration. Nothing Modes needs it for Mode deactivation. |
| Remove WhatsApp-specific features | Not relevant to Nothing Modes. |
| Defer P4 features (variables, control flow) | Not MVP. Port later if needed. |
| Defer brain-android (LLM) | Optional, Phase 14. |
| Defer geofence/sensor triggers | Not MVP. Port later. |

## Upstream Compatibility Strategy

1. **Engine types stay compatible:** Trigger, Condition, Action sealed interfaces keep the same `@SerialName` wire names. This means future Argus features can be ported without breaking existing automations.
2. **New Nothing-specific actions use new serial names:** `set_extra_dim`, `set_brightness`, `set_glyph`, `set_glyph_matrix`, `set_screen_timeout`. These don't conflict with Argus names.
3. **Schema versioning:** Start at version 1 for Nothing Modes. If we later want to import Argus automations, we add a migration path.
4. **Test architecture:** Keep the same TDD approach for engine-core. JVM tests, fast, comprehensive.

## Porting Order

1. Port engine-core (model + runtime + safety) — get tests passing
2. Port core-shizuku — get gateway working
3. Port data — get Room schema working (new, version 1)
4. Port automation-android — get scheduling working
5. Port device-tools — get state reading working
6. Add capabilities module — new
7. Add nothing-integrations module — new
8. Replace ui module — new Nothing design system
9. Wire app module — Hilt, navigation
10. Add import-export — new
11. Add widget + quicksettings — new (later phases)
12. Port brain-android — Phase 14 (optional AI)
