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
}
