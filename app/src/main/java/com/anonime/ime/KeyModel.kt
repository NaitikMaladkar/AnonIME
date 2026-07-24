package com.anonime.ime

/**
 * The set of actions a key press can produce.
 *
 * Phase 1 only models the primitive actions needed for a QWERTY + number row
 * keyboard with shift/caps. Phase 2+ will extend this (symbols panel, voice,
 * language switch, etc.) without changing the existing action contract.
 */
sealed interface KeyAction {
    /** Commit a literal character to the input connection. */
    data class Character(val char: Char) : KeyAction

    /** Delete one code point before the cursor. */
    data object Backspace : KeyAction

    /** Toggle / activate shift state. */
    data object Shift : KeyAction

    /** Commit a space. Modeled separately so the space bar can render full-width. */
    data object Space : KeyAction

    /** Trigger the IME action (Go / Search / Send / Done / Next) on the input. */
    data object Enter : KeyAction

    /** Switch to the symbols panel — stub for Phase 2, included so the model is stable. */
    data object ToggleSymbols : KeyAction
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
    Symbols,     // ?123 toggle
}

/**
 * A row of keys. Rows in different layouts share the same model so the
 * renderer doesn't need to know the layout kind.
 */
data class KeyRow(val keys: List<Key>)

/**
 * The set of named layouts the IME can show.
 *
 * Phase 1 ships only [Letters] (with [ShiftState] controlling case).
 * [Symbols] is reserved for Phase 2.
 */
enum class LayoutKind { Letters, Symbols }

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
