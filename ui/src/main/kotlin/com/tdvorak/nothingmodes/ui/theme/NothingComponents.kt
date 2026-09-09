package com.tdvorak.nothingmodes.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// LocalUiStyle and ThemeManager are in the same package (com.tdvorak.nothingmodes.ui.theme).

/**
 * Nothing Design System reusable components.
 *
 * Principles:
 * - OLED-black canvas, dark grey (#161616-#1C1C1C) surfaces.
 * - Large, calm corner radii (24-28 dp) on cards and sheets.
 * - Dot-matrix (Doto) for hero numbers and large values.
 * - Monochrome icon chips (rounded square, ~48 dp, surfaceVariant fill).
 * - Nothing Red (#FF3030) used only as accent: selected filters, FAB, toggles, dots.
 */

// ── Cards ────────────────────────────────────────────────────────────────────

@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    borderless: Boolean = false,
    content: @Composable () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = NothingShapes.card,
        border =
            if (borderless) {
                null
            } else {
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                )
            },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(NothingSpacing.lg)) {
            content()
        }
    }
}

@Composable
fun NothingCardLarge(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = NothingShapes.cardLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(NothingSpacing.lg)) {
            content()
        }
    }
}

// ── Screen Hero (Doto headline + mono caption) ────────────────────────────────

@Composable
fun NothingScreenHero(
    title: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (classic) title else title.uppercase(),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = if (classic) null else Doto,
        )
        if (caption != null) {
            Spacer(modifier = Modifier.height(NothingSpacing.xxs))
            Text(
                text = if (classic) caption else caption.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.0.sp,
            )
        }
    }
}

// ── Section Header ───────────────────────────────────────────────────────────

@Composable
fun NothingSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Text(
        text = if (classic) text else text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.4.sp,
        modifier =
            modifier.padding(
                top = NothingSpacing.xl,
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
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Text(
        text = if (classic) text else text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        letterSpacing = 1.1.sp,
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
            fontFamily = NothingFonts.mono(),
        )
    }
}

// ── Divider ──────────────────────────────────────────────────────────────────

@Composable
fun NothingDivider(modifier: Modifier = Modifier) {
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
    val accent = NothingColors.accent
    Canvas(modifier = modifier.size(size.dp)) {
        drawCircle(
            color = accent,
            radius = (size / 2).dp.toPx(),
        )
    }
}

// ── Dot Row Indicator ────────────────────────────────────────────────────────

@Composable
fun NothingDotRow(
    total: Int,
    filled: Int,
    modifier: Modifier = Modifier,
    dotSize: Float = 6f,
    activeColor: Color = NothingColors.accent,
    spacing: Float = 4f,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
    ) {
        val count = total.coerceAtLeast(0)
        val active = filled.coerceIn(0, count)
        repeat(count) { index ->
            Box(
                modifier =
                    Modifier
                        .size(dotSize.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < active) {
                                activeColor
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
            )
        }
    }
}

enum class ModeDotState { ACTIVE, FIRING_SOON, ENABLED, DISABLED }

@Composable
fun ModeDotRow(
    states: List<ModeDotState>,
    modifier: Modifier = Modifier,
    dotSize: Float = 6f,
    spacing: Float = 4f,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val enabledColor = NothingColors.accent
    val disabledColor = MaterialTheme.colorScheme.surfaceVariant
    val infiniteTransition = rememberInfiniteTransition(label = "dot-pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dot-pulse",
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing.dp),
    ) {
        states.forEach { state ->
            val color =
                when (state) {
                    ModeDotState.ACTIVE -> activeColor.copy(alpha = pulseAlpha)
                    ModeDotState.FIRING_SOON -> Color.Transparent
                    ModeDotState.ENABLED -> enabledColor
                    ModeDotState.DISABLED -> disabledColor
                }
            Box(
                modifier =
                    Modifier
                        .size(dotSize.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (state == ModeDotState.FIRING_SOON) {
                                Modifier.border(1.5.dp, activeColor, CircleShape)
                            } else {
                                Modifier
                            },
                        ),
            )
        }
    }
}

