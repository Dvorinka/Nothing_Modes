package com.tdvorak.nothingmodes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Nothing OS-inspired shapes: minimal rounding, precise corners. */
object NothingShapes {
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(2.dp),
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(6.dp),
        large = RoundedCornerShape(8.dp),
        extraLarge = RoundedCornerShape(12.dp),
    )
}
