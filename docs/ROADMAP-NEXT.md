# Nothing Modes — Next Work Plan

> Compiled 2026-09-07 from user requirements + on-device analysis.
> Device under test: **Nothing Phone (3) — A024 "Metroid", Android 16 (SDK 36)**.
> This is the work queue; see "Completed" below for what has already shipped.

---

## Completed (shipped)

### Website / community library
- [x] Replace emoji icons with real Material Symbols on the library and submit pages.
- [x] Link website icon set with the Android in-app icon selector via `landing/icons.json` + sync script.
- [x] Improve Glyph preview visuals: square grid cells, faint inactive matrix, larger 320×320 modal, `requestAnimationFrame` playback.
- [x] Add icon + background colour picker to the submit form, with live preview.
- [x] Deploy landing site to Vercel and type-check the API layer.
- [x] Add admin delete for submissions and crash reports (`/api/delete` + admin UI).
- [x] Show delete buttons on `/library` when the admin token is present.
- [x] Rich template/mode detail modal on click: large icon, trigger, conditions, actions, capabilities, description, download.
- [x] Add `/api/preview` so modal views do not inflate the download counter.

### Android
- [x] Add `bolt` icon to the in-app icon catalog so the seed template resolves correctly.
- [x] Build and install the debug APK on the Nothing Phone 3; confirm launch.

## Completed this session

### Device validation (Nothing Phone 3)
- [x] `:app:assembleGithubDebug` builds clean; `:engine-core:test` and `:capabilities:test` pass.
- [x] Debug APK installed and launched on the connected Nothing Phone 3.
- [x] Torch, notification, screenshot, Wi-Fi/Bluetooth, vibration, flashlight, and Glyph debug actions verified on-device.
- [x] SMS action `send_sms` sent to the user-approved number; executor returned `Success`.
- [x] Phone/SMS trigger pipeline wired end-to-end:
  - `DebugActionReceiver` dispatches `phone_state` (ringing/idle) and `sms_received` events.
  - `AutomationService` routes them through `TriggerEvent.PhoneStateChanged`.
  - Engine matches `Trigger.PhoneState` for `INCOMING_CALL` and `SMS_RECEIVED` on the user-approved number.
  - Resulting `show_notification` actions posted and played sound on device.
- [x] Debug `manual` broadcast type added to `DebugActionReceiver` for testing any automation by id.

### Website / community library
- [x] `library.html` now falls back to `seed.json` for local static-server previews (`localhost:8888`) when `/api/library` is not available.
- [x] Client-side search, type tabs, sort, and capability chips work against the local seed fallback.
- [x] Template/glyph detail modal uses the local `_payload` without requiring `/api/preview` or `/api/item`.
- [x] Favicon link added to `library.html` to silence the `favicon.ico` 404.

## Completed this pass (2026-09-08)

### Remaining blocks
- [x] **Execution journal latency display** — added `latencyMillis` to `AuditEvent`/`AuditEntity`, Room migration 1→2, engine capture, and `ExecutionLogScreen` display. Verified non-zero latency in the audit log.
- [x] **F-Droid index generation** — fixed `repo_url` and `mirrors` config, generated signed `index-v1.json` from release APK, added `fdroid-local.sh` and `fdroid-release.sh`, `.gitignore` for secrets and built artifacts, removed `config.yml` from tracked files with `config.example.yml`.
- [x] **Calendar trigger merge into Time/Day** — added a "Source" selector in `CustomTimePicker` and `CalendarEventContent` to switch between clock and calendar sources; the standalone Calendar catalog entry is already gone.
- [x] **Glyph Studio consolidation** — the saved/community/import sections already exist; added an "Open Glyph Museum" link from the Import section.
- [x] **Live community e2e** — verified `/api/library?type=glyph`, `/api/item`, and `/api/preview` respond with approved glyph items on the live deployment. `CommunityApi` and `GlyphEditorScreen` already consume these endpoints.

## Completed in this follow-up pass (2026-09-08)

### Notification branding
- [x] Added `ic_notification.xml` (Nothing dot-grid logo) to `automation-android`.
- [x] `ModeNotificationHelper`, `AutomationService` foreground, and `PersistentMonitorService` use `R.drawable.ic_notification` as small icon and the app logo as large icon.
- [x] `RealActionExecutor.showNotification()` also uses `ic_notification` and the app logo via runtime resource lookup.
- [x] Verified on device: the persistent monitor notification and deep-link import toast show the Nothing Modes dot-grid icon.

### Website-to-app one-tap import
- [x] `MainActivity` handles `nothingmodes://import?type=<template|glyph>&id=<uuid>` deep links.
- [x] `AndroidManifest.xml` declares the `nothingmodes` scheme with `BROWSABLE` + `DEFAULT` categories.
- [x] Deep link path fetches the item through `CommunityApi.fetchItem()`, then uses `ImportExportService.import(..., overwrite = true)` so the community copy always installs/updates.
- [x] Verified on Nothing Phone 3: `adb shell am start -a VIEW -d nothingmodes://import?...` opens the app, fetches the live item, and the toast "Imported 1 mode(s)" appears. The routine is listed immediately.
- [x] `landing/library.html` now renders an **OPEN IN APP** button on Android user agents; desktop/unknown agents keep **DOWNLOAD JSON**.
- [x] `landing/library.html` card click opens the detail modal; the modal action button switches by user agent too.
- [x] `landing/index.html` cache-busting (`?v=2`) and `/seed.json` fallback now render the home-page library preview correctly instead of "LIBRARY UNAVAILABLE".
- [x] Website copy updated to explain: "On Android, tap OPEN IN APP to install directly into Nothing Modes. Elsewhere, download the JSON and import it in the app."

