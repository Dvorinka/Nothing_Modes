package com.tdvorak.nothingmodes.ui.components

import android.Manifest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingPillButton
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing

/**
 * Prominent disclosure dialog shown before a sensitive runtime permission request.
 *
 * Styled to match the Nothing UI: wide card, mono body text, doto title,
 * and non-wrapping action labels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionDisclosureDialog(
    title: String,
    body: String,
    confirmText: String = "Continue",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = NothingShapes.shapes.medium,
            modifier =
                Modifier
                    .fillMaxWidth(0.9f)
                    .padding(horizontal = NothingSpacing.md),
        ) {
            Column(
                modifier = Modifier.padding(NothingSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = NothingFonts.sans(),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(NothingSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm, Alignment.End),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = dismissText,
                            fontFamily = NothingFonts.mono(),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Spacer(modifier = Modifier.width(NothingSpacing.sm))
                    NothingPillButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

/** Title and body shown before the system permission dialog. */
data class PermissionDisclosure(
    val title: String,
    val body: String,
)

/**
 * Prominent disclosure for one runtime permission. Play requires this inside
 * the app, immediately before the system prompt, naming the data and the use.
 */
fun permissionDisclosure(permission: String): PermissionDisclosure =
    when (permission) {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        ->
            PermissionDisclosure(
                title = "Location",
                body =
                    "Nothing Modes uses your location to run geofence automations and " +
                        "at-location conditions you create. Location is processed on your " +
                        "device and is never sold, shared, or transmitted.",
            )
        Manifest.permission.ACCESS_BACKGROUND_LOCATION ->
            PermissionDisclosure(
                title = "Background location",
                body =
                    "Nothing Modes needs location set to \"Allow all the time\" so a place " +
                        "you set can run a mode when the app is closed. On the next screen: " +
                        "Permissions, Location, Allow all the time. Location stays on your device.",
            )
        Manifest.permission.READ_CALENDAR ->
            PermissionDisclosure(
                title = "Calendar",
                body =
                    "Nothing Modes reads your calendar so a mode can run when an event " +
                        "starts or ends. It uses the title, time, and calendar you choose. " +
                        "Calendar data stays on your device and is never uploaded or sold.",
            )
        Manifest.permission.SEND_SMS ->
            PermissionDisclosure(
                title = "Send SMS",
                body =
                    "Nothing Modes sends a text only when a mode you create runs a Send SMS " +
                        "action, using the number and message you set. Message content is " +
                        "not uploaded, sold, or shared.",
            )
        Manifest.permission.RECEIVE_SMS ->
            PermissionDisclosure(
                title = "Receive SMS",
                body =
                    "Nothing Modes reads incoming texts only to run an SMS-received trigger " +
                        "you create. It can match the sender and, if you set a rule, the " +
                        "message text. Texts stay on your device and are never uploaded or sold.",
            )
        Manifest.permission.READ_PHONE_STATE ->
            PermissionDisclosure(
                title = "Phone state",
                body =
                    "Nothing Modes reads whether the phone is ringing, in a call, or idle " +
                        "so a call trigger or call-state condition can run. It does not " +
                        "read your call history unless you also set a caller-number filter.",
            )
        Manifest.permission.READ_CALL_LOG ->
            PermissionDisclosure(
                title = "Call log",
                body =
                    "Nothing Modes needs call-log access only to read the number of a call " +
                        "you asked to match. Android leaves that number blank without this " +
                        "permission. The app does not browse your call history, and the " +
                        "number stays on your device.",
            )
        Manifest.permission.RECORD_AUDIO ->
            PermissionDisclosure(
                title = "Microphone",
                body =
                    "Nothing Modes uses the microphone so the Glyph music visualizer can " +
                        "react to playback. Audio is analysed on your device and is never " +
                        "recorded, stored, sold, or shared.",
            )
        Manifest.permission.READ_CONTACTS ->
            PermissionDisclosure(
                title = "Contacts",
                body =
                    "Nothing Modes reads contacts only when you tap Pick contact, to fill " +
                        "in a phone number. Contact data stays on your device and is never " +
                        "uploaded, sold, or shared.",
            )
        Manifest.permission.POST_NOTIFICATIONS ->
            PermissionDisclosure(
                title = "Notifications",
                body =
                    "Nothing Modes posts a notification so automations can keep running in " +
                        "the background, and to show the result of a Show notification action. " +
                        "Notification text stays on your device.",
            )
        Manifest.permission.BLUETOOTH_CONNECT ->
            PermissionDisclosure(
                title = "Nearby devices",
                body =
                    "Nothing Modes reads the names of paired Bluetooth devices so a trigger, " +
                        "condition, or connect action can target one device. Names stay on " +
                        "your device.",
            )
        Manifest.permission.BLUETOOTH_SCAN ->
            PermissionDisclosure(
                title = "Nearby devices",
                body =
                    "Nothing Modes may scan for nearby Bluetooth devices when you pick a " +
                        "device. Scans are not used to work out your location.",
            )
        else ->
            PermissionDisclosure(
                title = "Permission",
                body =
                    "Nothing Modes uses this permission only for the automation you are " +
                        "setting up. Data stays on your device and is never sold or shared.",
            )
    }

