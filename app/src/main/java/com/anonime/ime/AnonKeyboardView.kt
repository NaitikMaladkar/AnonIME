package com.anonime.ime

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.anonime.ui.theme.AnonIMETheme

/**
 * The Compose host view for the IME.
 *
 * Uses [AbstractComposeView] (the canonical choice for IME per AOSP/Compose
 * samples) rather than `ComposeView`. The difference: `AbstractComposeView`
 * exposes [Content] as an overridable `@Composable` function, which Compose's
 * tooling and saved-state machinery treat as the "natural" entry point.
 *
 * @param context   The IME service context (NOT applicationContext — the service
 *                  context carries the IME's theme and resources).
 * @param state     Hoisted UI state (shift / enter / etc.)
 * @param onAction  Callback for every key press — dispatched to the
 *                  InputConnection by [AnonIMEService.handleKeyAction].
 */
class AnonKeyboardView(
    context: Context,
    private val state: State<KeyboardUiState>,
    private val onAction: (KeyAction) -> Unit,
) : AbstractComposeView(context) {

    init {
        // Dispose the composition when the ViewTree's LifecycleOwner reaches
        // DESTROYED — i.e. when the IME service is destroyed. This is the
        // safest strategy for an IME because the input view can be re-attached
        // multiple times across show/hide cycles within the same service
        // lifetime, and we want the composition to survive those re-attaches.
        // DisposeOnViewTreeLifecycleDestroyed looks up the LifecycleOwner from
        // the ViewTree at dispose time — which works because we wire the
        // owners on the IME window's decorView in AnonIMEService.onCreateInputView.
        setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        // The IME adds this view with MATCH_PARENT × WRAP_CONTENT. Set the
        // same layout params on construction to avoid 0×0 measurement before
        // the IME's mInputFrame re-parents us.
        layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
        )
    }

    @Composable
    override fun Content() {
        AnonIMETheme {
            val current = state.value
            KeyboardScreen(
                state = current,
                onAction = onAction,
            )
        }
    }
}

