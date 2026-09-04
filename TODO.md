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
- [x] CI pipeline (ci.yml + release.yml)

### Engine (engine-core — pure Kotlin)
- [x] 11 trigger types
- [x] 12 condition types + And/Or/Not composites
- [x] 37 action types (23 base + 9 system toggles + 5 glyph advanced)
- [x] TriggerMatcher, ConditionEvaluator, FirePolicy (thread-safe)
- [x] CronSchedule parsing (Sunday=7 normalization)
- [x] kotlinx.serialization (schema v1)
- [x] State snapshot/restore on mode window (dedup by key, newest wins)
- [x] Conflict detection (affectedSettings)
- [x] 173 unit tests (all passing)

### Data Layer (Room)
- [x] 8 entities, 8 DAOs
- [x] RoomAutomationStore (all() returns all automations, not just armed)
- [x] RoomAuditSink, RoomExecutionJournal (implemented with FireClaimDao)
- [x] RoomStateSnapshotStore, RoomModeActivationProvider/Sink (transaction-safe)
- [x] Schema export enabled
- [x] fallbackToDestructiveMigration for future schema changes
- [x] Safe enum converters (no crash on stale DB values)

### Android Controllers (capabilities)
- [x] 16 base controllers (brightness, DND, volume, flashlight, etc.)
- [x] RealActionExecutor with Glyph + Shizuku wiring
- [x] 9 system toggle actions (auto-rotate, battery saver, airplane, data saver, hotspot, NFC, refresh rate, rotation, media control)
- [x] WriteSettingPolicy validation before all write paths
- [x] URL scheme restriction (http/https only)
- [x] Package name validation for LaunchApp
- [x] CapabilityDetector integrates NothingDeviceDetector + ShizukuGateway

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
- [x] AutomationService (foreground, tracks in-flight jobs, stopSelf after completion)
- [x] AutomationAlarmReceiver, AutomationScheduler (time + window + geofence, re-schedules after firing)
- [x] BootCompletedReceiver (starts AutomationService + PersistentMonitorService)
- [x] DeviceStateReceiver
- [x] PhoneStateReceiver (call + SMS with null-safe PDU handling)
- [x] ConnectivityReceiver (WiFi + Bluetooth, SSID passed as match field)
- [x] GeofenceReceiver + GeofenceMonitor (Play Services)
- [x] UsageStatsMonitor (foreground app polling on IO dispatcher)
- [x] PersistentMonitorService (battery + screen + charging source + temp)
- [x] AutomationNotificationListener (notification triggers)
- [x] FlipReceiver (Nothing flip-to-glyph, permission-protected)
- [x] NothingModesToyService (Glyph Toy, permission-protected)
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
- [x] Exported components permission-protected
- [x] allowBackup=false
- [x] No hardcoded secrets

---

## Phase 2: System Integration (COMPLETE)

### State & Conditions
- [x] StateProvider + AndroidStateProvider (battery, charging, screen, WiFi, BT, foreground app, active modes)
- [x] SettingReader + AndroidSettingReader
- [x] StateSnapshotStore + RoomStateSnapshotStore
- [x] ModeActivationProvider/Sink + Room implementations (transaction-safe)

### Trigger Dispatch
- [x] Time, TimeWindow, Boot, BatteryLevel, ScreenState
- [x] Notification (NotificationListenerService)
- [x] PhoneState (PhoneStateReceiver with SMS content reading, null-safe)
- [x] Connectivity (ConnectivityReceiver with SSID match)
- [x] AppOpened (UsageStatsMonitor on IO dispatcher)
- [x] Geofence (GeofenceMonitor + GeofenceReceiver + scheduler integration)
- [x] Persistent monitoring service (started from boot)
- [x] Flip-to-glyph (FlipReceiver, permission-protected)

### Automation Management
- [x] Create, delete, enable/disable, edit, duplicate
- [x] Import/export (JSON, per-item error handling)
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
- [x] Manifest registration with toy metadata + permission

---

## Phase 4: System Interaction (COMPLETE)

- [x] NotificationListenerService + filtering
- [x] PhoneStateReceiver + SMS content reading (null-safe)
- [x] Connectivity monitoring (WiFi, Bluetooth, SSID match)
- [x] Location/geofencing integration (GeofenceMonitor + scheduler)
- [x] Usage stats + foreground app detection (IO dispatcher)
- [x] System settings toggles (airplane, hotspot, NFC, battery saver, data saver, auto-rotate, refresh rate, rotation)
- [x] Media playback control (play/pause, next, previous, stop)
- [x] Camera flip detection (FlipReceiver)
- [x] Power & battery monitoring (source, temperature)

---

## Phase 5: Polish & Release (MOSTLY COMPLETE)

### UI/UX
- [x] Nothing OS design language (dark theme, monochrome + red accent)
- [x] Custom app icon (adaptive)
- [x] Onboarding flow
- [x] Splash screen
- [x] Dark/light/system theme toggle
- [x] Glyph preview screen
- [x] Execution timeline with timestamps
- [x] Statistics dashboard
- [x] Reorder priority UI
- [x] String resources (EN)
- [~] Dot-matrix typography (uses FontFamily.Monospace; full dot-matrix font asset TBD)

### Build & Distribution
- [x] Release build config (R8, resource shrinking)
- [x] Release signing config (env-based)
- [x] F-Droid fastlane metadata
- [x] GitHub release workflow
- [x] Privacy policy
- [x] Contributing guide
- [x] CI pipeline (ci.yml)
- [x] Release workflow (release.yml)

### Testing
- [x] 173 engine unit tests (all passing)
- [x] Engine edge case tests (empty, all-fail, mixed, exceptions)
- [x] ConditionEvaluator tests (all types, nested composites)
- [x] TriggerMatcher tests (all trigger types)
- [x] FirePolicy tests (cooldown, thread safety)
- [x] ImportExport edge case tests (corrupted JSON, schema version, duplicates)
- [x] E2E instrumented test (AutomationFlowTest)
- [x] Shizuku instrumented test
- [x] Device tools instrumented test

### Documentation
- [x] README (features, architecture, device matrix, build, setup)
- [x] User guide (installation, permissions, automations, troubleshooting)
- [x] Developer guide (module architecture, engine flow, DI, testing)
- [x] Privacy policy
- [x] Contributing guide
- [x] TASKS.md, TODO.md

### Security
- [x] Security review completed (receivers, WriteSettingPolicy, URL/package validation)
- [x] Exported component permissions (FlipReceiver, ToyService)
- [x] Hardcoded secrets removed
- [x] allowBackup=false
- [x] SECURITY.md policy

### Open Source
- [x] LICENSE (GPL-3.0)
- [x] CONTRIBUTING.md
- [x] CODE_OF_CONDUCT.md
- [x] SECURITY.md
- [x] .gitignore
- [x] FUNDING.yml

---

## Remaining Work (Post-Release)

- [ ] Dot-matrix font asset for full Nothing typography
- [ ] Notification glyph patterns
- [ ] Timer/countdown glyph presets
- [ ] Media session tracking
- [ ] Screen time tracking
- [ ] Location-based mode activation
- [ ] Dependency review
- [ ] Final repository audit (TODO/FIXME/placeholder)
- [ ] Release build verification

---

## Statistics

- 9 Gradle modules
- 109 Kotlin files
- 13,163 lines of code
- 37 action types
- 11 trigger types
- 12 condition types
- 8 UI screens
- 10 nav routes
- 173 engine unit tests
- 3 instrumented tests
- 14 glyph visual presets
- 8 Nothing device models supported
