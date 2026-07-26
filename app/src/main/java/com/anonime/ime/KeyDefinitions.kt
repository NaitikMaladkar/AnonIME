package com.anonime.ime

/**
 * Static definition of every key row for Phase 1 + Phase 2.
 *
 * Layouts are computed at composition time by [keyboardLayoutFor] so that
 * the current [ShiftState] is reflected without rebuilding the layout tree.
 *
 * Row sizing (relative weights, sum per row ≈ 10):
 *   - Number row:        10 keys × 1.0       = 10
 *   - Letters row1:      10 keys × 1.0       = 10
 *   - Letters row2:      9 keys × 1.0 + 0.5 left margin + 0.5 right margin = 10
 *   - Letters row3:      shift(1.5) + 7×1.0 + backspace(1.5) = 10
 *   - Bottom row (P2):   toggle(1.0) + comma(1.0) + globe(1.0) + space(4.5) + period(1.0) + enter(1.5) = 10
 *   - Emoji rows:        10 keys × 1.0       = 10
 *   - Emoji bottom row:  ABC(1.5) + space(5.5) + backspace(1.5) + enter(1.5) = 10
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

    // ── Bottom row (Letters) ───────────────────────────────────────────────────
    //
    // Phase 2: added a globe key between comma and space so the user can
    // reach the emoji panel from the letters layout. Weights re-balanced to
    // keep the row total at 10:
    //   ?123(1.0)  ,(1.0)  globe(1.0)  space(4.5)  .(1.0)  enter(1.5) = 10.0
    val bottomRow: KeyRow = KeyRow(
        keys = listOf(
            Key(label = "?123", width = 1.0f, action = KeyAction.SwitchLayout(LayoutKind.Symbols1), visual = KeyVisual.Symbols),
            Key(label = ",", width = 1.0f, action = KeyAction.Character(',')),
            Key(label = "globe", width = 1.0f, action = KeyAction.SwitchLayout(LayoutKind.Emojis), visual = KeyVisual.Globe),
            Key(label = "space", width = 4.5f, action = KeyAction.Space, visual = KeyVisual.Space),
            Key(label = ".", width = 1.0f, action = KeyAction.Character('.')),
            Key(label = "enter", width = 1.5f, action = KeyAction.Enter, visual = KeyVisual.Enter),
        )
    )

    /** Full Phase 1+2 letters layout — 5 rows. */
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
    //   [=\<] , [globe] [space] . [↵]
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

    /** Symbols1 row 4 — bottom row with `=\<` toggle to Symbols2 + globe key. */
    val symbols1BottomRow: KeyRow = KeyRow(
        keys = listOf(
            Key(label = "=\\<", width = 1.0f, action = KeyAction.SwitchLayout(LayoutKind.Symbols2), visual = KeyVisual.Symbols),
            Key(label = ",", width = 1.0f, action = KeyAction.Character(',')),
            Key(label = "globe", width = 1.0f, action = KeyAction.SwitchLayout(LayoutKind.Emojis), visual = KeyVisual.Globe),
            Key(label = "space", width = 4.5f, action = KeyAction.Space, visual = KeyVisual.Space),
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
    //   [?123] , [globe] [space] . [↵]
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

    /** Symbols2 row 4 — bottom row with `?123` toggle back to Symbols1 + globe key. */
    val symbols2BottomRow: KeyRow = KeyRow(
        keys = listOf(
            Key(label = "?123", width = 1.0f, action = KeyAction.SwitchLayout(LayoutKind.Symbols1), visual = KeyVisual.Symbols),
            Key(label = ",", width = 1.0f, action = KeyAction.Character(',')),
            Key(label = "globe", width = 1.0f, action = KeyAction.SwitchLayout(LayoutKind.Emojis), visual = KeyVisual.Globe),
            Key(label = "space", width = 4.5f, action = KeyAction.Space, visual = KeyVisual.Space),
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

    // ── Emoji panel ────────────────────────────────────────────────────────────
    //
    // 5 rows × 10 emojis = 50 emojis covering the most common categories:
    //   row 1: smileys + hearts
    //   row 2: gestures + people
    //   row 3: animals + nature
    //   row 4: food + drink
    //   row 5: activities + travel + objects + symbols
    //
    // Each emoji commits via [KeyAction.InsertText] — emojis are surrogate
    // pairs in UTF-16 (e.g. 😀 = U+1F600 = "\uD83D\uDE00"), so they cannot
    // be carried as a single [Char].
    //
    // We deliberately keep the per-row weight = 10 (10 keys × 1.0) so the
    // renderer's Row+weight() layout stays uniform with the letters layout.
    private fun emojiRow(emojis: List<String>): KeyRow = KeyRow(
        keys = emojis.map { e ->
            Key(label = e, action = KeyAction.InsertText(e))
        }
    )

    val emojiRow1: KeyRow = emojiRow(
        listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
        )
    )

    val emojiRow2: KeyRow = emojiRow(
        listOf(
            "🙂", "😉", "😍", "🥰", "😘", "😋", "😛", "😎", "🤩", "🥳",
        )
    )

    val emojiRow3: KeyRow = emojiRow(
        listOf(
            "👍", "👎", "👏", "🙌", "🤝", "🙏", "👋", "💪", "✌️", "🤞",
        )
    )

    val emojiRow4: KeyRow = emojiRow(
        listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "💖",
        )
    )

    val emojiRow5: KeyRow = emojiRow(
        listOf(
            "🔥", "✨", "🎉", "⭐", "💯", "✅", "❌", "⚠️", "❓", "❗",
        )
    )

    val emojiRow6: KeyRow = emojiRow(
        listOf(
            "🐶", "🐱", "🦊", "🐻", "🐼", "🐨", "🦁", "🐯", "🐸", "🦉",
        )
    )

    val emojiRow7: KeyRow = emojiRow(
        listOf(
            "🍎", "🍊", "🍌", "🍉", "🍇", "🍓", "🥑", "🥦", "🍕", "🍔",
        )
    )

    val emojiRow8: KeyRow = emojiRow(
        listOf(
            "⚽", "🏀", "🎮", "🎵", "🎁", "🚗", "✈️", "🏠", "💡", "📱",
        )
    )

    /** Emoji bottom row — ABC toggle (back to Letters), space, backspace, enter. */
    val emojiBottomRow: KeyRow = KeyRow(
        keys = listOf(
            Key(label = "ABC", width = 1.5f, action = KeyAction.SwitchLayout(LayoutKind.Letters), visual = KeyVisual.Symbols),
            Key(label = "space", width = 5.5f, action = KeyAction.Space, visual = KeyVisual.Space),
            Key(label = "backspace", width = 1.5f, action = KeyAction.Backspace, visual = KeyVisual.Backspace),
            Key(label = "enter", width = 1.5f, action = KeyAction.Enter, visual = KeyVisual.Enter),
        )
    )

    /** Full Emojis layout — 9 rows (8 emoji rows + 1 bottom row). */
    val emojisLayout: List<KeyRow> = listOf(
        emojiRow1,
        emojiRow2,
        emojiRow3,
        emojiRow4,
        emojiRow5,
        emojiRow6,
        emojiRow7,
        emojiRow8,
        emojiBottomRow,
    )
}

/**
 * Resolve the active layout for the given [LayoutKind].
 */
fun keyboardLayoutFor(kind: LayoutKind): List<KeyRow> = when (kind) {
    LayoutKind.Letters  -> KeyDefinitions.lettersLayout
    LayoutKind.Symbols1 -> KeyDefinitions.symbols1Layout
    LayoutKind.Symbols2 -> KeyDefinitions.symbols2Layout
    LayoutKind.Emojis   -> KeyDefinitions.emojisLayout
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
