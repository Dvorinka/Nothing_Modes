package com.tdvorak.nothingmodes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tdvorak.nothingmodes.ui.R

/**
 * Nothing Design System typography.
 *
 * Font stack:
 *   Display  → Doto (dot-matrix, hero moments only, 36sp+)
 *   Body/UI  → Geist Sans (clean grotesk, primary interface text)
 *   Data     → Geist Mono (monospace, labels, numbers, ALL CAPS)
 *
 * Type scale follows the design system tokens:
 *   display-xl  72sp  → hero numbers, time displays
 *   display-lg  48sp  → section heroes
 *   display-md  36sp  → page titles
 *   heading     24sp  → section headings
 *   subheading  18sp  → subsections
 *   body        16sp  → body text
 *   body-sm     14sp  → secondary body
 *   caption     12sp  → timestamps, footnotes
 *   label       11sp  → ALL CAPS monospace labels
 */

val GeistSans = FontFamily(
    Font(R.font.geist_sans_light, FontWeight.Light),
    Font(R.font.geist_sans_regular, FontWeight.Normal),
    Font(R.font.geist_sans_medium, FontWeight.Medium),
    Font(R.font.geist_sans_bold, FontWeight.Bold),
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_bold, FontWeight.Bold),
)

val Doto = FontFamily(
    Font(R.font.doto, FontWeight.Normal),
    Font(R.font.doto, FontWeight.Medium),
    Font(R.font.doto, FontWeight.SemiBold),
    Font(R.font.doto, FontWeight.Bold),
)

// Legacy aliases for any external callers; new code should use GeistSans/GeistMono.
val SpaceGrotesk = GeistSans
val SpaceMono = GeistMono

object NothingTypography {

    val typography = Typography(
        // Display — Doto (dot-matrix, hero only)
        displayLarge = TextStyle(
            fontFamily = Doto,
            fontWeight = FontWeight.Normal,
            fontSize = 72.sp,
            lineHeight = 72.sp,
            letterSpacing = (-2.5).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = Doto,
            fontWeight = FontWeight.Normal,
            fontSize = 48.sp,
            lineHeight = 50.sp,
            letterSpacing = (-1.5).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = Doto,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 40.sp,
            letterSpacing = (-1.0).sp,
        ),

        // Headings — Geist Sans
        headlineLarge = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.5).sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Medium,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.3).sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 29.sp,
            letterSpacing = (-0.25).sp,
        ),

        // Titles — Geist Sans
        titleLarge = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.2.sp,
        ),

        // Body — Geist Sans
        bodyLarge = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            letterSpacing = 0.15.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = GeistSans,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.25.sp,
        ),

        // Labels — Geist Mono, ALL CAPS, wide tracking
        labelLarge = TextStyle(
            fontFamily = GeistMono,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.8.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = GeistMono,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.6.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = GeistMono,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            letterSpacing = 0.9.sp,
        ),
    )
}
