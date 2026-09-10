package com.tdvorak.nothingmodes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

/**
 * Nothing Design System color palette.
 *
 * Dark mode: OLED black background, white data glowing.
 * Light mode: off-white paper (#F5F5F5), black ink.
 *
 * Gray scale IS the hierarchy:
 *   text-display   100% → hero numbers, headlines
 *   text-primary    90% → body text
 *   text-secondary  60% → labels, captions
 *   text-disabled   40% → disabled, hints
 *
 * Red (#FF3030) is an interrupt, not decoration. One per screen max.
 */

// ── Dark Mode ────────────────────────────────────────────────────────────────
private val NothingDark =
    darkColorScheme(
        primary = Color(0xFFFFFFFF), // text-display
        onPrimary = Color(0xFF000000), // black
        secondary = Color(0xFFE8E8E8), // text-primary
        onSecondary = Color(0xFF000000),
        tertiary = Color(0xFFFF3030), // Nothing Red accent
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFF000000), // pure black canvas
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF141414), // card surfaces
        onSurface = Color(0xFFE8E8E8), // text-primary on surface
        surfaceVariant = Color(0xFF1A1A1A), // elevated / raised surfaces
        onSurfaceVariant = Color(0xFF8A8A8A), // text-secondary
        outline = Color(0xFF2B2B2B), // visible borders
        outlineVariant = Color(0xFF222222), // hairline separators
        error = Color(0xFFFF3030),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFF3A0A0A),
        onErrorContainer = Color(0xFFFF3030),
    )

// ── Light Mode ───────────────────────────────────────────────────────────────
private val NothingLight =
    lightColorScheme(
        primary = Color(0xFF000000), // text-display (black ink)
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF1A1A1A), // text-primary
        onSecondary = Color(0xFFFFFFFF),
        tertiary = Color(0xFFFF3030), // Nothing Red accent
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFF5F5F5), // warm off-white
        onBackground = Color(0xFF000000),
        surface = Color(0xFFFFFFFF), // white cards on off-white
        onSurface = Color(0xFF1A1A1A), // text-primary
        surfaceVariant = Color(0xFFF0F0F0), // surface-raised
        onSurfaceVariant = Color(0xFF666666), // text-secondary
        outline = Color(0xFFE8E8E8), // visible borders
        outlineVariant = Color(0xFFF5F5F5), // subtle separators
        error = Color(0xFFFF3030),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFF0F0),
        onErrorContainer = Color(0xFFFF3030),
    )

// ── Classic Theme (curated, non-dynamic) ─────────────────────────────────────
// Soft teal accent over clean neutral surfaces — calm and deliberate without
// the dot-matrix chrome. Identical on every device — no wallpaper tinting.
private val ClassicDark =
    darkColorScheme(
        primary = Color(0xFF8ED1C8), // soft teal for dark canvas
        onPrimary = Color(0xFF0B2624),
        primaryContainer = Color(0xFF26413E), // deep teal container, FAB surface
        onPrimaryContainer = Color(0xFFA8E6DC),
        secondary = Color(0xFFA8A8A8),
        onSecondary = Color(0xFF1A1A1A),
        secondaryContainer = Color(0xFF363636),
        onSecondaryContainer = Color(0xFFE0E0E0),
        tertiary = Color(0xFF8A8A8A),
        onTertiary = Color(0xFF1A1A1A),
        background = Color(0xFF101010), // near-black neutral canvas
        onBackground = Color(0xFFECECEC),
        surface = Color(0xFF191919), // raised cards
        onSurface = Color(0xFFECECEC),
        surfaceVariant = Color(0xFF232323), // elevated surfaces
        onSurfaceVariant = Color(0xFFA0A0A0), // secondary text
        outline = Color(0xFF333333), // visible borders
        outlineVariant = Color(0xFF262626), // hairline separators
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
    )

