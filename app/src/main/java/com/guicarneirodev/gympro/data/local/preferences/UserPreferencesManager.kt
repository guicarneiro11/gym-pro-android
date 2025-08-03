package com.guicarneirodev.gympro.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_preferences")

class UserPreferencesManager(private val context: Context) {

    companion object {
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_LAST_SYNC = longPreferencesKey("last_sync")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_LANGUAGE = stringPreferencesKey("language")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                userId = preferences[KEY_USER_ID],
                lastSync = preferences[KEY_LAST_SYNC] ?: 0L,
                themeMode = preferences[KEY_THEME_MODE] ?: "system",
                language = preferences[KEY_LANGUAGE] ?: "system"
            )
        }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun setUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
        }
    }

    suspend fun updateLastSync() {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC] = System.currentTimeMillis()
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }
}

data class UserPreferences(
    val userId: String?,
    val lastSync: Long,
    val themeMode: String,
    val language: String
)