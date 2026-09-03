# Changelog

## [Unreleased]

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