private val ClassicLight =
    lightColorScheme(
        primary = Color(0xFF00695F), // deep teal for white contrast
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFBCEBE2),
        onPrimaryContainer = Color(0xFF00201C),
        secondary = Color(0xFF5A5A5A),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE6E6E6),
        onSecondaryContainer = Color(0xFF1A1A1A),
        tertiary = Color(0xFF7A7A7A),
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFFAFAFA), // neutral off-white canvas
        onBackground = Color(0xFF1A1A1A),
        surface = Color(0xFFFFFFFF), // white cards
        onSurface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFFF1F1F1), // raised surfaces
        onSurfaceVariant = Color(0xFF5F5F5F), // secondary text
        outline = Color(0xFFDADADA), // visible borders
        outlineVariant = Color(0xFFECECEC), // hairline separators
        error = Color(0xFFB3261E),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
    )

// ── Semantic Colors (identical in both modes) ────────────────────────────────
object NothingColors {
    private val accentConstant = Color(0xFFFF3030)
    private val accentSubtleConstant = Color(0x33FF3030)
    private val mutedConstant = Color(0xFF555555)

    /**
     * Nothing Red accent. In CLASSIC UI style this resolves to the Material 3
     * primary color so shared components read as a normal Material 3 app.
     */
    val accent
        @Composable get() =
            if (LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC) {
                MaterialTheme.colorScheme.primary
            } else {
                accentConstant
            }

    val accentSubtle
        @Composable get() =
            if (LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                accentSubtleConstant
            }

    val interactive
        @Composable get() =
            if (LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC) {
                MaterialTheme.colorScheme.primary
            } else {
                accentConstant
            }

    val interactiveLight
        @Composable get() =
            if (LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC) {
                MaterialTheme.colorScheme.primary
            } else {
                accentConstant
            }

    val muted
        @Composable get() =
            if (LocalUiStyle.current == ThemeManager.UiStyle.CLASSIC) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                mutedConstant
            }
}

/**
 * The active visual language — resolved [ThemeManager.UiStyle].
 * Components may read this to degrade Nothing-specific decoration
 * (dot-matrix glyphs, hairline frames) to plain Material equivalents.
 */
val LocalUiStyle = compositionLocalOf { ThemeManager.UiStyle.NOTHING }

@Composable
fun NothingModesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    uiStyle: ThemeManager.UiStyle = ThemeManager.UiStyle.NOTHING,
    content: @Composable () -> Unit,
) {
    if (uiStyle == ThemeManager.UiStyle.CLASSIC) {
        // Curated classic Material 3 — single indigo accent over warm-neutral
        // surfaces. No dynamic color: identical look across all devices.
        // Geist Sans typography matches the landing page's normal style.
        val scheme = if (darkTheme) ClassicDark else ClassicLight
        CompositionLocalProvider(LocalUiStyle provides ThemeManager.UiStyle.CLASSIC) {
            MaterialTheme(
                colorScheme = scheme,
                typography = ClassicTypography.typography,
                shapes = Shapes(),
                content = content,
            )
        }
        return
    }
    CompositionLocalProvider(LocalUiStyle provides ThemeManager.UiStyle.NOTHING) {
        MaterialTheme(
            colorScheme = if (darkTheme) NothingDark else NothingLight,
            typography = NothingTypography.typography,
            shapes = NothingShapes.shapes,
            content = content,
        )
    }
}

/**
 * Theme wrapper that reads the persisted theme mode and UI style
 * from [ThemeManager]. AUTO style resolves to Nothing on Nothing
 * devices, classic Material everywhere else.
 */
@Composable
fun NothingModesThemeDynamic(content: @Composable () -> Unit) {
    val themeManager = ThemeManager.instance
    val mode by themeManager.mode.collectAsState()
    val uiStyle by themeManager.uiStyle.collectAsState()
    val isDark =
        when (mode) {
            ThemeManager.ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeManager.ThemeMode.DARK -> true
            ThemeManager.ThemeMode.LIGHT -> false
        }
    val resolvedStyle =
        when (uiStyle) {
            ThemeManager.UiStyle.AUTO ->
                if (Build.MANUFACTURER.equals("nothing", ignoreCase = true)) {
                    ThemeManager.UiStyle.NOTHING
                } else {
                    ThemeManager.UiStyle.CLASSIC
                }
            else -> uiStyle
        }
    NothingModesTheme(
        darkTheme = isDark,
        uiStyle = resolvedStyle,
        content = content,
    )
}
