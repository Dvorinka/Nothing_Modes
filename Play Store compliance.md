# Play Store compliance roadmap

This document tracks everything that must be resolved before `Nothing Modes` can be published on the Google Play Store. It also defines the `github` / `play` product-flavor split, because the full-featured GitHub build uses permissions and capabilities that Play will not accept in the consumer store.

## Two-version strategy

| Build | Package | Channel | Feature set |
|---|---|---|---|
| `github` | `com.tdvorak.nothingmodes` | GitHub Releases, F-Droid, direct APK | Full features: in-app updates, all actions, Device Admin lock screen |
| `play` | `com.tdvorak.nothingmodes` | Google Play Store | Restricted: no in-app APK installer, no lock screen, reduced permission set |

Both versions share the same source code. The differences are controlled by product flavors in `app/build.gradle.kts` and a few `BuildConfig` feature flags in the UI. This keeps the GitHub build unchanged while producing a Play-compliant AAB.

> **Important signing note:** If the Play version uses Google Play App Signing with a Google-managed signing key, users who installed the GitHub version will not be able to update over it (signatures differ). The options are:
> 1. Give GitHub and Play different `applicationId` values (e.g. `com.tdvorak.nothingmodes.play` vs `com.tdvorak.nothingmodes`). This is the safest for a first release.
> 2. Upload the GitHub signing key to Play during first-time setup so Google uses the same key.
> 3. Accept that users must uninstall one version to switch channels.

This roadmap assumes option 2 is used: the same `applicationId` and the same release key for both channels. CI must sign both AAB and APK with the same upload key.

## Current baseline

- `applicationId`: `com.tdvorak.nothingmodes`
- `minSdk`: 28, `targetSdk`: 36, `compileSdk`: 36
- `versionCode`: 3, `versionName`: `0.10.0`
- Build type `release` runs R8/ProGuard and builds successfully after removing hardcoded keystore passwords.
- Privacy policy (`PRIVACY_POLICY.md`) is now accurate about permissions and network usage.
- `desloppify` strict score is `17.5/100` (security 100%, test health 0%, file health 88.8%, code quality 62.7%, duplication 90%).

## 1. Permissions and policy blockers

### 1.1 `QUERY_ALL_PACKAGES` — must be removed

**Why Play rejects it:** `QUERY_ALL_PACKAGES` is a restricted permission. It is normally approved only for launchers, antivirus, file managers, or app stores.

**What the app actually uses:** The "Launch app" action and the app picker call `queryIntentActivities()` with the standard `ACTION_MAIN / CATEGORY_LAUNCHER` intent. That does **not** require `QUERY_ALL_PACKAGES`.

**Cost / impact:** Low. Replace `QUERY_ALL_PACKAGES` with a `<queries>` block that declares the launcher intent. Both flavors keep the app-launch feature.

### 1.2 `REQUEST_INSTALL_PACKAGES` — must be removed from `play`

**Why Play rejects it:** In-app self-update via downloaded APK is not allowed. Apps must update through the Play Store.

**What the app uses:** `UpdateManager` downloads the latest GitHub release APK and launches the package installer.

**Cost / impact:**
- `github` flavor: unchanged.
- `play` flavor: remove the permission. The "Check for updates" UI must either be hidden or changed to open the Play Store listing. The `UpdateDownloadReceiver` must be disabled for `play`.
- Feature loss in Play version: in-app update check/install becomes "open Play Store".

### 1.3 `DeviceAdmin` / `BIND_DEVICE_ADMIN` — must be removed from `play`

**Why Play rejects it:** Device admin is effectively restricted to enterprise / MDM apps. Consumer apps with a "lock screen" action are routinely rejected.

**What the app uses:** `NothingDeviceAdminReceiver` to support the `LOCK_SCREEN` action.

**Cost / impact:**
- `github` flavor: unchanged.
- `play` flavor: remove the receiver and the `LOCK_SCREEN` action from the catalog. The action will not be available in the Play build.
- Feature loss in Play version: lock screen action is gone.

