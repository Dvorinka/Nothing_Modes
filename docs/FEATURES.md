# Nothing Modes — Feature Ideas

This file tracks features that are already implemented and candidates that could be useful. It is a living list, not a commitment.

## Currently implemented

See `README.md` and `TASKS.md` for the full checklist. Highlights:

- Automation engine with triggers, conditions, and actions.
- Triggers: time, time window, notification, phone state, connectivity, boot, battery, screen state, app opened, geofence, Bluetooth device, Wi-Fi connected, calendar event, charger, device lock/unlock, torch, media playback.
- Actions: connectivity toggles (Wi-Fi, Bluetooth, mobile data, airplane, hotspot, NFC), DND, ringer, volume, brightness, dark mode, extra dim, screen timeout, refresh rate, screen rotation, AOD, wallpaper, flashlight, media control, SMS, lock screen, screenshot, clear notifications, launch app, open URL, copy text, wait, write setting, Glyph light/matrix.
- Restore previous state for windowed modes.
- Import/export, custom builder, presets, MCP debug control surface.

## Candidate features

### New actions

- **Stay awake while charging** — keep `stay_on_while_plugged_in` on while a mode is active. Already added via `Action.SetStayAwake`.
- **Set display size / font scale** — change `Settings.Global.DENSITY_DPI` or font scale.
- **Set color inversion / grayscale** — accessibility color filters.
- **Set pocket mode / prevent accidental touch** — proximity-gated screen off.
- **Send clipboard text** — write arbitrary text to clipboard (current `CopyText` only copies).
- **Auto-respond to SMS / calls** — reply with a preset message or reject calls from a matching number.
- **Screenshot to file / OCR** — capture and optionally read text.
- **Voice note / record audio** — start/stop recording via `MediaRecorder`.
- **Vibration pattern** — custom Morse or pulse vibration, not just fixed duration.
- **Open quick settings / notification shade** — for demo or accessibility flows.

### New triggers

- **Shake / motion** — accelerometer threshold, useful for quick manual activation.
- **Proximity covered** — hand over phone or in pocket.
- **Sunrise / sunset** — use `sunrise-sunset.org` or `libnova`-style calculation; needs location.
- **Driving / vehicle mode** — `ActivityRecognition` or Bluetooth A2DP / car dock.
- **USB connected / disconnected** — OTG, charging source, ADB state.
- **NFC tag scanned** — read an NFC tag ID, not just enable/disable NFC.
- **Bluetooth LE beacon / iBeacon** — detect presence of a specific beacon.
- **Headset / microphone connected** — wired or Bluetooth headset state.
- **Call state: outgoing call started** — currently only incoming, ended, and SMS.
- **Missed call** — notification-style trigger for missed calls.
- **Alarm / timer dismissed** — complement to alarm ringing.
- **Power connected source** — separate AC, USB, wireless triggers.
- **Battery temperature threshold** — condition exists; could also be a trigger.
- **Thermal level threshold** — condition exists; could also be a trigger.
- **Screen off for N minutes** — condition exists; could be a trigger.
- **App closed** — detect transition from foreground to background.
- **Specific notification removed** — currently only posted.

### New conditions

- **Network metered / unmetered** — `ConnectivityManager.isActiveNetworkMetered`.
- **Current Wi-Fi SSID or BSSID** — for home/work checks.
- **Bluetooth device connected** — by name or address.
- **Current playback package** — media app package matches.
- **Headphones connected** — condition already exists; could split by wired/Bluetooth.
- **Charging source** — AC, USB, wireless.
- **Call state in progress** — not just ringing.
- **Mode last run time** — prevent re-running within a custom window.
- **Count of notifications from app** — more than N.

### UI / integration

- **Quick settings tile** — one-tap run for selected manual modes.
- **Home screen widget** — same as the tile, with a larger state list.
- **Launcher shortcuts** — long-press app icon → run mode.
- **Voice assistant integration** — Google Assistant / Tasker plugin.
- **Mode sharing via deep link / QR** — share a mode URL and import it.
- **Wear OS companion** — start a mode from a watch.
- **Mode run history / execution log** — persist and display recent runs.
- **Cloud backup / restore** — optional encrypted backup.
- **Community mode library** — curated presets, user submissions.
- **Light stripe visual editor** — currently only text input; a visual channel picker.

### System / device

- **Adaptive refresh rate schedule** — high refresh during apps/games, low otherwise.
- **App-specific rotation lock** — force portrait/landscape per app.
- **Battery health / charging limit** — stop charging at 80 % on supported hardware.
- **Sleep schedule** — one mode that chains DND + dark mode + low brightness + AOD off.
- **Flip to record** — use the existing flip receiver to start audio/video recording.
- **Find my phone** — whistle / clap or via another device to make it beep + flash.
- **Pocket mode** — lock screen when proximity is covered for N seconds.

## Intentionally not planned

- **Per-app volume** — Android does not expose a public per-app volume API. Stream-level volume plus conditions is the practical ceiling.
- **Call recording** — illegal in many jurisdictions and blocked on modern Android.
- **Remote control / cloud execution** — out of scope; the app is intentionally local and privacy-first.
- **Screen content reading / click automation** — requires accessibility service for general automation, which would be a major security surface.

## Notes on adding a feature

1. Add the `Action` or `Trigger` model in `engine-core` with a stable wire name.
2. Map it in `CapabilityIds`, `CapabilityRequirements`, `CapabilityResolver`, and `CapabilityLabels`.
3. Implement the controller / executor in `capabilities`.
4. Add an editor in `ActionConfigScreen` and `ActionConfigSheet`, a catalog item in `ActionCatalogScreen`, and a label/description in `Descriptions.kt`.
5. If it writes a setting, add the semantic key to `Action.affectedSettings` and `AndroidSettingReader` if needed for restore.
6. Add an entry to `ModeExplainer` for MCP support.
