package com.anonime.ime

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
 * ── Stability note (Phase 2 bugfix) ─────────────────────────────────────────
 * Earlier versions keyed `pointerInput` on the [onLongPress] lambda itself.
 * Because call sites create fresh closure instances on every recomposition
 * (`{ onLongPressKey(key, keyBounds) }`), the gesture detector was being
 * torn down and rebuilt on every frame that recomposed the key. The visible
 * symptom was long-press sometimes failing to fire — the user held the key,
 * a layout pass recomposed it, and the new pointerInput coroutine hadn't
 * yet armed its long-press timeout before the touch stream was handed off.
 *
 * Fix: use `Unit` as the key (the gesture detector lives for the lifetime
 * of the composable) and forward calls through [rememberUpdatedState] so
 * the detector always invokes the latest lambdas without being restarted.
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
    // Hold the latest lambdas in state so the pointerInput coroutine — which
    // is launched once and never restarted — can call the freshest versions
    // without needing its key to change.
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentOnClick by rememberUpdatedState(onClick)

    this.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = if (currentOnLongPress != null) {
                { currentOnLongPress?.invoke() }
            } else null,
            onTap = { currentOnClick() },
        )
    }
}
