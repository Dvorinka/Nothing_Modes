# Nothing Modes — Progress

## Current Phase
Play Store release track. App is feature-complete and shipping; work is
polish, store compliance, and crash-driven fixes.

## Current Version
0.19.1 (versionCode 26) — pending release.

## Recently Shipped
- 0.19.0: tap-to-cycle Quick Settings tiles (screen timeout, brightness,
  ultra dim, volume, Glyph lights, scene presets, mode runner), six custom
  tiles, home-screen widgets
- 0.18.1: Play in-app updates (play flavor), community library one-tap
  imports
- 0.18.0: ultra-dim overlay via accessibility service, live notification
  control, capability warning layer, priority conflict resolution

## Active Problems
- Fixed in 0.19.1: Play startup crash (PlatformInAppUpdate constructed
  before activity attach — NPE on cold start, 0.18.1+)

## Infrastructure
- CI: `./gradlew test assembleDebug lint` on every PR
- Release: tag `v*` → signed GitHub APK + Play AAB → alpha track upload
  with `fastlane/.../changelogs/<versionCode>.txt` as release notes
- Crash pipeline: opt-in reporting + post-crash prompt →
  `POST nothing-modes.vercel.app/api/crash` → Neon `crash_reports` →
  admin page + email notification

## Build Notes
- JDK 17 required (JDK 27 breaks the Kotlin compiler — set
  `JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` locally)
- Local release builds fall back to debug signing when
  `NOTHING_MODES_KEYSTORE` env vars are unset; CI signs via secrets