/** One disclosure body for a gate that asks for several permissions at once. */
fun combinedDisclosure(permissions: List<String>): String =
    permissions.map { permissionDisclosure(it).body }.distinct().joinToString("\n\n")

/**
 * Special-access screens (not runtime permission dialogs). Play still wants
 * the data use named in the app before Android's settings page opens.
 */
enum class SpecialAccess {
    WRITE_SETTINGS,
    NOTIFICATION_POLICY,
    NOTIFICATION_LISTENER,
    USAGE_ACCESS,
    EXACT_ALARM,
    OVERLAY,
}

fun specialAccessDisclosure(access: SpecialAccess): PermissionDisclosure =
    when (access) {
        SpecialAccess.WRITE_SETTINGS ->
            PermissionDisclosure(
                title = "System settings",
                body =
                    "Nothing Modes changes a setting only when a mode you create includes " +
                        "that change, such as brightness or screen timeout. Android opens " +
                        "Modify system settings next. You can skip this.",
            )
        SpecialAccess.NOTIFICATION_POLICY ->
            PermissionDisclosure(
                title = "Do Not Disturb",
                body =
                    "Nothing Modes changes Do Not Disturb only when a mode you create turns " +
                        "it on or off. Android opens Notification policy access next. " +
                        "You can skip this.",
            )
        SpecialAccess.NOTIFICATION_LISTENER ->
            PermissionDisclosure(
                title = "Notification access",
                body =
                    "Nothing Modes reads the app, title, and text of notifications only to " +
                        "run a notification trigger or condition you create. That content " +
                        "stays on this phone and is not uploaded, sold, or shared. Android " +
                        "opens the notification access list next. You can skip this.",
            )
        SpecialAccess.USAGE_ACCESS ->
            PermissionDisclosure(
                title = "Usage access",
                body =
                    "Nothing Modes checks which app is in the foreground only for an " +
                        "app-opened trigger or an app-in-foreground condition you create. " +
                        "The check stays on this phone and is not uploaded. Android opens " +
                        "Usage access next. You can skip this.",
            )
        SpecialAccess.EXACT_ALARM ->
            PermissionDisclosure(
                title = "Exact alarms",
                body =
                    "Nothing Modes uses exact alarms so a time trigger you create can run " +
                        "at the minute you set. Android opens Alarms & reminders next. " +
                        "You can skip this. Time triggers may then run a few minutes late.",
            )
        SpecialAccess.OVERLAY ->
            PermissionDisclosure(
                title = "Display over other apps",
                body =
                    "Nothing Modes draws a dimming layer over the screen only while an " +
                        "ultra-dim mode you create is on. It does not read what is on " +
                        "screen. Android opens Display over other apps next.",
            )
    }

/** Screen-capture prompt for the Glyph visualizer. Not a runtime permission. */
fun audioCaptureDisclosure(): PermissionDisclosure =
    PermissionDisclosure(
        title = "Audio capture",
        body =
            "Nothing Modes asks Android for capture permission so the Glyph music " +
                "visualizer can hear playback, including headphones and Bluetooth. " +
                "The capture is for that visualizer only. Audio is analysed on this " +
                "phone and is not recorded, stored, or uploaded. The next screen is " +
                "Android's own capture prompt.",
    )
