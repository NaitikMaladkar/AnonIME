package com.anonime.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The menu panel — replaces the keyboard layout when the user taps the Menu
 * icon on the toolbar.
 *
 * Shows a grid of "small buttons" (chips) for every [ToolbarItemKind] in
 * [ToolbarItemKind.MENU_ITEMS]. Each chip can be:
 *   - Tapped → dispatches its action via [ToolbarItemKind.toAction].
 *   - Long-pressed + dragged → up to the toolbar drop zone to pin.
 *
 * ── Drag-and-drop ────────────────────────────────────────────────────────────
 * Chip dragging uses [detectDragGesturesAfterLongPress] so a regular tap
 * still fires the action (taps don't trigger the long-press detector).
 *
 * On drag start, we record the chip's [ToolbarItemKind] in the shared
 * [ToolbarDragState] and the toolbar highlights itself as a drop zone.
 * On drag end, we check [ToolbarDragState.endDrag] — if the drop was over
 * the toolbar, we dispatch [KeyAction.AddToolbarItem].
 *
 * While dragging, the chip scales up and follows the finger via a graphicsLayer
 * translation. This gives the user visual feedback that the chip is "picked up".
 *
 * ── Pinned indicator ─────────────────────────────────────────────────────────
 * Chips that are already in the user's pinned toolbar show a small dot in
 * the corner. Tapping them still fires the action — the dot is informational
 * only. To unpin, the user long-presses the toolbar item itself (not the chip).
 *
 * @param activeItems   The user's currently pinned toolbar items. Used to
 *                      show the "pinned" indicator on matching chips.
 * @param dragState     Shared drag state — same instance as the Toolbar's.
 * @param onAction      Callback for any [KeyAction] dispatched by the panel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MenuPanel(
    activeItems: List<ToolbarItemKind>,
    dragState: ToolbarDragState,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        color = colors.surface,
        contentColor = colors.onSurface,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(align = Alignment.Top),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Toolbar shortcuts",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            Text(
                text = "Tap to use  •  Hold and drag up to pin",
                fontSize = 10.sp,
                color = colors.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = (-2).dp),
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ToolbarItemKind.MENU_ITEMS.forEach { kind ->
                    MenuChip(
                        kind = kind,
                        isPinned = activeItems.contains(kind),
                        isDragging = dragState.draggingItem == kind,
                        dragState = dragState,
                        onTap = {
                            kind.toAction()?.let(onAction)
                        },
                        onDrop = { droppedKind ->
                            if (droppedKind != null) {
                                onAction(KeyAction.AddToolbarItem(droppedKind))
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * A single chip in the menu panel.
 *
 * Visual states:
 *   - Default: rounded square with icon + label.
 *   - Pinned:  small primary-colored dot in the top-right corner.
 *   - Dragging: scales up to 1.15× and follows the pointer via graphicsLayer.
 *
 * Interaction:
 *   - Tap → [onTap] (fires the chip's action).
 *   - Long-press + drag → starts drag in [dragState]. On end, [onDrop] is
 *     called with the kind if dropped over the toolbar, otherwise null.
 */
@Composable
private fun MenuChip(
    kind: ToolbarItemKind,
    isPinned: Boolean,
    isDragging: Boolean,
    dragState: ToolbarDragState,
    onTap: () -> Unit,
    onDrop: (ToolbarItemKind?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    // The chip's window-relative bounds — needed so we can compute the
    // pointer's starting position when a drag begins.
    var chipBounds by remember { mutableStateOf(IntRect.Zero) }

    // Track whether this chip is currently being dragged (separate from
    // isDragging param so we don't fight with parent-driven recompositions).
    var localDragging by remember { mutableStateOf(false) }

    val scale = if (localDragging) 1.15f else 1f
    val alpha = if (localDragging) 0.4f else 1f

    Surface(
        color = colors.surfaceVariant.copy(alpha = if (localDragging) 0.5f else 1f),
        contentColor = colors.onSurfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .size(width = 64.dp, height = 64.dp)
            .onGloballyPositioned { coords ->
                val pos = coords.positionInWindow()
                val size = coords.size
                chipBounds = IntRect(
                    left = pos.x.toInt(),
                    top = pos.y.toInt(),
                    right = (pos.x + size.width).toInt(),
                    bottom = (pos.y + size.height).toInt(),
                )
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            // Tap detection (handled by imeKeyClickable) + drag detection
            // (handled by pointerInput below). These don't conflict because
            // detectDragGesturesAfterLongPress only fires after a long-press,
            // while imeKeyClickable fires on quick tap.
            .imeKeyClickable(onClick = onTap)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // Translate the local offset to window coords by
                        // adding the chip's window position.
                        val startPos = Offset(
                            x = chipBounds.left.toFloat() + offset.x,
                            y = chipBounds.top.toFloat() + offset.y,
                        )
                        dragState.startDrag(kind, startPos)
                        localDragging = true
                    },
                    onDrag = { change, delta ->
                        change.consume()
                        dragState.updateDrag(delta)
                    },
                    onDragEnd = {
                        localDragging = false
                        val dropped = dragState.endDrag()
                        onDrop(dropped)
                    },
                    onDragCancel = {
                        localDragging = false
                        dragState.cancelDrag()
                    },
                )
            },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = kind.icon,
                    contentDescription = kind.displayLabel,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = kind.displayLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // Pinned indicator — small dot in the top-right corner.
            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(6.dp)
                        .background(
                            color = colors.primary,
                            shape = RoundedCornerShape(50),
                        ),
                )
            }
        }
    }
}