// ── Dot Matrix Background ────────────────────────────────────────────────────

@Composable
fun NothingDotGrid(
    modifier: Modifier = Modifier,
    dotSize: Float = 1.5f,
    spacing: Float = 16f,
    alpha: Float = 0.12f,
) {
    val baseColor = MaterialTheme.colorScheme.outline
    val dotColor = baseColor.copy(alpha = alpha.coerceIn(0.10f, 0.20f))
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val spacingPx = spacing.dp.toPx()
        val dotRadiusPx = (dotSize / 2).dp.toPx()

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
    showLabels: Boolean = true,
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(200),
        label = "toggle",
    )

    val trackWidth = if (showLabels) 64.dp else 44.dp
    val trackHeight = 24.dp
    val thumbSize = 18.dp
    val thumbPadding = 3.dp

    Box(
        modifier =
            modifier
                .width(trackWidth)
                .height(trackHeight)
                .clip(CircleShape)
                .background(
                    if (checked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ).clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        if (showLabels) {
            Text(
                text = if (checked) "ON" else "OFF",
                modifier = Modifier.padding(horizontal = 8.dp).align(if (checked) Alignment.CenterStart else Alignment.CenterEnd),
                style = MaterialTheme.typography.labelSmall,
                color = if (checked) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier =
                Modifier
                    .padding(start = thumbPadding + ((trackWidth - thumbSize - thumbPadding * 2) * thumbOffset))
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    ),
        )
    }
}

// ── Segmented Control (filter pills) ─────────────────────────────────────────

@Composable
fun NothingSegmentedControl(
    segments: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(NothingShapes.pill)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline,
                    NothingShapes.pill,
                ),
    ) {
        segments.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(if (isSelected) NothingColors.accent else Color.Transparent)
                        .clickable { onSelected(index) },
            ) {
                Text(
                    text = if (classic) label else label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onTertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    textAlign = TextAlign.Center,
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
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Surface(
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        shape = NothingShapes.pill,
        modifier =
            modifier
                .defaultMinSize(minWidth = 80.dp)
                .height(48.dp)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = if (classic) text else text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .wrapContentSize(Alignment.Center)
                    .padding(horizontal = 24.dp),
        )
    }
}

@Composable
fun NothingPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    NothingPrimaryButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    )
}

