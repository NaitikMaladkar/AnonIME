package com.anonime.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anonime.data.KeyHeight
import com.anonime.data.SettingsRepository
import com.anonime.data.ThemeMode
import com.anonime.ui.components.SettingOptionCard
import com.anonime.ui.components.SettingActionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { SettingsRepository.get(ctx) }
    val state by repo.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", fontWeight = FontWeight.SemiBold) },
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
            // ── Theme ──────────────────────────────────────────────────────────
            SectionLabel("Theme")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEach { mode ->
                    PickerChip(
                        label = mode.label(),
                        selected = mode == state.themeMode,
                        onClick = { repo.setThemeMode(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ── Color ──────────────────────────────────────────────────────────
            SectionLabel("Color")
            val supportsDynamic = Build.VERSION.SDK_INT >= 31
            SettingOptionCard(
                icon = Icons.Outlined.DynamicFeed,
                title = "Dynamic color",
                description = if (supportsDynamic)
                    "Match colors to your wallpaper (Android 12+)."
                else
                    "Requires Android 12 or newer. Unavailable on this device.",
                checked = state.dynamicColor,
                onCheckedChange = { repo.setDynamicColor(it) },
                enabled = supportsDynamic,
                comingSoon = !supportsDynamic,
            )

            // ── Layout ─────────────────────────────────────────────────────────
            SectionLabel("Layout")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                KeyHeight.entries.forEach { h ->
                    PickerChip(
                        label = h.label(),
                        selected = h == state.keyHeight,
                        onClick = { repo.setKeyHeight(h) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SettingOptionCard(
                icon = Icons.Outlined.RoundedCorner,
                title = "Rounded key shape",
                description = "Soften the corners of each key.",
                checked = true,
                onCheckedChange = {},
                comingSoon = true,
            )

            // ── Preview hint ────────────────────────────────────────────────────
            SectionLabel("Preview")
            Text(
                text = "Open any text field to see your changes live. The keyboard reads these settings every time it opens.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun PickerChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = bg),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 8.dp)
                .fillMaxWidth(),
        )
    }
}

private fun ThemeMode.label() = when (this) {
    ThemeMode.SYSTEM -> "System"
    ThemeMode.LIGHT -> "Light"
    ThemeMode.DARK -> "Dark"
}

private fun KeyHeight.label() = when (this) {
    KeyHeight.COMPACT -> "Compact"
    KeyHeight.NORMAL -> "Normal"
    KeyHeight.TALL -> "Tall"
}
