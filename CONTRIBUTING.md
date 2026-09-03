# Contributing

## Development Setup

1. Clone the repository
2. Install Android Studio (Hedgehog or newer) with Android SDK 36
3. Open the project — Gradle will sync automatically
4. Run on a Nothing Phone device or emulator (API 28+)

## Architecture

The project is split into 9 Gradle modules:

- `engine-core` — Pure Kotlin automation engine (triggers, conditions, actions, fire policy)
- `data` — Room database, DAOs, entity definitions
- `capabilities` — Android API controllers (brightness, volume, DND, flashlight, etc.)
- `core-shizuku` — Shizuku integration for privileged operations
- `nothing-integrations` — Nothing Glyph SDK wrapper (light stripe + matrix)
- `automation-android` — Automation lifecycle (service, receivers, scheduler)
- `device-tools` — Device detection utilities
- `ui` — Compose screens, theme, components
- `app` — Application module, DI graph, navigation

## Build

```bash
./gradlew assembleDebug
```

## Test

```bash
./gradlew :engine-core:test
```

Engine tests are pure JVM — no device required. Instrumented tests require a device or emulator.

## Code Style

- Kotlin, no semicolons
- 4-space indentation
- `data class` for models, `sealed interface` for discriminated unions
- kotlinx.serialization for JSON
- Hilt for DI
- Compose for UI (no XML layouts)

## Commit Messages

Use conventional commits:

```
feat: add new trigger type
fix: resolve cooldown calculation
docs: update README
refactor: extract glyph preset logic
test: add conflict resolution tests
```

Include the authorship trailer:

```
Authored By: TDvorak <info@tdvorak.dev>
```

## Pull Requests

1. Create a feature branch from `main`
2. Write tests for new engine logic
3. Ensure `./gradlew :engine-core:test` passes
4. Open a PR with a clear description

## Adding a New Action Type

1. Add the action class to `engine-core/.../model/Action.kt`
2. Add the type ID to `ActionTypeIds`
3. Add to `affectedSettings` if it modifies system state
4. Add to `supportsRestore` if it should be snapshot/restored
5. Add to `CapabilityRequirements.actionCapabilities`
6. Implement the action in `RealActionExecutor`
7. Add to `actionDescription` in `Descriptions.kt`
8. Add to the action picker in `CustomAutomationBuilderScreen`

## Adding a New Trigger Type

1. Add the trigger class to `engine-core/.../model/Trigger.kt`
2. Add matching logic to `TriggerMatcher`
3. Add the trigger event to `TriggerEvent`
4. Add dispatch logic to `AutomationService`
5. Create a BroadcastReceiver or monitor in `automation-android`
6. Register in `AndroidManifest.xml`
7. Add scheduling logic to `AutomationScheduler` if needed
