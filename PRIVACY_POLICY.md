# Privacy Policy

Nothing Modes is an offline automation manager for Nothing Phone devices.

## Data Collection

**None.** The app does not collect, transmit, or share any personal data.

- No internet permission is declared in the manifest.
- No analytics, telemetry, or crash reporting SDKs are included.
- No network requests are made by the app.

## Data Storage

All data is stored locally on the device:

- **Automation configurations**: Room database (internal app storage)
- **Execution audit log**: Room database (internal app storage, capped at 50 recent entries)
- **Theme preference**: SharedPreferences (internal app storage)
- **Exported JSON**: User-selected location via Storage Access Framework

No data is synced to any server. Uninstalling the app removes all stored data.

## Permissions

The app requests the following permissions to function:

| Permission | Purpose | Required? |
|---|---|---|
| WRITE_SETTINGS | Change brightness, screen timeout, auto-rotate | Yes |
| ACCESS_NOTIFICATION_POLICY | Enable Do Not Disturb | Yes |
| RECEIVE_BOOT_COMPLETED | Reschedule automations after reboot | Yes |
| SCHEDULE_EXACT_ALARM | Fire time-based triggers precisely | Yes |
| FOREGROUND_SERVICE | Process triggers in background | Yes |
| POST_NOTIFICATIONS | Show automation notifications | Yes |
| CAMERA | Toggle flashlight | Yes |
| VIBRATE | Vibration feedback | Yes |
| READ_PHONE_STATE | Detect incoming calls | Optional (call triggers) |
| RECEIVE_SMS | Read SMS content for SMS triggers | Optional (SMS triggers) |
| ACCESS_FINE_LOCATION | Geofence triggers | Optional (location triggers) |
| PACKAGE_USAGE_STATS | Detect foreground app | Optional (app triggers) |
| BIND_NOTIFICATION_LISTENER_SERVICE | Notification triggers | Optional (notification triggers) |

## Shizuku

The app optionally integrates with Shizuku for privileged operations (WiFi, Bluetooth, mobile data, airplane mode toggles). Shizuku runs as a separate app and manages its own permissions. Nothing Modes communicates with Shizuku via IPC, not over the network.

## Open Source

The app is open source. The full source code is available at the project repository.
