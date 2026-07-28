package com.anonime.ime

/**
 * The set of actions a key press can produce.
 *
 * Phase 1 only modeled the primitive actions needed for a QWERTY + number row
 * keyboard with shift/caps. Phase 2 adds:
 *   - [InsertText]   : used by the emoji panel and by long-press accent popups
 *                      to commit multi-codepoint strings (e.g. "é", "😀", "ñ").
 *   - [SwitchLayout] : now also targets the [LayoutKind.Emojis] layout via the
 *                      globe key on the bottom row.
 */
sealed interface KeyAction {
    /** Commit a literal character to the input connection. */
    data class Character(val char: Char) : KeyAction

    /**
     * Commit an arbitrary text string to the input connection.
     *
     * Used for:
     *   - Emoji keys (e.g. "😀", "❤️") — every emoji is a surrogate pair in
     *     UTF-16, so it cannot fit in a single [Char].
     *   - Long-press accent variants (e.g. "é", "ñ", "ü") — single Unicode
     *     codepoint, but using the same code path as emoji keeps the dispatch
     *     simple and uniform.
     */
    data class InsertText(val text: String) : KeyAction

    /** Delete one code point before the cursor. */
    data object Backspace : KeyAction

    /** Toggle / activate shift state. */
    data object Shift : KeyAction

    /** Commit a space. Modeled separately so the space bar can render full-width. */
    data object Space : KeyAction

    /** Trigger the IME action (Go / Search / Send / Done / Next) on the input. */
    data object Enter : KeyAction

    /**
     * Switch to a different [LayoutKind]. Used by the layout-toggle keys:
     *   - `?123`  : Letters  -> Symbols1
     *   - `ABC`   : Symbols1 -> Letters (or Symbols2 -> Letters, or Emojis -> Letters)
     *   - `=\<`   : Symbols1 -> Symbols2
     *   - `?123`  : Symbols2 -> Symbols1
     *   - globe   : any non-Emoji layout -> Emojis
     *   - menu    : any layout -> Menu (toolbar customization panel)
     */
    data class SwitchLayout(val kind: LayoutKind) : KeyAction

    /**
     * Dispatch a toolbar item tap. The IME service routes the action based on
     * [ToolbarItemKind.toAction] — see [AnonIMEService.handleKeyAction].
     *
     * Used for items that don't have a dedicated [KeyAction] subtype
     * (Settings, Theme, Paste, Search, Translate, …). For items that do
     * (Emoji → [SwitchLayout], Backspace → [Backspace]), the toolbar calls
     * those actions directly.
     */
    data class ToolbarAction(val kind: ToolbarItemKind) : KeyAction

    /**
     * Add [kind] to the user's pinned toolbar items. Dispatched when the user
     * drags a chip from the menu panel onto the toolbar drop zone. The
     * repository persists the new list, and the next composition picks it up.
     */
    data class AddToolbarItem(val kind: ToolbarItemKind) : KeyAction

    /**
     * Remove [kind] from the user's pinned toolbar items. Dispatched when the
     * user long-presses a toolbar item and confirms removal. Fixed items
     * (Voice, Menu) are ignored — they cannot be removed.
     */
    data class RemoveToolbarItem(val kind: ToolbarItemKind) : KeyAction
}

/**
 * Visual + semantic model of a single key on the keyboard.
 *
 * - [label]     : what to render on the key cap.
 * - [width]     : relative width inside the row (1.0 = single key).
 * - [action]    : what to do when tapped. Null = modifier visual only.
 * - [visual]    : hints to the renderer (e.g. shift arrow icon, backspace icon).
 */
data class Key(
    val label: String,
    val width: Float = 1f,
    val action: KeyAction? = null,
    val visual: KeyVisual = KeyVisual.Text,
)

enum class KeyVisual {
    Text,        // plain character label
    Shift,       // up-arrow, state-aware tint
    Backspace,   // delete icon
    Space,       // wide bar
    Enter,       // context-aware label/icon
    Symbols,     // ?123 / ABC / =\< toggle
    Globe,       // emoji/language switcher — globe icon
}

/**
 * A row of keys. Rows in different layouts share the same model so the
 * renderer doesn't need to know the layout kind.
 */
data class KeyRow(val keys: List<Key>)

/**
 * The set of named layouts the IME can show.
 *
 *  - [Letters]  : QWERTY + number row + shift/caps.
 *  - [Symbols1] : primary symbols panel — punctuation, currency, math basics.
 *                 Toggled via the `?123` key on the letters bottom row.
 *  - [Symbols2] : extended symbols panel — more currency, math, brackets.
 *                 Toggled via the `=\<` key on the Symbols1 bottom row.
 *  - [Emojis]   : grid of common emojis across smileys / hearts / hands /
 *                 animals / food / activities / travel / objects categories.
 *                 Toggled via the globe key on every bottom row. Returns to
 *                 Letters via the `ABC` key on the Emojis bottom row.
 *  - [Menu]     : toolbar customization panel — a grid of "small buttons"
 *                 (chips) for every [ToolbarItemKind]. Chips can be dragged
 *                 up to the toolbar drop zone to pin them, and tapped to
 *                 perform their action. Returns to Letters via the back
 *                 button or by tapping the menu icon again.
 */
enum class LayoutKind { Letters, Symbols1, Symbols2, Emojis, Menu }

/**
 * Shift state machine — three explicit states keep the renderer honest.
 *
 *  - Off       : lowercase letters
 *  - OnNext    : next key press commits uppercase, then auto-reverts to Off
 *  - Locked    : caps lock — uppercase until shift is pressed again
 */
enum class ShiftState {
    Off, OnNext, Locked;

    val uppercase: Boolean get() = this != Off

    /** Pressing shift cycles Off → OnNext → Locked → Off. */
    fun next(): ShiftState = when (this) {
        Off -> OnNext
        OnNext -> Locked
        Locked -> Off
    }
}
