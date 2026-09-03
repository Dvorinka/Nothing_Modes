package com.dvoranka.nothingmodes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Nothing OS-inspired palette: monochrome with red accent
private val NothingDark = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFD0D0D0),
    onSecondary = Color(0xFF000000),
    tertiary = Color(0xFFFF0000),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF111111),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFCCCCCC),
    outline = Color(0xFF333333),
)

private val NothingLight = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF333333),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFF0000),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = Color(0xFF333333),
    outline = Color(0xFFCCCCCC),
)

@Composable
fun NothingModesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) NothingDark else NothingLight,
        content = content,
    )
}
