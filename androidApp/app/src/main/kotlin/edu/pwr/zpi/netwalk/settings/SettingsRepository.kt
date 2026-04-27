package edu.pwr.zpi.netwalk.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
) {
    companion object {
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        const val DEFAULT_URL = "http://10.0.2.2:8000"
    }

    val serverUrl: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[SERVER_URL_KEY] ?: DEFAULT_URL
        }

    suspend fun updateServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url
        }
    }
}