### Website visual review
- [x] `index.html`, `library.html`, `submit.html`, `privacy.html`, and `admin.html` loaded in Playwright; no console errors beyond the expected local `/api/library` 404 (handled silently by `/seed.json` fallback).
- [x] `library.html` modal displays the icon, trigger/condition/action summary, required capabilities, and download/open actions.
- [x] `submit.html` icon picker is visible and populated with real Material symbols.
- [x] Dark theme is the default render for the site in the test harness; light mode is supported through the existing toggle.

### Privacy / security
- [x] Repository re-scanned for the user's phone number; no matches in working tree. `docs/DEVICE-TEST-MATRIX.md` and `docs/ROADMAP-NEXT.md` use `[user-approved number]` placeholders.
- [x] Test screenshots and pulled device files (`/tmp/nm3.db`, `/tmp/notif*.png`, `/tmp/deeplink*.png`) deleted.

### Messaging-channel monitoring — assessed
- [x] Documented assessment: modern Android blocks direct access to third-party messaging content. The viable path is a generic **notification received from app** trigger using `NotificationListenerService` with package + title/body text matching (e.g. Telegram, WhatsApp, Instagram when they post notifications). Accessibility scraping and unofficial API hooks are not appropriate.
- [x] Implement the generic "notification from app" trigger in the app catalog and document supported/unsupported cases.

### SMS/call tests — confirmed
- [x] SMS send to `[user-approved number]` delivered; the user received the test messages.
- [x] `SMS_RECEIVED` trigger matched on the user-approved number and posted the notification.
- [x] `INCOMING_CALL` trigger matched through the service-path simulation (`phone_state` debug broadcast).
- [ ] Real carrier-delivered incoming call still pending a second endpoint or call-forwarding; service-path simulation is the best verification available from a single device.

---

## 0. Verified on-device findings (bugs & intel)

### Save routine button — investigated, works mechanically, UX is the bug
Walked the full create flow on the connected Phone 3 (screencap + taps):
`+` → builder → Add action → Wi-Fi → sheet → Done → "ADD 1 ACTION" → CREATE AUTOMATION → routine saved and listed.

Real defects found and fixed in this pass:

1. **Action is configured twice.** `ActionCatalogScreen` opens `ActionConfigSheet` on select, then `CustomAutomationBuilderScreen` re-opened the *same* sheet per added action via `pendingActionIndices`. Fixed: removed the `pendingActionIndices`/`pendingConditionIndices` re-open queue in the builder; catalog results are now added once.
2. **Catalog "Done" button silently returned with zero actions.** If `selected` was empty, the bar still read "Done" and popped back, leaving the save button disabled with no explanation. Fixed: `NothingBottomActionBar` is disabled and shows "Select at least one action/condition" when empty.
3. **`save()` had zero error handling.** Fixed: wrapped `store.save()` and widget refresh in `runCatching`, exposed `saveError`, and showed a `Snackbar` on failure.
4. **Disabled save gave no reason.** Fixed: added a subtitle under the bottom bar: "Add at least one action to save." when `state.actions.isEmpty()`.
5. Dead code: `CreateAutomationScreen.kt` is not in the nav graph at all. **Pending** — remove or wire.
6. **Navigation route JSON not URL-encoded.** `Routes.triggerConfig/conditionConfig/actionConfig` passed raw JSON into the route query string; characters like `"`, `{`, `:` can break Compose Navigation matching. Fixed: use `URLEncoder.encode(..., "UTF-8")` before navigating.
7. **"Add at least one action to save" shown permanently.** Fixed: removed the red subtitle and enabled the save button. A mode can now be saved with zero actions, and the hint no longer appears.

### Device intel (Phone 3, adb)
- Model `A024` / `Metroid`, Android 16, SDK 36, Nothing OS.
- **Shizuku installed** (`moe.shizuku.privileged.api`) — privileged actions viable.
- Permissions already granted to `.debug`: READ_CALENDAR, SEND_SMS, RECEIVE_SMS, CAMERA, RECORD_AUDIO, READ_PHONE_STATE, FINE+COARSE location, BLUETOOTH_CONNECT, POST_NOTIFICATIONS, WRITE_SETTINGS (allow).
- Notification listener: **enabled** (`AutomationNotificationListener`).
- Accessibility service: not enabled — **not needed**: LockScreen verified working via Device Admin (see below).
- **Lock screen: VERIFIED WORKING on this device.** `NothingDeviceAdminReceiver` is an active device admin with `force-lock` declared; fired via debug broadcast → `lockNow()` → `Success`, screen went to Dozing. Caveat: after an admin lock, Android requires PIN once before fingerprint resumes (platform security rule, unavoidable without an accessibility-service path).
- Play-flavor compliance gap: `NothingDeviceAdminReceiver` is declared in the `automation-android` library manifest, so it ships in **play** builds too — the compliance doc says it must be play-excluded. The action itself is correctly gated by `FeatureFlags.enableLockScreen` (play returns `Unsupported` silently — which is also why a play-flavor debug install appears to "do nothing" on lock screen).
- 8 calendars present (personal, work, holidays) — calendar trigger has real data.
- Glyph stack present: `com.nothing.glyphmatrix`, `com.nothing.communitywidgets`, `com.nothinglondon.toys`, plus third-party: **Glyph Museum** (`com.pauwma.glyphmuseum`), **GlyphBeat** (`com.pauwma.glyphbeat`), **SmartGlyph** (`com.voidtechstudios.smartglyph`), **GlyphEyes** (`com.example.glypheyes`).
- `com.nothing.glyphmatrix` itself crashes in logcat (WaterfallToyService unbind bug) — Nothing's bug, not ours; note for support noise.
- Crash reporting: opted in on debug install, queue dir empty, endpoint live (405 on GET = route exists).
- **Shizuku action matrix verified on device**: added `ShizukuActionMatrixTest`; with Shizuku authorized, `SetBluetooth`, `SetMobileData`, `SetAirplaneMode`, `SetDataSaver`, `SetHotspot`, `SetAutoSync`, `SetLocationMode`, `SetAlwaysOnDisplay`, `SetBatterySaver`, `SetExtraDim`, and `WriteSetting` report `Success`; `SetNfc` falls back to `NeedsUserAction` (system panel) as expected.
- **USB/MCP agent wired**: added debug-only `ModeControlReceiver` + `tools/mcp-usb.py`; host can `list/get/run/delete` modes and `save` JSON over `adb`. Verified `list` and `run` on the connected device.
- Builder IF row → `TriggerConfigScreen` and discard-dialog DISCARD button could not be activated with `adb input tap` in this pass. May be a coordinate/click-target issue on the test harness; needs manual verification or scrcpy to confirm.

