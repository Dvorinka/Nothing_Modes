# Nothing Modes — Master TODO

## Legend
- [x] Done
- [~] In progress
- [ ] Not started
- [!] Blocked

---

## Phase 1: Foundation (COMPLETE)

### Package & Build
- [x] Refactor package name `com.dvoranka` → `com.tdvorak`
- [x] Gradle RAM limits configured
- [x] CI pipeline optimized (single invocation, --parallel --build-cache)
- [x] 9 Gradle modules + version catalog
- [x] Hilt DI wired across all modules

### Engine (engine-core — pure Kotlin)
- [x] 12 trigger types defined (Time, TimeWindow, Immediate, Notification, PhoneState, Connectivity, Boot, BatteryLevel, ScreenState, AppOpened, Geofence)
- [x] 12 condition types + And/Or/Not composites
- [x] 23 action types defined
- [x] TriggerMatcher
- [x] ConditionEvaluator
- [x] FirePolicy (cooldown suppression)
- [x] CronSchedule parsing
- [x] kotlinx.serialization (schema v1)
- [x] Sleep/Morning E2E test passing

### Data Layer (Room)
- [x] NothingModesDatabase (8 entities, version 1)
- [x] AutomationDao (CRUD, armed query)
- [x] AuditDao (insert, observeRecent, forAutomation, forExecution, prune)
- [x] ExecutionJournalDao
- [x] StateSnapshotDao
- [x] DraftDao, FireClaimDao, ModeActivationDao, ScheduledTimeAlarmDao
- [x] RoomAutomationStore
- [x] RoomAuditSink
- [x] RoomExecutionJournal
- [x] RoomStateSnapshotStore
- [x] Schema export enabled

### Android Controllers (capabilities)
- [x] SetBrightness (Settings.System)
- [x] SetAutoBrightness
- [x] SetExtraDim (API 33+)
- [x] SetDnd (NotificationManager)
- [x] SetDarkMode (UiModeManager)
- [x] SetVolume (AudioManager)
- [x] SetRinger (AudioManager)
- [x] SetScreenTimeout (Settings.System)
- [x] SetFlashlight (CameraManager)
- [x] Vibrate (VibratorManager)
- [x] CopyText (ClipboardManager)
- [x] LaunchApp (PackageManager)
- [x] OpenUrl (Intent.ACTION_VIEW)
- [x] OpenSettingsScreen (8 screens)
- [x] ShowNotification (NotificationCompat)
- [x] Wait (kotlinx.coroutines.delay)
- [x] RealActionExecutor wired

### Shizuku Integration (core-shizuku)
- [x] ShizukuGateway (lifecycle, permission, status observation)
- [x] ShizukuPrivilegedShell (UserService transport)
- [x] SetWifi (svc wifi)
- [x] SetBluetooth (svc bluetooth)
- [x] SetMobileData (svc data)
- [x] WriteSetting (settings put)
- [x] PrioritizedPrivilegedShellTest
- [x] ShizukuGatewayTest

### Nothing Glyph Integration (nothing-integrations)
- [x] NothingDeviceDetector (Phone 1/2/2a/3a/3/4a/4a Pro/4b)
- [x] NothingGlyphProvider (light stripe: toggle, turnOff, animate)
- [x] NothingGlyphMatrixProvider (matrix: setFrame, closeFrame, turnOff)
- [x] GlyphResult sealed interface
- [x] GlyphHardware enum
- [x] SDK AAR extracted into module

### Automation Lifecycle (automation-android)
- [x] AutomationService (foreground service, @AndroidEntryPoint)
- [x] AutomationAlarmReceiver (time/window_start/window_end)
- [x] AutomationScheduler (AlarmManager exact alarms)
- [x] BootCompletedReceiver (reschedule + Boot trigger dispatch)
- [x] SeedAutomations (Sleep + Morning on first launch)
- [x] Hilt DI graph (12 singletons)

