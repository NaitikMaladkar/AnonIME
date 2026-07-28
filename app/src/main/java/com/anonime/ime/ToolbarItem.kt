package com.anonime.ime

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The set of items the user can pin to the keyboard toolbar.
 *
 * Two of these (VOICE, MENU) are "fixed" — they always appear at the left and
 * right ends of the toolbar respectively and cannot be removed or reordered.
 * The rest are user-customizable: they appear in the middle of the toolbar in
 * the order the user has chosen, and can be added / removed / reordered via
 * the Menu panel (drag-and-drop).
 *
 * ── Actions ─────────────────────────────────────────────────────────────────
 * Each kind maps to a [KeyAction] via [toAction]. The IME service's
 * [AnonIMEService.handleKeyAction] is the single dispatch point — adding a
 * new toolbar item only requires (1) adding it to this enum and (2) handling
 * its action in the service. No UI plumbing needed.
 *
 * ── Persistence ──────────────────────────────────────────────────────────────
 * The user's chosen set + order is persisted in SharedPreferences as a
 * comma-separated list of names (see [SettingsRepository]). The default set
 * is [DEFAULT_TOOLBAR] — three of the most commonly-used items.
 */
enum class ToolbarItemKind {
    // ── Fixed (always present, not draggable) ────────────────────────────────
    /** Microphone — leftmost slot. Placeholder for future voice typing. */
    VOICE,

    /** Four-block grid — rightmost slot. Toggles the menu panel. */
    MENU,

    // ── User-customizable (pinned to the middle of the toolbar) ──────────────
    /** Open the emoji panel. */
    EMOJI,

    /** Open the system settings Activity for this IME. */
    SETTINGS,

    /** Cycle theme mode: System → Light → Dark → System. */
    THEME,

    /** Paste from the system clipboard. */
    PASTE,

    /** Open a web search for the selected text (or cursor word). */
    SEARCH,

    /** Placeholder for a future translator feature. */
    TRANSLATE,

    /** Placeholder for a future stickers/GIF picker. */
    STICKERS,

    /** Open the clipboard history (Phase 3 — currently a no-op). */
    CLIPBOARD,

    /** Toggle one-handed mode (Phase 3 — currently a no-op). */
    ONE_HANDED,

    /** Move cursor one character left. */
    CURSOR_LEFT,

    /** Move cursor one character right. */
    CURSOR_RIGHT,

    /** Delete one codepoint before the cursor (same as the backspace key). */
    BACKSPACE,
    ;

    /** Human-readable label shown under the icon in the menu panel. */
    val displayLabel: String get() = when (this) {
        VOICE       -> "Voice"
        MENU        -> "Menu"
        EMOJI       -> "Emoji"
        SETTINGS    -> "Settings"
        THEME       -> "Theme"
        PASTE       -> "Paste"
        SEARCH      -> "Search"
        TRANSLATE   -> "Translate"
        STICKERS    -> "Stickers"
        CLIPBOARD   -> "Clipboard"
        ONE_HANDED  -> "One-hand"
        CURSOR_LEFT -> "◀ Cursor"
        CURSOR_RIGHT-> "Cursor ▶"
        BACKSPACE   -> "Delete"
    }

    /** Material icon to render on the toolbar / menu chip. */
    val icon: ImageVector get() = when (this) {
        VOICE        -> Icons.Outlined.Mic
        MENU         -> Icons.Outlined.Apps            // 4-block illustration
        EMOJI        -> Icons.Outlined.EmojiEmotions
        SETTINGS     -> Icons.Outlined.Settings
        THEME        -> Icons.Outlined.Palette
        PASTE        -> Icons.Outlined.ContentPaste
        SEARCH       -> Icons.Outlined.Search
        TRANSLATE    -> Icons.Outlined.Translate
        STICKERS     -> Icons.Outlined.Image
        CLIPBOARD    -> Icons.Outlined.Assignment
        ONE_HANDED   -> Icons.Outlined.Smartphone
        CURSOR_LEFT  -> Icons.AutoMirrored.Outlined.ArrowBack
        CURSOR_RIGHT -> Icons.AutoMirrored.Outlined.ArrowForward
        BACKSPACE    -> Icons.Outlined.Backspace
    }

    /** Whether this item is fixed in the toolbar (not user-removable). */
    val isFixed: Boolean get() = this == VOICE || this == MENU

    /**
     * The [KeyAction] to dispatch when this toolbar item is tapped.
     *
     * Returns null for [MENU] because the menu button is handled specially by
     * the toolbar itself (it toggles LayoutKind.Menu, which is a layout
     * switch, not a one-shot action).
     */
    fun toAction(): KeyAction? = when (this) {
        VOICE        -> KeyAction.ToolbarAction(this)  // placeholder, handled but no-op
        MENU         -> null                            // handled by toolbar toggle
        EMOJI        -> KeyAction.SwitchLayout(LayoutKind.Emojis)
        SETTINGS     -> KeyAction.ToolbarAction(this)
        THEME        -> KeyAction.ToolbarAction(this)
        PASTE        -> KeyAction.ToolbarAction(this)
        SEARCH       -> KeyAction.ToolbarAction(this)
        TRANSLATE    -> KeyAction.ToolbarAction(this)
        STICKERS     -> KeyAction.ToolbarAction(this)
        CLIPBOARD    -> KeyAction.ToolbarAction(this)
        ONE_HANDED   -> KeyAction.ToolbarAction(this)
        CURSOR_LEFT  -> KeyAction.ToolbarAction(this)
        CURSOR_RIGHT -> KeyAction.ToolbarAction(this)
        BACKSPACE    -> KeyAction.Backspace
    }

    companion object {
        /** Default pinned set — shown to first-time users. */
        val DEFAULT_TOOLBAR: List<ToolbarItemKind> = listOf(
            EMOJI,
            SETTINGS,
            THEME,
        )

        /** All customizable items, in the order they appear in the menu panel. */
        val MENU_ITEMS: List<ToolbarItemKind> = listOf(
            EMOJI,
            PASTE,
            SEARCH,
            SETTINGS,
            THEME,
            TRANSLATE,
            STICKERS,
            CLIPBOARD,
            ONE_HANDED,
            CURSOR_LEFT,
            CURSOR_RIGHT,
            BACKSPACE,
        )
    }
}

/**
 * Parse a comma-separated list of [ToolbarItemKind] names.
 *
 * Unknown / invalid names are silently dropped so a corrupt prefs string
 * never crashes the IME. Returns [ToolbarItemKind.DEFAULT_TOOLBAR] if the
 * input is empty or all-invalid.
 */
fun parseToolbarItems(serialized: String?): List<ToolbarItemKind> {
    if (serialized.isNullOrBlank()) return ToolbarItemKind.DEFAULT_TOOLBAR
    val parsed = serialized.split(',')
        .mapNotNull { token ->
            runCatching { ToolbarItemKind.valueOf(token.trim()) }
                .getOrNull()
                ?.takeUnless { it.isFixed }  // never persist fixed items
        }
    return parsed.ifEmpty { ToolbarItemKind.DEFAULT_TOOLBAR }
}

/** Serialize a list of [ToolbarItemKind] for SharedPreferences storage. */
fun serializeToolbarItems(items: List<ToolbarItemKind>): String =
    items.filter { !it.isFixed }.joinToString(",") { it.name }
