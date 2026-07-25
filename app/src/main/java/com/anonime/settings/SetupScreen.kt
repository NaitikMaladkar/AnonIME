package com.anonime.settings

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.anonime.data.SettingsRepository
import com.anonime.ui.components.StatusChip

/**
 * The first-launch setup screen — a vertical list of step cards.
 *
 * Each card shows a live status chip that turns green when the corresponding
 * system setting is on. The "Continue" button is disabled until both steps
 * are green. Tapping it sets `setupCompleted = true` in [SettingsRepository]
 * and navigates the user to the Home screen.
 *
 * Refreshes status on every onStart (i.e. when the user returns from the
 * system Settings screen they were sent to).
 */
@Composable
fun SetupScreen(onCompleted: () -> Unit) {
    val ctx = LocalContext.current
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }
    val repo = remember { SettingsRepository.get(ctx) }

    var enabled by remember { mutableStateOf(isImeEnabled(imm)) }
    var isDefault by remember { mutableStateOf(isImeDefault(ctx)) }

    LifecycleStartEffect(Unit) {
        enabled = isImeEnabled(imm)
        isDefault = isImeDefault(ctx)
        onStopOrDispose { }
    }

    BackHandler(enabled = true) {
        // Block back-navigation away from setup until both steps are complete —
        // we don't want the user stranded with no keyboard.
    }

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SetupHeader()

            StepCard(
                icon = Icons.Outlined.Keyboard,
                title = "Enable AnonIME",
                description = "Open Android's keyboard settings and toggle AnonIME on. You'll see a green checkmark here when it's enabled.",
                buttonLabel = "Open keyboard settings",
                done = enabled,
                onAction = { ctx.startActivitySafe(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS) },
            )

            StepCard(
                icon = Icons.Outlined.Settings,
                title = "Set AnonIME as default",
                description = "Pick AnonIME from the list of available keyboards so the system uses it whenever you tap a text field.",
                buttonLabel = "Choose default keyboard",
                done = isDefault,
                onAction = { imm.showInputMethodPicker() },
            )

            PrivacyMiniCard()

            Box(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    repo.setSetupCompleted(true)
                    onCompleted()
                },
                enabled = enabled && isDefault,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = "Continue to settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            if (!enabled || !isDefault) {
                Text(
                    text = "Both steps must be completed before you can continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun SetupHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Welcome to AnonIME",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "A privacy-first keyboard that learns nothing. Let's get you typing in two quick steps.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    done: Boolean,
    onAction: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
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
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (done) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(
                    ok = done,
                    labelOk = "Done",
                    labelPending = "Pending",
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
                enabled = !done,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(buttonLabel)
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
