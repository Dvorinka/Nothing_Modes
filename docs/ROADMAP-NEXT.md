# Nothing Modes — Open Work

> Cleaned 2026-09-10 — everything shipped was removed; history lives in git.
> Latest release: **0.17.0** (vc22) — Play alpha, GitHub, self-hosted F-Droid.
> Device under test: **Nothing Phone (3) — A024 "Metroid", Android 16 (SDK 36)**.

---

## 1. Execution verification — the remaining test gap

The configuration side is swept (all 20 triggers / 34 conditions / 51 actions
open real sheets, wired, capability-gated). What's not yet proven end-to-end:

- [ ] **Fire real events, watch the audit log** — create one mode per major
  trigger class (charger, screen on/off, time window, notification, BT device,
  Wi-Fi SSID, geofence, calendar, phone call), let them fire naturally on the
  device, confirm the Execution Log shows correct latency + action results.
- [ ] **Shizuku action matrix** — `ShizukuActionMatrixTest` covers the batch;
  run it once more against the final build and record results.
- [ ] **Audio visualizer routes** — `GlyphMusic` over wired headphones,
  Bluetooth, and speaker: confirm `AudioPlaybackCapture` via MediaProjection
  wins over `Visualizer(0)`, and simulation only kicks in as last fallback.
  Verify each route reports the right capture source in the UI.
- [ ] **Windowed restore on device** — set a 2-min window flipping DND +
  brightness, confirm snapshot/restore lands both ways.
- [ ] **Geofence enter/exit on real movement** — place a small fence at home,
  walk in/out, confirm both transitions fire.
- [ ] **Notification-present condition** — post a test notification, confirm
  the condition gates the mode correctly.

## 2. Classic theme re-walk

- [ ] The CLASSIC style was rebuilt mid-sweep; re-walk every screen in
  classic (catalog, builder, settings, glyph sheets, maps) on a non-Nothing
  surface to catch any residual Nothing-styled leakage or contrast issues.

## 3. Builder interaction check

- [ ] **IF row + discard-dialog tap** — `adb input tap` couldn't activate the
  builder's trigger row or the DISCARD button in one pass; likely a coordinate
  issue, but confirm by hand (or scrcpy) that both respond to real taps.

## 4. Play Console — manual only, cannot be automated

- [ ] **Foreground Service permissions declaration** (BLOCKING releases) —
  App content → FGS declaration: the first `gplay release` for 0.17.0
  uploaded vc22 to the internal track but the edit can't commit until this
  is done. One-time per permission type; also needed before CI auto-publish
  can complete.
- [ ] Content rating questionnaire (if not already complete)
- [ ] Data safety form (if not already complete) — answer is "no data
  collected" except voluntary community submissions
- [ ] Health apps declaration — "not a health app"
- [ ] Ads declaration — "no ads"
- [ ] Final "Send for review" click on Publishing overview
- [ ] **Geo-blocking note**: closed-track country limits are fine; at
  production rollout either enable all EU member states or none.

## 5. New actions — shipped 2026-09-18

Nine actions added end-to-end (model, serialization, capability gating,
executor, catalog, config sheets, icons, descriptions, mode explainer,
windowed-mode restore via `affectedSettings` snapshots):

| Action | Path | Device-verified |
|---|---|---|
| Font scale (85–130%) | `Settings.System.FONT_SCALE` — public `WRITE_SETTINGS`, Play-safe | `font_scale=1.3` landed |
| Night light + warmth | `settings put secure night_display_*` via Shizuku; night-display panel fallback | `night_display_color_temperature=3500` landed |
| Color inversion | `accessibility_display_inversion_enabled` via Shizuku; accessibility panel fallback | landed |
| Daltonizer (color correction) | `accessibility_display_daltonizer_enabled` via Shizuku; accessibility panel fallback | `=1` landed |
| Sensor privacy (mic/camera) | `cmd sensor_privacy` via Shizuku; privacy panel fallback | camera block verified via camera-app dialog |
| One-handed mode | `one_handed_mode_enabled` via Shizuku; display panel fallback | `=1` landed |
| Set alarm | `AlarmClock.ACTION_SET_ALARM` (install-grant `SET_ALARM`) | "nm test" 07:30 appeared in DeskClock |
| Set timer | `AlarmClock.ACTION_SET_TIMER` | ⚠ see limitation |
| Open clock (alarms/timers) | `ACTION_SHOW_ALARMS` / `ACTION_SHOW_TIMERS` | opens correct tab |

