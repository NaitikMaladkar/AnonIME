package com.anonime.ime

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `bottom row has 5 keys with correct weights`() {
        val row = KeyDefinitions.bottomRow
        assertEquals(5, row.keys.size)
        val totalWeight = row.keys.sumOf { it.width.toDouble() }
        // Should sum to 10.0 (1.5 + 1.0 + 5.0 + 1.0 + 1.5)
        assertEquals(10.0, totalWeight, 0.001)
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
    }

    @Test
    fun `every layout-toggle key points to a valid LayoutKind`() {
        // Walk every SwitchLayout key in every layout and verify the target
        // is one of the three valid LayoutKinds. This catches typos like
        // accidentally switching to a removed `Symbols` enum value.
        val allKeys = (KeyDefinitions.lettersLayout +
                KeyDefinitions.symbols1Layout +
                KeyDefinitions.symbols2Layout)
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
        // The minimum requirement: every non-Letters layout has an ABC key
        // that returns to Letters, and Symbols1 has a key to Symbols2 and vice versa.
        val lettersToggles = KeyDefinitions.lettersLayout.flatMap { it.keys }
            .filter { it.action is KeyAction.SwitchLayout }
            .map { (it.action as KeyAction.SwitchLayout).kind }
        assertTrue("Letters layout must have a toggle to Symbols1",
            LayoutKind.Symbols1 in lettersToggles)

        val symbols1Toggles = KeyDefinitions.symbols1Layout.flatMap { it.keys }
            .filter { it.action is KeyAction.SwitchLayout }
            .map { (it.action as KeyAction.SwitchLayout).kind }
        assertTrue("Symbols1 layout must have a toggle back to Letters",
            LayoutKind.Letters in symbols1Toggles)
        assertTrue("Symbols1 layout must have a toggle to Symbols2",
            LayoutKind.Symbols2 in symbols1Toggles)

        val symbols2Toggles = KeyDefinitions.symbols2Layout.flatMap { it.keys }
            .filter { it.action is KeyAction.SwitchLayout }
            .map { (it.action as KeyAction.SwitchLayout).kind }
        assertTrue("Symbols2 layout must have a toggle back to Letters",
            LayoutKind.Letters in symbols2Toggles)
        assertTrue("Symbols2 layout must have a toggle back to Symbols1",
            LayoutKind.Symbols1 in symbols2Toggles)
    }
}
