# Changelog

## [Unreleased]

## [0.18.0]

### Added
- Ultra-dim overlay is now hosted by an optional accessibility service as a trusted `TYPE_ACCESSIBILITY_OVERLAY` — touches pass through cleanly (sign-in sheets, permission dialogs, account pickers keep working) at any intensity; falls back to the regular overlay when the service is off
- Live ultra-dim control: persistent notification with -10% / +10% / Turn off actions plus a floating slider panel with a "Reset" snap-back to the mode-configured level — adjust without editing the mode
- Ultra-dim overlay now sizes to the real display bounds (covers nav bar, status bar, cutout) and re-sizes on rotation
- "Enable tap-friendly dimming" affordance in the ultra-dim action config opens accessibility settings

### Fixed
- Android 15 boot crash: BOOT_COMPLETED can no longer start `specialUse` foreground services — service starts now hop through an expedited WorkManager job, which is exempt
- Deprecated `decorFitsSystemWindows` dialog parameter removed (Compose 1.8 edge-to-edge readiness); widget config activity now calls `enableEdgeToEdge()`

## [Unreleased history]

### Added
- Universal capability warning layer: catalog badges, pick-time warning with per-gap "Fix it" deep links, Shizuku-state-aware guidance, builder save summary, and fire-time heads-up notification for lost requirements
- Priority conflict resolution in the engine: higher-priority modes claim settings and suppress lower-priority conflicts, with `SUPPRESS_CONFLICT` audit events
- Exact-alarm runtime prompt in Time/TimeWindow trigger configuration
- `ConflictAndRestoreTest` coverage for priority conflict suppression
- Full Nothing OS visual overhaul merged from `proxmox-20260905-185035`:
  OLED black canvas, dark `#161616` cards, dot-matrix (Doto) hero type,
  red accent for toggles/FAB/selected dots, 24-28dp rounded cards and
  sheets, monochrome icon chips, and Nothing-style top bars with a
  circular back button
- Glyph Toys system integration: required `toy.image`/`toy.introduction` metadata, `GlyphToysBridge` to enumerate toys + open the Nothing OS manager/AOD-picker/timeout screens, system section on the Glyph Preview screen
- Glyph reset semantics: modes auto-clear glyph output on window end, "Glyph off" catalog action, TURN OFF control on Glyph Preview
- New `ActionResult.NeedsUserAction`: shell actions without Shizuku now open the matching system panel (Wi-Fi connectivity, mobile data, Bluetooth, airplane, hotspot, NFC, data saver, battery saver, auto-sync, AOD, location) instead of silently failing
- Per-action requirement hints shown in the action config sheet (Nothing hardware, Shizuku, permissions, panel fallback)
- Friendlier glyph config: preset dropdown, zone selector, matrix on/off/custom modes
- New `NothingPickers` components: wheel time picker, calendar date picker, timezone field (device default + searchable list + raw-id advanced input), large neutral day selector
- Settings: Manage button on every permission row so access can be granted and revoked from the app; restricted-settings guidance for sideloaded installs (App Info → Allow restricted settings)
- Settings: full Shizuku flow — Get Shizuku (Play/GitHub), Open Shizuku, Authorize, per-status guidance
- Builder: Save / Discard / Cancel exit dialog, Advanced section with Enabled toggle + Priority, Enabled is now persisted per mode
- Catalogs: multi-select with ADDED state and sticky Done bar (no auto-close after one pick)
- Trigger config: trigger-type picker dialog (grouped list) replaces the inline chip grid

### Fixed
- System back and back-swipe gesture now show the unsaved-changes dialog instead of silently leaving the builder
- TimeWindow trigger/condition use real time pickers and a timezone dropdown instead of raw text fields
- Long TimeWindow descriptions no longer include the timezone when it matches the device zone

## [0.15.0]

### Added
- App-name resolution everywhere: package IDs like `com.whatsapp` render as "WhatsApp" in trigger descriptions, the recent-notification picker, and the execution log
- Notification trigger fields carry explanations (title/text/sender/group-conversation matching) and the recent-notification picker
- Device-state triggers show live "Now:" values; thermal trigger explains its 0–6 severity scale
- Settings: Creator Profile (display name, handle, email, GitHub) feeds the publish sheet; Interface style selector explains itself; Shizuku section explains what it is and why it is optional
- Onboarding explains Shizuku in plain language and installs it from the Play Store
- Engine: `endAutomation()` + service-side removal dispatch so deleted or disabled modes restore snapshots and deactivate
- `rememberAppLabelResolver()` shared app-label cache
- Unit coverage: 10 new matcher tests (battery crossings, POWER bridge, phone-number formats, overnight windows) and 11 canonical-JSON tests

