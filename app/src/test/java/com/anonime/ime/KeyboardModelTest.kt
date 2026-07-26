package com.anonime.ime

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-Kotlin tests for the keyboard model. We can't easily test the
 * InputMethodService in a JVM unit test (it needs Android framework classes),
 * but we CAN test the shift state machine and key definitions — which are
 * the most error-prone pure-logic pieces.
 */
class KeyboardModelTest {

    @Test
    fun `shift state cycles Off to OnNext to Locked to Off`() {
        assertEquals(ShiftState.OnNext, ShiftState.Off.next())
        assertEquals(ShiftState.Locked, ShiftState.OnNext.next())
        assertEquals(ShiftState.Off, ShiftState.Locked.next())
        // Full cycle: Off -> OnNext -> Locked -> Off
        assertEquals(ShiftState.Off, ShiftState.Off.next().next().next())
    }

    @Test
    fun `uppercase is true for OnNext and Locked, false for Off`() {
        assertTrue(ShiftState.OnNext.uppercase)
        assertTrue(ShiftState.Locked.uppercase)
        assertEquals(false, ShiftState.Off.uppercase)
    }

    @Test
    fun `letters layout has 5 rows`() {
        assertEquals(5, KeyDefinitions.lettersLayout.size)
    }

    @Test
    fun `number row has 10 keys`() {
        assertEquals(10, KeyDefinitions.numberRow.keys.size)
        KeyDefinitions.numberRow.keys.forEach { key ->
            assertEquals(1, key.label.length)
            assertTrue(key.label[0].isDigit())
        }
    }