### Modes vs Routines — answer
In code they are already the same object. `type` is derived at save:
`TimeWindow` trigger → `MODE`, anything else → `ROUTINE`
(`CustomAutomationBuilderScreen.kt` ~line 300). The only real difference is semantics:
a mode has a start **and** end, so the engine snapshots state and can restore it
(`Engine.kt` `snapshotSettings`/`restoreSnapshots`). A routine is one-shot: event → actions.

**Recommendation: keep ONE concept ("routine" or just "automation").** The "After it ends"
section appears automatically whenever the chosen trigger has an end (time window,
and later: "until condition stops", "until second trigger"). No separate mode type needed.

---

## 1. Mode builder rework ("If / Then / After") — DONE

- [x] **Rename "routines" → "modes" everywhere.** Builder copy uses "NEW MODE", "EDIT MODE", "CREATE MODE"; `AFTER IT ENDS` label in place. `AutomationListScreen` still has a "Routines" filter tab — see §5.
- [x] **Icon personalization**: in the icon picker, allow custom **icon tint color** AND **background color** (defaults stay white icon on dark gray/black). `Automation.iconTint` added; `IconColorPickerSheet` supports search + background + tint.
- [x] Rename trigger section header to **"IF"**. **"ONLY IF"**, **"THEN"**, and **"AFTER IT ENDS"** are now the builder sections.
- [x] Add a third section **"AFTER IT ENDS"**. Visible when the trigger is a `TimeWindow`; lists restorable actions with per-action restore toggles.
- [x] Per-action "after" policy on every restorable action: **Restore previous value** (default, snapshot before run) vs **Keep new value**. `Action.canRestore`/`withRestore`/`supportsRestore` extended to Wi-Fi, Bluetooth, mobile data, flashlight, AOD, NFC, hotspot, location mode, auto-sync, ringer.
- [x] Engine: snapshot *before* applying each action; `Engine.restoreActionFor` maps snapshot keys back to real `Action`s, including boolean toggles, `LocationMode`, `VolumeStream`, `RingerMode`, and ordinary `WriteSetting` keys. Glyph and flashlight keep explicit caveats.
- [x] Non-windowed routines: "after" means "when a second run/reverse trigger fires" — decide scope: for now only windowed triggers get the section. Decided: AFTER is only for `TimeWindow` in this pass.
- [x] Default trigger = **Manual**. New builder state defaults to `Trigger.Manual`.
- [x] **Remove timezones everywhere** — builder no longer exposes `tz`; `CustomTimePicker` uses the device default; `Trigger.Time`/`TimeWindow` still carry `tz` for the scheduler.

### Time trigger → "Time / Day"
- [x] Rename "Time" → **"Time / Day"**. `TriggerConfigScreen` label updated.
- [x] Recurrence: daily / **weekly (multi-day select)** / **monthly (multi-day-of-month select)** / **yearly (multi-month + multi-day select)**. Implemented in `CustomTimePicker` using comma-separated cron fields.
- [x] Quick actions: "Weekdays", "Weekend", "Every day", "1st of month", "15th", "Last day" (true `L` cron marker; engine matcher uses month length).
- [x] True "last day of month" / "whole month" / multi-month quick presets and per-month day lists. Cron can't express "last day" cleanly; requires an engine evaluator.

### Calendar trigger — merge into Time/Day
User is right: asking for a `calendarId` string is backwards.
- [x] Merge into Time/Day config as a **"From calendar"** source option.
- [x] On select: request `READ_CALENDAR` **in context** (see §7), then show a calendar picker (default: **all calendars**, optional restrict to one).
- [x] Show upcoming events from the chosen calendars so the user can pick a specific event or a title pattern. Keep `calendarId`/`titleMatch`/`direction` model fields — they're fine; it's purely a UX rewrite.
- [x] Remove the standalone "Calendar" trigger type from the catalog (fold into Time/Day).

### New triggers requested
- [x] **Torch/flashlight active** — `CameraManager.registerTorchCallback` (no permission needed for state). `Trigger.TorchState(on: Boolean)` is modeled, in the trigger catalog, and verified on-device.
- [x] **Device connects** — `Trigger.BluetoothDevice` has a bonded-device picker (`BondedDevicePickerDialog`) in the trigger config.
- [x] **Wi-Fi active / connected** — `Trigger.WifiConnected(ssid)` has an SSID picker: "Use current network" (from `WifiManager`/`ConnectivityManager`), manual entry, and blank = any network.
- [x] **SMS received — specific contact or custom number + text match** — `Trigger.PhoneState(SMS_RECEIVED, number, textMatch)` is modeled; the in-app UI has phone/SMS event, number, text-match fields, and a `READ_CONTACTS`-gated contact picker.
- [x] Template: **"Locate my phone"** — SMS "LOCATE" keyword → flashlight on, mobile data on, high-accuracy location on, reply SMS. Shipped as a built-in template with description.
- [~] **Condition parity pass** — Added generic `Condition.BooleanState`, `Condition.NumericState`, `Condition.AtLocation`, `Condition.EventActive`, and `Condition.NotificationPresent` with a closed `StateKeys` registry. Covered: device locked, Wi-Fi/Bluetooth/mobile data/hotspot/AOD/DND/torch radios, brightness, refresh rate, screen timeout, at-location radius, calendar event active, notification present. Done.

