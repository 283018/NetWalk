package edu.pwr.zpi.netwalk.ui

import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoFetcher
import edu.pwr.zpi.netwalk.fetcher.toMeasurementsRequest
import edu.pwr.zpi.netwalk.network.NetworkClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

class NetworkViewModel : ViewModel() {
    var uiState by mutableStateOf<NetworkInfoData?>(null)
        private set
    var lastStatus by mutableStateOf("Waiting for first fetch...")
        private set

    // na razie jest host dostosowany do android emulator któty jest dostępny razem z android sdk
    private val client = NetworkClient("http://10.0.2.2:8000")
    private var collectionJob: Job? = null

    fun startCollection(
        tm: TelephonyManager,
        context: Context,
    ) {
        // zapobiegamy rozpoczęciu kilku collectionJob
        if (collectionJob?.isActive == true) return

        collectionJob = viewModelScope.launch {
            while (isActive) {
                NetworkInfoFetcher.fetchNetworkInfoSafe(tm, context) { fetchedData ->

                    // tu można dodać processing etc.
                    uiState = fetchedData
                    sendToServer(fetchedData)
                }
                delay(5000)
            }
        }
    }

    private fun sendToServer(data: NetworkInfoData) {
        viewModelScope.launch {
            val request = data.toMeasurementsRequest()

            client
                .sendFullUpdate(request)
                .onSuccess {
                    lastStatus = "Last send: Success (${LocalTime.now()})"
                }.onFailure {
                    lastStatus = "Error: ${it.localizedMessage}"
                    println("Network Error: ${it.message}")
                }
        }
    }
}
