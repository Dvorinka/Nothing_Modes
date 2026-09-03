# Nothing Modes

Open-source automation app for Nothing phones. Samsung Modes & Routines simplicity meets Argus-class automation engine, wrapped in Nothing OS design language.

## Features

- **Modes** — persistent state configurations (Sleep, Work, Gaming) with automatic state restoration
- **Routines** — event-based automations (trigger + conditions + actions)
- **Custom Automation Builder** — WHEN/IF/THEN UI with all trigger, condition, and action types
- **Nothing Glyph / Glyph Matrix** integration (light stripe + matrix on supported devices)
- **Glyph Toy** service for Glyph Matrix toy integration
- **Shizuku** support for privileged operations (optional, graceful degradation)
- **Capability-based** feature detection (no hardcoded device assumptions)
- **State restoration** — modes snapshot and restore previous values on deactivation
- **Conflict management** — priority-based deterministic execution order
- **Import/Export** — portable JSON automation format with schema versioning
- **11 trigger types** — Time, TimeWindow, Notification, PhoneState, Connectivity, Boot, BatteryLevel, ScreenState, AppOpened, Geofence, Immediate
- **12 condition types** — TimeWindow, DayOfWeek, BatteryLevel, Charging, WiFi, Bluetooth, ScreenState, CurrentModeActive, AppInForeground, AND, OR, NOT
- **28 action types** — WiFi, Bluetooth, MobileData, DND, Ringer, Volume, Flashlight, DarkMode, Brightness, AutoBrightness, ExtraDim, ScreenTimeout, Glyph (7 types), Vibrate, CopyText, Wait, WriteSetting, LaunchApp, OpenUrl, OpenSettings, ShowNotification
- **Nothing OS design language** — monochrome, geometric, technical, red accent

## Target Devices

| Device | Glyph Stripe | Glyph Matrix | Glyph Toy |
|---|---|---|---|
| Nothing Phone (1) | Yes | No | No |
| Nothing Phone (2) | Yes | No | No |
| Nothing Phone (2a) / (2a)+ | Yes | No | No |
| Nothing Phone (3a) / (3a) Pro | Yes | No | No |
| Nothing Phone (3) | Yes | Yes (25x25) | Yes |
| Nothing Phone (4a) | Yes | No | No |
| Nothing Phone (4a) Pro | Yes | Yes (13x13) | Yes |
| Nothing Phone (4b) | Yes | No | No |
| Non-Nothing devices | No | No | No |

## Build

```bash
# Debug build
./gradlew assembleDebug

# Unit tests
./gradlew test

# Release build (R8 + resource shrinking)
./gradlew assembleRelease

# Lint
./gradlew lint
```

Requires JDK 17+ and Android SDK with API 36 (Android 16).

## Architecture

```
app (entry point, nav host, manifest)
├── ui (Compose screens, theme, design primitives)
├── automation-android (services, receivers, scheduler, DI)
│   ├── engine-core (pure Kotlin: triggers, conditions, actions, matcher, evaluator, fire policy)
│   ├── data (Room: 8 entities, 8 DAOs, stores)
│   ├── capabilities (action executors, state provider, capability resolver)
│   ├── core-shizuku (Shizuku gateway, privileged shell)
│   ├── device-tools (shell-based state readers)
│   └── nothing-integrations (Glyph SDK: stripe + matrix providers, channels, presets)
```

The engine is pure Kotlin with no Android dependencies. The capability layer resolves each action through: Public Android API → Nothing API → Shizuku → Unsupported.

## Modules

| Module | Lines | Role |
|---|---|---|
| `engine-core` | ~2,200 | Pure Kotlin automation engine |
| `device-tools` | ~1,700 | Shell-based device state readers |
| `automation-android` | ~1,500 | Android lifecycle, services, receivers |
| `ui` | ~1,300 | Jetpack Compose screens + theme |
| `core-shizuku` | ~1,200 | Shizuku privileged shell transport |
| `capabilities` | ~1,200 | Action executors, state provider, capability resolver |
| `nothing-integrations` | ~800 | Nothing Glyph SDK wrapper |
| `data` | ~600 | Room database |
| `app` | ~150 | Application entry, nav host |

## Setup

1. Install on a Nothing phone (or any Android 9+ device for non-Glyph features)
2. Follow the onboarding guide to grant permissions
3. Optionally install Shizuku for WiFi/Bluetooth/mobile data toggles
4. Create automations from templates or use the custom builder
5. Automations fire based on triggers, evaluate conditions, and execute actions

## Shizuku

Shizuku is an optional capability provider. Without it, the app still works for all actions that use public Android APIs. Shizuku enables:
- WiFi toggle
- Bluetooth toggle
- Mobile data toggle
- Dark mode toggle (on some Android versions)
- Extra dim toggle (on some Android versions)
- Write system settings

Install Shizuku from [GitHub](https://github.com/RikkaApps/Shizuku/releases) or Play Store.

## Testing

```bash
# Engine unit tests (pure Kotlin)
./gradlew :engine-core:test

# All unit tests
./gradlew test

# Instrumented tests (require device/emulator)
./gradlew connectedCheck
```

37 unit tests covering: engine execution, trigger matching, condition evaluation, conflict management, state restoration, cooldown suppression, import/export, serialization round-trips.

## Proxmox Build Worker

For resource-constrained local machines, a Proxmox worker can be used for heavy builds:

```bash
ssh proxmox
cd /opt/Nothing_Modes
git pull origin main
./gradlew assembleDebug test --no-daemon --parallel --build-cache
```

## License

GPL-3.0 (derivative of [Argus](https://github.com/JackRushante/argus))

## Documentation

- [Architecture](docs/compatibility.md) — device compatibility matrix
- [Nothing SDK](docs/nothing-sdk.md) — Glyph SDK reference
- [Shizuku](docs/shizuku.md) — Shizuku integration guide
- [TASKS.md](TASKS.md) — task checklist
- [DECISIONS.md](DECISIONS.md) — architecture decision records
- [CHANGELOG.md](CHANGELOG.md) — changelog

---

Authored By: TDvorak <info@tdvorak.dev>
