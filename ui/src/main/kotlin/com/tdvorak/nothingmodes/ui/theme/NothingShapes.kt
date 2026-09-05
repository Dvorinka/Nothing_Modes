package com.tdvorak.nothingmodes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Nothing Design System shape scale.
 *
 * Nothing OS uses razor-sharp surfaces. Most cards have a subtle radius;
 * small interactive tiles and bottom sheets stay technical and flat.
 *
 *   technical  → 4dp
 *   compact    → 8dp
 *   cards      → 8dp (small) / 12dp (large)
 *   buttons    → pill (999dp)
 */
object NothingShapes {
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

    val pill = RoundedCornerShape(999.dp)
    val technical = RoundedCornerShape(4.dp)
    val compact = RoundedCornerShape(12.dp)
    val input = RoundedCornerShape(16.dp)
    val chip = RoundedCornerShape(16.dp)
    val iconChip = RoundedCornerShape(16.dp)
    val card = RoundedCornerShape(24.dp)
    val cardLarge = RoundedCornerShape(32.dp)
    val sheet = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val dialog = RoundedCornerShape(28.dp)
}
