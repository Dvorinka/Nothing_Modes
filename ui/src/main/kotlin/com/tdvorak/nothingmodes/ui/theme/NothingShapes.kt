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
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(12.dp),
        extraLarge = RoundedCornerShape(12.dp),
    )

    val pill = RoundedCornerShape(999.dp)
    val technical = RoundedCornerShape(4.dp)
    val compact = RoundedCornerShape(8.dp)
    val card = RoundedCornerShape(8.dp)
    val cardLarge = RoundedCornerShape(12.dp)
    val sheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    // Modals per components.md: 16px radius dialog surface.
    val dialog = RoundedCornerShape(16.dp)
}