---

## 2. Icon picker fix — DONE

`IconColorPickerSheet.kt` now uses `LazyVerticalGrid(columns = GridCells.Fixed(6))` with a bounded sheet height and search filtering, so all icons are scrollable and reachable.

- [x] Replace `FlowRow` + `heightIn` clip with a `LazyVerticalGrid(columns = 6)` bounded by height — all icons reachable by scroll.
- [x] Fill the last row: `GridCells.Fixed(6)` and the `LazyVerticalGrid` measure policy avoid the dead gap.
- [x] Keep search as-is (it already searches all icons).

---

## 3. Action catalog polish

Confirmed on device: config sheets work, but labels/options need work.

- [x] **Toggle actions**: label = feature name only ("Wi-Fi", not "Wi-Fi enabled"); toggle styled Nothing-red with **ON/OFF text** next to it. `BooleanRow` now strips the `enabled` suffix and shows ON/OFF.
- [x] **Add Condition parity**: condition catalog now behaves like action catalog — tapping a condition opens `ConditionConfigSheet`, configured conditions appear in a "Selected" list, and each can be edited or removed before returning.
- [x] **Screen timeout**: dropdown of presets (15s/30s/1m/2m/5m/10m/30m/never), not free input.
- [x] **Always-On Display**: off / tap-to-show / always / schedule modes mapped to `doze_always_on`, `doze_tap_gesture`, and `doze_*_hour/minute` secure settings.
- [x] **Volume**: show all streams at once (media, ring, alarm, notification) as labeled sliders — no dropdown.
- [x] **Vibrate**: dropdown with duration presets (Short/Medium/Long/1 second).
- [x] **Ringer mode**: explanation added below the radio options.
- [x] **Descriptions on every action** — catalog rows show a one-line hint; action config sheets show the same note.
- [x] **NFC**: use an NFC glyph/icon (`Icons.Outlined.Nfc`), not Bluetooth.
- [x] **Location mode**: in-app description added.
- [x] **Group related items**: hotspot now in Connections, Auto-rotate and Screen rotation grouped.
- [x] **Refresh rate**: preset dropdown populated from `Display.getSupportedModes()` (API 23+), with custom Hz input fallback. Used in `ActionConfigSheet` and `ActionConfigScreen`.
- [x] **Auto-sync**: description added.
- [x] **Lock screen**: description added in the action config sheet.
- [x] **Screenshot + experimental actions**: `CapabilityDetector` now reads active device admin + MediaProjection; `CapabilityResolver` gates `ACTION_LOCK_SCREEN` and `ACTION_TAKE_SCREENSHOT`. Catalog rows for lock screen and screenshot show "Detected: may not work on this device" with an "Override: try anyway" toggle. Both actions are now data classes with a `force` flag; the executor refuses to run unless `force = true` or the capability is satisfied.
- [x] **Copy text**: removed from catalog.
- [~] **Open URL**: description and URL input wired. Optional browser/app picker not yet implemented.
- [x] **Launch app**: `AppPicker` queries installed launchable apps; `LaunchApp` now takes a package list and `MultiAppPicker` shows icons, package names, search, and checkboxes.
- [x] **Open settings**: `SettingsScreen` enum expanded; app-details package input and description note added.
- [x] **Wait**: preset dropdown (1s/5s/10s/30s/1m/5m) + Custom.
- [x] **Send SMS**: number and message inputs wired; contact picker uses READ_CONTACTS in context; on-device send test completed.
- [x] **Write setting**: presets (animation scale, font scale, show taps) and a shared `WriteSettingSelector`; custom key/value hidden behind the "Custom" preset.

### New actions requested — feasibility triage
| Action | Path | Notes |
|---|---|---|
| Flashlight | exists (`SetFlashlight`, CameraManager torch) | works |
| GPS / location mode | exists (`SetLocationMode`, Shizuku) | works w/ Shizuku |
| Airplane mode | exists (Shizuku) | works w/ Shizuku |
| Dark/white mode | exists (`SetDarkMode`) | works |
| Do not disturb | exists (`SetDnd`, needs policy access — granted) | works |
| Alarm (set/show) | `AlarmClock` intents | only "show alarms"/"set alarm" intents — no silent API |
| Screen record | MediaProjection — per-capture consent each time | poor fit; experimental-flag it |
| Battery share / reverse charging | Nothing OS specific — investigate `com.nothing.*` APIs/Settings.Global keys | unknown |
| Night light + intensity | `Settings.Secure.NIGHT_DISPLAY_*` via Shizuku | feasible |
| Quick Share / Nearby Share | system intent only | launch action, not toggle |
| Color inversion / color correction | `Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED` / daltonizer via Shizuku | feasible |
| Cast screen | `MediaRouter`/Cast intents — app-level only | launch intent at best |
| QR scanner | launch camera/Nothing QR tile intent | app quick action |
| Mic/camera access toggles | `SensorPrivacyManager` (`setSensorPrivacy`) — needs permission/Shizuku | feasible w/ Shizuku |
| Data saver | exists (Shizuku) | works |
| Work apps / work profile | `DevicePolicyManager`/`cross-profile` intents — only if a work profile exists | conditional |
| One-handed mode | `Settings.Secure.ONE_HANDED_MODE`? OEM-specific — test on device | investigate |
| Font size | `Settings.System.FONT_SCALE` via WRITE_SETTINGS | feasible |
| Nothing quick actions (Essential Recorder, Focus mode, Live Caption, Song Search/Now Playing, Glyph Timer, TV remote) | These are Nothing OS app features — only available as **app/shortcut/tile intents**, not APIs. Catalog them as "launch" actions where a stable intent exists; mark the rest unavailable | partial |

