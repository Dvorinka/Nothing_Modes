package com.tdvorak.nothingmodes.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nothing Design System reusable components.
 *
 * Principles:
 * - Flat surfaces, border separation. No shadows.
 * - Labels: Space Mono, ALL CAPS, wide tracking.
 * - Containers: spacing > dividers > borders > surface cards.
 * - Red accent: one per screen, never decorative.
 */

// ── Cards ────────────────────────────────────────────────────────────────────

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    borderless: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        shape = NothingShapes.card,
        border = if (borderless) null else BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(NothingSpacing.cardPadding)) {
            content()
        }
    }
}

@Composable
fun NothingCardLarge(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, NothingShapes.cardLarge),
    ) {
        Column(modifier = Modifier.padding(NothingSpacing.lg)) {
            content()
        }
    }
}

// ── Section Header ───────────────────────────────────────────────────────────

@Composable
fun NothingSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        modifier = modifier.padding(
            top = NothingSpacing.lg,
            bottom = NothingSpacing.sm,
        ),
    )
}

// ── Label (Space Mono, ALL CAPS) ─────────────────────────────────────────────

@Composable
fun NothingLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        letterSpacing = 1.0.sp,
        modifier = modifier,
    )
}

// ── Info Row (label left, value right) ───────────────────────────────────────

@Composable
fun NothingInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NothingLabel(text = label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontFamily = SpaceMono,
        )
    }
}

// ── Divider ──────────────────────────────────────────────────────────────────

@Composable
fun NothingDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
        modifier = modifier,
    )
}

// ── Red Accent Dot ───────────────────────────────────────────────────────────

@Composable
fun NothingRedDot(
    modifier: Modifier = Modifier,
    size: Float = 4f,
) {
    Canvas(modifier = modifier.size(size.dp)) {
        drawCircle(
            color = NothingColors.accent,
            radius = (size / 2).dp.toPx(),
        )
    }
}

// ── Dot Matrix Background ────────────────────────────────────────────────────

@Composable
fun NothingDotGrid(
    modifier: Modifier = Modifier,
    dotSize: Float = 1.5f,
    spacing: Float = 16f,
    alpha: Float = 0.08f,
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val spacingPx = spacing.dp.toPx()
        val dotRadiusPx = (dotSize / 2).dp.toPx()
        val dotColor = Color.White.copy(alpha = alpha)

        var x = 0f
        while (x <= canvasWidth) {
            var y = 0f
            while (y <= canvasHeight) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadiusPx,
                    center = Offset(x, y),
                )
                y += spacingPx
            }
            x += spacingPx
        }
    }
}

// ── Toggle / Switch ──────────────────────────────────────────────────────────

@Composable
fun NothingToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "toggle",
    )

    val trackWidth = 44.dp
    val trackHeight = 24.dp
    val thumbSize = 18.dp
    val thumbPadding = 3.dp

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(CircleShape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbPadding + ((trackWidth - thumbSize - thumbPadding * 2) * thumbOffset))
                .size(thumbSize)
                .clip(CircleShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        )
    }
}

// ── Segmented Control ────────────────────────────────────────────────────────

@Composable
fun NothingSegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(NothingShapes.technical)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                NothingShapes.technical,
            ),
    ) {
        segments.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onSelected(index) },
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.wrapContentSize(Alignment.Center),
                )
            }
        }
    }
}

// ── Button Variants ──────────────────────────────────────────────────────────

@Composable
fun NothingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        shape = NothingShapes.pill,
        modifier = modifier
            .height(44.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.wrapContentSize(Alignment.Center),
        )
    }
}

@Composable
fun NothingSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Transparent,
        shape = NothingShapes.pill,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.wrapContentSize(Alignment.Center),
        )
    }
}

@Composable
fun NothingGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun NothingDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Transparent,
        shape = NothingShapes.pill,
        border = BorderStroke(1.dp, NothingColors.accent),
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = NothingColors.accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.wrapContentSize(Alignment.Center),
        )
    }
}

// ── Tag / Chip ───────────────────────────────────────────────────────────────

