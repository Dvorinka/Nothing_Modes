package com.tdvorak.nothingmodes.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Reusable Nothing OS-style UI components. */

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(NothingSpacing.cardPadding)) {
            content()
        }
    }
}

@Composable
fun NothingSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = NothingTypography.typography.labelMedium.letterSpacing,
        modifier = modifier.padding(top = NothingSpacing.sm, bottom = NothingSpacing.xs),
    )
}

@Composable
fun NothingInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun NothingDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline,
        thickness = 0.5.dp,
    )
}

/** Red accent dot — Nothing's signature visual element. */
@Composable
fun NothingRedDot(
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        androidx.compose.ui.geometry.Offset
        drawCircle(
            color = androidx.compose.ui.graphics.Color.Red,
            radius = 3.dp.toPx(),
        )
    }
}
