package com.anonime.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LockReset
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.anonime.ui.components.SettingActionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    var enabled by remember { mutableStateOf(false) }
    var isDefault by remember { mutableStateOf(false) }

    // The "Enable" and "Set as default" cards both open the guided setup
    // sheet instead of deep-linking straight to system settings. The sheet
    // walks the user through both steps in order, which is friendlier than
    // dropping them into a system Settings page they might not recognize.
    var showGuide by remember { mutableStateOf(false) }

    LifecycleStartEffect(Unit) {
        enabled = isImeEnabled(imm)
        isDefault = isImeDefault(ctx)
        onStopOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("General", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { BackArrow(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingActionCard(
                icon = if (enabled) Icons.Outlined.CheckCircle else Icons.Outlined.Keyboard,
                title = if (enabled) "AnonIME is enabled" else "Enable AnonIME",
                description = if (enabled)
                    "Already toggled on in system settings. Tap to re-open the guided setup."
                else
                    "Open the guided setup to walk through enabling AnonIME in system settings.",
                onClick = { showGuide = true },
            )

            SettingActionCard(
                icon = if (isDefault) Icons.Outlined.CheckCircle else Icons.Outlined.Apps,
                title = if (isDefault) "AnonIME is your default keyboard" else "Set AnonIME as default",
                description = if (isDefault)
                    "Currently the active keyboard. Tap to re-open the guided setup."
                else
                    "Open the guided setup to pick AnonIME as your default keyboard.",
                onClick = { showGuide = true },
            )

            SettingActionCard(
                icon = Icons.Outlined.Settings,
                title = "Open system keyboard settings",
                description = "Jump directly to Android's keyboard & input method settings page.",
                onClick = { ctx.startActivitySafe(Settings.ACTION_INPUT_METHOD_SETTINGS) },
            )

            SettingActionCard(
                icon = Icons.Outlined.Info,
                title = "AnonIME version",
                description = "Phase 2 (globe key, emoji panel, long-press accents, live settings) — version 0.3.0-phase2 (build 3).",
                onClick = { /* no-op informational */ },
                enabled = false,
            )

            SettingActionCard(
                icon = Icons.Outlined.LockReset,
                title = "Reset to defaults",
                description = "Reset all AnonIME settings to their default values. Doesn't touch your system keyboard settings.",
                onClick = { /* TODO Phase 3: reset SettingsRepository */ },
                enabled = false,
            )
        }
    }

    if (showGuide) {
        GuidedSetupSheet(onDismiss = { showGuide = false })
    }
}

@Composable
internal fun BackArrow(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── Helpers (shared across all settings screens) ──────────────────────────────
internal const val IME_ID = "com.anonime/.ime.AnonIMEService"

internal fun isImeEnabled(imm: InputMethodManager): Boolean =
    imm.enabledInputMethodList.any { it.id == IME_ID }

internal fun isImeDefault(ctx: Context): Boolean =
    Settings.Secure.getString(
        ctx.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD,
    ) == IME_ID

internal fun Context.startActivitySafe(action: String) {
    runCatching {
        startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

internal fun Context.startIntentSafe(intent: Intent) {
    runCatching {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
