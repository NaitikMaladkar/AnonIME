package com.anonime.ime

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Clickable modifier for IME keys.
 *
 * We deliberately do NOT include:
 *   - ripple indication (silent UI per user spec)
 *   - haptic feedback (silent UI per user spec — Phase 2 may re-add it
 *     behind the haptic toggle, but only when that setting is on)
 *   - sound on click (silent UI per user spec)
 *
 * Phase 2: switched from `Modifier.clickable` to `pointerInput + detectTapGestures`
 * so we can also detect long-press without losing the silent-UI behavior.
 * `detectTapGestures` does not emit ripples or haptics by default, which
 * preserves the Phase 1 contract.
 *
 * ── Parameter order ────────────────────────────────────────────────────────
 * [onClick] is intentionally the LAST parameter so existing call sites that
 * use trailing-lambda syntax (e.g. `.imeKeyClickable { onAction(it) }`)
 * continue to compile without changes. [onLongPress] is optional and comes
 * first so callers that need long-press can pass it as a named argument:
 * `.imeKeyClickable(onLongPress = { ... }) { onClick() }`.
 *
 * @param onLongPress Optional — fires when the user holds the key for ~500ms
 *                    (the system long-press timeout). Used by long-press
 *                    accents on letter keys.
 * @param onClick     Fires on a regular tap.
 */
fun Modifier.imeKeyClickable(
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    // interactionSource is unused for indication now (we use pointerInput),
    // but kept for any future hover/focus work without breaking the API.
    @Suppress("UNUSED_VARIABLE")
    val ignored = interaction
    this.pointerInput(onLongPress) {
        detectTapGestures(
            onLongPress = if (onLongPress != null) {
                { onLongPress() }
            } else null,
            onTap = { onClick() },
        )
    }
}
