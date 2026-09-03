# Nothing Modes — Master TODO

## Legend
- [x] Done
- [ ] Not started

---

## Phase 1: Foundation (COMPLETE)

### Package & Build
- [x] Package name `com.tdvorak`
- [x] 9 Gradle modules + version catalog
- [x] Hilt DI wired across all modules
- [x] CI pipeline (self-hosted Android runner)

### Engine (engine-core — pure Kotlin)
- [x] 12 trigger types
- [x] 12 condition types + And/Or/Not composites
- [x] 37 action types (23 base + 9 system toggles + 5 glyph advanced)
- [x] TriggerMatcher, ConditionEvaluator, FirePolicy
- [x] CronSchedule parsing
- [x] kotlinx.serialization (schema v1)
- [x] State snapshot/restore on mode window
- [x] Conflict detection (affectedSettings)
- [x] 37 unit tests (all passing)

### Data Layer (Room)
- [x] 8 entities, 8 DAOs
- [x] RoomAutomationStore, RoomAuditSink, RoomExecutionJournal
- [x] RoomStateSnapshotStore, RoomModeActivationProvider/Sink
- [x] Schema export enabled

### Android Controllers (capabilities)
- [x] 16 base controllers (brightness, DND, volume, flashlight, etc.)
- [x] RealActionExecutor with Glyph + Shizuku wiring
- [x] 9 new system toggle actions (auto-rotate, battery saver, airplane, data saver, hotspot, NFC, refresh rate, rotation, media control)

### Shizuku Integration (core-shizuku)
- [x] ShizukuGateway, ShizukuPrivilegedShell
- [x] WiFi, Bluetooth, MobileData, WriteSetting, NFC, airplane, hotspot, battery saver, data saver

### Nothing Glyph Integration (nothing-integrations)
- [x] NothingDeviceDetector (Phone 1/2/2a/3a/3/4a/4a Pro/4b)
- [x] NothingGlyphProvider (light stripe: toggle, animate, progress, zones)
- [x] NothingGlyphMatrixProvider (matrix: text, image, layers, marquee, presets)
- [x] GlyphChannels (per-device channel maps + zone presets)
- [x] GlyphPresets (14 visual patterns)
- [x] GlyphResult sealed interface

### Automation Lifecycle (automation-android)
- [x] AutomationService (foreground, 10 action handlers)
- [x] AutomationAlarmReceiver, AutomationScheduler (time + window + geofence)
- [x] BootCompletedReceiver, DeviceStateReceiver
- [x] PhoneStateReceiver (call + SMS with content reading)
- [x] ConnectivityReceiver (WiFi + Bluetooth)
- [x] GeofenceReceiver + GeofenceMonitor (Play Services)
- [x] UsageStatsMonitor (foreground app polling)
- [x] PersistentMonitorService (battery + screen + charging source + temp)
- [x] AutomationNotificationListener (notification triggers)
- [x] FlipReceiver (Nothing flip-to-glyph)
- [x] NothingModesToyService (Glyph Toy for Phone 3/4a Pro)
- [x] SeedAutomations (Sleep + Morning)

### UI (Compose)
- [x] AutomationListScreen (enable/disable, delete, reorder up/down, FAB)
- [x] AutomationDetailScreen (delete, duplicate, edit)
- [x] CreateAutomationScreen (6 presets + custom builder link)
- [x] CustomAutomationBuilderScreen (WHEN/IF/THEN, 11 triggers, 60+ actions, 11 conditions)
- [x] SettingsScreen (device, permissions, Shizuku, theme, import/export, glyph preview, onboarding)
- [x] ExecutionLogScreen (timeline with timestamps + statistics dashboard)
- [x] OnboardingScreen (7-step permission guide)
- [x] GlyphPreviewScreen (stripe + matrix canvas rendering, 14 presets)
- [x] NothingModesTheme + NothingModesThemeDynamic (dark/light/system toggle)
- [x] NothingComponents, NothingTypography, NothingSpacing, NothingShapes
- [x] NothingModesNavHost (10 routes)

### Manifest
- [x] All permissions declared
- [x] All receivers + services declared

---

