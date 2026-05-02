package edu.pwr.zpi.netwalk.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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

        // TODO: blocking calls are dangerous in ui
        fun currentValue(): T = runBlocking { flow.first() }
    }

    val serverUrl = PreferenceItem(stringPreferencesKey("server_url"), "http://10.0.2.2:8000")
    val iperfIp = PreferenceItem(stringPreferencesKey("iperf_ip"), "10.0.2.2")
    val iperfPort = PreferenceItem(stringPreferencesKey("iperf_port"), "5201")
    val iperfArgs = PreferenceItem(stringPreferencesKey("iperf_args"), "-t 3")
}