### UI (Compose)
- [x] AutomationListScreen (real data, enable/disable, delete icon, FAB)
- [x] AutomationDetailScreen (real data, delete button, duplicate button)
- [x] CreateAutomationScreen (6 preset templates + custom builder link)
- [x] CustomAutomationBuilderScreen (WHEN/IF/THEN, 11 triggers, 47 actions, 11 conditions)
- [x] SettingsScreen (device caps, permissions, Shizuku, import/export, onboarding link)
- [x] ExecutionLogScreen (real audit data)
- [x] OnboardingScreen (7-step permission guide)
- [x] NothingModesTheme (Nothing OS-inspired palette + typography + shapes + spacing)
- [x] NothingComponents (NothingCard, NothingSectionHeader, NothingInfoRow, NothingDivider, NothingRedDot)
- [x] NothingModesNavHost (8 routes)

### Manifest
- [x] RECEIVE_BOOT_COMPLETED
- [x] SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM
- [x] FOREGROUND_SERVICE, FOREGROUND_SERVICE_SPECIAL_USE
- [x] POST_NOTIFICATIONS
- [x] WRITE_SETTINGS
- [x] ACCESS_NOTIFICATION_POLICY
- [x] CAMERA
- [x] VIBRATE
- [x] WAKE_LOCK
- [x] QUERY_ALL_PACKAGES
- [x] Glyph permissions (QUERY_GLYPH_STATE, CONTROL_GLYPH)
- [x] com.nothing.ketchum.permission.ENABLE
- [x] NothingKey meta-data (test key)
- [x] Service + receiver declarations

---

## Phase 2: System Integration (IN PROGRESS)

### State & Conditions
- [x] StateProvider interface in engine-core
- [x] AndroidStateProvider (battery, charging, screen state, active mode IDs)
- [x] Engine calls stateProvider.read() before condition evaluation
- [x] SettingReader interface
- [x] AndroidSettingReader (Settings.System)
- [x] StateSnapshotStore + RoomStateSnapshotStore
- [x] Restore-previous-state on ModeWindowEnd
- [x] ModeActivationProvider + RoomModeActivationProvider
- [x] ModeActivationSink + RoomModeActivationSink
- [ ] Extend AndroidStateProvider: WiFi connection state (needs location permission)
- [ ] Extend AndroidStateProvider: Bluetooth connection state
- [ ] Extend AndroidStateProvider: foreground app (UsageStatsManager)

### Trigger Dispatch
- [x] Time trigger (AlarmManager)
- [x] TimeWindow trigger (start + end alarms)
- [x] Boot trigger (BootCompletedReceiver → AutomationService)
- [x] BatteryLevel trigger (DeviceStateReceiver — needs persistent service)
- [x] ScreenState trigger (DeviceStateReceiver — needs persistent service)
- [ ] Notification trigger (needs NotificationListenerService)
- [ ] PhoneState trigger (needs PhoneStateReceiver)
- [ ] Connectivity trigger (needs ConnectivityManager callback)
- [ ] AppOpened trigger (needs UsageStatsManager polling)
- [ ] Geofence trigger (needs LocationManager / GeofencingClient)
- [ ] Persistent monitoring service (keep service alive for dynamic receivers)

### Automation Management
- [x] Create automations (6 preset templates)
- [x] Delete automations (list + detail)
- [x] Enable/disable toggle
- [x] Edit existing automations (custom builder edit mode)
- [x] Duplicate automation
- [x] Import/export (JSON)
- [x] Custom automation builder (trigger picker + action picker + condition picker)
- [ ] Reorder priority

---

## Phase 3: Full Glyph Integration (NOT STARTED)

### Glyph Light Stripe — Per-Device Channel Maps
- [ ] Define channel constants for Phone (1): A1, B1, C1-C4, D1_1-D1_8, E1
- [ ] Define channel constants for Phone (2): A1, A2, B1, C1_1-C1_16, C2-C6, D1_1-D1_8, E1
- [ ] Define channel constants for Phone (2a/2a+): A, B, C1-C24
- [ ] Define channel constants for Phone (3a/3a Pro): A1-A11, B1-B5, C1-C20
- [ ] Define channel constants for Phone (4a): A1-A6
- [ ] Define channel constants for Phone (4b): A1-A4
- [ ] Per-device zone presets (A/B/C/D/E zones)

