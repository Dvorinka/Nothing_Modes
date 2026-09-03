# Nothing SDK Integration

## Overview

Nothing provides a unified Glyph SDK (`com.nothing.ketchum`) that handles both legacy Glyph (light stripe) and Glyph Matrix (dot matrix display). The SDK is distributed as an AAR file.

## SDK Source

- **Unified SDK AAR:** `glyph-matrix-sdk-2.0.aar` from [GlyphMatrix-Developer-Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
- **Legacy docs:** [Glyph-Developer-Kit](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) (covers Phone 1/2/2a/3a/4a/4b light stripe)
- **Matrix docs:** GlyphMatrix-Developer-Kit README (covers Phone 3/4a Pro matrix display)
- **Existing AAR in our repos:** `Dvorinka/Nothing-GlyphMatrix/app/libs/glyph-matrix-sdk-2.0.aar`

## License

The GlyphMatrix-Developer-Kit repo contains a `LICENSE.md` file. The SDK is provided under the Nothing Developer Programme terms. We use it as a dependency (AAR) only. We do not extract or redistribute proprietary Nothing assets.

## API Key

- **Android 16+ (API 36+):** API key restriction removed. No key needed.
- **Android 14-15 (API 34-35):** API key required in `AndroidManifest.xml`:
  ```xml
  <meta-data android:name="NothingKey" android:value="{API Key}"/>
  ```
- **Debug:** Use `android:value="test"` for development.
- **Recommendation:** Keep the `NothingKey` meta-data in manifest for stability across all system versions.

## Permission

```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE"/>
```

## Minimum Requirements

- Android 14 (API 34) or newer
- Nothing device
- Foreground application only (SDK does not work in background)

## Package

`com.nothing.ketchum`

## Device Detection — `Common` class

| Method | Device |
|---|---|
| `is20111()` | Phone (1) |
| `is22111()` | Phone (2) |
| `is23111()` | Phone (2a) |
| `is23113()` | Phone (2a) Plus |
| `is24111()` | Phone (3a) / Phone (3a) Pro |
| `is25111()` | Phone (4a) |
| `is25131()` | Phone (4b) |

## Device Constants — `Glyph` class

| Constant | Device | Glyph Type |
|---|---|---|
| `DEVICE_20111` | Phone (1) | Light stripe |
| `DEVICE_22111` | Phone (2) | Light stripe |
| `DEVICE_23111` | Phone (2a) | Light stripe |
| `DEVICE_23113` | Phone (2a) Plus | Light stripe |
| `DEVICE_24111` | Phone (3a) / (3a) Pro | Light stripe |
| `DEVICE_25111` | Phone (4a) | Light stripe |
| `DEVICE_25131` | Phone (4b) | Light stripe |
| `DEVICE_23112` | Phone (3) | Glyph Matrix (25x25) |
| `DEVICE_25111p` | Phone (4a) Pro | Glyph Matrix (13x13, AOD only, no touch) |

## Glyph Light Stripe API (Phone 1/2/2a/3a/4a/4b)

### GlyphManager

| Method | Description |
|---|---|
| `getInstance(Context)` | Get GlyphManager instance |
| `init(Callback)` | Bind to service (call in onCreate) |
| `unInit()` | Unbind service (call in onDestroy) |
| `register()` | Verify app authorization |
| `register(String targetDevice)` | Register for specific device |
| `getGlyphFrameBuilder()` | Get GlyphFrame.Builder |
| `openSession()` | Open session (after onServiceConnected) |
| `closeSession()` | Close session |
| `toggle(GlyphFrame)` | Enable/disable channels |
| `animate(GlyphFrame)` | Breathing animation |
| `displayProgress(GlyphFrame, int)` | Progress on C1/D1 |
| `displayProgress(GlyphFrame, int, boolean)` | Progress with reverse |
| `displayProgressAndToggle(GlyphFrame, int, boolean)` | Toggle + progress |
| `turnOff()` | Turn off all glyphs |

### GlyphFrame.Builder

| Method | Description |
|---|---|
| `buildPeriod(int ms)` | Duration on |
| `buildCycles(int)` | Number of cycles |
| `buildInterval(int)` | Interval between cycles |
| `buildChannel(int)` | Set specific glyph channel |
| `buildChannelA()` - `buildChannelE()` | Set zone of glyphs |
| `build()` | Create GlyphFrame |

### GlyphManager.Callback

| Method | Description |
|---|---|
| `onServiceConnected(ComponentName)` | Service connected |
| `onServiceDisconnected(ComponentName)` | Service disconnected |

### Channel Layouts Per Device

#### Phone (1)
- A1: 0, B1: 1, C1-C4: 2-5, D1_1-D1_8: 7-14, E1: 6

#### Phone (2)
- A1: 0, A2: 1, B1: 2, C1_1-C1_16: 3-18, C2-C6: 19-23, D1_1-D1_8: 25-32, E1: 24

#### Phone (2a) / (2a) Plus
- A: 25, B: 24, C1-C24: 0-23

#### Phone (3a) / (3a) Pro
- A1-A11: 20-30, B1-B5: 31-35, C1-C20: 0-19

#### Phone (4a)
- A1-A6: 0-5

#### Phone (4b)
- A1-A4: 0-3

## Glyph Matrix API (Phone 3 / 4a Pro)

### GlyphMatrixManager

| Method | Description |
|---|---|
| `init(Callback)` | Bind to service |
| `unInit()` | Unbind service |
| `register(String target)` | Register for device (`Glyph.DEVICE_23112` or `Glyph.DEVICE_25111p`) |
| `setMatrixFrame(int[] color)` | Raw 25x25 (or 13x13) color array |
| `setMatrixFrame(GlyphMatrixFrame)` | Structured frame |
| `setAppMatrixFrame(int[] color)` | App-mode raw frame (system 20250801+) |
| `setAppMatrixFrame(GlyphMatrixFrame)` | App-mode structured frame (system 20250801+) |
| `closeAppMatrix()` | Close app matrix display |
| `turnOff()` | Turn off all glyphs |

**Important:** Use `setAppMatrixFrame` (not `setMatrixFrame`) for third-party app control. `setMatrixFrame` is for Glyph Toy services. App-mode requires system version 20250801 or later.

**Priority:** Glyph Toys have higher display priority than third-party app content. If user interacts with Glyph Button, the toy carousel overrides app content.

### GlyphMatrixFrame

| Method | Description |
|---|---|
| `render()` | Render all objects into matrix data array |

### GlyphMatrixFrame.Builder

| Method | Description |
|---|---|
| `addTop(GlyphMatrixObject)` | Add to top layer (above mid and low) |
| `addMid(GlyphMatrixObject)` | Add to middle layer |
| `addLow(GlyphMatrixObject)` | Add to bottom layer |
| `build(Context)` | Construct GlyphMatrixFrame |

**Limit:** Maximum 3 GlyphMatrixObjects per frame (1 per layer).

### GlyphMatrixObject

| Property | Range | Default |
|---|---|---|
| imageSource | Bitmap (1:1) | — |
| text | String | — |
| positionX | int | 0 |
| positionY | int | 0 |
| orientation | 0-360 degrees | 0 |
| scale | 0-200 | 100 |
| brightness | 0-255 | 255 |

### GlyphMatrixObject.Builder

```java
GlyphMatrixObject butterfly = new GlyphMatrixObject.Builder()
    .setImageSource(bitmap)
    .setScale(100)
    .setOrientation(0)
    .setPosition(0, 0)
    .setBrightness(255)
    .build();
```

### Matrix Dimensions

| Device | Matrix Size |
|---|---|
| Phone (3) | 25x25 |
| Phone (4a) Pro | 13x13 |

Use `Common.getDeviceMatrixLength()` for runtime detection.

## Glyph Toy Service (Optional)

For creating Glyph Toys that appear in the system's Glyph Toys manager:

```xml
<service android:name="com.nothing.demo.TestToy"
    android:exported="true">
    <intent-filter>
        <action android:name="com.nothing.glyph.TOY"/>
    </intent-filter>
    <meta-data android:name="com.nothing.glyph.toy.name" android:resource="@string/toy_name"/>
    <meta-data android:name="com.nothing.glyph.toy.image" android:resource="@drawable/preview"/>
</service>
```

### Glyph Button Events

| Event | Description |
|---|---|
| `EVENT_CHANGE` | Long press |
| `EVENT_AOD` | AOD tick (every minute, for AOD toys) |
| `action_down` | Touch down |
| `action_up` | Touch up |

### Guiding Users to Toys Manager

```java
Intent intent = new Intent();
intent.setComponent(new ComponentName("com.nothing.thirdparty",
    "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"));
startActivity(intent);
```

Requires system version 20250829 or later.

## Integration Strategy for Nothing Modes

### NothingGlyphProvider (Light Stripe — Phone 1/2/2a/3a/4a/4b)

Wraps `GlyphManager`:
- `init()` / `unInit()` — lifecycle
- `register()` — authorization
- `openSession()` / `closeSession()` — session
- `toggle(channels)` — on/off specific channels
- `turnOff()` — all off
- `animate(channels, period, cycles, interval)` — animation
- Device detection via `Common.isXXXXX()`

### NothingGlyphMatrixProvider (Matrix — Phone 3/4a Pro)

Wraps `GlyphMatrixManager`:
- `init()` / `unInit()` — lifecycle
- `register(device)` — authorization
- `setAppMatrixFrame(int[] colors)` — raw frame
- `setAppMatrixFrame(GlyphMatrixFrame)` — structured frame
- `closeAppMatrix()` — stop
- `turnOff()` — all off
- Matrix length via `Common.getDeviceMatrixLength()`

### Capability Detection

```
NothingGlyphProvider:
  - is Nothing device? (Common.isXXXXX())
  - has light stripe? (device != Phone 3, != Phone 4a Pro)
  - SDK available? (can load com.nothing.ketchum)
  - service connected? (onServiceConnected callback)

NothingGlyphMatrixProvider:
  - is Phone 3? (Glyph.DEVICE_23112)
  - is Phone 4a Pro? (Glyph.DEVICE_25111p)
  - SDK available?
  - service connected?
  - system version >= 20250801? (for setAppMatrixFrame)
```

### Limitations

1. **Foreground only** — SDK does not work in background. Automation actions must execute from a foreground context or use a foreground service briefly.
2. **Android 14+ minimum** — SDK does not work on Android 13 or lower.
3. **Nothing devices only** — SDK checks manufacturer. Non-Nothing devices return UNSUPPORTED.
4. **API key on Android < 16** — Need valid NothingKey or "test" key for debug.
5. **App matrix priority** — Glyph Toys override app content. If a toy is active, app content may not be visible.
6. **Phone (4a) Pro** — No Glyph Touch, AOD only, 13x13 matrix.
7. **System version requirements** — `setAppMatrixFrame` requires 20250801+. Toys manager intent requires 20250829+.

### Debug Mode

```bash
adb shell settings put global nt_glyph_interface_debug_enable 1
```

Auto-disables after 48 hours. Sends a notification when activated.