### 1.4 `WRITE_SETTINGS`, `SCHEDULE_EXACT_ALARM`, `FOREGROUND_SERVICE_SPECIAL_USE`, `ACCESS_NOTIFICATION_POLICY`

**Status:** Allowed, but each requires a Play Console permission declaration and a short video demonstrating the core feature.

**Cost / impact:** Low to medium. Prepare 15–30 second screen recordings for:
- Brightness / screen timeout / auto-rotate system settings change
- Creating a scheduled time trigger
- Background automation running
- Enabling Do Not Disturb

### 1.5 Optional sensitive permissions

These are requested only when the user creates a matching trigger or action and are declared as optional. They still need Play Console declarations with videos:

- `READ_PHONE_STATE`, `RECEIVE_SMS`, `SEND_SMS`, `READ_CALENDAR`
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`
- `BLUETOOTH`, `BLUETOOTH_CONNECT`
- `PACKAGE_USAGE_STATS`
- `BIND_NOTIFICATION_LISTENER_SERVICE`
- `com.nothing.ketchum.permission.*` (Nothing Glyph)
- `moe.shizuku.manager.permission.API_V23` (Shizuku)

**Cost / impact:** Medium. For each one, record a video showing the user enabling the feature and the app using it. If a permission is not core, consider hiding the related trigger/action in the `play` flavor to reduce declaration burden.

## 2. Build and release setup

### 2.1 Build AAB, not APK

Google Play requires an Android App Bundle (`.aab`). The current build produces an APK.

**Action:** Run `./gradlew :app:bundlePlayRelease` and upload `app/build/outputs/bundle/playRelease/app-play-release.aab`.

### 2.2 Signing key

- Generate an upload keystore and keep it in a password manager / CI secret.
- Set `NOTHING_MODES_KEYSTORE`, `NOTHING_MODES_KEYSTORE_PASSWORD`, `NOTHING_MODES_KEY_ALIAS`, `NOTHING_MODES_KEY_PASSWORD` in CI.
- Decide whether to let Google manage the final signing key or upload your own.

### 2.3 Versioning

Bump `versionCode` and `versionName` for every upload. `versionCode` must be a single integer and must increase.

## 3. Store listing assets

| Asset | Requirement | Status |
|---|---|---|
| 512×512 app icon | PNG or JPEG, max 1 MB | Need to produce or verify `mipmap-xxxhdpi/ic_launcher` |
| 1024×500 feature graphic | PNG or JPEG | Missing |
| Phone screenshots | 2–8 screenshots, 16:9 or 9:16 | `art/screenshots/` exist but may need Play-safe sizing |
| 7" tablet screenshots | Optional | Missing |
| 10" tablet screenshots | Optional | Missing |
| Short description | 80 chars | Missing |
| Full description | 4000 chars | Missing |
| Privacy policy URL | Public URL | `https://raw.githubusercontent.com/Dvorinka/Nothing_Modes/main/PRIVACY_POLICY.md` |

## 4. Console forms

- **Data safety:** declare that the app does not collect user data, and explain the anonymous network requests (update check, template fetch).
- **Content rating:** complete the questionnaire. Likely `Everyone` or `Teen` because of SMS/location features.
- **App content:** answer health, COVID-19, gambling, etc. (all no).
- **Target audience:** not designed for children.
- **Ads / monetization:** none.
- **Permissions declarations:** one per sensitive permission, with a video if required.

## 5. `github` vs `play` feature flag plan

Use Gradle product flavors and `BuildConfig` flags:

```kotlin
android {
    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"github\"")
            buildConfigField("boolean", "ENABLE_IN_APP_UPDATES", "true")
            buildConfigField("boolean", "ENABLE_LOCK_SCREEN", "true")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"play\"")
            buildConfigField("boolean", "ENABLE_IN_APP_UPDATES", "false")
            buildConfigField("boolean", "ENABLE_LOCK_SCREEN", "false")
        }
    }
}
```

Then guard the relevant code:

