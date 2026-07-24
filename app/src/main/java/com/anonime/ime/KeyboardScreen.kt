package com.anonime.ime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.KeyboardTab
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        override val icon: ImageVector? = Icons.Outlined.Send
    }

    data object Next : EnterActionSpec {
        override val label: String? = null
        override val icon: ImageVector? = Icons.Filled.KeyboardTab
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
 */
@Composable
fun KeyboardScreen(
    state: KeyboardUiState,
    onAction: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
    ) {
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun KeyRowView(
    row: KeyRow,
    state: KeyboardUiState,
    onAction: (KeyAction) -> Unit,
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

    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(46.dp)
            .imeKeyClickable { key.action?.let(onAction) },
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
    if (spec.icon != null) {
        Icon(
            imageVector = spec.icon,
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