### "After" availability — full coverage
Every action gets an `afterEnd` policy: `RESTORE_PREVIOUS` (default where restorable) / `KEEP` / `RUN_ACTION` (future: arbitrary follow-up). Model: add `onEnd: EndPolicy` to `Action` or a per-index map on `Automation`.

---

## 4. Glyph consolidation — the big one

Current: **12 separate glyph action types** (set_glyph, glyph_matrix, preset, text, scrolling text, icon, number, countdown, progress, animate, music, app). Agreed: too many.

**Target: 3 glyph actions in the catalog**

1. **"Glyph" (Glyph Studio action)** — one entry point. Opens Glyph Studio to pick:
   - my saved designs, presets, matrix frames, icons, text/scrolling text, numbers, countdown, progress, animations, music visualizer
   - import: JSON paste/file, community library (see §5), Glyph Museum link
   - config: duration, brightness, loop, "always-on" (if system allows — else prompt the user to enable the toy once, deep-link to system setting)
   - Internally it can still map to the existing action types — the model can stay; the UX collapses to one picker. Or introduce `Action.GlyphShow(descriptor)` that wraps a stored glyph-design ID.
2. **"Glyph off"** — stays separate (genuinely useful).
3. **"Glyph flashlight"** — torch + glyph matrix all-on at max brightness. New action combining `SetFlashlight` + full-white `SetGlyphMatrix`; also useful alone ("supportive flashlight").

**Glyph Studio screen rework** (`GlyphEditorScreen.kt` / `GlyphPreviewScreen.kt`):
- [~] Shrink the top/preview section so the scrollable content gets the majority of the screen.
- [x] Remove "Registered toys" read-only list (info noise) — not present in current build; verified.
- [x] Remove "System toy table diagnostics" — diagnostics were already removed.
- [x] Remove "Sleep mode glyph preset" selector — not present.
- [x] Remove the "Hardware detected → TURN OFF" card — not present.
- [x] Remove "Open toys / Always-on / Timeout" buttons that no-op — replaced by a single "Open system Glyph settings" deep link in `GlyphPreviewScreen`.
- [x] Storage tab (my designs), Community tab (website library), Import (JSON/file) — sections in `GlyphEditorScreen` reorganized and link to Museum added.
- [x] Link out to Glyph Museum app (installed on this device — `com.pauwma.glyphmuseum`).

---

## 5. Community sharing — templates + glyphs (website + app + admin)

**Status: deployed and wired end-to-end (`ade9c6f` and earlier).**

Built:
- `POST /api/share` — validate, static-analyze, redact PII (numbers/SSIDs/coords/calendar ids), rate-limit 10/day, dedupe by content hash, email admin via Resend.
- `GET /api/library` + `GET /api/item` — public approved index + download; never exposes email/ip/moderation internals.
- `GET /api/preview` — public item payload for the modal, without bumping downloads.
- `GET /api/submissions` + `POST /api/moderate` — admin queue, also the AI-agent contract (`landing/api/AGENT.md`); author emailed on decision.
- `POST /api/delete` — admin delete for submissions and crash reports.
- `landing/api/lib/analyze.ts` — static analyzer: action allowlist (`write_setting` banned), https-only URLs, no real SMS numbers, bounds on waits/animation/text, verdicts ok/flag/reject.
- `landing/library.html` — public catalog with real Material icons, Glyph animation preview, rich template detail modal, and admin delete when signed in.
- `landing/admin.html` — submissions tab with approve/reject/delete; crash reports with per-report delete.
- `landing/submit.html` — submission form with icon + background picker.
- `data/community/CommunityApi.kt` — app client; template catalog browses + publishes, glyph editor browses/imports + publishes.

Still open in this feature:
- [x] Live e2e smoke: submit via `POST /api/share` returns 201, `GET /api/library` and `GET /api/item` return approved items on live deployment.
- [x] Env check: `DATABASE_URL` required at handler entry; `RESEND_API_KEY`/`CRASH_NOTIFY_EMAIL` are best-effort (email functions skip when missing); `ADMIN_TOKEN` optional.
- [x] Content-hash verification on import (JSONB reorders keys — needs canonical serialization first).

**Creator profile**: **handle** is the public attribution. Email is asked optionally and kept private (used only for the decision notice). Optional GitHub link shown publicly. Handle lives in Settings → Creator Profile and is pre-filled when publishing.

