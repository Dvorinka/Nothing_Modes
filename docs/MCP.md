# Nothing Modes — Model Context Protocol (MCP) / USB Agent Surface

## What it is

MCP is a debug-only control surface that lets an AI agent on a host computer create, validate, inspect, and run Nothing Modes over `adb`. It is useful for:

- Generating a mode from a natural-language spec.
- Validating a mode before saving it.
- Listing modes and checking why one cannot run.
- Running a manual mode immediately for testing.

It is **not exposed in release builds**. It requires USB debugging and `adb`.

## Host tool

Use `tools/mcp-usb.py`:

```bash
python3 tools/mcp-usb.py list
python3 tools/mcp-usb.py get <mode-id>
python3 tools/mcp-usb.py run <mode-id>
python3 tools/mcp-usb.py delete <mode-id>
python3 tools/mcp-usb.py save < mode.json
python3 tools/mcp-usb.py validate < mode.json
python3 tools/mcp-usb.py explain --id <mode-id>
python3 tools/mcp-usb.py guide
```

You can also call it directly with `adb`:

```bash
adb shell am broadcast -n com.tdvorak.nothingmodes.debug/com.tdvorak.nothingmodes.agent.ModeControlReceiver \
  --es command <command> \
  [--es id <mode-id>] \
  [--es json '<minified-json>']
```

## Protocol

The receiver accepts three extras:

- `command` — `list`, `get`, `save`, `validate`, `explain`, `run`, `delete`, `guide`.
- `id` — mode identifier for `get`/`run`/`delete`/`explain`.
- `json` — minified JSON for `save`/`validate`/`explain`.

The response is written to the app's cache as `mcp-response.json` and printed to `logcat -s NothingMcp`.

## JSON schema

```json
{
  "id": "my-mode",
  "name": "My Mode",
  "type": "MODE",
  "createdBy": "LLM",
  "status": "ARMED",
  "trigger": {"type": "manual"},
  "actions": [{"type": "wait", "durationMs": 0}],
  "enabled": true
}
```

- `id` — string, unique. Use only letters, digits, `-` and `_`.
- `name` — human-readable name.
- `type` — `MODE` or `ROUTINE` (deprecated; prefer `MODE`).
- `createdBy` — `LLM`, `USER`, or `IMPORT`.
- `status` — `ARMED`, `PENDING_APPROVAL`, `DISABLED`, `NEEDS_REVIEW`.
- `trigger` — a trigger object (see Trigger type reference below).
- `actions` — a list of action objects.
- `conditions` — optional condition object.
- `enabled` — boolean.
- `cooldownMs` — optional minimum time between runs.
- `quickAction` — optional, shows in widget/tile.

## Workflow for creating a mode

1. **Explain / design**: translate the user's spec into a trigger and one or more actions.
2. **Validate**: send the JSON with `command=validate`. The receiver returns:
   - `ok` — true if the mode can run on this device.
   - `explanation` — a human-readable summary.
   - `requiredCapabilities` — what the mode needs.
   - `missingCapabilities` — what is missing and why.
   - `validation` — a map of checks (`schema`, `actions`, `capabilities`).
3. **Fix if missing**: if `missingCapabilities` is not empty, the agent must not run the mode. Either change the spec or tell the user to grant the permission (e.g. notification listener, Shizuku, location).
4. **Save**: once validation is green, call `command=save` with the same JSON.
5. **Run (manual only)**: `command=run --es id <id>` starts a manual mode.

## Validation rules the agent must follow

- **Do not include real phone numbers in JSON sent to Git or MCP logs.** The mode data lives on the user's device and is acceptable; do not paste it into commit messages or documentation.
- **Only `manual` modes can be run over MCP.** Other triggers require real-world events.
- **Do not save a mode that the user did not approve.** The MCP is an agent tool, not an autonomous writer.
- **If `missingCapabilities` is non-empty, stop and explain.** Do not assume the permission can be silently granted.
- **Destructive actions need explicit confirmation.** `SetAirplaneMode`, `SetDataSaver`, `SetBatterySaver`, `SetHotspot`, `SendSms`, `LockScreen`, `TakeScreenshot`, and `WriteSetting` can change system state or cost money. Ask the user before saving.

## Trigger type reference

