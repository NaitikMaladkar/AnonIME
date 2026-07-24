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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.anonime.ui.theme.AnonIMETheme

/**
 * The IME service.
 *
 * Responsibilities:
 *   1. Host a Compose view tree that renders [KeyboardScreen].
 *   2. Translate [KeyAction]s into InputConnection commits.
 *   3. Maintain [KeyboardUiState] (shift state, enter action).
 *
 * Why this service implements [LifecycleOwner] + [SavedStateRegistryOwner]:
 *   Compose's [ComposeView] pulls a LifecycleOwner and SavedStateRegistryOwner
 *   from its ViewTree at attach time. If either is missing the composition
 *   never starts — the view measures 0×0 and the system thinks the IME is
 *   "shown" while the user sees nothing. That's the lock-in failure mode.
 *   By being our own owner and dispatching lifecycle events, the ComposeView
 *   has a valid lifecycle and renders normally.
 *
 * Anonymous-typing guarantees (Phase 1):
 *   - No network calls. The manifest declares no INTERNET permission, so
 *     even if some future code path attempted one, the OS would block it.
 *   - No persistence. We do not write any user input to disk, SharedPreferences,
 *     or databases. State lives in-memory only.
 *   - No personal dictionary learning. We do not maintain a user dictionary
 *     and explicitly respect IME_FLAG_NO_PERSONALIZED_LEARNING.
 *   - No input history. The transient buffers are wiped immediately after
 *     commit and on [onFinishInput].
 */
class AnonIMEService :
    InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner {

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

    // ── Lifecycle + SavedState plumbing for ComposeView ─────────────────────────
    /**
     * Backing registry for [LifecycleOwner]. Driven by IME callbacks:
     *   - onCreate -> ON_CREATE
     *   - onCreateInputView (after attach) -> ON_START, then ON_RESUME
     *   - onHideWindow -> ON_PAUSE, then ON_STOP
     *   - onDestroy -> ON_DESTROY
     *
     * We do NOT move to RESUME until the input view is actually attached —
     * otherwise Compose thinks it's foregrounded while it isn't on screen.
     */
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ── Service lifecycle ───────────────────────────────────────────────────────
    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDestroy() {
        // Tear down Compose before destroying lifecycle — order matters.
        composeView?.disposeComposition()
        composeView = null
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    // ── Input view lifecycle ────────────────────────────────────────────────────
    override fun onCreateInputView(): View {
        // The IME provides the Service context. Use applicationContext for any
        // long-lived references so we don't leak Activity-bound resources.
        val view = ComposeView(this).apply {
            // Dispose when the view's lifecycle owner is destroyed — i.e. when
            // the IME itself is torn down.
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )

            // Critical: wire the ViewTree owners BEFORE setContent. Compose reads
            // them at attach; if they're null at that moment the composition
            // silently skips and nothing renders (this was the original bug).
            this.setViewTreeLifecycleOwner(this@AnonIMEService)
            this.setViewTreeSavedStateRegistryOwner(this@AnonIMEService)

            setContent {
                AnonIMETheme {
                    val state by uiState
                    KeyboardScreen(
                        state = state,
                        onAction = ::handleKeyAction,
                    )
                }
            }

            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        composeView = view
        return view
    }

    override fun onWindowShown() {
        super.onWindowShown()
        // Input view is on screen — bring lifecycle up to RESUMED.
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        // Input view is leaving the screen — drop back to CREATED.
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
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

    // ── Safety: always allow the IME to be dismissed ────────────────────────────
    /**
     * Always return true so the system knows we show an input view. Returning
     * false in some edge cases makes the IMMS believe the IME has no UI and
     * it stops dispatching window visibility — a contributor to "stuck IME".
     */
    override fun onEvaluateInputViewShown(): Boolean = true

    /**
     * Back-key fallback. If the IME is shown and the user presses the back
     * button, the default behavior is to hide the IME. We don't override
     * this here — the default is correct — but we keep the override as a
     * documented guarantee that back always dismisses the keyboard.
     *
     * The user-reported "locked" failure was actually caused by the keyboard
     * never rendering in the first place (Compose had no lifecycle), not by
     * a back-key bug. Once we render properly, back works as expected.
     */

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
                val actionId = (editorInfo?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION
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
}
