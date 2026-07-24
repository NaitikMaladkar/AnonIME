package com.anonime.ime

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Clickable modifier for IME keys.
 *
 * We deliberately do NOT include:
 *   - ripple indication (silent UI per user spec)
 *   - haptic feedback (silent UI per user spec)
 *   - sound on click (silent UI per user spec)
 *
 * Phase 2 may re-introduce optional feedback via a settings flag.
 */
fun Modifier.imeKeyClickable(
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    this.clickable(
        interactionSource = interaction,
        indication = null, // no ripple — silent keypress
        onClick = onClick,
    )
}
