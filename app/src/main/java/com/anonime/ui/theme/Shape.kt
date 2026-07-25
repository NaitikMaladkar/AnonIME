package com.anonime.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Shape scale tuned for a playful pastel look.
 *
// Extra-rounded corners are the defining visual cue — every card, button,
// and surface rounds generously (12-28dp) to keep the UI feeling soft and
// friendly rather than corporate.
 */
val AnonIMEShapes = Shapes(
    // Small: chips, badges, small buttons.
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    // Medium: option cards, list rows, dialogs.
    medium = RoundedCornerShape(20.dp),
    // Large: category cards on the home screen, hero banners.
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
