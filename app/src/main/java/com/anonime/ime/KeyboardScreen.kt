package com.anonime.ime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * Render-time description of the IME action button.
 *
 * Derived from the EditorInfo's imeOptions in [AnonIMEService].
 * Phase 1 ships a fixed set; rarer actions fall back to [Default].
 */
sealed interface EnterActionSpec {
    val label: String?
    val icon: ImageVector?

    data object Default : EnterActionSpec {
        override val label: String? = null
        override val icon: ImageVector? = Icons.AutoMirrored.Filled.KeyboardReturn
    }

    data object Go : EnterActionSpec {
        override val label: String? = "Go"
        override val icon: ImageVector? = null
    }

    data object Search : EnterActionSpec {
        override val label: String? = null
        override val icon: ImageVector? = Icons.Outlined.Search
    }

    data object Send : EnterActionSpec {
        override val label: String? = "Send"
        override val icon: ImageVector? = null
    }

    data object Next : EnterActionSpec {
        override val label: String? = null
        override val icon: ImageVector? = Icons.AutoMirrored.Filled.KeyboardTab
    }

    data object Done : EnterActionSpec {
        override val label: String? = "Done"
        override val icon: ImageVector? = null
    }
}

@Immutable
data class KeyboardUiState(
    val layout: LayoutKind = LayoutKind.Letters,
    val shift: ShiftState = ShiftState.Off,
    val enter: EnterActionSpec = EnterActionSpec.Default,
    val enabled: Boolean = true, // false greys out the keyboard (e.g. incognito indicator)
)

/**
 * The keyboard surface — the only thing the IME renders into its input view.
 *
 * Stateless on purpose: all state lives in the parent (the IME service via
 * Compose state holders). The [onAction] callback fires whenever the user
 * taps a key.
 *
 * @param keyHeightDp       Per-row height in dp. Driven by the user's Appearance
 *                          setting (compact / normal / tall).
 * @param longPressAccents  When true, long-pressing a Latin letter key opens
 *                          an accent popup (é, ñ, ü, …) above that key. When
 *                          false, long-press does nothing.
 */