@Composable
fun NothingTag(
    text: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = Color.Transparent,
        shape = NothingShapes.pill,
        border = BorderStroke(
            1.dp,
            if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

// ── Status Indicator ─────────────────────────────────────────────────────────

@Composable
fun NothingStatusDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Float = 6f,
) {
    Canvas(modifier = modifier.size(size.dp)) {
        drawCircle(
            color = color,
            radius = (size / 2).dp.toPx(),
        )
    }
}

// ── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun NothingEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = NothingSpacing.xl, vertical = NothingSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Top Bar Action ───────────────────────────────────────────────────────────

data class TopBarAction(
    val label: String,
    val icon: ImageVector? = null,
    val onClick: () -> Unit,
)

// ── Nothing Top Bar ──────────────────────────────────────────────────────────

@Composable
fun NothingTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: List<TopBarAction> = emptyList(),
    showLeadingDot: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(88.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = NothingSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Back button — circular, thin chevron
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "<",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.width(NothingSpacing.md))
        }

        // Optional leading red dot — Nothing brand mark for the home screen
        if (showLeadingDot) {
            NothingRedDot(size = 8f)
            Spacer(modifier = Modifier.width(NothingSpacing.sm))
        }

        // Title — Space Mono ALL CAPS, larger for visibility
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f),
        )

        // Actions — text labels with optional icon and larger tap targets
        actions.forEach { action ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = action.onClick)
                    .padding(horizontal = NothingSpacing.sm, vertical = NothingSpacing.sm),
            ) {
                action.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(NothingSpacing.xs))
                }
                Text(
                    text = action.label.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.0.sp,
                    modifier = Modifier.padding(horizontal = NothingSpacing.xs),
                )
            }
        }
    }
}

// ── Nothing Pill Button ──────────────────────────────────────────────────────

@Composable
fun NothingPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        shape = NothingShapes.pill,
        modifier = modifier
            .height(48.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .wrapContentSize(Alignment.Center)
                .padding(horizontal = NothingSpacing.lg),
        )
    }
}

// ── Nothing Circle Button (icon + label) ─────────────────────────────────────

@Composable
fun NothingCircleButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Column(
        modifier = modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(1.dp, if (enabled) color else MaterialTheme.colorScheme.outline, CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) color else MaterialTheme.colorScheme.outline,
                fontFamily = SpaceMono,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            fontFamily = SpaceMono,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Nothing Input (underline style) ──────────────────────────────────────────

@Composable
fun NothingInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NothingLabel(
            text = label,
            modifier = Modifier.padding(bottom = NothingSpacing.xs),
        )
        androidx.compose.material3.OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, color = MaterialTheme.colorScheme.outline) }
            } else null,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMono),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            shape = NothingShapes.technical,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Enum Selector (inline accordion with Nothing borders) ─────────────────────

@Composable
fun NothingEnumSelector(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        NothingLabel(
            text = label,
            modifier = Modifier.padding(bottom = NothingSpacing.xs),
        )
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = NothingShapes.technical,
            border = BorderStroke(
                1.dp,
                if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = value.uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = SpaceMono,
                )
                Text(
                    text = if (expanded) "▴" else "▾",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = NothingShapes.technical,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        if (index > 0) NothingDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(option)
                                    expanded = false
                                }
                                .padding(
                                    horizontal = NothingSpacing.md,
                                    vertical = NothingSpacing.md,
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = option.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = SpaceMono,
                            )
                            if (option.equals(value, ignoreCase = true)) {
                                Text(
                                    text = "●",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NothingColors.accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Segmented Progress Bar ───────────────────────────────────────────────────

@Composable
fun NothingSegmentedBar(
    total: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    fillColor: Color = MaterialTheme.colorScheme.primary,
    height: Float = 8f,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0 until total) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp)
                    .clip(NothingShapes.technical)
                    .background(
                        if (i < filled) fillColor
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

// ── Circular Icon Area (monoline, no fill) ───────────────────────────────────

/**
 * Circular monoline icon container. Surface background, thin border, monochrome
 * glyph drawn via [content]. No shadows, no fills, no multi-color icons.
 *
 * Design tokens: surface bg, 1px border-visible, 12dp radius circle, 40–48dp.
 */
@Composable
fun NothingIconCircle(
    modifier: Modifier = Modifier,
    size: Float = 44f,
    accent: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (accent) NothingColors.accent else MaterialTheme.colorScheme.outline,
                CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ── Selection Top Bar ────────────────────────────────────────────────────────

/**
 * Top bar for multi-selection mode. Shows count + action labels.
 * Replaces the standard top bar while selection is active.
 *
 * Actions are Space Mono ALL CAPS text labels (no icons). One accent moment
 * allowed (typically the destructive action).
 */
@Composable
fun NothingSelectionTopBar(
    count: Int,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    actions: List<TopBarAction> = emptyList(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = NothingSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "[$count]",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp,
        )
        Spacer(modifier = Modifier.width(NothingSpacing.sm))
        Text(
            text = "SELECTED",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.0.sp,
            modifier = Modifier.weight(1f),
        )
        actions.forEach { action ->
            Text(
                text = action.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.0.sp,
                modifier = Modifier
                    .clickable(onClick = action.onClick)
                    .padding(horizontal = NothingSpacing.sm),
            )
        }
        Text(
            text = "[X]",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.0.sp,
            modifier = Modifier
                .clickable(onClick = onCancel)
                .padding(start = NothingSpacing.sm),
        )
    }
}

// ── Circular Add Button ──────────────────────────────────────────────────────

/**
 * Circular monoline "+" button. Surface bg, border-visible, accent "+" glyph.
 * Used as the create affordance when a pill would compete with content.
 */
@Composable
fun NothingAddCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Float = 56f,
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

