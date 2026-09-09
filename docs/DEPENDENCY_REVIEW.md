# Nothing Modes — Dependency Review

Review date: 2025-09-09. Versions are pinned in `gradle/libs.versions.toml`.

## Core platform

| Dependency | Version | Purpose | Notes |
|------------|---------|---------|-------|
| Android Gradle Plugin | 8.13.2 | Build | Current stable. |
| Kotlin | 2.1.0 | Language, Compose compiler | Latest stable. |
| KSP | 2.1.0-1.0.29 | Annotation processing | Matches Kotlin version. |

## AndroidX / Compose

| Dependency | Version | Purpose | Notes |
|------------|---------|---------|-------|
| core-ktx | 1.15.0 | Core Kotlin extensions | Standard. |
| lifecycle | 2.9.1 | ViewModel, Service lifecycle | Standard. |
| activity-compose | 1.9.3 | Activity result contracts, Compose entry | Used for gallery/photo picker. |
| navigation-compose | 2.8.5 | Compose navigation | Type-safe args via serialization. |
| compose-bom | 2025.05.01 | Compose Bill of Materials | Recent stable. |
| material3 | 1.3.1 | Compose Material 3 | Standard. |
| room | 2.6.1 | Local database | Includes `room-ktx` and `room-testing`. |
| datastore-preferences | 1.1.1 | Typed preferences | Used for settings and flags. |
| work-runtime-ktx | 2.10.0 | Background work | Standard. |
| glance-appwidget | 1.1.1 | Widgets | Standard. |

## Kotlin libraries

| Dependency | Version | Purpose | Notes |
|------------|---------|---------|-------|
| kotlinx-serialization-json | 1.7.3 | JSON persistence | Used for `Action`/`Trigger` JSON. |
| kotlinx-coroutines | 1.9.0 | Coroutines / Flow | Includes test support. |

## Third party

| Dependency | Version | Purpose | Notes |
|------------|---------|---------|-------|
| Hilt / Dagger | 2.57.1 | Dependency injection | Standard Android DI. |
| androidx.hilt | 1.3.0 / 1.2.0 | Hilt ViewModel / WorkManager | Stable. |
| Shizuku | 13.1.5 | Privileged operations | Latest stable; used for `cmd` settings. |
| libphonenumber | 9.0.37 | Phone metadata / country picker | Google library; used for dial codes and E.164. |
| re2j | 1.8 | Safe regex | Used instead of JDK regex in untrusted paths. |
| osmdroid | 6.1.20 | Map display | Stable. |
| play-services-location | 21.3.0 | Geofencing, Fused location | Standard. |
| reorderable | 3.1.0 | Draggable lists | Compose reordering. |

## Test

| Dependency | Version | Purpose | Notes |
|------------|---------|---------|-------|
| junit5 | 5.11.3 | Unit tests (engine-core) | Stable. |
| junit4 | 4.13.2 | Unit tests (Robolectric) | Required by Android test harness. |
| robolectric | 4.14.1 | JVM Android fakes | Used for `RealActionExecutor` tests. |
| androidx-test-core / ext-junit / runner | 1.6.1 / 1.2.1 / 1.6.2 | Instrumentation helpers | Standard. |

## Security / supply-chain notes

- All versions are pinned; no floating `latest` or wildcard ranges.
- No known security advisories for the pinned versions at the time of review.
- `libphonenumber` is the only library that handles international numbering metadata. It does not store or transmit user phone numbers; all parsing/formatting is local.
- `shizuku` is sideloaded via the user’s Shizuku manager; the app does not bundle or distribute it.
- Network permission (`INTERNET`) is declared, but no analytics or cloud SDKs are present. The only network use is the update/manual URL handlers and user-initiated imports.
- No code obfuscation beyond R8/ProGuard is applied in release builds.

## Upgrade watchlist

- `shizuku`: check for Nothing OS API changes if SDK behavior shifts.
- `libphonenumber`: update when new country codes are needed.
- `play-services-location`: may be replaced with a non-GMS alternative for the F-Droid flavor in a future release.