@Composable
fun KeyboardScreen(
    state: KeyboardUiState,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
    keyHeightDp: Int = 46,
    longPressAccents: Boolean = false,
) {
    // Active accent popup state. Null = no popup.
    // Holds the long-pressed [Key] and the on-screen bounds (px) of that key
    // so the popup can be positioned above it.
    var accentPopup by remember { mutableStateOf<AccentPopupState?>(null) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top),
        color = MaterialTheme.colorScheme.surface,
    ) {
        // The accent popup is a separate window (Compose Popup), so touches
        // on the keyboard are handled by Popup's dismissOnClickOutside when
        // the popup is showing — we don't need a scrim here.
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                val rows = keyboardLayoutFor(state.layout)
                for (row in rows) {
                    KeyRowView(
                        row = row,
                        state = state,
                        onAction = onAction,
                        keyHeightDp = keyHeightDp,
                        longPressAccents = longPressAccents,
                        onLongPressKey = { key, bounds ->
                            if (longPressAccents && AccentMap.hasAccents(key.label)) {
                                accentPopup = AccentPopupState(
                                    baseChar = key.label[0],
                                    bounds = bounds,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Accent popup overlay. Rendered on top of the key grid via Popup
            // so it floats above the keyboard without pushing layout.
            accentPopup?.let { popup ->
                AccentPopup(
                    state = popup,
                    shiftUppercase = state.shift.uppercase,
                    onAccent = { accent ->
                        onAction(KeyAction.InsertText(accent))
                        accentPopup = null
                    },
                    onDismiss = { accentPopup = null },
                )
            }
        }
    }
}

private data class AccentPopupState(
    val baseChar: Char,
    val bounds: IntRect, // px-relative-to-root bounds of the long-pressed key
)

@Composable
private fun AccentPopup(
    state: AccentPopupState,
    shiftUppercase: Boolean,
    onAccent: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val accents = AccentMap.accentsFor(state.baseChar) ?: emptyList()
    // Optionally prepend the base letter so the user can confirm-then-commit
    // (like Gboard). Keep it simple for now — just show accents.
    val colors = MaterialTheme.colorScheme

    Popup(
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                // Center the popup horizontally over the key, place it ABOVE the key.
                val centerX = state.bounds.left + state.bounds.width / 2
                val x = (centerX - popupContentSize.width / 2)
                    .coerceAtLeast(8)
                    .coerceAtMost((windowSize.width - popupContentSize.width - 8).coerceAtLeast(0))
                val y = (state.bounds.top - popupContentSize.height - 8).coerceAtLeast(4)
                return IntOffset(x, y)
            }
        },
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            color = colors.surfaceVariant,
            contentColor = colors.onSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 6.dp,
            tonalElevation = 3.dp,
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                accents.forEach { accent ->
                    val display = if (shiftUppercase) accent.uppercase() else accent
                    Surface(
                        color = colors.surface,
                        contentColor = colors.onSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .size(width = 38.dp, height = 44.dp)
                            .imeKeyClickable { onAccent(display) },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Text(
                                text = display,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRowView(
    row: KeyRow,
    state: KeyboardUiState,
    onAction: (KeyAction) -> Unit,
    keyHeightDp: Int,
    longPressAccents: Boolean,
    onLongPressKey: (key: Key, bounds: IntRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        for (key in row.keys) {
            KeyView(
                key = key,
                state = state,
                onAction = onAction,
                keyHeightDp = keyHeightDp,
                longPressAccents = longPressAccents,
                onLongPressKey = onLongPressKey,
                modifier = Modifier.weight(key.width),
            )
        }
    }
}

@Composable
private fun KeyView(
    key: Key,
    state: KeyboardUiState,
    onAction: (KeyAction) -> Unit,
    keyHeightDp: Int,
    longPressAccents: Boolean,
    onLongPressKey: (key: Key, bounds: IntRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isModifier = key.visual != KeyVisual.Text

    val bg = when {
        key.visual == KeyVisual.Space -> colors.surfaceVariant
        isModifier -> colors.secondaryContainer
        else -> colors.surfaceVariant
    }
    val fg = when {
        key.visual == KeyVisual.Space -> colors.onSurfaceVariant
        isModifier -> colors.onSecondaryContainer
        else -> colors.onSurfaceVariant
    }

    // Capture this key's on-screen bounds so we can position the accent popup.
    // onGloballyPositioned fires on layout changes; we cache the latest bounds.
    var keyBounds by remember { mutableStateOf(IntRect.Zero) }

    // Long-press only fires for letter keys with accents.
    val canLongPress = longPressAccents && AccentMap.hasAccents(key.label)

    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(keyHeightDp.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                keyBounds = IntRect(
                    left = pos.x.toInt(),
                    top = pos.y.toInt(),
                    right = (pos.x + size.width).toInt(),
                    bottom = (pos.y + size.height).toInt(),
                )
            }
            .imeKeyClickable(
                onLongPress = if (canLongPress) {
                    { onLongPressKey(key, keyBounds) }
                } else null,
            ) { key.action?.let(onAction) },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (key.visual) {
                KeyVisual.Text -> Text(
                    text = labelFor(key, state.shift),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = fg,
                )

                KeyVisual.Shift -> ShiftIcon(state.shift, fg)

                KeyVisual.Backspace -> Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Backspace",
                    tint = fg,
                )

                KeyVisual.Space -> Text(
                    text = "space",
                    fontSize = 14.sp,
                    color = fg.copy(alpha = 0.7f),
                )

                KeyVisual.Enter -> EnterContent(state.enter, fg)

                KeyVisual.Symbols -> Text(
                    text = key.label,
                    fontSize = 14.sp,
                    color = fg,
                )

                KeyVisual.Globe -> Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = "Emoji",
                    tint = fg,
                )
            }
        }
    }
}

@Composable
private fun ShiftIcon(shift: ShiftState, tint: Color) {
    when (shift) {
        ShiftState.Off -> Icon(
            imageVector = Icons.Outlined.KeyboardArrowUp,
            contentDescription = "Shift",
            tint = tint,
        )

        ShiftState.OnNext -> Icon(
            imageVector = Icons.Outlined.KeyboardArrowUp,
            contentDescription = "Shift (one uppercase)",
            tint = tint,
            modifier = Modifier.alpha(0.6f),
        )

        ShiftState.Locked -> Icon(
            imageVector = Icons.Filled.KeyboardCapslock,
            contentDescription = "Caps lock",
            tint = tint,
        )
    }
}

@Composable
private fun EnterContent(spec: EnterActionSpec, tint: Color) {
    val icon = spec.icon
    if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = spec.label ?: "Enter",
            tint = tint,
        )
    } else {
        Text(
            text = spec.label ?: "Enter",
            fontSize = 14.sp,
            color = tint,
        )
    }
}

// ── Layout-coordinate helper ────────────────────────────────────────────────
// No extension needed — LayoutCoordinates.positionInWindow() is provided by
// Compose and returns an Offset, which we convert to IntRect above. The IME
// window == root window, so window-relative coordinates are correct for
// popup positioning.
