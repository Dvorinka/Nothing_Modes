# Security Policy

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| latest  | :white_check_mark: |
| < latest| :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in Nothing Modes, please report it
responsibly:

1. **Do NOT open a public GitHub issue.**
2. Email your findings to info@tdvorak.dev with the subject line
   `[SECURITY] Nothing Modes vulnerability`.
3. Include a clear description of the issue, steps to reproduce, and potential
   impact.
4. You will receive an acknowledgment within 48 hours.

## Security Measures

Nothing Modes implements the following security measures:

- **WriteSettingPolicy validation**: All `Action.WriteSetting` actions are validated
  against a key/value format policy before being executed via Shizuku or public APIs.
- **URL scheme restriction**: `Action.OpenUrl` only accepts `http` and `https` schemes.
- **Package name validation**: `Action.LaunchApp` validates package name format
  before dispatching intents.
- **Receiver permissions**: Exported receivers (FlipReceiver, NothingModesToyService)
  require `com.nothing.ketchum.permission.ENABLE` to prevent unauthorized triggering.
- **No hardcoded secrets**: API keys and credentials are not stored in the repository.
- **Backup disabled**: `android:allowBackup="false"` prevents automation data extraction.
- **kotlinx.serialization**: Uses safe sealed-class deserialization (no arbitrary class
  instantiation from untrusted JSON).

## Shizuku Security

Shizuku integration uses `ProcessBuilder` with `List<String>` arguments (not `sh -c`),
preventing classic shell injection. User-controlled values pass through
`WriteSettingPolicy` validation before reaching privileged commands.
