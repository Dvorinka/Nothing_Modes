package com.tdvorak.nothingmodes.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Nothing Design System spacing scale (8px base).
 *
 * Tight (4–8px)   = "These belong together"
 * Medium (16px)    = "Same group, different items"
 * Wide (32–48px)   = "New group starts here"
 * Vast (64–96px)   = "This is a new context"
 */
object NothingSpacing {
    val xxs = 2.dp    // optical adjustments only
    val xs = 4.dp     // icon-to-label gaps, tight padding
    val sm = 8.dp     // component internal spacing
    val md = 16.dp    // standard padding, element gaps
    val lg = 24.dp    // group separation
    val xl = 32.dp    // section margins
    val xxl = 48.dp   // major section breaks
    val xxxl = 64.dp  // page-level vertical rhythm
    val hero = 96.dp  // hero breathing room

    val cardPadding = 20.dp
    val screenPadding = 16.dp
    val sectionGap = 32.dp
    val itemGap = 8.dp
}