    @Test
    fun `letter row 1 is QWERTY`() {
        val labels = KeyDefinitions.letterRow1.keys.map { it.label }
        assertEquals(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), labels)
    }

    @Test
    fun `letter row 3 has shift and backspace modifiers`() {
        val row = KeyDefinitions.letterRow3
        assertEquals(KeyVisual.Shift, row.keys.first().visual)
        assertEquals(KeyVisual.Backspace, row.keys.last().visual)
        assertEquals(1.5f, row.keys.first().width, 0.001f)
        assertEquals(1.5f, row.keys.last().width, 0.001f)
    }

    @Test
    fun `bottom row has 6 keys including a globe key`() {
        // Phase 2: bottom row gained a globe key between comma and space.
        // New weights: ?123(1.0) + ,(1.0) + globe(1.0) + space(4.5) + .(1.0) + enter(1.5) = 10.0
        val row = KeyDefinitions.bottomRow
        assertEquals(6, row.keys.size)
        val totalWeight = row.keys.sumOf { it.width.toDouble() }
        assertEquals(10.0, totalWeight, 0.001)

        // The globe key must be present, with the Globe visual, and target Emojis.
        val globeKey = row.keys.first { it.visual == KeyVisual.Globe }
        assertEquals("globe", globeKey.label)
        assertEquals(KeyAction.SwitchLayout(LayoutKind.Emojis), globeKey.action)

        // The ?123 toggle key is still first.
        assertEquals("?123", row.keys.first().label)
        assertEquals(KeyVisual.Symbols, row.keys.first().visual)

        // The enter key is still last.
        assertEquals(KeyVisual.Enter, row.keys.last().visual)
    }

    @Test
    fun `labelFor uppercases letters when shift is on`() {
        val key = Key(label = "a", action = KeyAction.Character('a'))
        assertEquals("a", labelFor(key, ShiftState.Off))
        assertEquals("A", labelFor(key, ShiftState.OnNext))
        assertEquals("A", labelFor(key, ShiftState.Locked))
    }

    @Test
    fun `labelFor does not change non-letter keys`() {
        val numberKey = Key(label = "1", action = KeyAction.Character('1'))
        assertEquals("1", labelFor(numberKey, ShiftState.OnNext))

        val spaceKey = Key(label = "space", action = KeyAction.Space, visual = KeyVisual.Space)
        assertEquals("space", labelFor(spaceKey, ShiftState.Locked))
    }

    @Test
    fun `keyboardLayoutFor Letters returns letters layout`() {
        val layout = keyboardLayoutFor(LayoutKind.Letters)
        assertEquals(5, layout.size)
        assertEquals(KeyDefinitions.lettersLayout, layout)
    }

    @Test
    fun `every character key has a non-null action`() {
        KeyDefinitions.lettersLayout.flatMap { it.keys }.forEach { key ->
            if (key.visual == KeyVisual.Text) {
                assertNotNull("Key ${key.label} has no action", key.action)
                assertTrue(
                    "Key ${key.label} action is not Character",
                    key.action is KeyAction.Character
                )
            }
        }
    }

    @Test
    fun `lifecycle event ordering is valid for Recomposer startup`() {
        // Document the expected event sequence: ON_CREATE -> ON_START (from onBindInput)
        // -> ... -> ON_STOP -> ON_DESTROY. The Recomposer requires STARTED state
        // to run its effect loop — verify the enum value exists and is correct.
        assertEquals(Lifecycle.State.CREATED, Lifecycle.Event.ON_CREATE.targetState)
        assertEquals(Lifecycle.State.STARTED, Lifecycle.Event.ON_START.targetState)
        assertEquals(Lifecycle.State.RESUMED, Lifecycle.Event.ON_RESUME.targetState)
        assertEquals(Lifecycle.State.STARTED, Lifecycle.Event.ON_PAUSE.targetState)
        assertEquals(Lifecycle.State.CREATED, Lifecycle.Event.ON_STOP.targetState)
        assertEquals(Lifecycle.State.DESTROYED, Lifecycle.Event.ON_DESTROY.targetState)
    }

    // ── Symbols panel tests ──────────────────────────────────────────────────────

    @Test
    fun `symbols1 layout has 4 rows`() {
        assertEquals(4, KeyDefinitions.symbols1Layout.size)
    }

    @Test
    fun `symbols1 row 1 is the number row`() {
        // Symbols1 reuses the letters-layout number row so users see a consistent
        // top row across both layouts.
        assertEquals(KeyDefinitions.numberRow, KeyDefinitions.symbols1Layout[0])
    }

    @Test
    fun `symbols1 row 2 has the expected 10 punctuation chars`() {
        val labels = KeyDefinitions.symbols1Layout[1].keys.map { it.label }
        assertEquals(listOf("@", "#", "$", "%", "&", "-", "+", "(", ")", "/"), labels)
    }

    @Test
    fun `symbols1 row 3 starts with ABC toggle and ends with backspace`() {
        val row = KeyDefinitions.symbols1Layout[2]
        assertEquals("ABC", row.keys.first().label)
        assertEquals(KeyVisual.Symbols, row.keys.first().visual)
        assertEquals(KeyAction.SwitchLayout(LayoutKind.Letters), row.keys.first().action)
        assertEquals(KeyVisual.Backspace, row.keys.last().visual)
        assertEquals(1.5f, row.keys.first().width, 0.001f)
        assertEquals(1.5f, row.keys.last().width, 0.001f)
    }

    @Test
    fun `symbols1 bottom row starts with the equals-backslash-less-than toggle`() {
        val row = KeyDefinitions.symbols1Layout[3]
        assertEquals("=\\<", row.keys.first().label)
        assertEquals(KeyAction.SwitchLayout(LayoutKind.Symbols2), row.keys.first().action)
    }

    @Test
    fun `symbols2 layout has 4 rows`() {
        assertEquals(4, KeyDefinitions.symbols2Layout.size)
    }

    @Test
    fun `symbols2 row 1 has 10 keys`() {
        val row = KeyDefinitions.symbols2Layout[0]
        assertEquals(10, row.keys.size)
        row.keys.forEach { key ->
            assertEquals(1, key.label.length)
            assertNotNull("Key ${key.label} has no action", key.action)
        }
    }

    @Test
    fun `symbols2 row 2 includes a backslash character`() {
        val row = KeyDefinitions.symbols2Layout[1]
        val backslashKey = row.keys.first { it.label == "\\" }
        assertEquals(KeyAction.Character('\\'), backslashKey.action)
    }

    @Test
    fun `symbols2 row 3 starts with ABC toggle and ends with backspace`() {
        val row = KeyDefinitions.symbols2Layout[2]
        assertEquals("ABC", row.keys.first().label)
        assertEquals(KeyAction.SwitchLayout(LayoutKind.Letters), row.keys.first().action)
        assertEquals(KeyVisual.Backspace, row.keys.last().visual)
    }

    @Test
    fun `symbols2 bottom row starts with the ?123 toggle back to Symbols1`() {
        val row = KeyDefinitions.symbols2Layout[3]
        assertEquals("?123", row.keys.first().label)
        assertEquals(KeyAction.SwitchLayout(LayoutKind.Symbols1), row.keys.first().action)
    }

    @Test
    fun `every symbols row has weights summing to 10`() {
        // Row weights must sum to 10 (the renderer divides the row width by 10
        // to assign per-key weights). Verifying this prevents layout drift
        // where one row is wider/narrower than the others.
        val allRows = KeyDefinitions.symbols1Layout + KeyDefinitions.symbols2Layout
        allRows.forEachIndexed { idx, row ->
            val total = row.keys.sumOf { it.width.toDouble() }
            assertEquals("Symbols row $idx weights do not sum to 10", 10.0, total, 0.001)
        }
    }

    @Test
    fun `every emoji row has weights summing to 10`() {
        KeyDefinitions.emojisLayout.forEachIndexed { idx, row ->
            val total = row.keys.sumOf { it.width.toDouble() }
            assertEquals("Emoji row $idx weights do not sum to 10", 10.0, total, 0.001)
        }
    }

    @Test
    fun `every text key in symbols layouts commits a Character action`() {
        val allKeys = (KeyDefinitions.symbols1Layout + KeyDefinitions.symbols2Layout)
            .flatMap { it.keys }
        allKeys.forEach { key ->
            if (key.visual == KeyVisual.Text) {
                assertNotNull("Key ${key.label} has no action", key.action)
                assertTrue(
                    "Key ${key.label} action is not Character",
                    key.action is KeyAction.Character
                )
            }
        }
    }

    @Test
    fun `keyboardLayoutFor returns the correct layout for every LayoutKind`() {
        assertEquals(KeyDefinitions.lettersLayout, keyboardLayoutFor(LayoutKind.Letters))
        assertEquals(KeyDefinitions.symbols1Layout, keyboardLayoutFor(LayoutKind.Symbols1))
        assertEquals(KeyDefinitions.symbols2Layout, keyboardLayoutFor(LayoutKind.Symbols2))
        assertEquals(KeyDefinitions.emojisLayout, keyboardLayoutFor(LayoutKind.Emojis))
    }

    @Test
    fun `every layout-toggle key points to a valid LayoutKind`() {
        // Walk every SwitchLayout key in every layout and verify the target
        // is one of the four valid LayoutKinds. This catches typos like
        // accidentally switching to a removed `Symbols` enum value.
        val allKeys = (KeyDefinitions.lettersLayout +
                KeyDefinitions.symbols1Layout +
                KeyDefinitions.symbols2Layout +
                KeyDefinitions.emojisLayout)
            .flatMap { it.keys }
        val validKinds = LayoutKind.entries.toSet()
        allKeys.forEach { key ->
            val action = key.action
            if (action is KeyAction.SwitchLayout) {
                assertTrue(
                    "Toggle key ${key.label} targets invalid layout ${action.kind}",
                    action.kind in validKinds
                )
            }
        }
    }

    @Test
    fun `layout graph is fully navigable`() {
        // Verify the user can move between any two layouts via toggle keys:
        //   Letters  <-> Symbols1  (via ?123 / ABC)
        //   Symbols1 <-> Symbols2  (via =\< / ?123)
        //   Letters  <-> Symbols2  (via ABC on Symbols2, but no direct key from Letters)
        //   Letters  <-> Emojis    (via globe / ABC)  ← Phase 2
        //   Symbols1 <-> Emojis    (via globe / ABC)  ← Phase 2
        //   Symbols2 <-> Emojis    (via globe / ABC)  ← Phase 2
        //
        // The minimum requirement: every non-Letters layout has an ABC key
        // that returns to Letters, Symbols1 has a key to Symbols2 and vice versa,
        // and every bottom row has a globe key that goes to Emojis.
        val lettersToggles = KeyDefinitions.lettersLayout.flatMap { it.keys }
            .filter { it.action is KeyAction.SwitchLayout }
            .map { (it.action as KeyAction.SwitchLayout).kind }
        assertTrue("Letters layout must have a toggle to Symbols1",
            LayoutKind.Symbols1 in lettersToggles)
        assertTrue("Letters layout must have a toggle to Emojis (via globe key)",
            LayoutKind.Emojis in lettersToggles)

        val symbols1Toggles = KeyDefinitions.symbols1Layout.flatMap { it.keys }
            .filter { it.action is KeyAction.SwitchLayout }
            .map { (it.action as KeyAction.SwitchLayout).kind }
        assertTrue("Symbols1 layout must have a toggle back to Letters",
            LayoutKind.Letters in symbols1Toggles)
        assertTrue("Symbols1 layout must have a toggle to Symbols2",
            LayoutKind.Symbols2 in symbols1Toggles)
        assertTrue("Symbols1 layout must have a toggle to Emojis (via globe key)",
            LayoutKind.Emojis in symbols1Toggles)

        val symbols2Toggles = KeyDefinitions.symbols2Layout.flatMap { it.keys }
            .filter { it.action is KeyAction.SwitchLayout }
            .map { (it.action as KeyAction.SwitchLayout).kind }
        assertTrue("Symbols2 layout must have a toggle back to Letters",
            LayoutKind.Letters in symbols2Toggles)
        assertTrue("Symbols2 layout must have a toggle back to Symbols1",
            LayoutKind.Symbols1 in symbols2Toggles)
        assertTrue("Symbols2 layout must have a toggle to Emojis (via globe key)",
            LayoutKind.Emojis in symbols2Toggles)

        val emojisToggles = KeyDefinitions.emojisLayout.flatMap { it.keys }
            .filter { it.action is KeyAction.SwitchLayout }
            .map { (it.action as KeyAction.SwitchLayout).kind }
        assertTrue("Emojis layout must have a toggle back to Letters (via ABC key)",
            LayoutKind.Letters in emojisToggles)
    }

    // ── Emoji panel tests ──────────────────────────────────────────────────────

    @Test
    fun `emojis layout has 9 rows`() {
        // 8 emoji rows + 1 bottom row = 9 total.
        assertEquals(9, KeyDefinitions.emojisLayout.size)
    }

    @Test
    fun `every emoji row has exactly 10 keys`() {
        // Renderer expects 10-unit-wide rows so the layout aligns with the
        // letters keyboard.
        KeyDefinitions.emojisLayout.dropLast(1).forEachIndexed { idx, row ->
            assertEquals("Emoji row $idx should have 10 keys", 10, row.keys.size)
        }
    }

    @Test
    fun `every emoji key commits an InsertText action with its label`() {
        // Walk all 8 emoji rows (skip the bottom row which has space/backspace/enter).
        KeyDefinitions.emojisLayout.dropLast(1).flatMap { it.keys }.forEach { key ->
            val action = key.action
            assertTrue(
                "Emoji key ${key.label} should have InsertText action, got $action",
                action is KeyAction.InsertText,
            )
            assertEquals(
                "Emoji key ${key.label} should commit its label as text",
                key.label,
                (action as KeyAction.InsertText).text,
            )
        }
    }

    @Test
    fun `emoji bottom row has ABC toggle that returns to Letters`() {
        val row = KeyDefinitions.emojisLayout.last()
        val abcKey = row.keys.first()
        assertEquals("ABC", abcKey.label)
        assertEquals(KeyVisual.Symbols, abcKey.visual)
        assertEquals(KeyAction.SwitchLayout(LayoutKind.Letters), abcKey.action)
    }

    @Test
    fun `emoji bottom row has backspace and enter keys`() {
        val row = KeyDefinitions.emojisLayout.last()
        // 4 keys: ABC + space + backspace + enter
        assertEquals(4, row.keys.size)
        val visuals = row.keys.map { it.visual }
        assertTrue("Bottom row should have a space key", KeyVisual.Space in visuals)
        assertTrue("Bottom row should have a backspace key", KeyVisual.Backspace in visuals)
        assertTrue("Bottom row should have an enter key", KeyVisual.Enter in visuals)
    }

    // ── Accent map tests ──────────────────────────────────────────────────────

    @Test
    fun `accent map returns variants for common Latin letters`() {
        // Cover the most common accents used in Spanish / French / German / Portuguese.
        val aAccents = AccentMap.accentsFor('a')
        assertNotNull(aAccents)
        assertTrue("á should be in a-accents", "á" in aAccents!!)
        assertTrue("à should be in a-accents", "à" in aAccents)
        assertTrue("ä should be in a-accents", "ä" in aAccents)

        val eAccents = AccentMap.accentsFor('e')
        assertNotNull(eAccents)
        assertTrue("é should be in e-accents", "é" in eAccents!!)
        assertTrue("è should be in e-accents", "è" in eAccents)
        assertTrue("ë should be in e-accents", "ë" in eAccents)

        val nAccents = AccentMap.accentsFor('n')
        assertNotNull(nAccents)
        assertTrue("ñ should be in n-accents", "ñ" in nAccents!!)

        val uAccents = AccentMap.accentsFor('u')
        assertNotNull(uAccents)
        assertTrue("ü should be in u-accents", "ü" in uAccents!!)
        assertTrue("ú should be in u-accents", "ú" in uAccents)

        val cAccents = AccentMap.accentsFor('c')
        assertNotNull(cAccents)
        assertTrue("ç should be in c-accents", "ç" in cAccents!!)
    }

    @Test
    fun `accent map is case-insensitive on input`() {
        // 'A' and 'a' should return the same list.
        val lower = AccentMap.accentsFor('a')
        val upper = AccentMap.accentsFor('A')
        assertEquals(lower, upper)
    }

    @Test
    fun `accent map returns null for letters without accents and for non-letters`() {
        // 'q', 'w', 'v' etc. have no accents in our map.
        assertNull(AccentMap.accentsFor('q'))
        assertNull(AccentMap.accentsFor('w'))
        assertNull(AccentMap.accentsFor('v'))
        // Digits and punctuation have no accents.
        assertNull(AccentMap.accentsFor('1'))
        assertNull(AccentMap.accentsFor('?'))
        assertNull(AccentMap.accentsFor(' '))
    }

    @Test
    fun `hasAccents is true for single Latin letters in the map, false otherwise`() {
        assertTrue(AccentMap.hasAccents("a"))
        assertTrue(AccentMap.hasAccents("A"))
        assertTrue(AccentMap.hasAccents("e"))
        assertTrue(AccentMap.hasAccents("n"))

        // Letters not in the map.
        assertFalse(AccentMap.hasAccents("q"))
        assertFalse(AccentMap.hasAccents("w"))

        // Multi-character labels and non-letters.
        assertFalse(AccentMap.hasAccents("ab"))
        assertFalse(AccentMap.hasAccents("?"))
        assertFalse(AccentMap.hasAccents("1"))
        assertFalse(AccentMap.hasAccents(""))
        assertFalse(AccentMap.hasAccents("space"))
    }

    @Test
    fun `every accent in the map is a single non-empty string`() {
        // Defensive: catch any accidental empty / multi-char entries.
        for (c in 'a'..'z') {
            val accents = AccentMap.accentsFor(c) ?: continue
            accents.forEach { accent ->
                assertTrue("Accent for $c should be non-empty", accent.isNotEmpty())
                // We allow surrogate pairs (emoji-like multi-codepoint strings)
                // but for the accent map every entry should be a single
                // grapheme cluster of length <= 2 UTF-16 chars.
                assertTrue(
                    "Accent '$accent' for $c is too long (${accent.length} chars)",
                    accent.length <= 2,
                )
            }
        }
    }
}