### Device quirks recorded

- **`cmd` under Shizuku exits 255** with empty stderr even when the command
  *applied* — `sensor_privacy` is invoked via `sh -c` first, then bare-name,
  then legacy int id. Android 16 takes sensor **names**
  (`microphone`/`camera`); older builds take ints (`1`/`2`).
- **Google DeskClock ignores `ACTION_SET_TIMER` extras** on NP3 — the intent
  reaches `HandleSetApiCalls` (verified in logcat, identical via `adb am`) but
  opens an empty timer-setup sheet. Alarm intents work correctly. Same
  limitation will apply wherever the OEM clock drops timer extras.
- **`manual_automation_id` service starts are silently dropped** when the app
  isn't in FGS/foreground state — expected BAL behavior, not a bug; fires from
  real triggers and the UI are unaffected.

### Not shipped (still open)

| Action | Path | Status |
|---|---|---|
| Battery share / reverse charging | Nothing OS internal | unknown |
| Quick Share / Nearby Share | system intent | launch-only |
| Nothing quick actions (Essential Recorder, Focus, Live Caption, Now Playing, Glyph Timer) | app intents only | catalog as launch actions where a stable intent exists |

## 6. Deferred ideas

- [ ] `RUN_ACTION` end policy — arbitrary follow-up action when a mode ends
  (currently: restore/keep only).
- [ ] IzzyOnDroid listing — needs APK under ~30 MB (currently ~57 MB).
- [ ] Second-trigger "until" semantics — a mode that ends when another
  trigger fires, not just a time window.
- [ ] **Sensor-privacy readback** — `cmd sensor_privacy` applies the toggle but
  exits 255 under Shizuku, and no readable state exists (`dumpsys` is sparse,
  no settings key). Verification today is functional (open camera, look for
  the block dialog). A state probe — e.g. `service call sensor_privacy` or
  polling the privacy chip — would let the executor confirm the write and
  report honest success/failure.
- [ ] **`cmd` under Shizuku is unreliable** — `settings`/`am`/`pm` scripts run
  fine, but `cmd` exits 255 with empty stderr (observed on NP3/Android 16,
  even when the mutation applied). Any future action needing `cmd <service>`
  should prefer a `settings put` key, an `am`/`pm` equivalent, or a direct
  binder call — and treat `cmd` as last resort behind a `sh -c` wrap.
- [ ] **Clock-app capability probe** — DeskClock on NP3 silently drops
  `SET_TIMER` extras while honoring `SET_ALARM`. A first-run probe (resolve
  the handler, note the package) could warn in the config sheet when the
  default clock is known-bad for timers, or offer "install/open a compatible
  clock" guidance.
- [ ] **Pending-unlock history + retry diagnostics** — the queue works, but a
  drained entry leaves no trace. Persisting "what replayed after unlock" into
  the audit log would help diagnose deferred actions the user never saw.
- [ ] **Restore coverage for privileged settings** — windowed modes snapshot
  `affectedSettings` keys, but secure keys written via Shizuku may be
  reconciled by the OS between snapshot and restore (observed: Nothing OS
  flipped some toggles back). Worth a device pass on each new setting to see
  which actually stay restored.
- [ ] **Automation smoke-test script** — half the device session was spent on
  `run-as` DB patching and flaky `startservice` timing. A small debug-only
  broadcast receiver ("fire automation X now") would make on-device
  verification repeatable instead of hand-rolled each session.
