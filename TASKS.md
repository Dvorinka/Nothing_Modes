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
- [x] Restore-previous-state on ModeWindowEnd
- [x] Extend AndroidStateProvider: WiFi connection state
- [x] Extend AndroidStateProvider: Bluetooth connection state
- [x] Extend AndroidStateProvider: foreground app (UsageStatsManager)
- [ ] Extend AndroidStateProvider: active mode IDs (query ModeActivationDao)

### Trigger Dispatch
- [x] Time trigger (AlarmManager)
- [x] TimeWindow trigger (start + end alarms)
- [x] Boot trigger (BootCompletedReceiver → AutomationService)
- [x] BatteryLevel trigger (DeviceStateReceiver)
- [x] ScreenState trigger (DeviceStateReceiver)
- [x] Notification trigger (NotificationListenerService)
- [x] PhoneState trigger (PhoneStateReceiver)
- [x] Connectivity trigger (ConnectivityReceiver)
- [x] AppOpened trigger (UsageStatsMonitor)
- [x] Geofence trigger (GeofencingClient)
- [x] Persistent monitoring service

### Automation Management
- [x] Create automations (6 preset templates)
- [x] Delete automations (list + detail)
- [x] Enable/disable toggle
- [x] Edit existing automations (pre-populate creation screen)
- [ ] Duplicate automation
- [ ] Import/export (JSON)
- [ ] Custom automation builder (trigger picker + action picker + condition picker)

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
- [x] Toy manifest registration
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
- [ ] SMS content reading (for text match triggers)
- [ ] Call glyph pattern (pulsing during ring)
- [ ] SMS glyph pattern (brief flash)

### Connectivity
- [x] ConnectivityManager broadcast receivers
- [x] WiFi SSID detection
- [x] Bluetooth device name detection
- [ ] Airplane mode detection
- [ ] Connectivity glyph visual

### Location
- [x] GeofencingClient integration
- [x] Geofence enter/exit trigger dispatch
- [ ] Location permission request flow (in-app prompt)
- [ ] Location-based mode activation (home/work)

### Usage Stats
- [x] UsageStatsManager polling for foreground app
- [x] PACKAGE_USAGE_STATS permission detection
- [x] App-opened trigger dispatch
- [ ] Screen time tracking

### System Settings (via Shizuku where needed)
- [ ] Airplane mode toggle
- [ ] Hotspot toggle
- [ ] Location mode toggle
- [ ] Auto-rotate toggle
- [ ] Battery saver toggle
- [ ] Data saver toggle
- [ ] NFC toggle

### Media & Audio
- [ ] Media session tracking (playing/paused state)
- [ ] Media playback control (play/pause/next/prev)
- [ ] Now playing info on Glyph Matrix
- [ ] Per-app volume control

### Camera & Display
- [ ] Camera flip detection
- [ ] Always-on display control
- [ ] Screen rotation lock
- [ ] Refresh rate control
- [ ] Wallpaper change on mode activation

### Power & Battery
- [ ] Battery saver auto-enable on low battery
- [ ] Charging state tracking (AC/USB/wireless)
- [ ] Battery temperature monitoring

---

## Phase 5: Polish & Release

### UI/UX
- [ ] Nothing OS design language (dot matrix font, monochrome + red accent)
- [ ] Centralized design primitives (NothingTheme, NothingTypography, NothingSpacing, etc.)
- [ ] Custom app icon (Nothing-style dot matrix)
- [ ] Onboarding flow (permission requests, Shizuku setup, Glyph test)
- [ ] Custom automation builder UI (WHEN/IF/THEN)
- [ ] Dark/light theme toggle
- [ ] Glyph preview in app
- [ ] Execution timeline view
- [ ] Statistics dashboard
- [ ] Compatibility/debug screen

### Build & Distribution
- [ ] Release build config (R8/ProGuard, signing)
- [ ] App icon (adaptive + legacy)
- [ ] Splash screen
- [ ] String resources (i18n)
- [ ] F-Droid metadata
- [ ] GitHub Releases with APK

### Testing
- [ ] Engine: conflict management tests
- [ ] Engine: restoration after reboot/process death
- [ ] Engine: overlapping automations
- [ ] Engine: duplicate automation
- [ ] Engine: import/export validation
- [ ] Android: boot, alarms, battery, screen tests
- [ ] Nothing: Glyph, Glyph Matrix, unsupported devices
- [ ] Shizuku: unavailable, denied, restart
- [ ] Data: migrations, import, export, corrupted input
- [ ] UI: creation, editing, enabling, deleting, duplicating
- [ ] E2E: full Sleep → trigger → execute → restore cycle

### Documentation
- [ ] Architecture document
- [ ] Module guide
- [ ] Setup/build guide
- [ ] Proxmox worker guide
- [ ] Shizuku integration guide
- [ ] Nothing SDK reference
- [ ] Supported device matrix
- [ ] Privacy policy
- [ ] Contributing guide

### Security & Review
- [ ] Security review (secrets, permissions, exported components, intents)
- [ ] Dependency review (abandoned, duplicate, vulnerabilities)
- [ ] Final repository audit (TODO/FIXME/placeholder/mock/dummy)
- [ ] Clean build verification
- [ ] Release build verification