@Composable
fun NothingCompactPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    val container =
        if (enabled) {
            NothingColors.accent
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    val content =
        if (enabled) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Surface(
        color = container,
        shape = NothingShapes.pill,
        modifier =
            modifier
                .defaultMinSize(minWidth = 64.dp)
                .height(32.dp)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (classic) text else text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = content,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
fun NothingSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Surface(
        color = Color.Transparent,
        shape = NothingShapes.pill,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier =
            modifier
                .height(48.dp)
                .clickable(enabled = enabled, onClick = onClick),
    ) {
        Text(
            text = if (classic) text else text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .wrapContentSize(Alignment.Center)
                    .padding(horizontal = 24.dp),
        )
    }
}

@Composable
fun NothingGhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    TextButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(
            text = if (classic) text else text.uppercase(),
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
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Surface(
        color = Color.Transparent,
        shape = NothingShapes.pill,
        border = BorderStroke(1.dp, NothingColors.accent),
        modifier =
            modifier
                .height(48.dp)
                .clickable(onClick = onClick),
    ) {
        Text(
            text = if (classic) text else text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = NothingColors.accent,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .wrapContentSize(Alignment.Center)
                    .padding(horizontal = 24.dp),
        )
    }
}

// ── Tag / Chip (filter pill) ─────────────────────────────────────────────────

@Composable
fun NothingTag(
    text: String,
    active: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        color = if (active) NothingColors.accent else Color.Transparent,
        shape = NothingShapes.pill,
        border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.then(clickable),
    ) {
        Text(
            text = if (classic) text else text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color =
                if (active) {
                    MaterialTheme.colorScheme.onTertiary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
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
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = NothingSpacing.xl, vertical = NothingSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        Column(
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        action?.invoke()
    }
}

// ── Top Bar Action ───────────────────────────────────────────────────────────

data class TopBarAction(
    val label: String,
    val icon: ImageVector? = null,
    val accent: Boolean = false,
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
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(88.dp)
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = NothingSpacing.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left side: back, dot, title
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back button — circular, thin chevron
            if (onBack != null) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "<",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = NothingFonts.mono(),
                    )
                }
                Spacer(modifier = Modifier.width(NothingSpacing.md))
            }

            // Optional leading red dot
            if (showLeadingDot) {
                NothingRedDot(size = 8f)
                Spacer(modifier = Modifier.width(NothingSpacing.sm))
            }

            // Title — Space Mono ALL CAPS
            Text(
                text = if (classic) title else title.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = NothingFonts.mono(),
                letterSpacing = 1.5.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Right side: icon chips, label only as a fallback
        Row(verticalAlignment = Alignment.CenterVertically) {
            actions.forEach { action ->
                Box(
                    modifier =
                        Modifier
                            .padding(start = NothingSpacing.xs)
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .clickable(onClick = action.onClick),
                    contentAlignment = Alignment.Center,
                ) {
                    if (action.icon != null) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.label,
                            tint = if (action.accent) NothingColors.accent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(
                            text = if (classic) action.label.take(4) else action.label.uppercase().take(4),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = NothingFonts.mono(),
                        )
                    }
                }
            }
        }
    }
}

// ── Circle Button (icon + label) ─────────────────────────────────────────────

@Composable
fun NothingCircleButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Column(
        modifier = modifier.width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
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
                fontFamily = NothingFonts.mono(),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (classic) label else label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            fontFamily = NothingFonts.mono(),
            textAlign = TextAlign.Center,
        )
    }
}

// ── Nothing Input ────────────────────────────────────────────────────────────

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
            placeholder =
                if (placeholder.isNotEmpty()) {
                    { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    null
                },
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = NothingFonts.mono()),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            shape = NothingShapes.input,
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
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        NothingLabel(
            text = label,
            modifier = Modifier.padding(bottom = NothingSpacing.xs),
        )
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = NothingShapes.input,
            border =
                BorderStroke(
                    1.dp,
                    if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = NothingSpacing.md, vertical = NothingSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (classic) value else value.uppercase(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = NothingFonts.mono(),
                )
                Text(
                    text = if (expanded) "[CLOSE]" else "[OPEN]",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = NothingFonts.mono(),
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(NothingSpacing.xs))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = NothingShapes.input,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        if (index > 0) NothingDivider()
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(option)
                                        expanded = false
                                    }.padding(
                                        horizontal = NothingSpacing.md,
                                        vertical = NothingSpacing.md,
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = if (classic) option else option.uppercase(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = NothingFonts.mono(),
                            )
                            if (option.equals(value, ignoreCase = true)) {
                                NothingRedDot(size = 6f)
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
    fillColor: Color = NothingColors.accent,
    height: Float = 8f,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0 until total) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(height.dp)
                        .background(
                            if (i < filled) {
                                fillColor
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
            )
        }
    }
}

// ── Icon Chip (rounded square, 48 dp, surfaceVariant fill) ───────────────────

@Composable
fun NothingIconCircle(
    modifier: Modifier = Modifier,
    size: Float = 48f,
    accent: Boolean = false,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    NothingIconChip(
        modifier = modifier,
        size = size,
        accent = accent,
        backgroundColor = backgroundColor,
        content = content,
    )
}

