package com.anonime.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anonime.data.SettingsRepository
import com.anonime.ui.components.SettingOptionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypingScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SettingsRepository.get(ctx) }
    val state by repo.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Typing", fontWeight = FontWeight.SemiBold) },
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
            // Long-press accents is now functional (Phase 2). The other four
            // toggles below still persist via SettingsRepository but the
            // keyboard doesn't yet honor them — each shows a "Coming soon"
            // chip until the matching behavior is implemented.
            SettingOptionCard(
                icon = Icons.Outlined.Visibility,
                title = "Key pop-up preview",
                description = "Show a magnified preview of the key you just tapped.",
                checked = state.keyPopupPreview,
                onCheckedChange = { repo.setKeyPopupPreview(it) },
                comingSoon = true,
                enabled = false,
            )

            SettingOptionCard(
                icon = Icons.Outlined.Abc,
                title = "Long-press accents",
                description = "Long-press a letter to access accents like é, ñ, ü.",
                checked = state.longPressAccents,
                onCheckedChange = { repo.setLongPressAccents(it) },
                comingSoon = false,
                enabled = true,
            )

            SettingOptionCard(
                icon = Icons.Outlined.Campaign,
                title = "Auto-capitalize",
                description = "Capitalize the first letter of each sentence.",
                checked = state.autoCapitalize,
                onCheckedChange = { repo.setAutoCapitalize(it) },
                comingSoon = true,
                enabled = false,
            )

            SettingOptionCard(
                icon = Icons.Outlined.KeyboardVoice,
                title = "Sound on keypress",
                description = "Play a soft tick when you tap a key. Off by default — AnonIME is silent.",
                checked = state.soundEnabled,
                onCheckedChange = { repo.setSoundEnabled(it) },
                comingSoon = true,
                enabled = false,
            )

            SettingOptionCard(
                icon = Icons.Outlined.Vibration,
                title = "Haptic on keypress",
                description = "Vibrate briefly when you tap a key. Off by default — AnonIME is silent.",
                checked = state.hapticEnabled,
                onCheckedChange = { repo.setHapticEnabled(it) },
                comingSoon = true,
                enabled = false,
            )
        }
    }
}
