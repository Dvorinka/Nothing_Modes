# Nothing Modes — User Guide

## Installation

### From APK

1. Download the APK from GitHub Releases
2. Enable "Install from unknown sources" for your file manager
3. Install the APK
4. Open Nothing Modes

### From F-Droid

Search for "Nothing Modes" in F-Droid and install.

## First Run

The app ships with two sample automations: **Sleep** and **Morning**.

1. **Grant permissions**: Open Settings (gear icon) and grant each permission marked "Not granted"
2. **Set up Shizuku** (optional, for WiFi/Bluetooth/mobile data toggles):
   - Install Shizuku from [shizuku.ketchum](https://shizuku.ketchum)
   - Start Shizuku via ADB or root
   - Return to Nothing Modes Settings and tap "Request Permission"
3. **Test Glyph**: Open Settings > "Glyph preview" to see available visual patterns

## Creating Automations

### From a Template

1. Tap the **+** button on the main screen
2. Choose a preset: Sleep, Morning, Work Focus, Movie, Meeting, Custom
3. The automation is created and armed

### Custom Builder

1. Tap **+** then "Custom Builder"
2. **WHEN**: Select a trigger (time, location, battery, notification, etc.)
3. **IF**: Optionally add conditions (battery level, WiFi connected, day of week, etc.)
4. **THEN**: Add actions (brightness, DND, volume, glyph, launch app, etc.)
5. Tap **Save**

## Managing Automations

- **Enable/Disable**: Toggle the switch on each card
- **Edit**: Tap a card to open details, then tap "Edit"
- **Duplicate**: Tap the detail screen's duplicate button
- **Delete**: Tap the trash icon on a card
- **Reorder**: Use the up/down arrows to change priority (higher priority executes last, overwriting lower)

## Modes vs Routines

- **Mode**: Activates at a start time and deactivates at an end time, restoring previous settings
- **Routine**: Fires once, executes actions, does not restore

## Glyph Interface

### Light Stripe (Phone 1/2/2a/3a/4a/4b)

- Toggle individual LED zones (A/B/C/D/E)
- Animate with breathing effect
- Display progress bars (0-100%)
- Visual presets: sleep, morning, charging, timer, call, SMS

### Glyph Matrix (Phone 3/4a Pro)

- Display text (short strings)
- Display scrolling text (marquee)
- Display images (bitmaps)
- Display structured frames with layers
- Battery level fill
- Notification count

### Glyph Toy

On Phone 3/4a Pro, Nothing Modes registers as a Glyph Toy:
- **Long press**: Cycle through mode visualizations
- **AOD**: Display active mode on always-on display
- Manage in Settings > Glyph > Toys Manager

## Backup

1. Settings > Backup > Export
2. Choose a location to save the JSON file
3. To restore: Settings > Backup > Import and select the JSON file

## Execution Log

Tap the history icon to view the execution timeline:
- Each event shows type, automation ID, and timestamp
- Statistics summary shows fire count, success rate, suppressions

## Troubleshooting

### Automations not firing

- Check the automation is enabled (switch is on)
- Check battery optimization is not killing the app
- Grant "SCHEDULE_EXACT_ALARM" permission (Settings > Alarms & reminders)
- For time-based triggers, ensure the time zone is correct

### Notification triggers not working

- Grant Notification Listener access in Settings
- The listener service must be enabled in Android Settings > Notification access

### Location triggers not working

- Grant ACCESS_FINE_LOCATION
- Location must be enabled in Android Settings
- Geofence monitoring requires Google Play Services

### Shizuku actions failing

- Ensure Shizuku is running (not just installed)
- Ensure Nothing Modes is authorized in Shizuku
- Shizuku must be restarted after device reboot

### Glyph not responding

- Ensure the device is a Nothing Phone
- Check Glyph permissions are granted
- Try toggling Glyph off and on in Settings