```kotlin
if (BuildConfig.ENABLE_IN_APP_UPDATES) { /* show check-for-updates UI */ }
if (BuildConfig.ENABLE_LOCK_SCREEN) { /* show lock screen action */ }
```

### Flavor-specific manifest overrides

- `github` source set includes `REQUEST_INSTALL_PACKAGES` and `DeviceAdmin` receiver.
- `play` source set omits `REQUEST_INSTALL_PACKAGES`, `DeviceAdmin` receiver, and the `QUERY_ALL_PACKAGES` permission. It adds a `<queries>` block for `ACTION_MAIN / CATEGORY_LAUNCHER`.

## 6. What will not work in the `play` build

| Feature | `github` | `play` | Notes |
|---|---|---|---|
| In-app APK download/install | Yes | No | Replaced by "Open Play Store" link |
| Lock screen action | Yes | No | Device Admin removed |
| All other actions (DND, brightness, Wi-Fi, BT, Glyph, etc.) | Yes | Yes | Justified as core automation |
| App launch picker | Yes | Yes | Uses `<queries>` instead of `QUERY_ALL_PACKAGES` |

## 7. Code health blockers

`desloppify` found 556 open issues. The most important for a Play release are:

1. **Test health 0%** — 145 missing tests. Add unit tests for `engine-core` (`Engine`, `ImportExportService`, `CapabilityResolver`, `ActionExecutor`) and at least smoke tests for the UI ViewModels.
2. **149 unused imports** — install `ktlint` and run `ktlint -F` to clean these.
3. **20 subjective dimensions unassessed** — run `desloppify review` or a manual code-quality pass before production.
4. **Deprecated API usage** (e.g. `LocalLifecycleOwner` import, deprecated `BluetoothAdapter` methods in `RealActionExecutor`) — not blockers but will be flagged during review.

## 8. Action checklist before first Play upload

- [ ] Create `github` and `play` product flavors with `BuildConfig` feature flags.
- [ ] Remove `QUERY_ALL_PACKAGES` and add `<queries>` block.
- [ ] Move `REQUEST_INSTALL_PACKAGES` and in-app update receiver to `github` flavor only.
- [ ] Move `DeviceAdmin` receiver and `LOCK_SCREEN` action to `github` flavor only.
- [ ] Build and test both `:app:assembleGithubRelease` and `:app:bundlePlayRelease`.
- [ ] Install `ktlint`, run format, fix remaining issues.
- [ ] Add engine-core and ViewModel unit tests.
- [ ] Generate 512×512 icon and 1024×500 feature graphic.
- [ ] Re-capture phone screenshots at Play-safe sizes.
- [ ] Write short and full Play Store descriptions.
- [ ] Create permission declaration videos.
- [ ] Complete Play Console data safety, content rating, and app content forms.
- [ ] Upload AAB to internal testing track and expect at least one policy review/appeal.

## 9. Cost summary

| Change | Dev effort | User-visible cost |
|---|---|---|
| Product flavors + feature flags | 2–3 hours | None for GitHub users; Play users lose lock screen and in-app update |
| Replace `QUERY_ALL_PACKAGES` | 30 minutes | None |
| Permission declaration videos | 2–3 hours one-time | None |
| Privacy policy / data safety | 1 hour | None |
| Test coverage | 1–2 days | None |
| ktlint / code cleanup | 2–4 hours | None |
| Store assets (icon, graphic, screenshots) | 1–2 days | None |
| Play Console setup and first review cycle | 1–2 weeks | None |

## 10. Biggest risk

The combination of `DeviceAdmin`, `REQUEST_INSTALL_PACKAGES`, `QUERY_ALL_PACKAGES`, and Shizuku-backed system-setting changes means the Play review team will scrutinize this app closely. Even after all the changes above, the first submission may still receive a rejection. The safest commercial path is:

1. Start with **GitHub Releases** and a small closed beta.
2. Run an **internal Play Store track** while fixing policy issues.
3. Only move to **production** after a successful closed-test review.
