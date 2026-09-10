# Nothing Modes — Open Work

> Cleaned 2026-09-10 — everything shipped was removed; history lives in git.
> Latest release: **0.13.0** (vc16) — Play alpha, GitHub, self-hosted F-Droid.
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

- [ ] Content rating questionnaire (if not already complete)
- [ ] Data safety form (if not already complete) — answer is "no data
  collected" except voluntary community submissions
- [ ] Health apps declaration — "not a health app"
- [ ] Ads declaration — "no ads"
- [ ] Final "Send for review" click on Publishing overview
- [ ] **Geo-blocking note**: closed-track country limits are fine; at
  production rollout either enable all EU member states or none.

## 5. New actions — feasibility triage leftovers

Investigate before promising; several may be impossible on stock Android:

| Action | Path | Status |
|---|---|---|
| Alarm (set/show) | `AlarmClock` intents | intent-only, no silent API |
| Battery share / reverse charging | Nothing OS internal | unknown |
| Night light + intensity | `Settings.Secure.NIGHT_DISPLAY_*` via Shizuku | feasible |
| Quick Share / Nearby Share | system intent | launch-only |
| Color inversion / daltonizer | `Settings.Secure` via Shizuku | feasible |
| Mic/camera privacy toggles | `SensorPrivacyManager` via Shizuku | feasible |
| One-handed mode | OEM-specific setting | investigate on device |
| Font size | `Settings.System.FONT_SCALE` via WRITE_SETTINGS | feasible |
| Nothing quick actions (Essential Recorder, Focus, Live Caption, Now Playing, Glyph Timer) | app intents only | catalog as launch actions where a stable intent exists |

## 6. Deferred ideas

- [ ] `RUN_ACTION` end policy — arbitrary follow-up action when a mode ends
  (currently: restore/keep only).
- [ ] IzzyOnDroid listing — needs APK under ~30 MB (currently ~57 MB).
- [ ] Second-trigger "until" semantics — a mode that ends when another
  trigger fires, not just a time window.
