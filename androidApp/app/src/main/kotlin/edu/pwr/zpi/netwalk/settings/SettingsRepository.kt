package edu.pwr.zpi.netwalk.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
    val iperfTime = PreferenceItem(stringPreferencesKey("iperf_time"), "10")
    val iperfParallel = PreferenceItem(stringPreferencesKey("iperf_parallel"), "4")

    val useUdp = PreferenceItem(booleanPreferencesKey("use_udp"), false)
    val packageSize = PreferenceItem(stringPreferencesKey("package_size"), "1000")
    val targetBandwidth = PreferenceItem(stringPreferencesKey("target_bandwidth"), "100M")
    val bufferLength = PreferenceItem(stringPreferencesKey("buffer_length"), "1000")

    val passiveInterval = PreferenceItem(longPreferencesKey("passive_interval"), 3_000L)

    // for now every 3 minutes, maybe will have to increase later
    val iperfInterval = PreferenceItem(longPreferencesKey("iperf_interval"), 180_000L)

    val sendImmediately = PreferenceItem(booleanPreferencesKey("send_immediately"), false)

    // not exposed to ui
    val maxQueueSize = PreferenceItem(longPreferencesKey("max_queue_size"), 30L)
}
