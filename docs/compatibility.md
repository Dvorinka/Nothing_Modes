# Compatibility Matrix

## Primary Target

| Property | Value |
|---|---|
| Device | Nothing Phone (3) |
| Identifier | `Glyph.DEVICE_23112` |
| Android | 16 (API 36) / 17 (API 37) |
| Nothing OS | 4.1 / 5.0 |
| Glyph | Light stripe (via Glyph SDK) |
| Glyph Matrix | 25x25, with Glyph Touch |
| Matrix system version | 20250801+ for `setAppMatrixFrame` |

## Device Matrix

| Device | Identifier | Android | Glyph | Glyph Matrix | DND | Extra Dim | Brightness | Shizuku | Notes |
|---|---|---|---|---|---|---|---|---|---|
| Phone (3) | `DEVICE_23112` | 16-17 | Yes | 25x25 + Touch | Yes | Yes | Yes | Yes | Reference device |
| Phone (4a) Pro | `DEVICE_25111p` | 16+ | No | 13x13, AOD only | Yes | Yes | Yes | Yes | No Glyph Touch, AOD toys only |
| Phone (4a) | `DEVICE_25111` / `is25111()` | 14+ | Yes (6 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (4b) | `DEVICE_25131` / `is25131()` | 14+ | Yes (4 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (3a) / (3a) Pro | `DEVICE_24111` / `is24111()` | 14+ | Yes (36 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (2a) Plus | `DEVICE_23113` / `is23113()` | 14+ | Yes (26 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (2a) | `DEVICE_23111` / `is23111()` | 14+ | Yes (26 LEDs) | No | Yes | Yes | Yes | Yes | Light stripe only |
| Phone (2) | `DEVICE_22111` / `is22111()` | 12+ | Yes (33 LEDs) | No | Yes | Yes | Yes | Yes | End of major OS support |
| Phone (1) | `DEVICE_20111` / `is20111()` | 12+ | Yes (14 LEDs) | No | Yes | Yes | Yes | Yes | End of major OS support |

## Capability Status Legend

- **Yes** — Supported via public Android API or Nothing SDK
- **Shizuku** — Requires Shizuku when public API is insufficient
- **No** — Unsupported on this device

## Feature Capability Details

### DND (Do Not Disturb)

- **API:** `NotificationManager.setInterruptionFilter()` / `setNotificationPolicy()`
- **Permission:** `ACCESS_NOTIFICATION_POLICY` (granted via system settings)
- **All devices:** Yes (standard Android API)

### Extra Dim

- **API:** `Settings.Secure.reduce_bright_colors_activated` (0/1)
- **Intensity:** `Settings.Secure.reduce_bright_colors_level` (0-100)
- **Permission:** `WRITE_SECURE_SETTINGS` (not granted to normal apps)
- **Fallback:** Shizuku `settings put secure reduce_bright_colors_activated 1`
- **Android 16 change:** Being integrated into brightness slider on some Pixel devices. Nothing devices may keep the toggle.
- **All Nothing devices:** Yes (via Shizuku) or limited (public API may not work without WRITE_SECURE_SETTINGS)

### Brightness

- **API:** `Settings.System.screen_brightness` (0-255)
- **Auto:** `Settings.System.screen_brightness_mode` (0=manual, 1=automatic)
- **Permission:** `WRITE_SETTINGS` (granted via system settings)
- **All devices:** Yes

### Screen Timeout

- **API:** `Settings.System.screen_off_timeout` (milliseconds)
- **Permission:** `WRITE_SETTINGS`
- **All devices:** Yes

### Dark Mode

- **API:** `UiModeManager.setNightMode()` (API 31+ — may be restricted to system apps)
- **Fallback:** Shizuku `cmd uimode night no|yes|auto`
- **All devices:** Yes (via Shizuku) or limited (UiModeManager may reject non-system apps)

### Volume

- **API:** `AudioManager.setStreamVolume()` for MEDIA, RING, ALARM, NOTIFICATION
- **Permission:** None for media/alarm. DND policy access for RING/NOTIFICATION to 0.
- **All devices:** Yes

### Glyph (Light Stripe)

- **API:** Nothing Glyph SDK (`com.nothing.ketchum.GlyphManager`)
- **Permission:** `com.nothing.ketchum.permission.ENABLE`
- **Min Android:** 14 (API 34)
- **Devices:** Phone (1), (2), (2a), (2a) Plus, (3a), (3a) Pro, (4a), (4b)
- **NOT on:** Phone (3) — uses Glyph Matrix instead (but SDK is unified)

### Glyph Matrix

- **API:** Nothing Glyph Matrix SDK (`com.nothing.ketchum.GlyphMatrixManager`)
- **Permission:** `com.nothing.ketchum.permission.ENABLE`
- **Min Android:** 14 (API 34)
- **Devices:** Phone (3) (25x25 + touch), Phone (4a) Pro (13x13, AOD only)
- **App mode:** Requires system version 20250801+ for `setAppMatrixFrame`

### Shizuku

- **All devices:** Yes (if Shizuku app installed and running)

## Runtime Detection

Nothing Modes does NOT hardcode device assumptions. At runtime:

1. `CapabilityDetector` identifies device model via `Build.MODEL`, `Build.MANUFACTURER`
2. Nothing SDK `Common.isXXXXX()` confirms Nothing device and specific model
3. Android version checked via `Build.VERSION.SDK_INT`
4. Nothing OS version detected via system properties (investigation needed)
5. Permission status checked per-feature
6. Shizuku status checked via `ShizukuGateway`
7. Nothing SDK availability checked via class loading / service binding

All results stored in `DeviceCapabilities` — a snapshot used by `CapabilityManager` and `CapabilityResolver`.

## Nothing OS 5.0

Verified on Nothing Phone (3), `C5.0-260924-2115`, Shizuku 13.6.

### Shell command changes

| Function | Nothing OS <=4.1 | Nothing OS 5.0 | Notes |
|---|---|---|---|
| Mobile data | `svc data enable\|disable` | `cmd phone data enable\|disable` | `svc data` on 5.0 exits 0 and changes nothing. Executor tries `cmd` first, `svc` only if it errors, then verifies via `mIsDataEnabled` in `dumpsys telephony.registry`. `global.mobile_data` is vestigial on 5.0; the live mirror is `global.mobile_data<subId>`. |
| Wi-Fi | `svc wifi` | `cmd wifi set-wifi-enabled enabled\|disabled` | `svc` variant still answers on 5.0 but is undocumented; kept as failure fallback. |
| Bluetooth | `svc bluetooth` | `cmd bluetooth_manager enable\|disable` | Same fallback pattern. |
| Airplane | `cmd connectivity airplane-mode` | unchanged | Verified via `global.airplane_mode_on`. |
| Location | `settings put secure location_mode` | unchanged | `cmd location set-location-enabled` also exists on 5.0 for the master switch; the mode key still works for all four modes. |
| Refresh rate | `secure.peak_refresh_rate` (Hz int) | `system.peak_refresh_rate` (float, e.g. `120.1`) | On 5.0 the secure key is a static mode id (128); writes land but do nothing. Executor writes the system float first, secure int as legacy fallback. |
| Battery saver | `settings put global low_power` | unchanged | Verified via the same key. `dumpsys power` reporting lags on 5.0 — trust `low_power`/`mSettingBatterySaverEnabled`. |
| Data saver | `cmd netpolicy set restrict-background` | unchanged | Verified via `cmd netpolicy get restrict-background`. `global.data_saver` is a dead key. |
| NFC | `svc nfc` (dead on Nothing OS) | still dead | Exits 0, adapter never toggles. Verification falls through to the NFC settings panel. Platform limitation on both versions. |

All Shizuku shell actions verify real device state after writing (polled read-back). A command that exits 0 without applying degrades to the settings panel instead of reporting success. Exit code 0 alone proved nothing on 5.0.

### New settings keys (usable via `WriteSetting` or the `Nothing feature` action + Shizuku)

Live-writable secure/global flags observed on 5.0 — all accept writes and hold state. Keys marked ★ are exposed as named choices in the `set_nothing_feature` action; the rest remain reachable through `WriteSetting`:

| Key | Feature |
|---|---|
| ★ `nt_generative_notification_enable` | Generative/AI notification summaries |
| ★ `nt_circle_to_search_support` | Circle to Search |
| `nt_essential_voice_action`, `nt_essential_voice_action_double_click`, `nt_essential_voice_action_hold`, `nt_block_essential_key` | Essential Key / Essential Space |
| `nt_flip_to_record_state` | Flip-to-record (Essential Recorder) |
| ★ `nt_mistouch_prevention_enable`, `nt_game_mode_gaming`, `nt_game_mode_mistouch_prevention`, `nt_game_mode_notification_display_mode` | Game mode |
| ★ `nt_face_recognition_unlock_with_mask` | Face unlock with mask |
| ★ `nt_private_space_auto_leaving_space` | Private space auto-exit |
| ★ `nt_power_off_verify` | Verification on power off |
| ★ `nt_generative_ringtone_enable`, `nt_generative_ringtone_enable_sim2` | Generative ringtones |
| `nt_lucent_theme_style` | Lucent theme variant (`echo`/`harmony`/`dramatic`) |
| `nt_wireless_reverse_charge` | Reverse charging mirror — writes but does NOT engage the coil; the `batteryshare` QS path goes through a Nothing service with no shell interface |
| `led_brightness_value`, `led_auto_brightness_enable`, `led_effect_*` (`call`/`charging`/`wlr_charging`/`music`/`gestures_flip`/`google_assistant`/`talk_reactive`/`volume_indicator`/`schedule_time`), `led_bed_time_custom_*` | Glyph light-stripe feature toggles and schedule |

New Nothing binder services on 5.0: `nothing.radio.ntphone` (INothingTelephony — likely the cellular path behind the new QS confirm modal), `nothing_ex_service`, `NtWifiService`/`NtWifiExtService` (driver-debug only), `dual_apps`, `nothing_thermal_mitigation_registry`, `com.nothing.camera.postproc`. None expose shell commands.

### Behavioral changes

- Toggling mobile data in the Quick Settings/internet panel now shows a Nothing confirmation modal. It is UI-side only — `cmd phone data` bypasses it. No settings key observed to disable it.
- OTA to 5.0 revoked the app's Shizuku grant — re-authorize once after the update.