| type | Required fields | Notes |
|------|-----------------|-------|
| `manual` | none | Only trigger that can be run over `command=run`. |
| `time` | `cron` or `at` or `afterMs`, `tz` | Cron string in standard 5-field cron. `precision` can be `flexible` or `exact`. |
| `time_window` | `startLocal`, `endLocal`, `tz` | Active between the two local times. |
| `notification` | `pkg` | Optional `sender`, `titleMatch`, `textMatch`, `conversationId`, `isGroup`. |
| `phone_state` | `event` | Optional `number`, `textMatch`. Events: `incoming_call`, `call_ended`, `sms_received`. |
| `connectivity` | `medium`, `state` | Media: `wifi`, `bt`, `data`, `airplane`, `power`. States: `connected`, `disconnected`, `enabled`, `disabled`. |
| `boot` | none | Fires after boot. |
| `battery_level` | `level` | Optional `direction`: `charging_started`, `charging_stopped`. |
| `screen_state` | `state` | `on` or `off`. |
| `app_opened` | `pkg` | Fires when app comes to foreground. |
| `geofence` | `lat`, `lng`, `radiusM`, `transition` | Transition: `enter` or `exit`. |
| `bt_device` | `state` | Optional `deviceName`, `deviceAddress`. |
| `wifi_connected` | — | Optional `ssid`. |
| `calendar_event` | — | Optional `calendarId`, `titleMatch`, `direction` (`start`, `end`). |
| `charger_connected` | `connected` | Optional `source`: `ac`, `usb`, `wireless`, `dock`. |
| `device_unlocked` | none | |
| `device_locked` | none | |
| `torch_state` | `on` | Boolean. |
| `media_playback` | `playing` | Optional `packageName`. |

## Action type reference

| type | Key fields | Capability / warning |
|------|------------|----------------------|
| `wait` | `durationMs` | Safe. |
| `vibrate` | `durationMs` | Safe. |
| `copy_text` | `text` | Copies to clipboard. |
| `set_wifi` / `set_bluetooth` / `set_mobile_data` | `on` | Requires Shizuku on most devices. |
| `set_dnd` | `mode` (`off`, `priority`, `total`) | May require notification policy access. |
| `set_volume` | `volumes` map (`media`, `ring`, `alarm`, `notification`) | Stream-level only; no per-app volume. |
| `set_brightness` / `set_auto_brightness` / `set_extra_dim` / `set_screen_timeout` | `level`/`on`/`timeoutMs` | Requires `WRITE_SETTINGS` or Shizuku. |
| `set_wallpaper` | `uri`, `which` (`home`/`lock`) | Uses system photo picker URI. No storage permission needed if URI is persisted. |
| `set_stay_awake` | `on` | Sets `Settings.Global.STAY_ON_WHILE_PLUGGED_IN`. Requires Shizuku. |
| `set_airplane_mode` / `set_data_saver` / `set_hotspot` / `set_nfc` / `set_battery_saver` / `set_location_mode` / `set_auto_sync` / `set_aod` | `on` or `mode` | Requires Shizuku. |
| `set_refresh_rate` | `hz` | Requires `WRITE_SETTINGS`. |
| `set_screen_rotation` | `orientation` (`auto`, `portrait`, `landscape`) | Requires `WRITE_SETTINGS` or Shizuku. |
| `set_glyp*h*...` | many | Requires Nothing phone with Glyph hardware. See `docs/nothing-sdk.md`. |
| `glyph_turnoff` | none | Safe on Nothing hardware. |
| `media_control` | `command` (`play_pause`, `next`, `previous`, `stop`) | |
| `send_sms` | `number`, `text` | Requires `SEND_SMS` permission and a phone plan. |
| `lock_screen` | `force` | Requires device admin or accessibility. |
| `take_screenshot` | `force` | Requires `MediaProjection` / Shizuku. |
| `write_setting` | `namespace` (`system`/`secure`/`global`), `key`, `value` | Privileged; Shizuku. |
| `clear_notifications` | none | Requires notification listener. |
| `launch_app` | `packages` | Opens first available package. |
| `open_url` | `url`, `packageName` | Opens URL. Validate URL scheme. |
| `open_settings_screen` | `screen` | Opens a settings screen. |
| `show_notification` | `title`, `text` | Shows a local notification. |

For every action, `restore: true` means the previous value is restored when a windowed mode ends.

## Capability response fields

- `requiredCapabilities`: the set of `CapabilityIds` this mode needs.
- `missingCapabilities`: map of missing capability ID to the reason it is unavailable.

Common missing-capability reasons:

- `Notification listener access required` — the user must enable the app in system notification-access settings.
- `Shizuku required but not authorized` — Shizuku is not installed, not running, or not authorized.
- `Detected: may not work on this device` — the feature may require hardware not present.
- `Location permission required` — fine-location permission not granted.
- `Exact alarm not granted` — time triggers may be delayed.

## Moving MCP to non-debug builds

The receiver class and manifest entry can be moved from `app/src/debug/` to `app/src/main/` and guarded by `BuildConfig.DEBUG` or a build-time flag. This is intentionally not done yet; doing it later only requires:

1. Move `ModeControlReceiver.kt` to `app/src/main/`.
2. Add the receiver to `app/src/main/AndroidManifest.xml`.
3. Add an internal runtime flag or signature check so it only responds when USB debugging is enabled.
4. Keep `mcp-usb.py` in `tools/`.

Keep it debug-only until you are ready to review the security surface.
