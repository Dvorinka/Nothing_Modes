package com.tdvorak.nothingmodes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
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
// One restrained indigo accent (#4F5DD3 family) over warm-neutral surfaces.
// Identical on every device — no Material You wallpaper tinting.
private val ClassicDark =
    darkColorScheme(
        primary = Color(0xFF8E97E8), // lightened indigo for dark canvas
        onPrimary = Color(0xFF14162B),
        primaryContainer = Color(0xFF363E7A),
        onPrimaryContainer = Color(0xFFDEE0FF),
        secondary = Color(0xFF9099B0), // cool low-chroma neutral
        onSecondary = Color(0xFF1A1C21),
        secondaryContainer = Color(0xFF363A45),
        onSecondaryContainer = Color(0xFFD6DAEC),
        tertiary = Color(0xFF6B73A0), // muted indigo-grey
        onTertiary = Color(0xFF1A1C21),
        background = Color(0xFF121316), // near-neutral dark canvas
        onBackground = Color(0xFFE6E6E4),
        surface = Color(0xFF1A1C21), // raised cards
        onSurface = Color(0xFFE6E6E4),
        surfaceVariant = Color(0xFF22252B), // elevated surfaces
        onSurfaceVariant = Color(0xFFA8ABBA), // secondary text
        outline = Color(0xFF2D3038), // visible borders
        outlineVariant = Color(0xFF22252B), // hairline separators
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
    )

private val ClassicLight =
    lightColorScheme(
        primary = Color(0xFF3F4DBF), // deepened indigo for white contrast
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDEE0FF),
        onPrimaryContainer = Color(0xFF1A1F4A),
        secondary = Color(0xFF5B6071), // cool low-chroma neutral
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFDDE0F0),
        onSecondaryContainer = Color(0xFF1A1C21),
        tertiary = Color(0xFF6B73A0), // muted indigo-grey
        onTertiary = Color(0xFFFFFFFF),
        background = Color(0xFFFAFAF8), // warm off-white canvas
        onBackground = Color(0xFF1A1C21),
        surface = Color(0xFFFFFFFF), // white cards on off-white
        onSurface = Color(0xFF1A1C21),
        surfaceVariant = Color(0xFFF0F0EE), // raised surfaces
        onSurfaceVariant = Color(0xFF5B6071), // secondary text
        outline = Color(0xFFD8D8D4), // visible borders
        outlineVariant = Color(0xFFECECE8), // hairline separators
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
