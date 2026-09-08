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
- [ ] Recurrence: daily / **weekly (multi-day select)** / **monthly (multi-day-of-month select)** / **yearly (multi-month + multi-day select)**.
- [ ] Quick actions: "Weekdays", "Weekend", "Every day", for monthly "1st of month", "Last day", "Whole month", multi-month pick for yearly.
- [ ] Model: `days: List<DayOfWeek>` exists for weekly; add `daysOfMonth: List<Int>`, `months: List<Int>` (nullable, EncodeDefault.NEVER). Back the engine by extending `CronSchedule` or a small `nextFire` evaluator — cron can't express "last day of month" cleanly.

### Calendar trigger — merge into Time/Day
User is right: asking for a `calendarId` string is backwards.
- [ ] Merge into Time/Day config as a **"From calendar"** source option.
- [ ] On select: request `READ_CALENDAR` **in context** (see §7), then show a calendar picker (default: **all calendars**, optional restrict to one).
- [ ] Show upcoming events from the chosen calendars so the user can pick a specific event or a title pattern. Keep `calendarId`/`titleMatch`/`direction` model fields — they're fine; it's purely a UX rewrite.
- [ ] Remove the standalone "Calendar" trigger type from the catalog (fold into Time/Day).

### New triggers requested
- [ ] **Torch/flashlight active** — `CameraManager.registerTorchCallback` (no permission needed for state). New `Trigger.TorchState(on: Boolean)`.
- [ ] **Device connects** — `Trigger.BluetoothDevice` exists; surface it clearly ("When this device connects") with bonded-device picker.
- [ ] **Wi-Fi active / connected** — `Trigger.WifiConnected(ssid)` exists; add SSID picker from `WifiManager.connectionInfo`/`configured` networks + "any network".
- [ ] **SMS received — specific contact or custom number + text match** — `Trigger.PhoneState(SMS_RECEIVED, number, textMatch)` already modeled. Build the UI: contact picker (`READ_CONTACTS`) OR custom number field + "any text / contains text" field. Wire `PhoneStateReceiver` to match.
- [ ] Template: **"Locate my phone"** — SMS "LOKALIZUJ SE" → GPS on, mobile data on, battery saver on, location shared. Ship as a built-in template with description.
- [ ] **Condition parity pass — audited, real gaps found** (user flagged: geofence exists as trigger but not as condition). New conditions to add, grouped:
  - `Condition.AtLocation(lat, lng, radiusM)` — "device is currently inside this area". Use `LocationManager.getCurrentLocation` with short timeout (last-known is too stale). Same permission as the geofence trigger — the gate is shared.
  - `Condition.EventActive` — a calendar event is in progress right now (calendar trigger exists; condition doesn't).
  - `Condition.TorchOn` — flashlight currently on (CameraManager torch callback state).
  - `Condition.DeviceLocked` — keyguard locked now (`KeyguardManager.isKeyguardLocked`).
  - `Condition.DndActive` — `SetDnd` action exists but its state isn't readable as a condition.
  - `Condition.HotspotOn`, `Condition.AodOn` — actions exist, conditions don't.
  - `Condition.WifiRadioOn` / `BluetoothOn` / `MobileDataOn` — existing conditions only cover *connected*, not *adapter enabled*.
  - `Condition.NotificationPresent(pkg, match)` — "app X currently has a notification" — the notification listener is already running, this is a read of state we already see.
  - `Condition.BrightnessLevel(op, level)` / refresh-rate / screen-timeout current value — settable, not checkable.
  - Rule going forward: **every settable state is checkable, every event trigger has a matching "is it true now" condition where the state is readable.**

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
- [ ] **Always-On Display**: expose modes — off / tap-to-show / always / schedule (map to Nothing OS `aod` settings where they exist).
- [ ] **Volume**: show all streams at once (media, ring, alarm, notification) as labeled sliders — no dropdown.
- [x] **Vibrate**: dropdown with duration presets (Short/Medium/Long/1 second).
- [x] **Ringer mode**: explanation added below the radio options.
- [x] **Descriptions on every action** — catalog rows show a one-line hint; action config sheets show the same note.
- [x] **NFC**: use an NFC glyph/icon (`Icons.Outlined.Nfc`), not Bluetooth.
- [x] **Location mode**: in-app description added.
- [x] **Group related items**: hotspot now in Connections, Auto-rotate and Screen rotation grouped.
- [~] **Refresh rate**: preset dropdown (60/90/120/144 Hz) + custom Hz input. Device-supported rate query not yet implemented.
- [x] **Auto-sync**: description added.
- [x] **Lock screen**: description added in the action config sheet.
- [ ] **Screenshot + experimental actions**: capability-detect at runtime (`CapabilityDetector`); show disabled with "Detected: may not work on this device" + an override toggle. Apply to screenshot, lock screen, and anything MediaProjection/device-admin dependent.
- [x] **Copy text**: removed from catalog.
- [~] **Open URL**: description and URL input wired. Optional browser/app picker not yet implemented.
- [ ] **Launch app**: real discovery already exists? Verify `AppPicker` queries installed launchable apps (not hardcoded); add **multi-select** + app icons via `PackageManager.getApplicationIcon`.
- [ ] **Open settings**: keep, add description, expand `SettingsScreen` enum (battery, storage, security, accessibility, notification, sound, display, apps, network, location, date, accounts).
- [~] **Wait**: duration input wired. Preset dropdown (1s/5s/10s/30s/1m/5m) + Custom not yet implemented.
- [~] **Send SMS**: number and message inputs wired. Contact picker and on-device send test not yet done.
- [ ] **Write setting**: hide behind "Advanced" by default; add plain-English explanation + a few safe presets (e.g., animation scale, font scale) instead of raw key/value.

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
- [ ] Shrink the top/preview section so the scrollable content gets the majority of the screen.
- [ ] Remove "Registered toys" read-only list (info noise).
- [ ] Remove "System toy table diagnostics".
- [ ] Remove "Sleep mode glyph preset" selector — it's preview-only, does nothing else. Confirmed dead UI.
- [ ] Remove the "Hardware detected → TURN OFF" card (broken on this device).
- [ ] Remove "Open toys / Always-on / Timeout" buttons that no-op — replace with one "Open system Glyph settings" deep link that actually resolves (`GlyphToysBridge.canOpenAodPicker` etc. — verify intents on Phone 3).
- [ ] Storage tab (my designs), Community tab (website library), Import (JSON/file/QR later).
- [ ] Link out to Glyph Museum app (installed on this device — `com.pauwma.glyphmuseum`).

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
- [ ] Live e2e smoke: submit → email → approve → visible on site + in app.
- [ ] Env check: `DATABASE_URL`, `RESEND_API_KEY`, `CRASH_NOTIFY_EMAIL` exist; `ADMIN_TOKEN` optional (falls back to `CRASH_ADMIN_TOKEN`).
- [ ] Content-hash verification on import (JSONB reorders keys — needs canonical serialization first).

**Creator profile**: **handle** is the public attribution. Email is asked optionally and kept private (used only for the decision notice). Optional GitHub link shown publicly. Handle lives in Settings → Creator Profile and is pre-filled when publishing.

### Backend (Vercel + Neon, same pattern as crash.ts)
- [x] `POST /api/share` — submit a template or glyph pack: `{type: "template"|"glyph", payload, handle, title, description}` → `shared_items` table with `status: pending|approved|rejected`.
- [x] `GET /api/library` — public endpoint: approved items only, sanitized (no emails, no raw payloads beyond what's needed to render).
- [x] `GET /api/submissions` + `POST /api/moderate` — bearer-token admin queue (was `/api/admin/pending` + `/api/admin/moderate` in the sketch).
- [x] `POST /api/delete` — admin delete for submissions / crash reports.
- [x] `GET /api/preview` — public item payload without bumping downloads.
- [x] Email notify on new submission (Resend, same as crash notify).
- [ ] Auto-generated description fallback: derive "what it does" from the automation JSON when the author leaves it blank.

### Security review pipeline (user's requirement — non-negotiable)
- [x] **Static analysis on ingest** (server-side, in `share.ts` / `analyze.ts`): reject payloads containing `send_sms` to non-e.164 numbers, `open_url` to non-https, `write_setting` to non-allowlisted keys, `launch_app` to known-bad packages, unbounded `wait`, scripts/`content://`/`file://` URIs. Maintain an allowlist of action types for shared content; anything else → auto-reject.
- [x] **Admin review queue** in `admin.html` (extend): view rendered summary + raw JSON, approve/reject/delete.
- [x] **AI-agent endpoint**: documented in `landing/api/AGENT.md`; agents can pull pending and POST decisions.
- [ ] Signing/integrity: server stores a content hash; app verifies downloaded payload hash before import.
- [x] Rate-limit submissions per handle/IP; payload size cap (e.g. 64 KB).
- [x] Privacy: never publish device identifiers; strip `number`, `ssid`, `pkg`, geofence coords from shared templates (or mark as "user fills in on import" placeholders). This is important — a shared "SMS from mom" template must not leak the author's contacts.

### App side
- [x] "Browse Templates" → tabbed: **Local / Community (remote)**. Search across both.
- [x] Publish flow: routine overflow → "Share" → attach handle → POST `/api/share` → "Pending review" state.
- [x] Same for glyphs: "Share design" from Glyph Studio → same pipeline.
- [x] Import from community: fetch `GET /api/library` → preview → import via existing `ImportExportService`.
- [ ] Website `index.html`: add a **Library** section/page rendering the public endpoint; credit handle prominently; download = raw JSON (copy/import via deep link `nothingmodes://import?...` later).

### Out of scope per user: GitHub / Play / Android-app integrations unchanged.

---

## 6. UI/UX fixes

- [ ] **Overview dot row becomes a live status map — status only, no interaction** (`NothingDotRow`, `AutomationListScreen.kt` ~558): currently filled by `routines/(routines+actions)` — meaningless. One dot per mode: red = enabled, gray = disabled, **pulsing red = currently active/in-window**. Dots are too small to identify or tap — deliberately non-interactive (decision: user).
- [ ] **Active state on the mode cards**: cards currently show only name/type/trigger/toggle — nothing marks a mode as *live*. The state exists (`ModeActivationProvider` + `ModeActivationDao` in Room). Add an unmistakable active treatment on the card: pulsing red dot + "ACTIVE" label, e.g. accent border or tinted card edge — make it visibly distinct at a glance, consistent with the dot row's pulse animation.
- [ ] **"Firing soon" visualization — a third card state**: next-fire time is already persisted (`scheduled_time_alarms.eventAtMillis`). When a mode's next fire is within a threshold (suggest 30 min, configurable in Settings), show a distinct look: e.g. hollow/outlined dot + "IN 12 MIN" countdown label on the card — visually different from both idle and ACTIVE. Only meaningful for time-based triggers; event triggers (SMS, Bluetooth, etc.) have no "soon".
- [ ] **Notification system (off by default everywhere, fully wired)** — per-mode is authoritative (user decision):
  - **Per-mode** (the real control): each mode gets its own notification rules — OFF by default. A mode can stack **multiple** notifications: "5 min before" AND "when it triggers" AND (windowed) "when it ends". Lead times: presets (5/10/15/30/60 min) **plus custom input**. Model: `notifyRules: List<NotifyRule>` — `BEFORE(minutes)`, `ON_TRIGGER`, `ON_END`.
  - **Settings → "Mode notifications"**: NOT a kill switch — it is only the **default for modes that haven't chosen**. An explicit per-mode setting always wins, even against global off (user decision: "if they specifically request this mode to always send notifications, settings should not override it").
  - **Blocked-permission detection (confirmed feasible)**: `NotificationManagerCompat.areNotificationsEnabled()` for the app switch, `NotificationChannel.getImportance() == IMPORTANCE_NONE` per channel (API 26+). When a user enables any notify rule while notifications are blocked → inline warning in the mode's Notify section: "Notifications are off for Nothing Modes — heads-ups can't reach you. [Enable]" → deep-link `Settings.ACTION_APP_NOTIFICATION_SETTINGS` (per-channel variant `ACTION_CHANNEL_NOTIFICATION_SETTINGS` when only the channel is blocked). Same warning surfaced in Settings → Mode notifications. Permission state re-checked on resume, not just once.
  - Wiring: each BEFORE rule = an AlarmManager alarm at `eventAtMillis − leadMs` → receiver → notification. ON_TRIGGER/ON_END = post from `AutomationAlarmReceiver`/engine completion path. Tap notification → opens the mode. Notification text explains what happens and when: mode name, "fires in 5 min — will set Wi-Fi on, glyph…" / "just ran — 2 actions applied" / "mode ended — 2 settings restored".
- [ ] **Bottom bar gap** (confirmed visually): `NothingBottomActionBar` floating pill shows content beneath it — wrap in an opaque `Surface` with `navigationBarsPadding` (it already does — but the catalog screens draw it in a `Box` overlay; ensure the container under the button is `background`-colored, not transparent) and push the button lower/solid. Check `ActionConfigSheet`/`ConditionConfigSheet` "Done" bars specifically.
- [ ] Toggle styling: Nothing-red track + ON/OFF text labels, app-wide.
- [ ] "If"/"Then" headers bigger (display/large-title), plus the new "After" section — see §1.
- [ ] **Classic theme restyle** — user dislikes it on app AND website. Full redesign pass; **do not touch the Nothing theme** (it stays as-is). Website has a "DOTS" style-toggle — restyle the non-dot variant.
- [ ] Settings load performance — audit `SettingsScreen` init (capability probes, `GlyphToysBridge` queries run on main thread? move to IO + cache).
- [ ] Show progress/confirmation when an action takes >~300 ms (e.g. "Turning on Wi-Fi…" → toast/snackbar/inline spinner) so users don't spam.
- [ ] Save/schedule feedback: confirm "Routine saved" on successful save.

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
| **Broken today** | 1 | `TakeScreenshot` — returns `Unsupported` unconditionally (dead code; needs MediaProjection consent flow) |

*needs WRITE_SETTINGS (granted on test device) · †needs notification listener (enabled) · ‡needs SEND_SMS (granted) · §needs device admin (active)

**Actionable findings:**
- [ ] `TakeScreenshot` is dead code — `RealActionExecutor` returns `Unsupported` unconditionally. Either implement via MediaProjection (per-capture consent makes it poor for automation — likely remove) or drop it from the catalog/model. **Decision needed: implement or remove.**
- [ ] `SetLocationMode` capability flag is wrong — model omits `SHIZUKU_REQUIRED` but the executor needs shell/`WRITE_SECURE_SETTINGS` for silent operation (falls back to opening the location settings panel). Add the flag so warnings/filters are accurate.
- [ ] On-device verification pass remains mandatory — matrix above is static analysis; user reports all Shizuku actions work on their setup, confirm each via the debug broadcast hook.

Triggers: **none need Shizuku**; the gated ones need runtime permissions/services — notification listener, SMS/phone state, usage access (app-opened), location (geofence), calendar read, BT connect. Conditions: most read via builtin providers; `STATE_READER_SETTING/SYSFS/DUMPSYS` conditions → Shizuku; foreground-app → usage access.

### 7c. Catalog filters
- [ ] Filter chips on action/condition/trigger catalogs: **"No Shizuku needed"**, **"Needs Shizuku"**, **"Needs setup"** (permission missing right now), **"Glyph"** (Nothing hardware), category chips. Multi-select.
- [ ] **Persist the filter** — a user who never installs Shizuku shouldn't wade through dead rows every time.
- [ ] **"Clear filters" button** — one tap resets all.

### 7d. Play vs GitHub flavor — what's lost + upsell path

**Play build loses** (per flavor flags + compliance doc):
| Feature | GitHub | Play |
|---|---|---|
| Lock screen (device admin) | works | `Unsupported` — admin receiver must also move out of shared manifest (currently leaks into play builds — fix) |
| In-app updates | works | becomes "open Play listing" |
| Launch-app full discovery | QUERY_ALL_PACKAGES | degraded to `<queries>` allowlist — picker may not see all apps |
| Play-policy exposure | free | Shizuku-adjacent settings writes invite review scrutiny |

- [ ] In the **play** build: a dismissible banner/notice in Settings (and on the capability warning sheet when a github-only feature is tapped): "This action isn't available in the Play version. The GitHub build unlocks device admin, full app discovery, and in-app updates — same app, free." → link to the GitHub releases page. Never nag — once per feature area, dismissible, persisted.

- [ ] Central `PermissionGate` helper: when a user selects a trigger/action requiring a runtime permission (calendar, contacts, SMS, location, notifications, camera/mic, exact alarm, accessibility, device admin, Shizuku), show an inline explainer row + "Grant" → system dialog **at that moment**, not buried in settings.
- [ ] A permission status section in Settings showing granted/missing with deep links.
- [ ] Already granted on debug device: calendar, SMS, location, notifications, camera, mic, WRITE_SETTINGS, notification listener, device admin. Nothing critical missing; exact alarm falls back to inexact (SCHEDULE_EXACT_ALARM denied by default on A13+ — prompt to grant for punctual triggers).

---

## 8. Backup / export

- [ ] Export exists (`ImportExportService`) — add **multi-select export** in the routine list (select mode → export selected as one bundle).
- [ ] Export entry points: routine list toolbar, routine detail overflow, settings.
- [ ] Round-trip test: export → wipe → import → verify identical behavior (write an instrumented test).

---

## 9. Engine reliability & performance

- [ ] Verify windowed semantics on-device: a 22:00–07:00 window must fire exactly twice (start, end) — no extra triggers. Add `AutomationFlowTest` coverage for overnight windows (there is `SleepMorningTest` — extend).
- [ ] Cooldown enforcement, priority conflict resolution — verify with instrumented tests on the real device.
- [ ] Audit receivers for duplicate registrations (`ConnectivityReceiver`, `DeviceStateReceiver`, `CalendarObserver`) — ensure idempotent re-registration after process death.
- [ ] Execution journal → surface latency in Execution Log screen ("fired in X ms").

---

## 10. Verification protocol (user-requested)

Every item above gets tested **on the real Phone 3** before being marked done:

- [ ] Joint review session: walk each trigger/action visually, confirm behavior, mark pass/fail.
- [ ] Create `docs/DEVICE-TEST-MATRIX.md`: table of feature × status (works / needs-Shizuku / broken / impossible-on-this-device) — the "scratch list" the user asked for.
- [ ] SMS send test to [redacted phone number] (user's own number) — confirm SEND_SMS actually delivers.
- [ ] Screenshot/capability gating verified on-device.

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

- [ ] Repo layout: metadata + index under `landing/fdroid/` → repo URL `https://nothing-modes.vercel.app/fdroid/`. APKs can be served from the same path or linked to GitHub release assets — the index supports external APK URLs.
- [ ] Generate with `fdroidserver` (`fdroid init` + `fdroid update`) against the **release-signed** APK. One signature rule: F-Droid updates fail if a user previously installed a differently-signed build (debug vs release) — document "uninstall debug first" in the repo listing and site.
- [ ] **Fastlane/Triple-T metadata** (`fastlane/metadata/android/en-US/`) — makes the repo render like a store listing:
  ```
  fastlane/metadata/android/en-US/
    title.txt                  # "Nothing Modes"
    short_description.txt      # <80 chars
    full_description.txt       # reuse landing-page copy
    changelogs/<versionCode>.txt
    images/icon.png
    images/phoneScreenshots/   # reuse landing/shots/
  ```
- [ ] **No extra build flavor needed.** The repo serves the existing `github` release APK — same signature, same binary. An `fdroid` flavor only matters for the *official* store, which is already ruled out.
- [ ] **Update mechanism (the "how does the client know" part)**: a self-hosted repo has no automatic checker — *we* regenerate the index when a release drops. `fdroid update` scans the repo dir, writes `index-v2.json`, and each client's refresh picks it up. Two ways to run it:
  - Manual: release script `scripts/fdroid-release.sh <apk-or-gh-release-url>` — regenerates index, commits `landing/fdroid/`, deploy. 
  - Automated: GitHub Action on `release: published` → download the release asset → `fdroid update` → commit + push → Vercel redeploys with the new index. Recommended once the manual path works.
- [ ] **Keep APKs out of git**: the index can point at GitHub release asset URLs — index files (KBs) live in the repo/Vercel, the 73 MB binary stays on GitHub Releases. Avoids bloating both.
- [ ] What F-Droid gives: distribution only — automatic updates for F-Droid-client users, store-style listing, de-Googled audience. No app features change.
- [ ] Optional later: an `fdroid` flavor with a `CapabilityResolver`-driven "reduced" build (no glyph, no geofence) if official listing is ever wanted.

## 12. Decisions (resolved)

1. Modes vs routines — **one concept, user-facing name "mode"** everywhere.
2. "After" section label — **"When it ends"**.
3. `TakeScreenshot` — **remove** from catalog and model (MediaProjection consent per capture makes it useless for automation).
4. "Surge" — was a voice-dictation artifact (Whispr Flow). Intended meaning: **fully wire everything across all surfaces** — app, website, GitHub — including search across library/templates. No specific product; fold into the community-pipeline work.
5. Creator profile — **keep handle + email (private, author contact) + optional GitHub field** added.
6. SMS send test — **approved** to [redacted phone number] when we reach it.
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
