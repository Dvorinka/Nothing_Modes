package com.tdvorak.nothingmodes.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

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

// ── Semantic Colors (identical in both modes) ────────────────────────────────
object NothingColors {
    val accent = Color(0xFFFF3030)
    val accentSubtle = Color(0x33FF3030)
    val interactive = Color(0xFFFF3030)
    val interactiveLight = Color(0xFFFF3030)
    val muted = Color(0xFF555555)
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
        // Plain Material 3 — dynamic color where the platform offers it,
        // stock typography and rounded shapes. No Nothing decoration.
        val context = LocalContext.current
        val scheme =
            when {
                Build.VERSION.SDK_INT >= 31 && darkTheme -> dynamicDarkColorScheme(context)
                Build.VERSION.SDK_INT >= 31 -> dynamicLightColorScheme(context)
                darkTheme -> darkColorScheme()
                else -> lightColorScheme()
            }
        CompositionLocalProvider(LocalUiStyle provides ThemeManager.UiStyle.CLASSIC) {
            MaterialTheme(
                colorScheme = scheme,
                typography = Typography(),
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
    NothingModesTheme(
        darkTheme = isDark,
        uiStyle = themeManager.resolvedUiStyle(),
        content = content,
    )
}
