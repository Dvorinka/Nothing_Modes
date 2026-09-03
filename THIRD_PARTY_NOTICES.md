# Third-Party Notices

## Project License

Nothing Modes is licensed under **GPL-3.0**.

## Derived Works

### Argus
- **Repository:** https://github.com/JackRushante/argus
- **License:** GPL-3.0
- **Usage:** Architecture derivation. Engine-core, automation-android, core-shizuku, data, device-tools modules ported and adapted. UI module replaced entirely.
- **Compatibility:** GPL-3.0 derivative — Nothing Modes must also be GPL-3.0.

### Easer
- **Repository:** https://github.com/renyuneyun/Easer
- **License:** GPL-3.0
- **Usage:** Secondary reference for automation concepts. No source code copied directly. EaserImporter (Phase 15) performs best-effort format conversion only.

### Shizuku
- **Repository:** https://github.com/RikkaApps/Shizuku
- **License:** Apache-2.0
- **Usage:** Used as dependency (`dev.rikka.shizuku:api:13.1.5`, `dev.rikka.shizuku:provider:13.1.5`). ShizukuGateway ported from Argus (which itself wraps the Shizuku API).

## SDK Dependencies

### Nothing Glyph / Glyph Matrix SDK
- **Repository:** https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
- **Distribution:** AAR file (`glyph-matrix-sdk-2.0.aar`)
- **License:** Nothing Developer Programme terms (see repository LICENSE.md)
- **Usage:** Used as local AAR dependency. Not redistributed. No proprietary assets extracted.
- **Package:** `com.nothing.ketchum`

### Android Jetpack Libraries
- **License:** Apache-2.0
- **Libraries:** Compose, Room, Navigation, Lifecycle, Activity, DataStore, WorkManager, Hilt

### Kotlin Serialization
- **License:** Apache-2.0
- **Library:** `org.jetbrains.kotlinx:kotlinx-serialization-json`

### Kotlin Coroutines
- **License:** Apache-2.0
- **Library:** `org.jetbrains.kotlinx:kotlinx-coroutines-core`

### RE2/J
- **License:** BSD-3-Clause
- **Library:** `com.google.re2j:re2j:1.8`
- **Usage:** Linear-time regex for user-controlled patterns on untrusted text

### JUnit 5
- **License:** Eclipse Public License 2.0
- **Library:** `org.junit.jupiter:junit-jupiter:5.11.3`

### Robolectric
- **License:** MIT
- **Library:** `org.robolectric:robolectric:4.14.1`

## Font

Nothing OS uses "Geist" typography. Nothing Modes does NOT extract proprietary font files from Nothing APKs. A legally distributable alternative is used (e.g., Space Grotesk for headings, JetBrains Mono for technical labels), with appropriate fallback.

## Icons

Nothing Modes uses custom icons inspired by Nothing's technical/hardware visual language. No proprietary Nothing icon assets are extracted or copied.