### Glyph Light Stripe — Advanced Operations
- [ ] openSession() / closeSession() lifecycle
- [ ] displayProgress(frame, progress) — progress bar on C1/D1
- [ ] displayProgress(frame, progress, reverse)
- [ ] displayProgressAndToggle(frame, progress, isReverse)
- [ ] Per-channel brightness control
- [ ] Zone-based toggle (buildChannelA/B/C/D/E)
- [ ] Notification glyph patterns (charging, alarm, timer, countdown)
- [ ] Charging animation preset
- [ ] Alarm visual preset
- [ ] Timer countdown preset
- [ ] Volume indicator preset

### Glyph Matrix — Structured Frames
- [ ] GlyphMatrixObject.Builder (image, text, position, rotation, scale, brightness)
- [ ] GlyphMatrixFrame.Builder (top/mid/low layers, max 3 objects)
- [ ] setAppMatrixFrame(GlyphMatrixFrame) — structured frame display
- [ ] GlyphMatrixUtils.drawableToBitmap — image conversion
- [ ] GlyphMatrixUtils.LetterMatrix — text rendering on matrix
- [ ] Text display on matrix (setText on GlyphMatrixObject)
- [ ] Image display on matrix (setImageSource with bitmap)
- [ ] Layer composition (top/mid/low)
- [ ] Brightness control per object
- [ ] Scale and rotation per object

### Glyph Matrix — Scrolling Text (Marquee)
- [ ] GlyphMatrixFrameWithMarquee integration
- [ ] TextMarqueeConfig (speed, direction, loop)
- [ ] Scrolling notification text on matrix
- [ ] Scrolling automation name on activation
- [ ] Scrolling song/artist info (needs notification listener)

### Glyph Matrix — Glyph Toy Service
- [ ] GlyphToy service implementation (com.nothing.glyph.TOY intent)
- [ ] Toy manifest registration (name, preview image, summary)
- [ ] onBind/onUnbind lifecycle
- [ ] EVENT_CHANGE handler (long press)
- [ ] EVENT_AOD handler (always-on display, 1-min tick)
- [ ] action_down / action_up handlers (touch-down/up)
- [ ] Toy preview image (SVG → drawable)
- [ ] Guide users to Toys Manager activity

### Glyph — Visual Presets & Patterns
- [ ] Mode activation visual (distinct pattern per mode)
- [ ] Mode deactivation visual (fade out)
- [ ] Automation fired visual (pulse)
- [ ] Error visual (red blink)
- [ ] Charging visual (battery fill animation)
- [ ] DND active visual (steady glow)
- [ ] Sleep mode visual (slow breathing)
- [ ] Morning visual (sunrise gradient)
- [ ] Work focus visual (focused beam)
- [ ] Custom visual editor (pick channels + period + cycles)

### Glyph — Notification Integration
- [ ] Notification glyph pattern on ShowNotification action
- [ ] Per-app notification patterns (different channel combos)
- [ ] Notification count display on matrix (number rendering)
- [ ] Priority notification visual (urgent vs normal)
- [ ] Notification glyph timeout (auto-off after N seconds)

### Glyph — Progress & Countdown
- [ ] Timer countdown on light stripe (D1 progress)
- [ ] Timer countdown on matrix (circular progress)
- [ ] Battery level display on matrix (percentage + fill)
- [ ] Charging progress animation
- [ ] Volume level indicator on light stripe
- [ ] Brightness level indicator on light stripe

---

## Phase 4: System Interaction (NOT STARTED)

### Notifications
- [ ] NotificationListenerService (for notification triggers + content)
- [ ] Notification glyph patterns on incoming notifications
- [ ] Notification filtering (per-app, per-priority)
- [ ] Notification history/log

