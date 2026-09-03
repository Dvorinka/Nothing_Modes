# Nothing Modes — Task Checklist

## Legend
- [x] Done
- [~] In progress
- [ ] Not started
- [!] Blocked

---

## Phase 1: Foundation — COMPLETE

All items verified complete. See TODO.md for historical record.

---

## Phase 2: System Integration

### State & Conditions
- [x] StateProvider interface in engine-core
- [x] AndroidStateProvider (battery, charging, screen state)
- [x] Engine calls stateProvider.read() before condition evaluation
- [x] SettingReader interface
- [x] AndroidSettingReader (Settings.System)
- [x] StateSnapshotStore + RoomStateSnapshotStore
- [x] Restore-previous-state on ModeWindowEnd (dedup by key, newest wins)
- [x] Extend AndroidStateProvider: WiFi connection state
- [x] Extend AndroidStateProvider: Bluetooth connection state
- [x] Extend AndroidStateProvider: foreground app (UsageStatsManager)
- [x] Extend AndroidStateProvider: active mode IDs (query ModeActivationDao)

### Trigger Dispatch
- [x] Time trigger (AlarmManager) — re-schedules next cron occurrence after firing
- [x] TimeWindow trigger (start + end alarms) — re-schedules after window end
- [x] Boot trigger (BootCompletedReceiver → AutomationService + PersistentMonitorService)
- [x] BatteryLevel trigger (DeviceStateReceiver)
- [x] ScreenState trigger (DeviceStateReceiver)
- [x] Notification trigger (NotificationListenerService)
- [x] PhoneState trigger (PhoneStateReceiver) — null-safe SMS PDU handling
- [x] Connectivity trigger (ConnectivityReceiver) — SSID passed as match field
- [x] AppOpened trigger (UsageStatsMonitor) — queryUsageStats on IO dispatcher
- [x] Geofence trigger (GeofencingClient)
- [x] Persistent monitoring service — started from BootCompletedReceiver

### Automation Management
- [x] Create automations (6 preset templates)
- [x] Delete automations (list + detail)
- [x] Enable/disable toggle
- [x] Edit existing automations (pre-populate creation screen)
- [x] Duplicate automation
- [x] Import/export (JSON) — per-item error handling, schema validation
- [x] Custom automation builder (trigger picker + action picker + condition picker)

---

## Phase 3: Full Glyph Integration

### Glyph Light Stripe
- [x] Per-device channel constants (Phone 1/2/2a/3a/4a/4b)
- [x] openSession/closeSession lifecycle
- [x] displayProgress
- [x] Zone-based toggle (buildChannelA/B/C/D/E)
- [x] Animate with period/cycles/interval
- [ ] Notification glyph patterns (charging, alarm, timer, countdown)
- [ ] Charging animation preset
- [ ] Alarm visual preset
- [ ] Timer countdown preset
- [ ] Volume indicator preset

### Glyph Matrix
- [x] GlyphMatrixObject.Builder (image, text, position, rotation, scale, brightness)
- [x] GlyphMatrixFrame.Builder (top/mid/low layers, max 3 objects)
- [x] setAppMatrixFrame(GlyphMatrixFrame)
- [x] Text display on matrix
- [x] Image display on matrix
- [x] Layer composition (top/mid/low)
- [x] Scrolling text (marquee)
- [x] Fill matrix
- [x] Percent fill
- [x] Display number
- [ ] Notification count display on matrix

### Glyph Toy
- [x] GlyphToy service implementation
- [x] Toy manifest registration (with permission protection)
- [x] onBind/onUnbind lifecycle
- [x] EVENT_CHANGE handler (long press)
- [x] EVENT_AOD handler
- [x] action_down / action_up handlers
- [ ] Toy preview image (drawable)
- [ ] Guide users to Toys Manager activity

### Glyph Visual Presets
- [x] Mode activation visual (sleep, morning, work, DND, etc.)
- [x] Mode deactivation visual
- [x] Automation fired visual
- [x] 15+ presets defined
- [ ] Custom visual editor (pick channels + period + cycles)

### Glyph Notification Integration
- [ ] Notification glyph pattern on ShowNotification action
- [ ] Per-app notification patterns
- [ ] Notification glyph timeout (auto-off after N seconds)

### Glyph Progress & Countdown
- [ ] Timer countdown on light stripe (D1 progress)
- [ ] Timer countdown on matrix (circular progress)
- [ ] Battery level display on matrix
- [ ] Charging progress animation
- [ ] Volume level indicator on light stripe
- [ ] Brightness level indicator on light stripe

---

## Phase 4: System Interaction

### Notifications
- [x] NotificationListenerService
- [ ] Notification glyph patterns on incoming notifications
- [ ] Notification filtering (per-app, per-priority)
- [ ] Notification history/log

