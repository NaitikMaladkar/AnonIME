package com.anonime.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide singleton for reading and writing user settings.
 *
 * ── Why a singleton? ─────────────────────────────────────────────────────────
 * The IME service and the settings Activity share a process (com.anonime),
 * so they share the same SharedPreferences file and the same in-memory
 * [StateFlow]. When the Activity writes a new value, the IME's next
 * composition picks it up via [state].value — no IPC, no restart.
 *
 * ── Why SharedPreferences and not DataStore? ─────────────────────────────────
 * We have a small fixed schema (9 booleans + 2 enums) with no migration
 * needs. SharedPreferences is synchronous on read, simpler to reason about,
 * and avoids an extra dependency. DataStore would be the right choice if
 * we ever add hundreds of keys or need transactional writes.
 *
 * ── Anonymous-typing guarantee ───────────────────────────────────────────────
 * Settings are device-local. No network, no cloud sync, no analytics on
 * which options users toggle. SharedPreferences is included in the app's
 * backup exclusion rules (see res/xml/data_extraction_rules.xml) so they
 * don't propagate to Google Drive either.
 */
class SettingsRepository private constructor(
    private val prefs: SharedPreferences,
) {

    private val _state = MutableStateFlow(loadFromPrefs())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    // ── Mutators ──────────────────────────────────────────────────────────────
    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }
    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }
    fun setKeyHeight(height: KeyHeight) = update { it.copy(keyHeight = height) }

    fun setKeyPopupPreview(enabled: Boolean) = update { it.copy(keyPopupPreview = enabled) }
    fun setLongPressAccents(enabled: Boolean) = update { it.copy(longPressAccents = enabled) }
    fun setAutoCapitalize(enabled: Boolean) = update { it.copy(autoCapitalize = enabled) }
    fun setSoundEnabled(enabled: Boolean) = update { it.copy(soundEnabled = enabled) }
    fun setHapticEnabled(enabled: Boolean) = update { it.copy(hapticEnabled = enabled) }

    // ── Internals ──────────────────────────────────────────────────────────────
    private fun update(transform: (SettingsState) -> SettingsState) {
        val next = transform(_state.value)
        prefs.edit {
            putString(KEY_THEME_MODE, next.themeMode.name)
            putBoolean(KEY_DYNAMIC_COLOR, next.dynamicColor)
            putString(KEY_KEY_HEIGHT, next.keyHeight.name)
            putBoolean(KEY_POPUP_PREVIEW, next.keyPopupPreview)
            putBoolean(KEY_LONGPRESS_ACCENTS, next.longPressAccents)
            putBoolean(KEY_AUTO_CAPITALIZE, next.autoCapitalize)
            putBoolean(KEY_SOUND_ENABLED, next.soundEnabled)
            putBoolean(KEY_HAPTIC_ENABLED, next.hapticEnabled)
        }
        _state.value = next
    }

    private fun loadFromPrefs(): SettingsState = SettingsState(
        themeMode = prefs.getString(KEY_THEME_MODE, null)
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        keyHeight = prefs.getString(KEY_KEY_HEIGHT, null)
            ?.let { runCatching { KeyHeight.valueOf(it) }.getOrNull() }
            ?: KeyHeight.NORMAL,
        keyPopupPreview = prefs.getBoolean(KEY_POPUP_PREVIEW, true),
        // Phase 2 default for longPressAccents flipped to true. Existing users
        // who never touched this toggle will get the new default on next read
        // because the prefs key was never written.
        longPressAccents = prefs.getBoolean(KEY_LONGPRESS_ACCENTS, true),
        autoCapitalize = prefs.getBoolean(KEY_AUTO_CAPITALIZE, true),
        soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, false),
        hapticEnabled = prefs.getBoolean(KEY_HAPTIC_ENABLED, false),
    )

    companion object {
        private const val PREFS_NAME = "anonime_settings"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_KEY_HEIGHT = "key_height"
        private const val KEY_POPUP_PREVIEW = "popup_preview"
        private const val KEY_LONGPRESS_ACCENTS = "longpress_accents"
        private const val KEY_AUTO_CAPITALIZE = "auto_capitalize"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_HAPTIC_ENABLED = "haptic_enabled"

        @Volatile private var INSTANCE: SettingsRepository? = null

        fun get(context: Context): SettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { INSTANCE = it }
            }
    }
}
