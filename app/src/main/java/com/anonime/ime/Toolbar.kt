package com.anonime.ime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The toolbar above the keyboard.
 *
 * Layout: [Voice] [user items…] [Menu]
 *
 * ── Fixed slots ──────────────────────────────────────────────────────────────
 * - Voice (leftmost) — placeholder mic icon, taps dispatch [ToolbarAction.VOICE]
 *   (currently a no-op). Always present, never removable.
 * - Menu (rightmost) — four-block grid icon. Tapping toggles the menu panel
 *   (LayoutKind.Menu). Always present, never removable.
 *
 * ── User items ──────────────────────────────────────────────────────────────
 * The middle slot shows the user's pinned items (from [ToolbarItemKind]
 * persisted in [SettingsState.toolbarItems]). Tapping dispatches the item's
 * action via [ToolbarItemKind.toAction]. Long-pressing an item shows a small
 * "remove" (×) badge; tapping the badge dispatches [KeyAction.RemoveToolbarItem].
 *
 * ── Drop zone ────────────────────────────────────────────────────────────────
 * While a chip is being dragged from the menu panel, the toolbar highlights
 * itself as a drop target via [ToolbarDragState.isOverToolbar]. The drag
 * state is hoisted at the [KeyboardScreen] level and shared between this
 * composable and [MenuPanel].
 *
 * @param items           The user's pinned items (NOT including Voice/Menu).
 * @param dragState       Shared drag state for drop detection.
 * @param isMenuOpen      True if the menu panel is currently shown. Used to
 *                        highlight the menu icon when active.
 * @param onAction        Callback for any [KeyAction] dispatched by the toolbar.
 */
@Composable
fun Toolbar(
    items: List<ToolbarItemKind>,
    dragState: ToolbarDragState,
    isMenuOpen: Boolean,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    // Track which toolbar item (if any) is showing its remove badge.
    var removeBadgeFor by remember { mutableStateOf<ToolbarItemKind?>(null) }

    Surface(
        color = colors.surface,
        contentColor = colors.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            // Register the toolbar's window-relative bounds so the drag state
            // can detect when the pointer enters the drop zone.
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                dragState.toolbarBounds = IntRect(
                    left = pos.x.toInt(),
                    top = pos.y.toInt(),
                    right = (pos.x + size.width).toInt(),
                    bottom = (pos.y + size.height).toInt(),
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // ── Left fixed slot: Voice ────────────────────────────────────
                ToolbarIconButton(
                    kind = ToolbarItemKind.VOICE,
                    onClick = {
                        ToolbarItemKind.VOICE.toAction()?.let(onAction)
                    },
                    tint = colors.onSurfaceVariant,
                )

                // ── User-customizable middle items ────────────────────────────
                items.forEach { kind ->
                    ToolbarItemButton(
                        kind = kind,
                        showRemoveBadge = removeBadgeFor == kind,
                        onClick = {
                            // Tap the badge → remove. Tap the icon → action.
                            if (removeBadgeFor == kind) {
                                onAction(KeyAction.RemoveToolbarItem(kind))
                                removeBadgeFor = null
                            } else {
                                kind.toAction()?.let(onAction)
                                removeBadgeFor = null
                            }
                        },
                        onLongClick = {
                            // Toggle the remove badge for this item.
                            removeBadgeFor =
                                if (removeBadgeFor == kind) null else kind
                        },
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }

                // ── Spacer to push the Menu icon to the right ────────────────
                Box(modifier = Modifier.weight(1f))

                // ── Right fixed slot: Menu ───────────────────────────────────
                ToolbarIconButton(
                    kind = ToolbarItemKind.MENU,
                    onClick = {
                        // Toggle the menu layout. If we're already on Menu,
                        // go back to Letters.
                        if (isMenuOpen) {
                            onAction(KeyAction.SwitchLayout(LayoutKind.Letters))
                        } else {
                            onAction(KeyAction.SwitchLayout(LayoutKind.Menu))
                        }
                    },
                    tint = if (isMenuOpen) colors.primary else colors.onSurfaceVariant,
                    highlight = isMenuOpen,
                )
            }

            // ── Drop zone overlay (only visible while dragging) ───────────────
            if (dragState.draggingItem != null) {
                DropZoneOverlay(
                    active = dragState.isOverToolbar,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * A square icon button used for the fixed Voice / Menu slots.
 *
 * No background fill by default; highlights with [highlight] = true (used
 * for the Menu button when the menu panel is open).
 */
@Composable
private fun ToolbarIconButton(
    kind: ToolbarItemKind,
    onClick: () -> Unit,
    tint: Color,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = if (highlight) colors.secondaryContainer else Color.Transparent,
        contentColor = tint,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.size(36.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .imeKeyClickable(onClick = onClick),
        ) {
            Icon(
                imageVector = kind.icon,
                contentDescription = kind.displayLabel,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * A user-customizable toolbar item with a tap action and a long-press to
 * reveal a remove badge.
 *
 * Long-press toggles a small red × badge in the corner. Tapping the badge
 * dispatches [KeyAction.RemoveToolbarItem]; tapping elsewhere dispatches
 * the item's normal action.
 */
@Composable
private fun ToolbarItemButton(
    kind: ToolbarItemKind,
    showRemoveBadge: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(36.dp)
            .imeKeyClickable(
                onLongPress = onLongClick,
                onClick = onClick,
            ),
    ) {
        Icon(
            imageVector = kind.icon,
            contentDescription = kind.displayLabel,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )

        // Remove badge — animated in/out so it doesn't pop.
        AnimatedVisibility(
            visible = showRemoveBadge,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Surface(
                color = colors.error,
                contentColor = colors.onError,
                shape = CircleShape,
                modifier = Modifier.size(16.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove ${kind.displayLabel} from toolbar",
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
    }
}

/**
 * Translucent overlay shown over the toolbar while the user is dragging a
 * menu chip. Turns solid green when the pointer is over the toolbar.
 */
@Composable
private fun DropZoneOverlay(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val targetColor = if (active) colors.primary.copy(alpha = 0.35f)
    else colors.outline.copy(alpha = 0.18f)
    val targetBorder = if (active) colors.primary
    else colors.outline.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .background(color = targetColor, shape = RoundedCornerShape(8.dp))
            .border(
                width = if (active) 2.dp else 1.dp,
                color = targetBorder,
                shape = RoundedCornerShape(8.dp),
            ),
    ) {
        if (active) {
            Text(
                text = "Drop to pin",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onPrimary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}
