package com.anonime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.anonime.data.SettingsRepository
import com.anonime.data.ThemeMode
import com.anonime.settings.AboutScreen
import com.anonime.settings.AppearanceScreen
import com.anonime.settings.GeneralScreen
import com.anonime.settings.HomeScreen
import com.anonime.settings.PrivacyScreen
import com.anonime.settings.SettingsCategory
import com.anonime.settings.SetupScreen
import com.anonime.settings.TypingScreen
import com.anonime.ui.theme.AnonIMETheme
import androidx.compose.foundation.isSystemInDarkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repo = remember { SettingsRepository.get(this) }
            val settings by repo.state.collectAsState()

            // Resolve the user's theme preference into a concrete dark/light value
            // for the Compose tree. The IME service does the same resolution on
            // its side so both the settings UI and the keyboard use the same theme.
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AnonIMETheme(darkTheme = darkTheme, dynamic = settings.dynamicColor) {
                val nav = rememberNavController()
                val startRoute = if (settings.setupCompleted) Routes.HOME else Routes.SETUP

                NavHost(navController = nav, startDestination = startRoute) {
                    composable(Routes.SETUP) {
                        SetupScreen(
                            onCompleted = {
                                nav.navigate(Routes.HOME) {
                                    popUpTo(Routes.SETUP) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable(Routes.HOME) {
                        HomeScreen(
                            onCategoryClick = { category ->
                                nav.navigate(category.route)
                            },
                        )
                    }
                    composable(Routes.GENERAL) { GeneralScreen(onBack = { nav.popBackStack() }) }
                    composable(Routes.APPEARANCE) { AppearanceScreen(onBack = { nav.popBackStack() }) }
                    composable(Routes.TYPING) { TypingScreen(onBack = { nav.popBackStack() }) }
                    composable(Routes.PRIVACY) { PrivacyScreen(onBack = { nav.popBackStack() }) }
                    composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
                }
            }
        }
    }
}

private object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val GENERAL = "general"
    const val APPEARANCE = "appearance"
    const val TYPING = "typing"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
}

private val SettingsCategory.route: String
    get() = when (this) {
        SettingsCategory.GENERAL -> Routes.GENERAL
        SettingsCategory.APPEARANCE -> Routes.APPEARANCE
        SettingsCategory.TYPING -> Routes.TYPING
        SettingsCategory.PRIVACY -> Routes.PRIVACY
        SettingsCategory.ABOUT -> Routes.ABOUT
    }
