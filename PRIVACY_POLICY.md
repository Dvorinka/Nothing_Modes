# Privacy Policy

Nothing Modes is an open-source automation manager for Android, with optional Nothing Phone Glyph integration.

## Data Collection

**No personal data is collected.**

- No analytics, telemetry, or crash-reporting SDKs are included.
- No account or sign-in is required.
- No user data is sent to our servers, because we do not operate any backend for the app.

The app does make a small number of network requests, all initiated by the user or for user-facing features:

| Request | Purpose | Data sent |
|---|---|---|
| GitHub API `releases/latest` | Check for app updates when the user taps "Check for updates" | None (anonymous) |
| `raw.githubusercontent.com/.../templates/` | Load the community template index and template bundles | None (anonymous) |
| GitHub Releases APK download | Download an update APK when the user approves an update | None (anonymous, via device DownloadManager) |

## Data Storage

All user data is stored locally on the device:

- **Automation configurations**: Room database (internal app storage)
- **Execution audit log**: Room database (internal app storage, capped at 50 recent entries)
- **Theme preference**: SharedPreferences (internal app storage)
- **Exported JSON / templates**: User-selected location via Storage Access Framework

Uninstalling the app removes all internal data.

## Permissions

The app uses the following permissions to function. Some are required at all times; others are requested only when the user enables a matching feature.

| Permission | Purpose | Required? |
|---|---|---|
| `INTERNET` | Check for updates and load community templates | Yes |
| `RECEIVE_BOOT_COMPLETED` | Reschedule automations after reboot | Yes |
| `SCHEDULE_EXACT_ALARM` | Fire time-based triggers precisely | Yes |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Process triggers in background | Yes |
| `POST_NOTIFICATIONS` | Show automation notifications | Yes |
| `WRITE_SETTINGS` | Change brightness, screen timeout, auto-rotate | Yes |
| `ACCESS_NOTIFICATION_POLICY` | Enable Do Not Disturb | Yes |
| `CAMERA` | Toggle flashlight | Yes |
| `VIBRATE` | Vibration feedback | Yes |
| `WAKE_LOCK` | Keep the device awake during automation execution | Yes |
| `QUERY_ALL_PACKAGES` | Show the list of installed apps for the "Launch app" action | Yes |
| `REQUEST_INSTALL_PACKAGES` | Install downloaded app updates (used only for GitHub builds) | Yes |
| `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE` | Detect Wi-Fi connect/disconnect triggers | Optional |
| `BLUETOOTH` / `BLUETOOTH_CONNECT` | Detect Bluetooth connect/disconnect triggers | Optional |
| `READ_PHONE_STATE` | Detect incoming calls | Optional (call triggers) |
| `RECEIVE_SMS` / `SEND_SMS` | Read/send SMS content for SMS triggers and actions | Optional (SMS features) |
| `READ_CALENDAR` | Trigger on calendar events | Optional (calendar trigger) |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` / `ACCESS_BACKGROUND_LOCATION` | Geofence triggers | Optional (location triggers) |
| `PACKAGE_USAGE_STATS` | Detect foreground app | Optional (app triggers) |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Notification triggers and clear notifications | Optional (notification features) |
| `com.nothing.ketchum.permission.ENABLE` / `QUERY_GLYPH_STATE` / `CONTROL_GLYPH` | Nothing Phone Glyph integration | Optional (Glyph features) |
| `moe.shizuku.manager.permission.API_V23` | Communicate with the Shizuku app for privileged actions | Optional (Shizuku mode) |

## Shizuku

The app optionally integrates with Shizuku for privileged operations such as toggling Wi-Fi, Bluetooth, mobile data, airplane mode, and writing system settings. Shizuku runs as a separate app and manages its own permissions. Nothing Modes communicates with Shizuku via local IPC, not over the network.

## Open Source

The full source code is available at the project repository. The app is licensed under GPL-3.0.
