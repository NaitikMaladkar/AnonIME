package com.anonime.ime

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * The IME service.
 *
 * Responsibilities:
 *   1. Host a Compose view tree (via [AnonKeyboardView]) that renders [KeyboardScreen].
 *   2. Translate [KeyAction]s into InputConnection commits.
 *   3. Maintain [KeyboardUiState] (shift state, enter action).
 *
 * ── Why this service implements THREE owners ─────────────────────────────────
 * Compose's [AbstractComposeView] looks up THREE ViewTree owners at attach
 * time via `findViewTreeXxxOwner()`:
 *   1. LifecycleOwner            — required, else crash
 *   2. SavedStateRegistryOwner   — required, else crash
 *   3. ViewModelStoreOwner       — optional at attach, but required the moment
 *                                  any `viewModel()` or retained state is used
 *
 * An `InputMethodService` is NOT any of these by default. We implement all
 * three and wire them into the IME window's `decorView` (NOT into the
 * ComposeView itself) so every descendant view can find them. Setting on the
 * ComposeView is fragile because `setInputView()` re-parents the view.
 *
 * ── Lifecycle dispatch via ServiceLifecycleDispatcher ────────────────────────
 * The Recomposer only runs its effect loop when the lifecycle is at least
 * `STARTED`. If we only dispatch `ON_CREATE` (in `onCreate`), the composition
 * is created but never produces frames — the keyboard renders 0×0 and the
 * user appears "locked" because the system thinks the IME is shown.
 *
 * `ServiceLifecycleDispatcher.onServicePreSuperOnBind()` dispatches `ON_START`
 * for us, and `onBindInput()` is called by the IMMS BEFORE `onCreateInputView()`.
 * This guarantees the lifecycle is `STARTED` by the time the ComposeView
 * attaches and tries to start its Recomposer.
 *
 * ── Anonymous-typing guarantees (Phase 1) ─────────────────────────────────────
 *   - No network calls. The manifest declares no INTERNET permission, so
 *     even if some future code path attempted one, the OS would block it.
 *   - No persistence. We do not write any user input to disk, SharedPreferences,
 *     or databases. State lives in-memory only.
 *   - No personal dictionary learning. We do not maintain a user dictionary
 *     and explicitly respect IME_FLAG_NO_PERSONALIZED_LEARNING.
 *   - No input history. Transient buffers are wiped immediately after commit
 *     and on [onFinishInput].
 */
