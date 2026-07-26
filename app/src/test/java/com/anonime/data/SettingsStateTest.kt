package com.anonime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-Kotlin tests for the [SettingsState] data class and its enums.
 *
 * These tests don't touch SharedPreferences or Android — they just verify
 * the shape, defaults, and enum-ness of the settings model. A regression
 * here (e.g. accidentally changing a default, or removing a field that
 * the UI depends on) would silently break user-visible behavior.
 */
class SettingsStateTest {

    @Test
    fun `default SettingsState has Phase 2 defaults`() {
        val s = SettingsState()
        // Appearance
        assertEquals(ThemeMode.SYSTEM, s.themeMode)
        assertTrue("dynamicColor should default to true", s.dynamicColor)
        assertEquals(KeyHeight.NORMAL, s.keyHeight)

        // Typing
        assertTrue("keyPopupPreview should default to true", s.keyPopupPreview)
        assertTrue("longPressAccents should default to true (Phase 2)",
            s.longPressAccents)
        assertTrue("autoCapitalize should default to true", s.autoCapitalize)
        assertFalse("soundEnabled should default to false", s.soundEnabled)
        assertFalse("hapticEnabled should default to false", s.hapticEnabled)
    }

    @Test
    fun `SettingsState no longer has setupCompleted field`() {
        // After the Phase 2 refactor, the setup gate was removed — the app
        // always opens at Home and the user enables the keyboard via the
        // GuidedSetupSheet. This test makes sure setupCompleted doesn't
        // silently come back via a merge conflict.
        val s = SettingsState()
        // Use reflection to enumerate constructor parameter names. If anyone
        // re-adds setupCompleted, this list will contain it and the assertion
        // will fail.
        val paramNames = s.javaClass.declaredConstructors
            .firstOrNull { it.parameterCount > 0 }
            ?.parameters
            ?.map { it.name }
            ?: emptyList()
        assertFalse(
            "setupCompleted should not exist on SettingsState (got $paramNames)",
            paramNames.any { it.equals("setupCompleted", ignoreCase = true) },
        )
    }

    @Test
    fun `ThemeMode has exactly three values SYSTEM LIGHT DARK`() {
        val values = ThemeMode.entries.map { it.name }.toSet()
        assertEquals(setOf("SYSTEM", "LIGHT", "DARK"), values)
    }

    @Test
    fun `KeyHeight has exactly three values with correct dp mappings`() {
        val byName = KeyHeight.entries.associateBy { it.name }
        assertEquals(3, byName.size)
        assertEquals(38, byName["COMPACT"]?.dp)
        assertEquals(46, byName["NORMAL"]?.dp)
        assertEquals(54, byName["TALL"]?.dp)
    }

    @Test
    fun `copy preserves fields not mentioned in the transform`() {
        // Defensive: a future refactor that adds a field but forgets to
        // thread it through copy() calls would silently reset the new field.
        // This test doesn't catch every such bug, but it does catch the
        // common case of "copy() returned a fresh default".
        val original = SettingsState(
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            keyHeight = KeyHeight.TALL,
            keyPopupPreview = false,
            longPressAccents = false,
            autoCapitalize = false,
            soundEnabled = true,
            hapticEnabled = true,
        )
        val updated = original.copy(themeMode = ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, updated.themeMode)
        // Everything else should be unchanged.
        assertFalse(updated.dynamicColor)
        assertEquals(KeyHeight.TALL, updated.keyHeight)
        assertFalse(updated.keyPopupPreview)
        assertFalse(updated.longPressAccents)
        assertFalse(updated.autoCapitalize)
        assertTrue(updated.soundEnabled)
        assertTrue(updated.hapticEnabled)
    }
}
