package edu.pwr.zpi.netwalk.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(
    private val context: Context,
) {
    inner class PreferenceItem<T>(
        private val key: Preferences.Key<T>,
        val defaultValue: T,
    ) {
        val flow: Flow<T> = context.dataStore.data
            .map { prefs -> prefs[key] ?: defaultValue }

        suspend fun update(value: T) {
            context.dataStore.edit { prefs -> prefs[key] = value }
        }

        // for crating state flow, that does not hang ui then getting settings
        fun asStateFlow(scope: CoroutineScope): StateFlow<T> =
            flow.stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = defaultValue,
            )
    }

    val serverUrl = PreferenceItem(stringPreferencesKey("server_url"), "http://10.0.2.2:8000")
    val iperfIp = PreferenceItem(stringPreferencesKey("iperf_ip"), "10.0.2.2")
    val iperfPort = PreferenceItem(stringPreferencesKey("iperf_port"), "5201")
    val iperfArgs = PreferenceItem(stringPreferencesKey("iperf_args"), "-t 3")

    val passiveInterval = PreferenceItem(longPreferencesKey("passive_interval"), 3_000L)
    val iperfInterval = PreferenceItem(longPreferencesKey("iperf_interval"), 10_000L)
}