### Phone State
- [x] PhoneStateReceiver (incoming call, offhook, idle)
- [x] SMS content reading (null-safe PDU handling)
- [ ] Call glyph pattern (pulsing during ring)
- [ ] SMS glyph pattern (brief flash)

### Connectivity
- [x] ConnectivityManager broadcast receivers
- [x] WiFi SSID detection (passed as match field)
- [x] Bluetooth device name detection
- [ ] Airplane mode detection
- [ ] Connectivity glyph visual

### Location
- [x] GeofencingClient integration
- [x] Geofence enter/exit trigger dispatch
- [ ] Location permission request flow (in-app prompt)
- [ ] Location-based mode activation (home/work)

### Usage Stats
- [x] UsageStatsManager polling for foreground app (IO dispatcher)
- [x] PACKAGE_USAGE_STATS permission detection
- [x] App-opened trigger dispatch
- [ ] Screen time tracking

### System Settings (via Shizuku where needed)
- [x] Airplane mode toggle
- [x] Hotspot toggle
- [x] Location mode toggle
- [x] Auto-rotate toggle
- [x] Battery saver toggle
- [x] Data saver toggle
- [x] NFC toggle
- [x] WriteSetting policy validation

### Media & Audio
- [x] Media playback control (play/pause/next/prev/stop)
- [ ] Media session tracking (playing/paused state)
- [ ] Now playing info on Glyph Matrix
- [ ] Per-app volume control

### Camera & Display
- [x] Camera flip detection (FlipReceiver)
- [x] Always-on display control (Glyph Toy AOD)
- [x] Screen rotation lock
- [x] Refresh rate control
- [ ] Wallpaper change on mode activation

### Power & Battery
- [x] Battery saver auto-enable on low battery
- [x] Charging state tracking (AC/USB/wireless)
- [x] Battery temperature monitoring

---

## Phase 5: Polish & Release

### UI/UX
- [x] Nothing OS design language (dark theme, monochrome + red accent)
- [x] Centralized design primitives (NothingTheme, NothingTypography, NothingSpacing, etc.)
- [x] Custom app icon (Nothing-style dot matrix)
- [x] Onboarding flow (permission requests, Shizuku setup, Glyph test)
- [x] Custom automation builder UI (WHEN/IF/THEN)
- [x] Dark/light theme toggle
- [x] Glyph preview in app
- [x] Execution timeline view
- [x] Statistics dashboard
- [x] Compatibility/debug screen
- [~] Dot-matrix typography (uses FontFamily.Monospace for labels; full dot-matrix font TBD)

### Build & Distribution
- [x] Release build config (R8/ProGuard, signing)
- [x] App icon (adaptive + legacy)
- [x] Splash screen
- [x] String resources (i18n)
- [x] F-Droid metadata
- [x] GitHub Releases with APK
- [x] CI pipeline (ci.yml)
- [x] Release workflow (release.yml)

### Testing
- [x] Engine: conflict management tests
- [x] Engine: restoration after reboot/process death
- [x] Engine: overlapping automations
- [x] Engine: import/export validation (edge cases, corrupted JSON, schema version)
- [x] Engine: condition evaluator (all condition types, nested composites)
- [x] Engine: trigger matcher (all trigger types)
- [x] Engine: fire policy (cooldown, thread safety)
- [x] Engine: edge cases (empty actions, all-fail, mixed, exceptions)
- [ ] Engine: duplicate automation
- [ ] Android: boot, alarms, battery, screen tests
- [ ] Nothing: Glyph, Glyph Matrix, unsupported devices
- [ ] Shizuku: unavailable, denied, restart
- [ ] Data: migrations, import, export, corrupted input
- [ ] UI: creation, editing, enabling, deleting, duplicating
- [ ] E2E: full Sleep → trigger → execute → restore cycle

### Documentation
- [x] README (features, architecture, device matrix, build, setup)
- [x] User guide
- [x] Developer guide
- [x] Privacy policy
- [x] Contributing guide
- [x] TASKS.md, TODO.md
- [ ] Architecture document
- [ ] Module guide
- [ ] Shizuku integration guide
- [ ] Nothing SDK reference
- [ ] Supported device matrix

### Security & Review
- [x] Security review (receivers, WriteSettingPolicy, URL/package validation, allowBackup)
- [x] Exported component permissions (FlipReceiver, ToyService)
- [x] Hardcoded secrets removed (NothingKey=test removed)
- [ ] Dependency review (abandoned, duplicate, vulnerabilities)
- [ ] Final repository audit (TODO/FIXME/placeholder/mock/dummy)
- [x] Clean build verification
- [ ] Release build verification

### Open Source Preparation
- [x] LICENSE (GPL-3.0)
- [x] CONTRIBUTING.md
- [x] CODE_OF_CONDUCT.md
- [x] SECURITY.md
- [x] .gitignore
- [x] FUNDING.yml
- [x] PR template
