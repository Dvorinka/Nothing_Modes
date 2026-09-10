package com.tdvorak.nothingmodes.ui.components

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
                    fontFamily = NothingFonts.doto(),
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
