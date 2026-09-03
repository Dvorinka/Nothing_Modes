# Nothing Modes — Developer Guide

## Module Architecture

```
app
├── engine-core          Pure Kotlin engine (triggers, conditions, actions, fire policy)
├── data                 Room database, DAOs, entities
├── capabilities         Android API controllers (brightness, volume, DND, etc.)
├── core-shizuku         Shizuku integration for privileged operations
├── nothing-integrations Nothing Glyph SDK wrapper (light stripe + matrix)
├── automation-android   Automation lifecycle (service, receivers, scheduler)
├── device-tools         Device detection utilities
├── ui                   Compose screens, theme, components
└── app                  Application module, DI graph, navigation
```

## Engine Flow

```
Trigger fires (alarm / receiver / listener)
    ↓
AutomationService.onStartCommand
    ↓
Handler builds TriggerEvent
    ↓
dispatchEvent wraps in TriggerEnvelope
    ↓
Engine.onTrigger(envelope)
    ↓
1. Select candidates (by ID or all armed)
2. TriggerMatcher.matches(trigger, event)
3. FirePolicy.evaluate (cooldown check)
4. ConditionEvaluator.result (if conditions present, reads DeviceState)
5. Snapshot settings (ModeWindowStart) / Restore (ModeWindowEnd)
6. ActionExecutor.execute for each action
7. Journal + Audit + ModeActivation
```

## Key Types

### Trigger (sealed interface)

12 trigger types. Each has a matching `TriggerEvent` subtype dispatched by the Android layer.

### Action (sealed interface)

40+ action types. Each is executed by `RealActionExecutor` which maps to Android APIs or Shizuku shell commands.

### Condition (sealed interface)

12 condition types including `And`/`Or`/`Not` composites. Evaluated against `DeviceState` read by `AndroidStateProvider`.

### Automation

```kotlin
data class Automation(
    val id: AutomationId,
    val name: String,
    val trigger: Trigger,
    val conditions: Condition?,
    val actions: List<Action>,
    val priority: Int,
    val type: AutomationType,     // MODE or ROUTINE
    val status: AutomationStatus,  // DRAFT, ARMED, PENDING_APPROVAL
    val enabled: Boolean,
    val cooldownMs: Long,
)
```

## Dependency Injection

Hilt is used throughout. The DI graph is defined in:

- `app/.../di/AppModule.kt` — Engine, store, executor
- `automation-android/.../di/AutomationModule.kt` — Scheduler, service
- `data/.../di/DataModule.kt` — Database, DAOs
- `capabilities/.../di/CapabilitiesModule.kt` — Controllers
- `core-shizuku/.../di/ShizukuModule.kt` — Shizuku gateway

## Serialization

All model types use `kotlinx.serialization` with `@SerialName` discriminators for stable JSON schema. The schema version is tracked in `ImportExportService`.

## Testing

### Engine Unit Tests (JVM, no device)

```bash
./gradlew :engine-core:test
```

37 tests covering:
- Trigger matching (all 12 trigger types)
- Condition evaluation (all 12 condition types)
- Fire policy (cooldown suppression)
- Conflict detection (overlapping settings)
- State restoration (snapshot/restore on mode window)
- Import/export (JSON serialization round-trip)

### Instrumented Tests (require device or emulator)

```bash
./gradlew connectedCheck
```

### Glyph SDK Tests

Require a Nothing Phone device. The Glyph SDK AAR is extracted into `nothing-integrations`.

## Building

### Debug

```bash
./gradlew assembleDebug
```

### Release (signed)

Set environment variables:
```bash
export NOTHING_MODES_KEYSTORE=/path/to/keystore.jks
export NOTHING_MODES_KEYSTORE_PASSWORD=...
export NOTHING_MODES_KEY_ALIAS=...
export NOTHING_MODES_KEY_PASSWORD=...
./gradlew assembleRelease
```

## CI

GitHub Actions workflow on self-hosted Android runner:
- `ci.yml` — Build + test on every push
- `release.yml` — Build signed APK on tag push, create GitHub Release

## Nothing Glyph SDK

The SDK is distributed as an AAR inside `nothing-integrations/libs/`. It provides:

- `GlyphManager` — Light stripe control (Phone 1/2/2a/3a/4a/4b)
- `GlyphMatrixManager` — Matrix control (Phone 3/4a Pro)
- `GlyphToy` — Toy service interface for Glyph Toys manager

Device detection uses `Build.MODEL` prefix matching. See `NothingDeviceDetector` for the full mapping.

## Shizuku Integration

Shizuku provides privileged shell access without root. The app uses it for:

- WiFi toggle (`svc wifi enable/disable`)
- Bluetooth toggle (`svc bluetooth enable/disable`)
- Mobile data toggle (`svc data enable/disable`)
- NFC toggle (`svc nfc enable/disable`)
- Settings writes (`settings put global/secure/system ...`)

The `PrivilegedShell` interface abstracts the transport. `ShizukuPrivilegedShell` implements it via Shizuku's UserService mechanism.
