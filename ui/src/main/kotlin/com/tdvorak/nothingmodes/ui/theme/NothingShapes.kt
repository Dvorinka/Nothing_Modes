package com.tdvorak.nothingmodes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Nothing Design System shapes.
 *
 * Cards: 12–16px radius. Compact: 8px. Technical: 4px.
 * Buttons: pill (999px) or technical (4–8px).
 * No border-radius > 16px on cards.
 */
object NothingShapes {
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),   // technical
        small = RoundedCornerShape(8.dp),        // compact cards, inputs
        medium = RoundedCornerShape(12.dp),      // standard cards
        large = RoundedCornerShape(16.dp),       // large cards
        extraLarge = RoundedCornerShape(16.dp),  // max 16px
    )

    val pill = RoundedCornerShape(999.dp)
    val technical = RoundedCornerShape(4.dp)
    val card = RoundedCornerShape(12.dp)
    val cardLarge = RoundedCornerShape(16.dp)
}
