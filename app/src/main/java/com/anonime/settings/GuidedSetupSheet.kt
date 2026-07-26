package com.anonime.settings

import android.content.Context
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import com.anonime.ui.components.StatusChip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/**
 * A floating bottom-sheet guide that walks the user through the two system
 * settings needed to enable AnonIME:
 *
 *   Step 1: Turn AnonIME on in system keyboard settings.
 *   Step 2: Set AnonIME as the default input method.
 *
 * The sheet stays open across the round-trips to system Settings: when the
 * user taps a Continue button we launch the right system Intent, and when
 * they come back (via back button or task switcher), [LifecycleStartEffect]
 * refreshes the status chips live. When both steps are Done, the sheet
 * auto-dismisses itself and the caller's home screen shows "Enabled".
 *
 * The sheet is invoked from two places:
 *   - HomeScreen: tapping the "Disabled" status chip in the top app bar.
 *   - GeneralScreen: tapping the "Enable AnonIME" or "Set as default" cards.
 *
 * @param onDismiss Called when the sheet closes (user gesture, back button,
 *                  or auto-dismiss when both steps are complete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedSetupSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    // Live status of the two system settings. Refreshed on every onStart
    // (i.e. when the user returns from a system Settings page).
    var enabled by remember { mutableStateOf(isImeEnabled(imm)) }
    var isDefault by remember { mutableStateOf(isImeDefault(ctx)) }

    // The bottom-sheet state. We use skipPartiallyExpanded so the sheet
    // opens at full expanded height by default — both step cards are
    // visible without the user needing to drag.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Refresh status whenever the host lifecycle transitions to STARTED.
    // This fires when the sheet first opens AND every time the user returns
    // to the app from system Settings (because returning brings the host
    // activity back to STARTED).
    LifecycleStartEffect(Unit) {
        enabled = isImeEnabled(imm)
        isDefault = isImeDefault(ctx)
        onStopOrDispose { }
    }

    // Auto-dismiss when both steps TRANSITION from not-done to all-done.
    //
    // We deliberately do NOT auto-dismiss if the sheet was opened when both
    // steps were already done (e.g. the user re-opened the guide from
    // GeneralScreen after completing setup). Without this guard, the sheet
    // would close instantly on open — the user wouldn't even see the green
    // "Done" chips. The snapshotFlow filter-+dropWhile pattern below ensures
    // the dismiss only fires when the user actually completes the last step
    // while the sheet is open.
    LaunchedEffect(Unit) {
        snapshotFlow { enabled && isDefault }
            .drop(1) // skip the initial emission — don't dismiss on open.
            .filter { it }
            .collectLatest {
                // Give the user a moment to see the "all green" state, then
                // animate the sheet away.
                sheetState.hide()
                onDismiss()
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header — short, friendly, explains what the sheet is for.
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Set up AnonIME",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Two quick steps to enable your privacy-first keyboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Step 1: Enable AnonIME in system keyboard settings.
            GuideStepCard(
                stepNumber = 1,
                icon = Icons.Outlined.Keyboard,
                title = "Enable AnonIME",
                description = "Open Android's keyboard settings and toggle AnonIME on. " +
                    "You'll see a green checkmark here when it's enabled.",
                buttonLabel = if (enabled) "Re-open keyboard settings" else "Open keyboard settings",
                done = enabled,
                onAction = { ctx.startActivitySafe(Settings.ACTION_INPUT_METHOD_SETTINGS) },
            )

            // Step 2: Set AnonIME as the default input method.
            GuideStepCard(
                stepNumber = 2,
                icon = Icons.Outlined.Apps,
                title = "Set AnonIME as default",
                description = "Pick AnonIME from the list of available keyboards so the " +
                    "system uses it whenever you tap a text field.",
                buttonLabel = if (isDefault) "Switch default keyboard" else "Choose default keyboard",
                done = isDefault,
                // Step 2 is only actionable after step 1 is done — Android's
                // default-keyboard picker only shows enabled IMEs.
                enabled = enabled,
                onAction = { imm.showInputMethodPicker() },
            )

            // Privacy mini-card — reassuring footer.
            PrivacyMiniCard()

            // Final hint — what to do when both steps are green.
            Text(
                text = if (enabled && isDefault)
                    "All set! This guide will close automatically."
                else
                    "When both steps are done, this guide closes automatically and the home screen shows \u201CEnabled.\u201D",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun GuideStepCard(
    stepNumber: Int,
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    done: Boolean,
    onAction: () -> Unit,
    enabled: Boolean = true,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.secondaryContainer
            else if (enabled) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (done) Icons.Outlined.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (done) MaterialTheme.colorScheme.onSecondaryContainer
                    else if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (done) MaterialTheme.colorScheme.onSecondaryContainer
                    else if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(
                    ok = done,
                    labelOk = "Done",
                    labelPending = "Step $stepNumber",
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onAction,
                enabled = enabled,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PrivacyMiniCard() {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Anonymous by design",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "No INTERNET permission, no telemetry, no input history. Nothing leaves your device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}