@Composable
fun NothingIconChip(
    modifier: Modifier = Modifier,
    size: Float = 48f,
    accent: Boolean = false,
    backgroundColor: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(size.dp)
                .clip(NothingShapes.iconChip)
                .background(
                    backgroundColor
                        ?: if (accent) {
                            NothingColors.accent.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                ).then(
                    if (accent) {
                        Modifier.border(1.dp, NothingColors.accent, NothingShapes.iconChip)
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ── List Row (icon chip + title + subtitle + chevron) ────────────────────────

@Composable
fun NothingListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val rowModifier =
        if (onClick != null) {
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        } else {
            modifier.fillMaxWidth()
        }
    Row(
        modifier =
            rowModifier
                .padding(vertical = NothingSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
    ) {
        if (leading != null) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                leading()
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NothingSpacing.xs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                maxLines = 2,
                softWrap = true,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ── Selection Top Bar ────────────────────────────────────────────────────────

@Composable
fun NothingSelectionTopBar(
    count: Int,
    modifier: Modifier = Modifier,
    onCancel: () -> Unit = {},
    actions: List<TopBarAction> = emptyList(),
) {
    val classic = LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC
    Row(
        modifier =
            modifier
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
                text = if (classic) action.label else action.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.0.sp,
                modifier =
                    Modifier
                        .clickable(onClick = action.onClick)
                        .padding(horizontal = NothingSpacing.sm),
            )
        }
        Text(
            text = "[X]",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.0.sp,
            modifier =
                Modifier
                    .clickable(onClick = onCancel)
                    .padding(start = NothingSpacing.sm),
        )
    }
}

// ── Checkbox (monoline square, red dot when checked) ─────────────────────────

@Composable
fun NothingCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Float = 20f,
) {
    Box(
        modifier =
            modifier
                .size(size.dp)
                .clip(NothingShapes.technical)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, NothingShapes.technical)
                .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            NothingRedDot(size = size * 0.5f)
        }
    }
}

// ── Radio (monoline circle, red dot when selected) ───────────────────────────

@Composable
fun NothingRadio(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Float = 20f,
) {
    Box(
        modifier =
            modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            NothingRedDot(size = size * 0.5f)
        }
    }
}

// ── Circular Add Button (red fill, white +) ──────────────────────────────────

@Composable
fun NothingAddCircle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Float = 56f,
) {
    Box(
        modifier =
            modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(NothingColors.accent)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Add",
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

// ── Bottom Sheet Drag Handle ─────────────────────────────────────────────────

@Composable
fun NothingDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .padding(vertical = NothingSpacing.sm)
                .size(36.dp, 4.dp)
                .clip(
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(2.dp),
                ).background(MaterialTheme.colorScheme.outline),
    )
}

// ── Bottom Action Bar (Done / Add) ───────────────────────────────────────────

@Composable
fun NothingBottomActionBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    subtitle: String = "",
    containerColor: Color = MaterialTheme.colorScheme.background,
) {
    Surface(
        color = containerColor,
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NothingSpacing.md)
                    .padding(top = NothingSpacing.sm, bottom = NothingSpacing.md),
        ) {
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = NothingColors.accent,
                    fontFamily = NothingFonts.mono(),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = NothingSpacing.sm),
                )
            }
            NothingPillButton(
                text = text,
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ponytail: two-button bottom bar for config sheets; background color + nav bar padding fix the gap
@Composable
fun NothingBottomActionBar(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryEnabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
) {
    Surface(
        color = containerColor,
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NothingSpacing.md)
                    .padding(top = NothingSpacing.sm, bottom = NothingSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(NothingSpacing.md),
        ) {
            NothingSecondaryButton(
                text = secondaryText,
                onClick = onSecondaryClick,
                modifier = Modifier.weight(1f),
            )
            NothingPillButton(
                text = primaryText,
                onClick = onPrimaryClick,
                enabled = primaryEnabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
