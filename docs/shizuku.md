# Shizuku Integration

## Overview

Shizuku is an optional privilege elevation framework. It allows apps to perform operations that normally require ADB or root, without actually having root. Nothing Modes uses Shizuku as a fallback when public Android APIs are insufficient.

## Repository

- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) — Apache 2.0
- Maven: `dev.rikka.shizuku:api:13.1.5` and `dev.rikka.shizuku:provider:13.1.5`

## Architecture (from Argus)

Argus implements a clean Shizuku gateway that we port directly:

### ShizukuGateway

State machine with 5 states:

```
NOT_INSTALLED → Shizuku app not on device
INSTALLED_NOT_RUNNING → app installed, service not started
RUNNING_NOT_AUTHORIZED → service running, user hasn't granted permission
AUTHORIZED → fully operational
UNSUPPORTED → pre-v11 Shizuku (too old)
```

Transitions are event-driven via Shizuku's binder listeners:
- `OnBinderReceivedListener` → service started
- `OnBinderDeadListener` → service stopped
- `OnRequestPermissionResultListener` → permission granted/denied

### PrivilegedShell

Interface for executing privileged shell commands. Implementation via Shizuku's `IUserService` mechanism:

1. App defines a `UserService` class that runs in Shizuku's process (shell UID)
2. App binds to the service via `Shizuku.newProcess()` or `Shizuku.bindUserService()`
3. Commands execute with shell privileges (UID 2000)

### Usage in Nothing Modes

Shizuku is used ONLY when:
1. A public Android API does not exist for the operation
2. A Nothing API does not cover the operation
3. The operation requires `settings put secure/global` (which needs WRITE_SECURE_SETTINGS permission)

**Typed commands only.** No arbitrary shell execution. Each Shizuku command is a typed class:

| Command | Purpose | Fallback for |
|---|---|---|
| `SetExtraDimCommand` | Toggle `reduce_bright_colors_activated` | Extra Dim when public API unavailable |
| `SetBrightnessCommand` | Set `screen_brightness` | Brightness when WRITE_SETTINGS denied |
| `SetScreenTimeoutCommand` | Set `screen_off_timeout` | Screen timeout when WRITE_SETTINGS denied |
| `SetDarkModeCommand` | `cmd uimode night no\|yes\|auto` | Dark mode when UiModeManager restricted |

## Permission Flow

1. User enables a Mode/Routine that requires Shizuku
2. App checks `ShizukuGateway.status()`
3. If `NOT_INSTALLED`: show "Install Shizuku" with link
4. If `INSTALLED_NOT_RUNNING`: show "Start Shizuku" instructions
5. If `RUNNING_NOT_AUTHORIZED`: call `ShizukuGateway.requestPermission()`
6. If `AUTHORIZED`: proceed with operation
7. If permission denied: show rationale, offer retry

## States to Test

| State | Expected Behavior |
|---|---|
| Not installed | Feature shows UNSUPPORTED, no crash |
| Installed, not running | Feature shows "Shizuku not running", no crash |
| Running, not authorized | Permission request shown, no crash |
| Authorized | Feature works via Shizuku |
| Permission revoked mid-execution | Action fails with SHIZUKU_REQUIRED, logged |
| Process restarted | Gateway detects via binder dead listener, status updates |
| Phone rebooted | Gateway re-evaluates status on next app interaction |

## Security

- **No arbitrary shell:** All commands are typed classes with fixed command templates
- **No AI-generated commands:** AI can only generate typed actions, never shell commands
- **No imported shell commands:** Import validates against closed action vocabulary
- **User consent:** Shizuku permission requires explicit user action
- **Audit:** All Shizuku operations logged in execution journal
