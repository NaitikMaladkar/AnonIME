package com.anonime.data

/**
 * The user's theme preference — drives both the settings Activity and the IME.
 *
 *  - [SYSTEM] : follow the device's dark/light setting.
 *  - [LIGHT]  : always light, regardless of system.
 *  - [DARK]   : always dark, regardless of system.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Per-row key height for the keyboard.
 *
 *  - [COMPACT] : 38 dp — more screen real estate, harder to type.
 *  - [NORMAL]  : 46 dp — the Phase 1 default.
 *  - [TALL]    : 54 dp — easier to type, less screen.
 */
enum class KeyHeight(val dp: Int) {
    COMPACT(38),
    NORMAL(46),
    TALL(54),
}

/**
 * Immutable snapshot of every user-tunable setting.
 *
 * Defaults match Phase 1 behavior so existing users see no regression.
 *
 * ── Persistence ──────────────────────────────────────────────────────────────
 *  - Stored in SharedPreferences via [SettingsRepository].
 *  - NOT backed by a database — preserves the anonymous-typing guarantee.
 *  - NOT backed by cloud sync — settings stay on the device.
 *
 * ── Live-apply contract ──────────────────────────────────────────────────────
 * The IME reads the current [SettingsState] from [SettingsRepository.state]
 * every time it draws a frame, so changes flow through without restart.
 * Settings that have no implementation yet (sound, haptic, accents, popups)
 * are still persisted here so the UI can be wired ahead of the logic.
 */
data class SettingsState(
    // ── Appearance ────────────────────────────────────────────────────────────
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val keyHeight: KeyHeight = KeyHeight.NORMAL,

    // ── Typing (stubs for now — toggles save but don't yet affect the keyboard) ─
    val keyPopupPreview: Boolean = true,
    val longPressAccents: Boolean = false,
    val autoCapitalize: Boolean = true,
    val soundEnabled: Boolean = false,
    val hapticEnabled: Boolean = false,

    // ── Setup state ────────────────────────────────────────────────────────────
    /**
     * Set to true the first time the user completes (or skips) the setup
     * wizard. Used by MainActivity to decide whether to land on the Setup
     * screen or the Home screen on launch.
     */
    val setupCompleted: Boolean = false,
)