### Phone State
- [ ] PhoneStateReceiver (incoming call, offhook, idle)
- [ ] SMS content reading (for text match triggers)
- [ ] Call glyph pattern (pulsing during ring)
- [ ] SMS glyph pattern (brief flash)

### Connectivity
- [ ] ConnectivityManager.NetworkCallback
- [ ] WiFi SSID detection (needs location permission)
- [ ] Bluetooth device name detection
- [ ] Airplane mode detection
- [ ] Connectivity glyph visual (WiFi/BT status on matrix)

### Location
- [ ] GeofencingClient integration
- [ ] Location permission request flow
- [ ] Geofence enter/exit trigger dispatch
- [ ] Location-based mode activation (home/work)

### Usage Stats
- [ ] UsageStatsManager polling for foreground app
- [ ] PACKAGE_USAGE_STATS permission request flow
- [ ] App-opened trigger dispatch
- [ ] Screen time tracking

### System Settings
- [ ] Airplane mode toggle (Shizuku)
- [ ] Hotspot toggle (Shizuku)
- [ ] Location mode toggle (Shizuku)
- [ ] Auto-rotate toggle
- [ ] Battery saver toggle
- [ ] Data saver toggle
- [ ] NFC toggle (Shizuku)

### Media & Audio
- [ ] Media session tracking (playing/paused state)
- [ ] Media playback control (play/pause/next/prev via Shizuku)
- [ ] Now playing info on Glyph Matrix (artist + title scrolling)
- [ ] Per-app volume control (AudioManager)
- [ ] Audio output routing (speaker/BT/headphones)

### Camera & Display
- [ ] Camera flip detection (Nothing phones with flip to glyph)
- [ ] Always-on display control
- [ ] Screen rotation lock
- [ ] Refresh rate control (via Settings.System)
- [ ] Wallpaper change on mode activation

### Power & Battery
- [ ] Battery saver auto-enable on low battery
- [ ] Charging state tracking (AC/USB/wireless)
- [ ] Power monitor widget
- [ ] Battery temperature monitoring

---

## Phase 5: Polish & Release (PARTIALLY DONE)

### UI/UX
- [x] Nothing OS design language (typography, spacing, shapes, components)
- [x] Custom app icon (Nothing-style dot matrix, adaptive)
- [x] Onboarding flow (permission requests, Shizuku setup, Glyph test)
- [x] Splash screen
- [x] String resources (EN)
- [ ] Automation visual editor (drag-and-drop trigger/action/condition)
- [ ] Dark/light theme toggle
- [ ] Glyph preview in app (simulate what glyph will show)
- [ ] Execution timeline view
- [ ] Statistics dashboard (automation fire count, success rate)

### Build & Distribution
- [x] Release build config (ProGuard/R8, resource shrinking)
- [x] App icon (adaptive + legacy)
- [x] Splash screen
- [x] String resources (i18n — at minimum EN)
- [ ] Release signing config
- [ ] F-Droid metadata
- [ ] Google Play Store listing
- [ ] GitHub Releases with APK

### Testing
- [x] Engine unit tests (37 tests: engine, triggers, conditions, conflicts, restore, import/export)
- [ ] Instrumented tests on CI (connectedCheck)
- [ ] Glyph SDK integration tests (requires Nothing device)
- [ ] Shizuku integration tests (requires Shizuku installed)
- [ ] E2E test: create automation → fire trigger → verify action
- [ ] Performance benchmarks
- [ ] Battery impact profiling

### Documentation
- [x] README (features, architecture, device matrix, build, setup)
- [x] TASKS.md, DECISIONS.md, CHANGELOG.md, PROGRESS.md
- [ ] User guide (how to set up Shizuku, create automations)
- [ ] Developer guide (architecture, module structure)
- [ ] Glyph SDK reference (which devices support what)
- [ ] Privacy policy
- [ ] Contributing guide
