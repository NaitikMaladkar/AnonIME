package com.anonime.ime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntRect

/**
 * Hoisted drag state shared between [Toolbar] and [MenuPanel].
 *
 * ── Why a separate holder? ───────────────────────────────────────────────────
 * Compose's `pointerInput` modifier captures pointer events inside a single
 * composable subtree. To detect "drop on the toolbar" while the user is
 * dragging a chip in the menu panel (a SIBLING subtree), we need to share
 * drag state across both. This holder is created once at the [KeyboardScreen]
 * level and passed down to both children.
 *
 * ── Drag lifecycle ───────────────────────────────────────────────────────────
 *   1. MenuPanel chip receives a long-press + drag start → sets
 *      [draggingItem] and starts updating [dragPosition] via drag deltas.
 *   2. Toolbar reports its on-screen bounds via [toolbarBounds] (set in
 *      onGloballyPositioned).
 *   3. While [draggingItem] != null, Toolbar renders as a highlighted drop
 *      zone. [isOverToolbar] tells us whether the current [dragPosition]
 *      is inside the toolbar bounds.
 *   4. On drag end, the menu panel checks [isOverToolbar]: if true, it
 *      dispatches [KeyAction.AddToolbarItem] and clears state.
 *   5. On drag cancel (finger lifted outside the drop zone), state is
 *      cleared with no action.
 *
 * ── Coordinate system ────────────────────────────────────────────────────────
 * All coordinates are window-relative (the IME window == root window). Both
 * [dragPosition] and [toolbarBounds] are in the same coordinate space, so
 * [contains] comparisons are correct.
 *
 * ── Stability ────────────────────────────────────────────────────────────────
 * [Stable] because all public properties are backed by [mutableStateOf],
 * which Compose's stability inference understands. This prevents unnecessary
 * recompositions when the holder is passed as a parameter.
 */
@Stable
class ToolbarDragState {
    /** The chip currently being dragged, or null when no drag is active. */
    var draggingItem: ToolbarItemKind? by mutableStateOf(null)
        internal set

    /** Window-relative position of the drag pointer. */
    var dragPosition: Offset by mutableStateOf(Offset.Zero)
        internal set

    /** Window-relative bounds of the toolbar (drop zone). */
    var toolbarBounds: IntRect by mutableStateOf(IntRect.Zero)
        internal set

    /** True iff [dragPosition] is currently inside [toolbarBounds]. */
    val isOverToolbar: Boolean
        get() = draggingItem != null && toolbarBounds.contains(
            androidx.compose.ui.unit.IntOffset(
                dragPosition.x.toInt(),
                dragPosition.y.toInt(),
            )
        )

    /** Begin a drag for [kind] at the given window-relative [startPosition]. */
    fun startDrag(kind: ToolbarItemKind, startPosition: Offset) {
        draggingItem = kind
        dragPosition = startPosition
    }

    /** Update the drag position (called from `onDrag` callback). */
    fun updateDrag(delta: Offset) {
        dragPosition += delta
    }

    /**
     * End the drag. Returns the dragged [ToolbarItemKind] if the drop target
     * is the toolbar (caller should dispatch [KeyAction.AddToolbarItem]),
     * otherwise null.
     */
    fun endDrag(): ToolbarItemKind? {
        val item = draggingItem
        val over = isOverToolbar
        draggingItem = null
        dragPosition = Offset.Zero
        return if (over) item else null
    }

    /** Cancel the drag with no drop. */
    fun cancelDrag() {
        draggingItem = null
        dragPosition = Offset.Zero
    }
}

/** Remember a new [ToolbarDragState] scoped to the current composition. */
@Composable
fun rememberToolbarDragState(): ToolbarDragState = remember { ToolbarDragState() }
