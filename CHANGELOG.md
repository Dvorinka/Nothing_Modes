# Changelog

## [Unreleased]

### Fixed
- Hotspot toggle never actually switched the AP: the action wrote `settings put global wifi_ap_state`, a legacy state mirror nothing reads — the shell reported success while the hotspot stayed off. The action now toggles the real tethered hotspot inside the Shizuku user service (shell uid) via `WifiManager.startTetheredHotspot`/`stopSoftAp`, keeping the device's saved SSID and passphrase. Without Shizuku it still opens the tether settings panel.
- Data Saver and master auto-sync toggles had the same false-success defect — `settings put global data_saver`/`auto_sync` write dead keys nothing reads. Data Saver now runs `cmd netpolicy set restrict-background` via Shizuku, auto-sync calls `ContentResolver.setMasterSyncAutomatically` inside the user service (shell uid holds WRITE_SYNC_SETTINGS). Both now report a real failure instead of a phantom success when Shizuku is missing.
- Data Saver conditions never fired: the state monitor read the same dead `data_saver` key. It now reads `ConnectivityManager.restrictBackgroundStatus` (with `ACTION_RESTRICT_BACKGROUND_CHANGED` + poll for transitions), snapshot restore routes through the Data Saver action instead of a generic dead write, and the app manifest gains ACCESS_NETWORK_STATE so the real read doesn't throw.
- Mobile-data snapshots read the stale `global.mobile_data` key, which per-subscription builds (API 26+) store under `mobile_data<subId>` — the reader now uses `TelephonyManager.isDataEnabled`, matching the device-state monitor.
- "Bluetooth connected" conditions could never match: the state reader counted *paired* devices as connected and compared the trigger's device name against the phone's own adapter name. It now asks BluetoothManager for the real connected set (headset/A2DP/GATT/LE-audio profiles) and reports the connected remote device's name.
- "Stay awake while charging" did nothing on wall chargers: the action wrote plug bitmask 2 (USB only); it now writes 7 (AC + USB + wireless), matching the platform dev option.
- Airplane-mode snapshot restores wrote the `airplane_mode_on` key, which doesn't drive the radio toggle — restores now go through the same `cmd connectivity airplane-mode` path as the action.
- "Clear notifications" reported success while only cancelling the app's own posts (`NotificationManager.cancelAll` can't touch other apps). The action now goes through the bound NotificationListenerService and reports permission required when notification access isn't granted.

## [0.19.7]

### Added
- Geofence deactivation policies: a location mode can now end manually (default), on the opposite edge ("End when I leave" / "End when I arrive"), or gated on live position. A gated end checks where the phone actually is — "inside" or "outside" the circle, your choice — before deactivating.
- Pending-end notification with actions: when a gated end can't run yet you get a heads-up with "End now" (always wins) and an inline snooze — type any number of minutes, or let the configured interval (default 15) re-check automatically. Re-checks are alarm-scheduled and bounded so a dead GPS can't loop forever.

### Fixed
- Geofence modes silently never fired in the background: registration only verified foreground location, and GMS happily accepts a fence it will never deliver without "Allow all the time". The geofence config screen now shows a background-location requirement card that deep-links to system settings (Android 11+), and the monitor logs when a fence is registered without it.
- Lifecycle geofences now register the inverse edge too — an ENTER mode ending on EXIT actually receives the EXIT event (previously a geofence mode could never auto-end at all), and the initial-trigger mirrors the registered edges so a fence registered while already inside/outside reports that immediately.

## [0.19.6]

### Fixed
- Play in-app updates downloaded but never installed on their own: the flexible sheet dismissed after confirmation and no install-state listener was registered, so `completeUpdate()` only ran on the next cold start — the app kept running the old version until a manual relaunch. Install state is now observed; the update applies as soon as the download finishes (Play's install overlay + restart), the listener re-arms on resume mid-download, and a stalled Play consent pause (e.g. metered data) relaunches the sheet instead of silently waiting.

## [0.19.5]

### Fixed
- Settings restored while the phone was locked were silently reverted by Nothing OS at the next unlock — most visibly Extra Dim, which a mode ending overnight switched off correctly only for the platform to switch it back on the moment the user unlocked. Successful setting writes executed under the keyguard are now queued and replayed on `USER_PRESENT`, after the platform's re-assert, so the mode's final state wins. Covers snapshot restores and explicit end actions for all settings (Extra Dim, DND, brightness, auto-brightness, and the rest).
- Mode-end failures are no longer invisible: snapshot-restore, end-action, and unlock-replay failures are recorded in the activity log (`ACTION_FAILED` with the action and reason), and background cancellation propagates instead of being swallowed.

## [0.19.4]

### Fixed
- Armed routines surfaced as the system "next alarm" in the status bar, quick settings, and ambient display while remaining invisible in the Clock app — a phantom alarm that couldn't be traced. `setAlarmClock` marks alarms as user-facing; routines now use `setExactAndAllowWhileIdle` — equally exact and Doze-safe, without the publication. Alarms re-arm through the new path automatically on app update.

## [0.19.3]

### Fixed
- Mode detail "When it ends" rows led with the mode-time action and buried the end behaviour in the subtitle. The title now shows the outcome (explicit end action, reverts to previous value, or keeps the mode's value) with "While active: …" as context.
- Crash on background sticky restarts: `PersistentMonitorService` threw `ForegroundServiceStartNotAllowedException` when the OS restarted it while the app was backgrounded. It now stops gracefully and returns on the next eligible start.
- Crash in the app pickers when a package exposes multiple launcher activities — duplicate LazyColumn keys. Apps are now deduplicated by package name.
- Deleting a mode from the detail screen had no confirmation — a single mis-tap destroyed it permanently. Now gated behind a confirm dialog.

## [0.19.2]

### Fixed
- Time and window triggers silently stayed inexact (up to a 1-hour slide under Doze) when `SCHEDULE_EXACT_ALARM` was granted after the alarms were already armed. The app now detects the grant on process start and on returning to the app, and re-arms all armed automations via `setAlarmClock`.
- Pending-unlock alert could not wake the screen: `ACQUIRE_CAUSES_WAKEUP` wakelocks hit the `TURN_SCREEN_ON` appop, which Nothing OS locks for background apps. The notification now also carries a full-screen intent (the sanctioned wake-over-lockscreen path); when `USE_FULL_SCREEN_INTENT` isn't granted it degrades to the existing heads-up.

## [0.19.1]

### Fixed
- Startup crash on Play builds: `PlatformInAppUpdate` was constructed as an activity field, so `AppUpdateManagerFactory.create()` called `getApplicationContext()` before attach — NPE in `MainActivity.<init>` on every cold start. Now lazy; first use is `check()` in `onCreate`, still before STARTED for `registerForActivityResult`.

### Added
- Post-crash report prompt: crashes are always queued locally; on next launch users without crash reporting enabled see the error and can send or discard it explicitly. Queue capped at 10 reports.

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
