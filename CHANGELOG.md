# Changelog

## [Unreleased]

### Added
- New `NothingPickers` components: wheel time picker, calendar date picker, timezone field (device default + searchable list + raw-id advanced input), large neutral day selector
- Settings: Manage button on every permission row so access can be granted and revoked from the app; restricted-settings guidance for sideloaded installs (App Info → Allow restricted settings)
- Settings: full Shizuku flow — Get Shizuku (Play/GitHub), Open Shizuku, Authorize, per-status guidance
- Builder: Save / Discard / Cancel exit dialog, Advanced section with Enabled toggle + Priority, Enabled is now persisted per routine
- Catalogs: multi-select with ADDED state and sticky Done bar (no auto-close after one pick)
- Trigger config: trigger-type picker dialog (grouped list) replaces the inline chip grid

### Fixed
- System back and back-swipe gesture now show the unsaved-changes dialog instead of silently leaving the builder
- TimeWindow trigger/condition use real time pickers and a timezone dropdown instead of raw text fields
- Long TimeWindow descriptions no longer include the timezone when it matches the device zone

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
