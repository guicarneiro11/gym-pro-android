package com.guicarneirodev.gympro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.guicarneirodev.gympro.data.local.preferences.UserPreferences
import com.guicarneirodev.gympro.data.local.preferences.UserPreferencesManager
import com.guicarneirodev.gympro.presentation.navigation.GymProNavHost
import com.guicarneirodev.gympro.presentation.theme.GymProTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    private val userPreferencesManager: UserPreferencesManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            userPreferencesManager.userPreferences.collect { prefs ->
                if (prefs.language != "system" && prefs.language.isNotEmpty()) {

                }
            }
        }

        enableEdgeToEdge()
        setContent {
            GymProApp()
        }
    }
}

@Composable
fun GymProApp() {
    val userPreferencesManager: UserPreferencesManager = koinInject()
    val userPreferences by userPreferencesManager.userPreferences.collectAsState(
        initial = UserPreferences(null, 0L, "system", "system")
    )

    val darkTheme = when (userPreferences.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    GymProTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            GymProNavHost(
                navController = navController,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}