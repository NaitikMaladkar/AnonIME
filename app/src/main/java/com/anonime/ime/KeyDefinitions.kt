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
            Key(label = "?123", width = 1.5f, action = KeyAction.SwitchLayout(LayoutKind.Symbols1), visual = KeyVisual.Symbols),
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

    // ── Symbols panel 1 (primary) ──────────────────────────────────────────────
    //
    //   1 2 3 4 5 6 7 8 9 0
    //   @ # $ % & - + ( ) /
    //   [ABC] * " ' : ; ! ? [⌫]
    //   [=\<] , [    space    ] . [↵]
    //
    /** Symbols1 row 2 — common punctuation / currency / arithmetic. */
    val symbols1Row2: KeyRow = KeyRow(
        keys = "@#$%&-+()/".map { ch ->
            Key(label = ch.toString(), action = KeyAction.Character(ch))
        }
    )

    /** Symbols1 row 3 — more punctuation, with ABC toggle and backspace. */
    val symbols1Row3: KeyRow = KeyRow(
        keys = buildList {
            add(Key(label = "ABC", width = 1.5f, action = KeyAction.SwitchLayout(LayoutKind.Letters), visual = KeyVisual.Symbols))
            add(Key(label = "*", action = KeyAction.Character('*')))
            add(Key(label = "\"", action = KeyAction.Character('"')))
            add(Key(label = "'", action = KeyAction.Character('\'')))
            add(Key(label = ":", action = KeyAction.Character(':')))
            add(Key(label = ";", action = KeyAction.Character(';')))
            add(Key(label = "!", action = KeyAction.Character('!')))
            add(Key(label = "?", action = KeyAction.Character('?')))
            add(Key(label = "backspace", width = 1.5f, action = KeyAction.Backspace, visual = KeyVisual.Backspace))
        }
    )

    /** Symbols1 row 4 — bottom row with `=\<` toggle to Symbols2. */
    val symbols1BottomRow: KeyRow = KeyRow(
        keys = listOf(
            Key(label = "=\\<", width = 1.5f, action = KeyAction.SwitchLayout(LayoutKind.Symbols2), visual = KeyVisual.Symbols),
            Key(label = ",", width = 1.0f, action = KeyAction.Character(',')),
            Key(label = "space", width = 5.0f, action = KeyAction.Space, visual = KeyVisual.Space),
            Key(label = ".", width = 1.0f, action = KeyAction.Character('.')),
            Key(label = "enter", width = 1.5f, action = KeyAction.Enter, visual = KeyVisual.Enter),
        )
    )

    /** Full Symbols1 layout — 4 rows. */
    val symbols1Layout: List<KeyRow> = listOf(
        numberRow, // reuse: 1 2 3 4 5 6 7 8 9 0
        symbols1Row2,
        symbols1Row3,
        symbols1BottomRow,
    )

    // ── Symbols panel 2 (extended) ─────────────────────────────────────────────
    //
    //   ~ ` | • ™ ® ° × ÷ π
    //   £ ¢ € ¥ ^ = { } \ §
    //   [ABC] % © ✓ [ ] < > [⌫]
    //   [?123] , [    space    ] . [↵]
    //
    /** Symbols2 row 1 — less common typographic / math symbols. */
    val symbols2Row1: KeyRow = KeyRow(
        keys = "~`|•™®°×÷π".map { ch ->
            Key(label = ch.toString(), action = KeyAction.Character(ch))
        }
    )

    /** Symbols2 row 2 — extended currency, brackets, section sign. */
    val symbols2Row2: KeyRow = KeyRow(
        keys = "£¢€¥^={}\\§".map { ch ->
            Key(label = ch.toString(), action = KeyAction.Character(ch))
        }
    )

    /** Symbols2 row 3 — more brackets / comparison, with ABC toggle and backspace. */
    val symbols2Row3: KeyRow = KeyRow(
        keys = buildList {
            add(Key(label = "ABC", width = 1.5f, action = KeyAction.SwitchLayout(LayoutKind.Letters), visual = KeyVisual.Symbols))
            add(Key(label = "%", action = KeyAction.Character('%')))
            add(Key(label = "©", action = KeyAction.Character('©')))
            add(Key(label = "✓", action = KeyAction.Character('✓')))
            add(Key(label = "[", action = KeyAction.Character('[')))
            add(Key(label = "]", action = KeyAction.Character(']')))
            add(Key(label = "<", action = KeyAction.Character('<')))
            add(Key(label = ">", action = KeyAction.Character('>')))
            add(Key(label = "backspace", width = 1.5f, action = KeyAction.Backspace, visual = KeyVisual.Backspace))
        }
    )

    /** Symbols2 row 4 — bottom row with `?123` toggle back to Symbols1. */
    val symbols2BottomRow: KeyRow = KeyRow(
        keys = listOf(
            Key(label = "?123", width = 1.5f, action = KeyAction.SwitchLayout(LayoutKind.Symbols1), visual = KeyVisual.Symbols),
            Key(label = ",", width = 1.0f, action = KeyAction.Character(',')),
            Key(label = "space", width = 5.0f, action = KeyAction.Space, visual = KeyVisual.Space),
            Key(label = ".", width = 1.0f, action = KeyAction.Character('.')),
            Key(label = "enter", width = 1.5f, action = KeyAction.Enter, visual = KeyVisual.Enter),
        )
    )

    /** Full Symbols2 layout — 4 rows. */
    val symbols2Layout: List<KeyRow> = listOf(
        symbols2Row1,
        symbols2Row2,
        symbols2Row3,
        symbols2BottomRow,
    )
}

/**
 * Resolve the active layout for the given [LayoutKind].
 */
fun keyboardLayoutFor(kind: LayoutKind): List<KeyRow> = when (kind) {
    LayoutKind.Letters  -> KeyDefinitions.lettersLayout
    LayoutKind.Symbols1 -> KeyDefinitions.symbols1Layout
    LayoutKind.Symbols2 -> KeyDefinitions.symbols2Layout
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
