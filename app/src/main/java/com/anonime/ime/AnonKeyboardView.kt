package com.anonime.ime

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.anonime.data.SettingsRepository
import com.anonime.data.ThemeMode
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

    private val settingsRepo = SettingsRepository.get(context)

    init {
        // Dispose the composition when the ViewTree's LifecycleOwner reaches
        // DESTROYED — i.e. when the IME service is destroyed. This is the
        // safest strategy for an IME because the input view can be re-attached
        // multiple times across show/hide cycles within the same service
        // lifetime, and we want the composition to survive those re-attaches.
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
        // Read the latest settings — recomposes when the user changes a
        // toggle in the settings Activity (same process, shared StateFlow).
        val settings by settingsRepo.state.collectAsState()

        // Resolve theme preference into a concrete dark/light value.
        val darkTheme = when (settings.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        AnonIMETheme(darkTheme = darkTheme, dynamic = settings.dynamicColor) {
            val current = state.value
            KeyboardScreen(
                state = current,
                onAction = onAction,
                keyHeightDp = settings.keyHeight.dp,
            )
        }
    }
}
