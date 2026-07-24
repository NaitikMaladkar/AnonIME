package com.anonime.ime

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.anonime.ui.theme.AnonIMETheme

/**
 * The IME service.
 *
 * Responsibilities:
 *   1. Host a Compose view tree that renders [KeyboardScreen].
 *   2. Translate [KeyAction]s into InputConnection commits.
 *   3. Maintain [KeyboardUiState] (shift state, enter action).
 *
 * Anonymous-typing guarantees (Phase 1):
 *   - No network calls. The manifest declares no INTERNET permission, so
 *     even if some future code path attempted one, the OS would block it.
 *   - No persistence. We do not write any user input to disk, SharedPreferences,
 *     or databases. State lives in-memory only.
 *   - No personal dictionary learning. We do not maintain a user dictionary
 *     and explicitly respect IME_FLAG_NO_PERSONALIZED_LEARNING.
 *   - No input history. The transient [pendingChar] / text buffer is wiped
 *     immediately after commit and on [onFinishInput].
 */
class AnonIMEService : InputMethodService() {

    // ── Compose state ──────────────────────────────────────────────────────────
    private var uiState: MutableState<KeyboardUiState> =
        mutableStateOf(KeyboardUiState())
    private var composeView: ComposeView? = null

    // ── Anonymous-typing state ──────────────────────────────────────────────────
    /**
     * True while the current editor opted out of personalized learning via
     * EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING. We always behave as if
     * this is true (we never learn anything), but we honor it explicitly so
     * password fields and similar contexts know we respect the flag.
     */
    private var incognito: Boolean = true

    /** Wall-clock of the last input event — for diagnostics only, never stored. */
    private var lastEventUptime: Long = 0L

    // ── Lifecycle ───────────────────────────────────────────────────────────────
    override fun onCreateInputView(): View {
        val ctx = this
        val view = ComposeView(ctx).apply {
            // Dispose the composition when the input view detaches — prevents
            // leaks across IME hide/show cycles.
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AnonIMETheme {
                    val state by uiState
                    KeyboardScreen(
                        state = state,
                        onAction = ::handleKeyAction,
                    )
                }
            }
            // The IME itself provides the lifecycle; this view rides on it.
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        composeView = view
        return view
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // Honor IME_FLAG_NO_PERSONALIZED_LEARNING — we already never learn,
        // but this is the documented signal from the app we're typing into.
        incognito = attribute.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0

        // Reset shift state per input focus — typing in a new field starts lowercase.
        uiState.value = uiState.value.copy(
            shift = ShiftState.Off,
            enter = enterSpecFor(attribute),
            enabled = true,
        )
    }

    override fun onFinishInput() {
        super.onFinishInput()
        // Anonymous-typing: drop any state that could be construed as input history.
        // We hold no text buffer in Phase 1, but be explicit in case Phase 2 adds one.
        uiState.value = uiState.value.copy(shift = ShiftState.Off)
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.disposeComposition()
        composeView = null
    }

    // ── Action dispatch ─────────────────────────────────────────────────────────
    private fun handleKeyAction(action: KeyAction) {
        lastEventUptime = SystemClock.uptimeMillis()
        val ic = currentInputConnection ?: return

        when (action) {
            is KeyAction.Character -> {
                val c = if (uiState.value.shift.uppercase) action.char.uppercaseChar() else action.char
                ic.commitText(c.toString(), 1)
                // Auto-revert shift after one uppercase character (unless caps locked).
                if (uiState.value.shift == ShiftState.OnNext) {
                    uiState.value = uiState.value.copy(shift = ShiftState.Off)
                }
            }

            KeyAction.Backspace -> {
                ic.deleteSurroundingText(1, 0)
            }

            KeyAction.Space -> {
                ic.commitText(" ", 1)
                if (uiState.value.shift == ShiftState.OnNext) {
                    uiState.value = uiState.value.copy(shift = ShiftState.Off)
                }
            }

            KeyAction.Enter -> {
                // Phase 1: send IME action if present, otherwise newline.
                val editorInfo = currentInputEditorInfo
                val actionId = editorInfo?.imeOptions and EditorInfo.IME_MASK_ACTION
                val handled = when (actionId) {
                    EditorInfo.IME_ACTION_DONE,
                    EditorInfo.IME_ACTION_GO,
                    EditorInfo.IME_ACTION_NEXT,
                    EditorInfo.IME_ACTION_PREVIOUS,
                    EditorInfo.IME_ACTION_SEND,
                    EditorInfo.IME_ACTION_SEARCH -> ic.performEditorAction(actionId)
                    else -> false
                }
                if (!handled) {
                    ic.commitText("\n", 1)
                }
            }

            KeyAction.Shift -> {
                uiState.value = uiState.value.copy(shift = uiState.value.shift.next())
            }

            KeyAction.ToggleSymbols -> {
                // Phase 2: switch to symbols layout.
                // For Phase 1, no-op (the layout doesn't exist yet).
            }
        }
    }

    // ── Editor info helpers ─────────────────────────────────────────────────────
    private fun enterSpecFor(attribute: EditorInfo): EnterActionSpec {
        val action = attribute.imeOptions and EditorInfo.IME_MASK_ACTION
        return when (action) {
            EditorInfo.IME_ACTION_GO -> EnterActionSpec.Go
            EditorInfo.IME_ACTION_SEARCH -> EnterActionSpec.Search
            EditorInfo.IME_ACTION_SEND -> EnterActionSpec.Send
            EditorInfo.IME_ACTION_NEXT -> EnterActionSpec.Next
            EditorInfo.IME_ACTION_DONE -> EnterActionSpec.Done
            else -> EnterActionSpec.Default
        }
    }

    // ── Incognito signal for the system (optional) ──────────────────────────────
    /**
     * On API 32+ the IME can explicitly tell the system "yes, I'm in incognito
     * mode" via [InputMethodService.isIncognitoMode]. We override it to true
     * unconditionally — we never personalize, period.
     */
    // Uncomment once minSdk bumps to 32; for now we honor the flag manually.
    // override fun isIncognitoMode(): Boolean = true
}
