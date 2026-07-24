package com.anonime.ime

/**
 * Static definition of every key row for Phase 1.
 *
 * Layouts are computed at composition time by [keyboardLayoutFor] so that
 * the current [ShiftState] is reflected without rebuilding the layout tree.
 *
 * Row sizing (relative weights, sum per row ≈ 10):
 *   - Number row:   10 keys × 1.0       = 10
 *   - Letters row1: 10 keys × 1.0       = 10
 *   - Letters row2: 9 keys × 1.0 + 0.5 left margin + 0.5 right margin = 10
 *   - Letters row3: shift(1.5) + 7×1.0 + backspace(1.5) = 10
 *   - Bottom row:   symbols(1.5) + comma(1.0) + space(5.0) + period(1.0) + enter(1.5) = 10
 */
object KeyDefinitions {

    // ── Number row ────────────────────────────────────────────────────────────
    val numberRow: KeyRow = KeyRow(
        keys = "1234567890".map { ch ->
            Key(
                label = ch.toString(),
                action = KeyAction.Character(ch),
            )
        }
    )

    // ── Letter rows (lowercase canonical; case applied at render) ──────────────
    val letterRow1: KeyRow = KeyRow(
        keys = "qwertyuiop".map { ch ->
            Key(label = ch.toString(), action = KeyAction.Character(ch))
        }
    )

    val letterRow2: KeyRow = KeyRow(
        keys = "asdfghjkl".map { ch ->
            Key(label = ch.toString(), action = KeyAction.Character(ch))
        }
    )

    val letterRow3: KeyRow = KeyRow(
        keys = buildList {
            add(Key(label = "shift", width = 1.5f, action = KeyAction.Shift, visual = KeyVisual.Shift))
            "zxcvbnm".forEach { ch ->
                add(Key(label = ch.toString(), action = KeyAction.Character(ch)))
            }
            add(Key(label = "backspace", width = 1.5f, action = KeyAction.Backspace, visual = KeyVisual.Backspace))
        }
    )

    // ── Bottom row ─────────────────────────────────────────────────────────────
    val bottomRow: KeyRow = KeyRow(
        keys = listOf(
            Key(label = "?123", width = 1.5f, action = KeyAction.ToggleSymbols, visual = KeyVisual.Symbols),
            Key(label = ",", width = 1.0f, action = KeyAction.Character(',')),
            Key(label = "space", width = 5.0f, action = KeyAction.Space, visual = KeyVisual.Space),
            Key(label = ".", width = 1.0f, action = KeyAction.Character('.')),
            Key(label = "enter", width = 1.5f, action = KeyAction.Enter, visual = KeyVisual.Enter),
        )
    )

    /** Full Phase 1 letters layout — 5 rows. */
    val lettersLayout: List<KeyRow> = listOf(
        numberRow,
        letterRow1,
        letterRow2,
        letterRow3,
        bottomRow,
    )
}

/**
 * Resolve the active layout for the given [LayoutKind].
 *
 * Phase 1 only renders [LayoutKind.Letters]; the Symbols branch returns
 * the same Letters layout as a placeholder so the UI never explodes.
 */
fun keyboardLayoutFor(kind: LayoutKind): List<KeyRow> = when (kind) {
    LayoutKind.Letters -> KeyDefinitions.lettersLayout
    LayoutKind.Symbols -> KeyDefinitions.lettersLayout // TODO Phase 2: real symbols panel
}

/**
 * Apply [shift] to a key label, when applicable.
 * Only alphabetic keys change case; everything else is rendered as-is.
 */
fun labelFor(key: Key, shift: ShiftState): String =
    if (shift.uppercase && key.label.length == 1 && key.label[0].isLetter()) {
        key.label.uppercase()
    } else {
        key.label
    }