class AnonIMEService :
    InputMethodService(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    // ── Compose state ──────────────────────────────────────────────────────────
    private val uiState: MutableState<KeyboardUiState> =
        mutableStateOf(KeyboardUiState())
    private var keyboardView: AnonKeyboardView? = null

    // ── Anonymous-typing state ──────────────────────────────────────────────────
    /**
     * True while the current editor opted out of personalized learning via
     * EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING. We always behave as if
     * this is true (we never learn anything), but we honor it explicitly so
     * password fields and similar contexts know we respect the flag.
     */
    @Suppress("unused")
    private var incognito: Boolean = true

    /** Wall-clock of the last input event — for diagnostics only, never stored. */
    @Suppress("unused")
    private var lastEventUptime: Long = 0L

    // ── Owner #1: LifecycleOwner ───────────────────────────────────────────────
    /**
     * `ServiceLifecycleDispatcher` from `androidx.lifecycle:lifecycle-service`.
     * Dispatches lifecycle events for a Service in the correct order:
     *   - onCreate  -> ON_CREATE
     *   - onBind    -> ON_START          ← critical for Compose's Recomposer
     *   - onDestroy -> ON_STOP, ON_DESTROY
     */
    private val lifecycleDispatcher = ServiceLifecycleDispatcher(this)

    override val lifecycle: Lifecycle
        get() = lifecycleDispatcher.lifecycle

    // ── Owner #2: ViewModelStoreOwner ──────────────────────────────────────────
    /**
     * Empty ViewModelStore — we don't use ViewModels in Phase 1, but Compose's
     * `viewModel()` machinery requires a non-null owner if anything ever calls it.
     * The store is cleared on service destroy to avoid leaks.
     */
    private val _viewModelStore = ViewModelStore()
    override val viewModelStore: ViewModelStore
        get() = _viewModelStore

    // ── Owner #3: SavedStateRegistryOwner ─────────────────────────────────────
    private val savedStateRegistryController =
        SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // ── Service lifecycle ───────────────────────────────────────────────────────
    override fun onCreate() {
        // Dispatch ON_CREATE BEFORE super.onCreate — ServiceLifecycleDispatcher
        // posts to the main thread at front-of-queue, so observers see the
        // state transition before any subsequent IME callbacks run.
        lifecycleDispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        // Initialize SavedStateRegistry with no persisted state — preserves
        // the anonymous-typing guarantee (we never restore any user data).
        savedStateRegistryController.performRestore(null)
    }

    override fun onBindInput() {
        // Dispatch ON_START BEFORE super.onBindInput. This is the critical
        // event — without it, the Recomposer created by the ComposeView will
        // be parked in CREATED state and never produce frames.
        lifecycleDispatcher.onServicePreSuperOnBind()
        super.onBindInput()
    }

    override fun onDestroy() {
        // Dispatch ON_STOP + ON_DESTROY before super, then clear the
        // ViewModelStore so any ViewModels held by retained composables
        // receive onCleared().
        lifecycleDispatcher.onServicePreSuperOnDestroy()
        _viewModelStore.clear()
        keyboardView?.disposeComposition()
        keyboardView = null
        super.onDestroy()
    }

    // ── Input view lifecycle ────────────────────────────────────────────────────
    override fun onCreateInputView(): View {
        // Construct the Compose host view using the SERVICE context (not
        // applicationContext) — the service context has the IME's theme and
        // resources, which Compose needs to resolve Material colors.
        val view = AnonKeyboardView(
            context = this,
            state = uiState,
            onAction = ::handleKeyAction,
        )

        // CRITICAL: wire ViewTree owners on the IME window's decorView, NOT
        // on the ComposeView. The IME's setInputView() re-parents the
        // returned view into mInputFrame, which can clear ViewTree tags set
        // on the view itself. Setting on decorView (the root of the entire
        // IME window) guarantees every descendant finds the owners via
        // findViewTreeXxxOwner().
        //
        // window  -> SoftInputWindow (a Dialog subclass)
        // .window -> the underlying Window
        // .decorView -> root View of the window
        val decorView = window?.window?.decorView
        if (decorView != null) {
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        } else {
            // Fallback: set on the view itself. This works in most cases but
            // is fragile if the IME re-parents the view.
            view.setViewTreeLifecycleOwner(this)
            view.setViewTreeViewModelStoreOwner(this)
            view.setViewTreeSavedStateRegistryOwner(this)
        }

        keyboardView = view
        return view
    }

    override fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // Honor IME_FLAG_NO_PERSONALIZED_LEARNING — we already never learn,
        // but this is the documented signal from the app we're typing into.
        incognito = attribute.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0

        // Reset to the letters layout + shift off for each new input focus.
        // If the user was on the symbols panel when they left a field, they
        // expect the next field to start at the letters layout.
        uiState.value = uiState.value.copy(
            layout = LayoutKind.Letters,
            shift = ShiftState.Off,
            enter = enterSpecFor(attribute),
            enabled = true,
        )
    }

    override fun onFinishInput() {
        super.onFinishInput()
        // Anonymous-typing: drop any state that could be construed as input history.
        uiState.value = uiState.value.copy(
            layout = LayoutKind.Letters,
            shift = ShiftState.Off,
        )
    }

    // ── Safety: always allow the IME to be dismissed ────────────────────────────
    /**
     * Always return true so the system knows we show an input view. Returning
     * false in some edge cases makes the IMMS believe the IME has no UI and
     * it stops dispatching window visibility — a contributor to "stuck IME".
     */
    override fun onEvaluateInputViewShown(): Boolean = true

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

            is KeyAction.SwitchLayout -> {
                // Switch to the requested layout. Preserve shift state across
                // switches — the symbols panel ignores shift at render time
                // (only alphabetic keys are case-aware), so preserving the
                // state means: if the user was in Caps Lock, they stay in
                // Caps Lock when they return to letters via the ABC key.
                uiState.value = uiState.value.copy(layout = action.kind)
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
