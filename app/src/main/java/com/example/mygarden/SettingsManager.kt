package com.example.mygarden

import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

data class AppSettings(
    val musicEnabled: Boolean = true,
    val soundEnabled: Boolean = true
)

private val Context.appSettingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsManager(private val context: Context) {
    private val appContext = context.applicationContext

    val settingsFlow: Flow<AppSettings> = appContext.appSettingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> preferences.toAppSettings() }

    suspend fun setMusicEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.MusicEnabled] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        appContext.appSettingsDataStore.edit { preferences ->
            preferences[Keys.SoundEnabled] = enabled
        }
    }

    private object Keys {
        val MusicEnabled = booleanPreferencesKey("music_enabled")
        val SoundEnabled = booleanPreferencesKey("sound_enabled")
    }

    private fun Preferences.toAppSettings(): AppSettings {
        return AppSettings(
            musicEnabled = this[Keys.MusicEnabled] ?: true,
            soundEnabled = this[Keys.SoundEnabled] ?: true
        )
    }
}
