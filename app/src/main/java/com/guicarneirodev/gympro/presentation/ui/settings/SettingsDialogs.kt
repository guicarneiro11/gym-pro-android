package com.guicarneirodev.gympro.presentation.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.guicarneirodev.gympro.R
import com.guicarneirodev.gympro.data.local.preferences.UserPreferencesManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.Locale

@Composable
fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_logout),
                contentDescription = null
            )
        },
        title = {
            Text(text = stringResource(R.string.logout_confirmation_title))
        },
        text = {
            Text(text = stringResource(R.string.logout_confirmation_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.logout_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun ThemeDialog(
    onDismiss: () -> Unit
) {
    val userPreferencesManager: UserPreferencesManager = koinInject()
    val scope = rememberCoroutineScope()
    var selectedTheme by remember { mutableStateOf("system") }

    LaunchedEffect(Unit) {
        userPreferencesManager.userPreferences.collect { prefs ->
            selectedTheme = prefs.themeMode
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_theme))
        },
        text = {
            Column {
                RadioButtonOption(
                    text = stringResource(R.string.theme_system),
                    selected = selectedTheme == "system",
                    onSelect = { selectedTheme = "system" }
                )
                RadioButtonOption(
                    text = stringResource(R.string.theme_light),
                    selected = selectedTheme == "light",
                    onSelect = { selectedTheme = "light" }
                )
                RadioButtonOption(
                    text = stringResource(R.string.theme_dark),
                    selected = selectedTheme == "dark",
                    onSelect = { selectedTheme = "dark" }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        userPreferencesManager.setThemeMode(selectedTheme)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun LanguageDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userPreferencesManager: UserPreferencesManager = koinInject()
    val scope = rememberCoroutineScope()

    var selectedLanguage by remember {
        mutableStateOf(
            when (Locale.getDefault().language) {
                "pt" -> "pt"
                "es" -> "es"
                "en" -> "en"
                else -> "system"
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.settings_language))
        },
        text = {
            Column {
                RadioButtonOption(
                    text = stringResource(R.string.language_system),
                    selected = selectedLanguage == "system",
                    onSelect = { selectedLanguage = "system" }
                )
                RadioButtonOption(
                    text = "English",
                    selected = selectedLanguage == "en",
                    onSelect = { selectedLanguage = "en" }
                )
                RadioButtonOption(
                    text = "Português",
                    selected = selectedLanguage == "pt",
                    onSelect = { selectedLanguage = "pt" }
                )
                RadioButtonOption(
                    text = "Español",
                    selected = selectedLanguage == "es",
                    onSelect = { selectedLanguage = "es" }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        userPreferencesManager.setLanguage(selectedLanguage)
                        changeAppLanguage(context, selectedLanguage)
                        onDismiss()
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun changeAppLanguage(context: Context, languageCode: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.getSystemService(LocaleManager::class.java).applicationLocales =
            if (languageCode == "system") {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(languageCode)
            }
    } else {
        val locale = when (languageCode) {
            "system" -> LocaleListCompat.getEmptyLocaleList()
            "pt" -> LocaleListCompat.create(Locale("pt", "BR"))
            "es" -> LocaleListCompat.create(Locale("es"))
            "en" -> LocaleListCompat.create(Locale("en"))
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locale)
    }
}

@Composable
private fun RadioButtonOption(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text)
    }
}