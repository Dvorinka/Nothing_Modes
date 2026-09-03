# Nothing Modes

Open-source automation app for Nothing phones. Samsung Modes & Routines simplicity meets Argus-class automation engine, wrapped in Nothing OS design language.

## Status

In development. See [PLAN.md](PLAN.md) for the full execution plan.

## Features

- **Modes** — persistent state configurations (Sleep, Work, Gaming)
- **Routines** — event-based automations (trigger + conditions + actions)
- **Nothing Glyph / Glyph Matrix** integration
- **Shizuku** support for privileged operations (optional)
- **Capability-based** feature detection (no hardcoded device assumptions)
- **State restoration** — modes restore previous values on deactivation
- **Import/Export** — portable JSON automation format
- **AI rule generation** (optional, never executes — only drafts)
- **Nothing OS design language** — monochrome, geometric, technical

## Target Devices

| Device | Priority |
|---|---|
| Nothing Phone (3) | Primary reference |
| Phone (4a), (4a) Pro, (3a), (3a) Pro, (2a), (2a) Plus | Secondary |
| Phone (2), (1) | Legacy graceful degradation |

## Build

```bash
# Debug build
./gradlew assembleDebug

# Unit tests
./gradlew test

# Release build
./gradlew assembleRelease
```

Requires JDK 17+ and Android SDK with API 36 (Android 16).

## Architecture

```
Automation Engine (pure Kotlin, no Android deps)
       │
  Capability API
       │
  ┌────┼────┐
  │    │    │
Android Nothing Shizuku
 API    API   API
  │    │    │
  └────┼────┘
       │
  Nothing OS
```

The engine is NOT Nothing-specific. Capability layer resolves each action through: Public Android API → Nothing API → Shizuku → Unsupported.

## License

GPL-3.0 (derivative of [Argus](https://github.com/JackRushante/argus))

## Documentation

See [docs/](docs/) for architecture, compatibility, permissions, security, and development guides.

---

Authored By: TDvorak <info@tdvorak.dev>