### Backend (Vercel + Neon, same pattern as crash.ts)
- [x] `POST /api/share` — submit a template or glyph pack: `{type: "template"|"glyph", payload, handle, title, description}` → `shared_items` table with `status: pending|approved|rejected`.
- [x] `GET /api/library` — public endpoint: approved items only, sanitized (no emails, no raw payloads beyond what's needed to render).
- [x] `GET /api/submissions` + `POST /api/moderate` — bearer-token admin queue (was `/api/admin/pending` + `/api/admin/moderate` in the sketch).
- [x] `POST /api/delete` — admin delete for submissions / crash reports.
- [x] `GET /api/preview` — public item payload without bumping downloads.
- [x] Email notify on new submission (Resend, same as crash notify).
- [x] Auto-generated description fallback: derive "what it does" from the automation JSON when the author leaves it blank.

### Security review pipeline (user's requirement — non-negotiable)
- [x] **Static analysis on ingest** (server-side, in `share.ts` / `analyze.ts`): reject payloads containing `send_sms` to non-e.164 numbers, `open_url` to non-https, `write_setting` to non-allowlisted keys, `launch_app` to known-bad packages, unbounded `wait`, scripts/`content://`/`file://` URIs. Maintain an allowlist of action types for shared content; anything else → auto-reject.
- [x] **Admin review queue** in `admin.html` (extend): view rendered summary + raw JSON, approve/reject/delete.
- [x] **AI-agent endpoint**: documented in `landing/api/AGENT.md`; agents can pull pending and POST decisions.
- [x] **Signing/integrity: server stores a content hash; app verifies downloaded payload hash before import.** `ImportExport.preview`/`import` accept `expectedContentHash` and reject on `jsonSha256` mismatch; `TemplateCatalogScreen` passes `item.contentHash` through.
- [x] Rate-limit submissions per handle/IP; payload size cap (e.g. 64 KB).
- [x] Privacy: never publish device identifiers; strip `number`, `ssid`, `pkg`, geofence coords from shared templates (or mark as "user fills in on import" placeholders). This is important — a shared "SMS from mom" template must not leak the author's contacts.

### App side
- [x] "Browse Templates" → tabbed: **Local / Community (remote)**. Search across both.
- [x] Publish flow: routine overflow → "Share" → attach handle → POST `/api/share` → "Pending review" state.
- [x] Same for glyphs: "Share design" from Glyph Studio → same pipeline.
- [x] Import from community: fetch `GET /api/library` → preview → import via existing `ImportExportService`.
- [x] Website `index.html`: add a **Library** section rendering the public endpoint.

### Out of scope per user: GitHub / Play / Android-app integrations unchanged.

---

## 6. UI/UX fixes

- [x] **Overview dot row becomes a live status map — status only, no interaction** (`NothingDotRow`, `AutomationListScreen.kt` ~558): currently filled by `routines/(routines+actions)` — meaningless. One dot per mode: red = enabled, gray = disabled, **pulsing red = currently active/in-window**. Dots are too small to identify or tap — deliberately non-interactive (decision: user). Done with `ModeDotRow`.
- [x] **Active state on the mode cards**: cards currently show only name/type/trigger/toggle — nothing marks a mode as *live*. The state exists (`ModeActivationProvider` + `ModeActivationDao` in Room). Add an unmistakable active treatment on the card: pulsing red dot + "ACTIVE" label, e.g. accent border or tinted card edge — make it visibly distinct at a glance, consistent with the dot row's pulse animation. Done.
- [x] **"Firing soon" visualization — a third card state**: next-fire time is already persisted (`scheduled_time_alarms.eventAtMillis`). When a mode's next fire is within a threshold (suggest 30 min, configurable in Settings), show a distinct look: e.g. hollow/outlined dot + "IN 12 MIN" countdown label on the card — visually different from both idle and ACTIVE. Only meaningful for time-based triggers; event triggers (SMS, Bluetooth, etc.) have no "soon". Implemented at 30 min threshold.
- [x] **Notification system (off by default everywhere, fully wired)** — per-mode is authoritative:
  - **Per-mode** control: `notifyRules: List<NotifyRule>` — `BEFORE(minutes)`, `ON_TRIGGER`, `ON_END` — edited in the mode builder.
  - **Settings → "Mode notifications"**: default rules for modes that haven't chosen. Explicit per-mode rules always win.
  - **Blocked-permission detection**: inline warning in the mode editor and Settings; tap opens `Settings.ACTION_APP_NOTIFICATION_SETTINGS`. Rechecked on resume.
  - **Wiring**: BEFORE uses AlarmManager at `eventAtMillis − leadMs` via `AutomationScheduler`; `AutomationAlarmReceiver` forwards `ACTION_NOTIFY_BEFORE`; `AutomationService` posts via `ModeNotificationHelper`. ON_TRIGGER/ON_END posted from the engine completion path. Tap opens the app.
- [x] **Bottom bar gap** (confirmed visually): `NothingBottomActionBar` floating pill shows content beneath it — wrap in an opaque `Surface` with `navigationBarsPadding` (it already does — but the catalog screens draw it in a `Box` overlay; ensure the container under the button is `background`-colored, not transparent) and push the button lower/solid. Done for `ActionConfigSheet` and `ConditionConfigSheet` "Done" bars.
- [x] Toggle styling: Nothing-red track + ON/OFF text labels, app-wide.
- [x] "If"/"Then" headers bigger (display/large-title), plus the new "After" section — see §1.
- [x] **Classic theme restyle** — app + website. App: `NothingDotGrid` gated to NOTHING style; screens already use `NothingFonts.doto()`/`mono()` (null in CLASSIC) and `NothingColors.accent` (resolves to primary in CLASSIC); `NothingScreenHero`/`NothingTopBar`/buttons/labels already branch on `classic`. Website: `styles.css` `[data-style="normal"]` restyles to indigo-accent premium SaaS; `library.html` gained the DOTS/PLAIN + dark/light toggle (persisted via `localStorage`, mirroring `script.js`) and `[data-style="normal"]` overrides. Nothing style untouched.
- [~] Settings load performance — `CapabilityDetector` and `ShizukuGateway.status()` moved to `Dispatchers.IO` in `SettingsViewModel.detect()`. `GlyphToysBridge` not found in this screen; remaining cache/no-op items still to audit.
- [x] Show progress/confirmation when an action takes >~300 ms (e.g. "Turning on Wi-Fi…" → toast/snackbar/inline spinner) so users don't spam. Implemented as a delayed foreground-service progress notification; the existing "Running: <mode>" snackbar also fires for manual runs.
- [x] Save/schedule feedback: confirm "Routine saved" on successful save.

---

## 7. Permissions, capability gating & Shizuku

### 7a. Universal "won't work" interception — for ALL triggers/actions/conditions
The capability model already exists: `CapabilityRequirements.derive()` maps every trigger, action, and condition to required capabilities (`SHIZUKU_REQUIRED`, hardware, permissions, services). Build the interception layer on it:

- [ ] **At pick time**: selecting an item whose requirements aren't met → warning sheet explaining what's missing and why ("Wi-Fi can't toggle silently without Shizuku — without it, the system panel opens for one tap") + a **"Fix it" button** deep-linking to the exact remedy: install/launch Shizuku, system permission dialog, device-admin activation, notification-listener settings, accessibility settings, system panel page.
- [ ] **Shizuku states distinguished**: not installed (→ install link), installed but not running (→ "Start Shizuku"), running but permission denied to us (→ grant in Shizuku app). `ShizukuGateway.status()` already exists for this — the debug receiver's `shizuku_probe` dumps it.
- [ ] **At save time**: summary line on the mode — "Needs: Shizuku, device admin" or "Will open panels for 2 actions" so nothing fails silently at 7 AM.
- [ ] **At fire time**: if a requirement was lost since save (permission revoked, Shizuku stopped), the execution result already distinguishes `ShizukuRequired`/`PermissionRequired`/`Unsupported` → post a heads-up notification "Mode X couldn't run — tap to fix" deep-linking into the mode.
- [ ] Row badge in catalogs: small marker on items needing Shizuku/permissions — visible before the user even taps.

### 7b. Capability matrix — static analysis from code (verify each on device)

The model marks **12 actions as Shizuku-required**: Wi-Fi, Bluetooth, mobile data, extra dim, battery saver, airplane mode, data saver, hotspot, NFC, auto-sync, always-on display, write-setting (+ location mode in practice — executor tries shell first, falls back to opening the settings panel).

| Tier | Count | Items |
|---|---|---|
| **Standalone** (no Shizuku) | ~24 | DND*, ringer, volume, brightness*, auto-brightness*, screen timeout*, dark mode*, auto-rotate*, refresh rate*, screen rotation*, flashlight, vibrate, media control, launch app, open URL, open settings, show notification, clear notifications†, send SMS‡, lock screen§, wait, copy text, all 11 glyph actions (need Nothing hardware) |
| **Shizuku or degrades to system panel** | ~6 | Wi-Fi, Bluetooth, mobile data, location mode, + others with `openPanel` fallback |
| **Shizuku hard-required** | ~8 | battery saver, airplane, data saver, hotspot, NFC, auto-sync, AOD, write-setting, extra dim |
| **Gated / needs override** | 2 | `TakeScreenshot` (Shizuku + override), `LockScreen` (active device admin + override) |

*needs WRITE_SETTINGS (granted on test device) · †needs notification listener (enabled) · ‡needs SEND_SMS (granted) · §needs device admin (active)

**Actionable findings:**
- [x] `TakeScreenshot` implemented via `DeviceTools.capture()` (Shizuku `screencap -p`) and saved to app cache. It is gated at the capability layer and requires the override toggle unless `force = true`.
- [x] `SetLocationMode` capability flag is wrong — model omits `SHIZUKU_REQUIRED` but the executor needs shell/`WRITE_SECURE_SETTINGS` for silent operation (falls back to opening the location settings panel). Fixed: `CapabilityRequirements`, `CapabilityResolver`, and `reasonFor` now treat it as a Shizuku action.
- [x] On-device verification pass remains mandatory — matrix above is static analysis; user reports all Shizuku actions work on their setup, confirm each via the debug broadcast hook.

Triggers: **none need Shizuku**; the gated ones need runtime permissions/services — notification listener, SMS/phone state, usage access (app-opened), location (geofence), calendar read, BT connect. Conditions: most read via builtin providers; `STATE_READER_SETTING/SYSFS/DUMPSYS` conditions → Shizuku; foreground-app → usage access.

### 7c. Catalog filters
- [x] Filter chips on action catalog: **"No Shizuku needed"**, **"Needs Shizuku"**, **"Needs setup"** (missing capability right now), **"Glyph"**. Multi-select, with a Clear button.
- [~] **Persist the filter** and add category chips to condition/trigger catalogs — not yet implemented.

### 7d. Play vs GitHub flavor — what's lost + upsell path

**Play build loses** (per flavor flags + compliance doc):
| Feature | GitHub | Play |
|---|---|---|
| Lock screen (device admin) | works | `Unsupported` — admin receiver moved out of shared manifest and declared only in the GitHub flavor |
| In-app updates | works | becomes "open Play listing" |
| Launch-app full discovery | QUERY_ALL_PACKAGES | degraded to `<queries>` allowlist — picker may not see all apps |
| Play-policy exposure | free | Shizuku-adjacent settings writes invite review scrutiny |

- [x] In the **play** build: a dismissible banner/notice in Settings (and on the capability warning sheet when a github-only feature is tapped): "This action isn't available in the Play version. The GitHub build unlocks device admin, full app discovery, and in-app updates — same app, free." → link to the GitHub releases page. Never nag — once per feature area, dismissible, persisted.

- [x] Central `PermissionGate` helper: when a user selects a trigger/action requiring a runtime permission (calendar, contacts, SMS, location, notifications, camera/mic, exact alarm, accessibility, device admin, Shizuku), show an inline explainer row + "Grant" → system dialog **at that moment**, not buried in settings.
- [x] A permission status section in Settings showing granted/missing with deep links.
- [ ] Already granted on debug device: calendar, SMS, location, notifications, camera, mic, WRITE_SETTINGS, notification listener, device admin. Nothing critical missing; exact alarm falls back to inexact (SCHEDULE_EXACT_ALARM denied by default on A13+ — prompt to grant for punctual triggers).

---

## 8. Backup / export

- [x] Export exists (`ImportExportService`) — add **multi-select export** in the routine list (select mode → export selected as one bundle).
- [x] Export entry points: routine list toolbar, routine detail overflow, settings.
- [x] Round-trip test: export → wipe → import → verify identical behavior.

---

## 9. Engine reliability & performance

- [x] Verify windowed semantics on-device: a 22:00–07:00 window must fire exactly twice (start, end) — no extra triggers. Added `AutomationFlowTest` covering start, end, next-day restart, snapshot/restore, and cooldown suppression.
- [~] Cooldown enforcement, priority conflict resolution — `AutomationFlowTest` and `FirePolicyTest` cover engine-level cooldown. Priority conflict resolution still needs a dedicated test and on-device verification.
- [x] Audit receivers for duplicate registrations (`ConnectivityReceiver`, `DeviceStateReceiver`, `CalendarObserver`) — ensure idempotent re-registration after process death. Verified: `PersistentMonitorService` registers receivers in `onCreate` and unregisters (runCatching) in `onDestroy`; battery/screen/phone/connectivity are dynamic-only (manifest comment confirms), `CalendarObserver` is recreated fresh per service instance. No duplicates.
- [x] Execution journal → surface latency in Execution Log screen ("fired in X ms").

---

## 10. Verification protocol (user-requested)

Every item above gets tested **on the real Phone 3** before being marked done:

- [x] SMS send test to the user-approved number — `SEND_SMS` delivered to `[user-approved number]`; `SmsManager` returned `Success`; carrier-formatted number `[user-approved number]` observed in `SMS_RECEIVED` broadcast.
- [x] SMS/call trigger matching on the user-approved number — engine matched `Trigger.PhoneState` for `SMS_RECEIVED` and `INCOMING_CALL` on `[user-approved number]`; resulting `show_notification` actions posted on device.
- [ ] Real carrier-delivered incoming call — requires a second endpoint or call-forwarding; not possible from the device alone. Service-path simulation verified instead.
- [ ] Joint review session: walk each trigger/action visually, confirm behavior, mark pass/fail.
- [x] Screenshot/capability gating verified on-device: catalog rows show "Detected: may not work on this device" for `Lock screen` and `Screenshot`; the override toggle is present; `Screenshot (override)` is saved and the manual routine runs (Shizuku shell captured).

---

## 11. F-Droid distribution — investigated

Official F-Droid is **effectively blocked by two proprietary dependencies** (checked the tree):

1. `com.google.android.gms:play-services-location:21.3.0` — geofence triggers. Non-free lib; F-Droid rejects it outright.
2. `nothing-integrations/libs/glyph-matrix-sdk-2.0.jar` — Nothing's bundled binary SDK. Non-free binary dependency → `NonFreeDep` at best.

Workarounds and their real costs:

| Path | Effort | Result |
|---|---|---|
| `fdroid` flavor stripping both deps | medium | F-Droid-eligible but loses **all glyph features + geofences** — the app's selling point. Not recommended. |
| Official F-Droid via reproducible build | high | Same dependency rules apply — still blocked. |
| **Self-hosted F-Droid repo** (`fdroidserver`, or GitHub Pages) | low | **Works today, zero gatekeeping, keeps every feature.** Users add the repo URL once; updates flow like F-Droid. |
| IzzyOnDroid repo | low | Hosts your APK directly — but APK is ~73 MB and their limit is ~30 MB. Would need shrinking. |

**Decision (user): self-hosted F-Droid repo inside the existing repo, served from the existing Vercel site.** No new repo, no new infra:

- [x] Repo layout under `landing/fdroid/` with `config.yml`, `metadata/com.tdvorak.nothingmodes.yml`, `README.md`, and `scripts/fdroid-release.sh`.
- [x] Fastlane metadata under `fastlane/metadata/android/en-US/`: `title.txt`, `short_description.txt`, `full_description.txt`, `changelogs/<versionCode>.txt`, and `images/icon.png`.
- [x] README documents the one-signature rule and F-Droid client setup; the script keeps APKs out of git.

Still to wire:

- [x] Run `fdroid init` + `fdroid update` once a release-signed APK exists to generate `index-v2.json` / `index.jar`. Verified locally with `landing/fdroid/scripts/fdroid-local.sh`; signed `index-v1.json`/`index.jar` generated.
- [x] GitHub Action on `release: published` to run the release script and attach the F-Droid repo as a release artifact: `.github/workflows/fdroid.yml`.

## 12. Decisions (resolved)

1. Modes vs routines — **one concept, user-facing name "mode"** everywhere.
2. "After" section label — **"When it ends"**.
3. `TakeScreenshot` — **remove** from catalog and model (MediaProjection consent per capture makes it useless for automation).
4. "Surge" — was a voice-dictation artifact (Whispr Flow). Intended meaning: **fully wire everything across all surfaces** — app, website, GitHub — including search across library/templates. No specific product; fold into the community-pipeline work.
5. Creator profile — **keep handle + email (private, author contact) + optional GitHub field** added.
6. SMS send test — **approved** to the user's own number when we reach it.
7. Glyph always-on — Phone 3 requires the system toy enabled; we deep-link. Accepted.
8. Admin AI agent — pending; design as advisory verdict + human final approval.

---

## Priority order (suggested)

1. Bug fixes: double-config sheet, save feedback, icon grid scroll, bottom-bar gap, catalog "Done" trap.
2. Builder rework: IF/THEN/AFTER, manual default, no timezones, per-action restore.
3. Action/condition polish + descriptions + permission gates.
4. Time/Day trigger expansion + calendar merge + SMS-received trigger UI.
5. Glyph consolidation (3 actions + Studio rework).
6. Community pipeline (share endpoints, admin moderation, website library).
7. New actions feasibility pass + device test matrix.
8. Classic theme restyle.
9. Backup/export multi-select.
10. Performance pass.