## Phase 2: System Integration (COMPLETE)

### State & Conditions
- [x] StateProvider + AndroidStateProvider (battery, charging, screen, WiFi, BT, foreground app, active modes)
- [x] SettingReader + AndroidSettingReader
- [x] StateSnapshotStore + RoomStateSnapshotStore
- [x] ModeActivationProvider/Sink + Room implementations

### Trigger Dispatch
- [x] Time, TimeWindow, Boot, BatteryLevel, ScreenState
- [x] Notification (NotificationListenerService)
- [x] PhoneState (PhoneStateReceiver with SMS content reading)
- [x] Connectivity (ConnectivityReceiver)
- [x] AppOpened (UsageStatsMonitor)
- [x] Geofence (GeofenceMonitor + GeofenceReceiver + scheduler integration)
- [x] Persistent monitoring service
- [x] Flip-to-glyph (FlipReceiver)

### Automation Management
- [x] Create, delete, enable/disable, edit, duplicate
- [x] Import/export (JSON)
- [x] Custom automation builder
- [x] Reorder priority (up/down arrows)

---

## Phase 3: Full Glyph Integration (COMPLETE)

### Glyph Light Stripe
- [x] Per-device channel maps (Phone 1/2/2a/3a/4a/4b)
- [x] Zone presets (A/B/C/D/E)
- [x] Session lifecycle (openSession/closeSession)
- [x] displayProgress, displayProgressAndToggle
- [x] Per-channel brightness, zone-based toggle
- [x] Visual presets (sleep, morning, work, DND, charging, timer, call, SMS, error, success)

### Glyph Matrix
- [x] Structured frames (GlyphMatrixObject + GlyphMatrixFrame)
- [x] Text, image, layer composition
- [x] Scrolling text (marquee)
- [x] Visual presets (fill, percent fill, number, battery level, now playing)

### Glyph Toy Service
- [x] NothingModesToyService (long press, AOD, touch events)
- [x] Manifest registration with toy metadata

---

## Phase 4: System Interaction (COMPLETE)

- [x] NotificationListenerService + filtering
- [x] PhoneStateReceiver + SMS content reading
- [x] Connectivity monitoring (WiFi, Bluetooth)
- [x] Location/geofencing integration (GeofenceMonitor + scheduler)
- [x] Usage stats + foreground app detection
- [x] System settings toggles (airplane, hotspot, NFC, battery saver, data saver, auto-rotate, refresh rate, rotation)
- [x] Media playback control (play/pause, next, previous, stop)
- [x] Camera flip detection (FlipReceiver)
- [x] Power & battery monitoring (source, temperature)

---

## Phase 5: Polish & Release (COMPLETE)

### UI/UX
- [x] Nothing OS design language
- [x] Custom app icon (adaptive)
- [x] Onboarding flow
- [x] Splash screen
- [x] Dark/light/system theme toggle
- [x] Glyph preview screen
- [x] Execution timeline with timestamps
- [x] Statistics dashboard
- [x] Reorder priority UI
- [x] String resources (EN)

### Build & Distribution
- [x] Release build config (R8, resource shrinking)
- [x] Release signing config (env-based)
- [x] F-Droid fastlane metadata
- [x] GitHub release workflow
- [x] Privacy policy
- [x] Contributing guide

### Testing
- [x] 37 engine unit tests (all passing)
- [x] E2E instrumented test (AutomationFlowTest)
- [x] Shizuku instrumented test
- [x] Device tools instrumented test

### Documentation
- [x] README (features, architecture, device matrix, build, setup)
- [x] User guide (installation, permissions, automations, troubleshooting)
- [x] Developer guide (module architecture, engine flow, DI, testing)
- [x] Privacy policy
- [x] Contributing guide
- [x] TASKS.md, DECISIONS.md, CHANGELOG.md, PROGRESS.md

---

## Statistics

- 9 Gradle modules
- 109 Kotlin files
- 13,163 lines of code
- 37 action types
- 12 trigger types
- 12 condition types
- 8 UI screens
- 10 nav routes
- 37 engine unit tests
- 3 instrumented tests
- 14 glyph visual presets
- 8 Nothing device models supported
