package com.tdvorak.nothingmodes.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tdvorak.nothingmodes.data.feedback.Feedback
import com.tdvorak.nothingmodes.ui.theme.NothingColors
import com.tdvorak.nothingmodes.ui.theme.NothingFonts
import com.tdvorak.nothingmodes.ui.theme.NothingInput
import com.tdvorak.nothingmodes.ui.theme.NothingSegmentedControl
import com.tdvorak.nothingmodes.ui.theme.NothingShapes
import com.tdvorak.nothingmodes.ui.theme.NothingSpacing
import kotlinx.coroutines.launch

private val CATEGORIES = listOf("bug", "idea", "other")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var categoryIndex by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    // null = untouched, true = sent, false = failed
    var sent by remember { mutableStateOf<Boolean?>(null) }

    BasicAlertDialog(onDismissRequest = { if (!sending) onDismiss() }) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = NothingShapes.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(NothingSpacing.md),
                verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
            ) {
                Text(
                    text = "Send feedback",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = NothingFonts.sans(),
                )
                Text(
                    text =
                        "Goes straight to the developer's inbox. " +
                            "App version and device model are attached; nothing else.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
                NothingSegmentedControl(
                    segments = CATEGORIES.map { it.uppercase() },
                    selectedIndex = categoryIndex,
                    onSelected = { categoryIndex = it },
                )
                NothingInput(
                    value = message,
                    onValueChange = { message = it.take(4000) },
                    label = "Message",
                    placeholder = "What happened, or what would you change?",
                    singleLine = false,
                )
                NothingInput(
                    value = contact,
                    onValueChange = { contact = it.take(200) },
                    label = "Email (optional)",
                    placeholder = "Only if you want a reply",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                sent?.let { ok ->
                    Text(
                        text =
                            if (ok) {
                                "[ SENT — THANK YOU ]"
                            } else {
                                "[ SEND FAILED — CHECK CONNECTION ]"
                            },
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (ok) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                NothingColors.accent
                            },
                        fontFamily = NothingFonts.mono(),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !sending,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (sent == true) "CLOSE" else "CANCEL",
                            fontFamily = NothingFonts.mono(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (sent != true) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    sending = true
                                    sent =
                                        Feedback.send(
                                            context,
                                            CATEGORIES[categoryIndex],
                                            message,
                                            contact,
                                        )
                                    sending = false
                                }
                            },
                            enabled = !sending && message.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                if (sending) "SENDING…" else "SEND",
                                fontFamily = NothingFonts.mono(),
                                color =
                                    if (!sending && message.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                }
            }
        }
    }
}
