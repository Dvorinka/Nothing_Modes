# Nothing Modes — Architecture

## Overview

Nothing Modes is an Android automation app built around a single user-facing concept: **Modes**. A mode is a collection of triggers and actions. When a trigger matches, the mode's actions run. The app is modular, with each module owning a single layer of the stack.

## Modules

| Module | Responsibility |
|--------|----------------|
| `app` | Application shell, `MainActivity`, navigation, Hilt setup, flavor manifests. |
| `ui` | Compose screens, components, theme, ViewModels, navigation graph. |
| `automation-android` | Foreground service, broadcast receivers, notification listener, scheduler, usage stats monitor, media session monitor. |
| `engine-core` | Domain models (`Trigger`, `Action`, `Condition`), serialization, `TriggerMatcher`, `ConditionEvaluator`, runtime state (`ActiveMedia`). |
| `capabilities` | Controller interfaces and `RealActionExecutor`; maps engine actions to Android APIs (wallpaper, DND, brightness, Glyph, etc.). |
| `data` | Room entities/DAOs, `NothingModesDatabase`, `AutomationStore`, data models. |
| `device-tools` | System settings, usage stats, state readers, battery/location helpers. |
| `core-shizuku` | Shizuku binder, privileged shell bridge. |
| `nothing-integrations` | Nothing Ketchum SDK integration, Glyph presets, matrix rendering. |

## Data flow

1. A system event (time, notification, connectivity, media session, etc.) is received by a monitor or broadcast receiver in `automation-android`.
2. The receiver posts an intent to `AutomationService`.
3. `AutomationService` builds a `TriggerEvent` and publishes it.
4. `Engine` matches the event against stored `Trigger`s.
5. For a match, `RealActionExecutor` in `capabilities` runs each `Action` using the appropriate controller.
6. Controllers delegate to Android APIs (`WallpaperManager`, `AudioManager`, `Settings.System`, Glyph SDK, Shizuku `cmd` calls).

## Key abstractions

- **Action / Trigger / Condition**: sealed data classes in `engine-core`. They are serializable and persisted as JSON by `data`.
- **CapabilityIds**: string tokens describing what a trigger or action needs from the device. `CapabilityResolver` checks them against `DeviceCapabilities`.
- **Controllers**: thin interfaces (`WallpaperController`, `VolumeController`, etc.) with Android implementations in `capabilities` and fakes in unit tests.
- **AutomationStore**: the source of truth for saved modes and execution logs.

## Threading

- Heavy work (database, shell, `UsageStatsManager`) runs on `Dispatchers.IO`.
- Compose UI observes `StateFlow` / Room `Flow` on the main thread.
- `AutomationService` is a foreground service that keeps event matching alive.
