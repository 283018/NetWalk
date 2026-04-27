package edu.pwr.zpi.netwalk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.pwr.zpi.netwalk.fetcher.MeasurementRequest
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoFetcher
import edu.pwr.zpi.netwalk.fetcher.toMeasurementsRequest
import edu.pwr.zpi.netwalk.location.getCurrentLocation
import edu.pwr.zpi.netwalk.network.NetworkClient
import edu.pwr.zpi.netwalk.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.Double
import kotlin.Pair

class NetworkViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    var uiStateNetwork by mutableStateOf<NetworkInfoData?>(null)
        private set
    var uiStateLocation by mutableStateOf<Pair<Double?, Double?>>(null to null)
        private set
    var lastStatus by mutableStateOf("Waiting for first fetch...")
        private set

    // na razie jest host dostosowany do android emulator któty jest dostępny razem z android sdk
    private var client: NetworkClient? = null
    private var collectionJob: Job? = null
    private var currentServerUrl: String? = null

    init {
        // obserwujemy zmiane url
        viewModelScope.launch {
            settingsRepository.serverUrl.collect { url ->
                if (url != currentServerUrl) {
                    client = NetworkClient(url)
                    currentServerUrl = url
                    lastStatus = "Server URL updated: $url"
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startCollection(
        tm: TelephonyManager,
        context: Context,
    ) {
        // zapobiegamy rozpoczęciu kilku collectionJob
        if (collectionJob?.isActive == true) return

        collectionJob = viewModelScope.launch {
            while (isActive) {
                if (NetworkInfoFetcher.hasRequiredPermissions(context)) {
                    val networkAsyncData = async { NetworkInfoFetcher.fetchNetworkInfo(tm, context) }
                    val locationAsyncData = async { getCurrentLocation(context) }

                    val networkData = networkAsyncData.await()
                    val locationData = locationAsyncData.await()

                    uiStateNetwork = networkData
                    uiStateLocation = locationData

                    val (lat, lon) = locationData
                    val request = networkData.toMeasurementsRequest(lat, lon)
                    sendToServer(request)
                } else {
                    lastStatus = "Permissions missing - cannot fetch data."
                }

                delay(5000)
            }
        }
    }

    private fun sendToServer(request: MeasurementRequest) {
        viewModelScope.launch {
            client
                ?.sendFullUpdate(request)
                ?.onSuccess {
                    lastStatus = "Last send: Success (${LocalTime.now()})"
                }?.onFailure {
                    lastStatus = "Error: ${it.localizedMessage}"
                    println("Network Error: ${it.message}")
                }
                ?: run {
                    lastStatus = "Error: NetworkClient not initialized"
                    println("Network Error: client is null")
                }
        }
    }

    // helper zeby aktualizować url z ui'u
    suspend fun updateServerUrl(url: String) {
        settingsRepository.updateServerUrl(url)
    }

    fun getCurrentServerUrl(): String = currentServerUrl ?: SettingsRepository.DEFAULT_URL
}
