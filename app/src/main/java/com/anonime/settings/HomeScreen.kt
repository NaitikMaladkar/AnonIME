package com.anonime.settings

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import com.anonime.ui.components.CategoryCard
import com.anonime.ui.components.StatusChip

/**
 * The home screen of the settings app.
 *
 * Layout: top app bar with the app name + a status chip (green when both
 * the IME is enabled AND set as default, otherwise red). Below the bar,
 * a vertical list of 5 category cards.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCategoryClick: (SettingsCategory) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val ctx = LocalContext.current
    val imm = remember { ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    var enabled by remember { mutableStateOf(false) }
    var isDefault by remember { mutableStateOf(false) }
    LifecycleStartEffect(Unit) {
        enabled = isImeEnabled(imm)
        isDefault = isImeDefault(ctx)
        onStopOrDispose { }
    }

    val allOk = enabled && isDefault

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AnonIME",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    StatusChip(
                        ok = allOk,
                        labelOk = "Enabled",
                        labelPending = "Disabled",
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        content = { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SettingsCategory.entries.forEach { category ->
                    CategoryCard(
                        icon = category.icon,
                        title = category.title,
                        description = category.description,
                        accentColor = category.accentColor,
                        onClick = { onCategoryClick(category) },
                    )
                }
            }
        }
    )
}

/** The set of categories shown on the home screen. Order = display order. */
enum class SettingsCategory(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color,
) {
    GENERAL(
        title = "General",
        description = "Enable, set default, open system keyboard settings",
        icon = Icons.Outlined.Settings,
        accentColor = Color(0xFFE25588),
    ),
    APPEARANCE(
        title = "Appearance",
        description = "Theme, key height, dynamic color",
        icon = Icons.Outlined.Palette,
        accentColor = Color(0xFF8E7CC3),
    ),
    TYPING(
        title = "Typing",
        description = "Pop-ups, accents, sound, haptics, auto-capitalize",
        icon = Icons.Outlined.Tune,
        accentColor = Color(0xFF4CB6A4),
    ),
    PRIVACY(
        title = "Privacy",
        description = "What we don't collect — and why",
        icon = Icons.Outlined.Lock,
        accentColor = Color(0xFFE5A100),
    ),
    ABOUT(
        title = "About",
        description = "Version, source code, license",
        icon = Icons.Outlined.Info,
        accentColor = Color(0xFF6FA8DC),
    ),
    ;
}

// (Helpers isImeEnabled, isImeDefault, startActivitySafe, IME_ID are
// declared as internal in GeneralScreen.kt — shared across this package.)