### Changed
- Community library is the sole template source — featured templates section removed
- Template install sheet lists the trigger and every action, flattening groups; capability summaries are human-readable
- Catalog requirement badges use readable words (CALENDAR, NOTIFICATIONS, USAGE ACCESS); filter chips are labeled
- Builder: Enabled toggle is always visible (moved out of Advanced); Save bar only appears when dirty; action grouping and deletion use icons; Create is disabled until a mode has an action
- Execution log shows mode names instead of IDs, plain event names, full stat labels, and pagination
- Glyph config is one combined sheet with a design-type selector and live matrix thumbnails on every picker entry
- Millisecond fields replaced by seconds/minutes or named presets (dwell delay, blink speed, glyph timeout, wait)
- Action/trigger summaries humanized — no more DND/AOD/app(s)/raw-enum output
- Mode detail shows consistent enabled state and clickable numbered action rows; share exports a real `.nothingmode.json` file
- Shizuku install flow prefers the Play Store, with GitHub releases as fallback

### Fixed
- Editing an action inside a group no longer deletes the whole group
- Day-filtered overnight windows deactivate on the correct day instead of staying active forever
- Wi-Fi-connected modes end on disconnect; network hops end SSID-filtered modes correctly
- Deleting or disabling an active mode restores snapshotted settings instead of leaving them stuck
- A malformed geofence no longer aborts rescheduling of every other automation on boot
- Cooldown is consumed only when a mode actually fires — manual runs, condition-blocked candidates, and window ends no longer burn it; window ends also bypass conditions
- Engine access is serialized, removing snapshot races between simultaneous triggers
- PendingIntents carry unique data URIs — hashCode collisions can no longer overwrite scheduled alarms
- Stale "starting soon" notifications re-check that the rule still exists before posting
- Battery triggers fire on threshold crossings even when broadcasts skip levels
- Geofence dwell delay actually reaches the geofencing request
- Legacy POWER connectivity triggers bridge to the charger event; phone-number filters match E.164 and national formats
- Import hash check no longer rejects valid files (canonical JSON now matches `JSON.stringify` exactly)
- No more false "this may not run" warnings for device-state triggers and monitor-backed capabilities
- Row subtitles wrap to two lines instead of truncating mid-word

## [0.10.0]

### Added
- Full Glyph SDK integration: per-device channel maps, structured frames, marquee, 15+ visual presets
- 6 new Glyph action types (GlyphAnimate, GlyphProgress, GlyphText, GlyphScrollingText, GlyphPreset, GlyphTurnOff)
- GlyphToy service for Glyph Matrix toy integration (Phone 3, 4a Pro)
- NotificationListenerService for notification triggers
- PhoneStateReceiver for call/SMS triggers
- ConnectivityReceiver for WiFi/BT state triggers
- GeofenceMonitor + GeofenceReceiver for location-based triggers
- PersistentMonitorService for continuous battery/screen monitoring
- UsageStatsMonitor for foreground app detection
- Automation edit flow (edit/{id} route, pre-populate name)
- Permission detection: UsageAccess, LocationPermission at runtime
- Settings UI: Usage Access + Location permission rows
- CapabilityResolver: real permission checks for TRIGGER_APP_OPENED, TRIGGER_GEOFENCE
- AndroidStateProvider: WiFi SSID, Bluetooth device name, foreground app
- TriggerEvent.GeofenceTriggered + TriggerMatcher support
- play-services-location dependency
- Comprehensive TODO.md, TASKS.md, PROGRESS.md, DECISIONS.md

### Fixed
- SwipeToDismissBox API replaced with delete icon on automation cards
- Smart cast issues in RealActionExecutor resolved with local variables
- DEVICE_25131 unresolved reference mapped to Glyph.DEVICE_25111
- Compilation errors: ToyService uses provider instead of direct SDK
- PhoneStateReceiver: ACTION_PHONE_STATE string constant
- AutomationNotificationListener: unused import removed
- store.get() returns Automation? not Flow — direct call in ViewModel
- Lint: @SuppressLint for BLUETOOTH_CONNECT/VIBRATE
- Lint: ObsoleteSdkInt check removed (minSdk 28 >= O 26)
- Lint: USE_EXACT_ALARM moved to app manifest (targetSdk 36)
- Lint: camera uses-feature declared optional

### Changed
- nothing-integrations: SDK jar changed from implementation to api for transitive visibility
- Nothing OS visual system: outlined monoline icons, theme-aware dot grid, 16 dp bottom sheets, zero-elevation cards, custom checkbox/radio, text-based manual RUN action
