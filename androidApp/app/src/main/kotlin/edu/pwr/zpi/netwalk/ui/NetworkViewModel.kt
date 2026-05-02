package edu.pwr.zpi.netwalk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import edu.pwr.zpi.netwalk.fetcher.MeasurementRequest
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoFetcher
import edu.pwr.zpi.netwalk.fetcher.toMeasurementsRequest
import edu.pwr.zpi.netwalk.iperf.IperfCallback
import edu.pwr.zpi.netwalk.iperf.IperfRunner
import edu.pwr.zpi.netwalk.location.getCurrentLocation
import edu.pwr.zpi.netwalk.network.NetworkClient
import edu.pwr.zpi.netwalk.settings.SettingsRepository
import edu.pwr.zpi.netwalk.system.SystemData
import edu.pwr.zpi.netwalk.system.SystemInfoFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.Double
import kotlin.Pair

class NetworkViewModel(
    val settings: SettingsRepository,
) : ViewModel() {
    var uiStateNetwork by mutableStateOf<NetworkInfoData?>(null)
        private set
    var uiStateLocation by mutableStateOf<Pair<Double?, Double?>>(null to null)
        private set

    var uiStateSystem by mutableStateOf<SystemData?>(null)
        private set

    var lastStatus by mutableStateOf("Waiting for first fetch...")
        private set

    // na razie jest host dostosowany do android emulator któty jest dostępny razem z android sdk
    private var client: NetworkClient? = null
    private var collectionJob: Job? = null
    private var currentServerUrl: String? = null

    val iperfLogs = mutableStateListOf<String>()
    private var iperfJob: Job? = null

    init {
        // obserwujemy zmiane url
        viewModelScope.launch {
            settings.serverUrl.flow.collect { url ->
                if (url != currentServerUrl) {
                    client = NetworkClient(url)
                    currentServerUrl = url
                    lastStatus = "Server URL updated: $url"
                }
            }
        }
    }

    fun runIperfTest() {
        if (iperfJob?.isActive == true) return
        iperfLogs.clear()

        iperfJob = viewModelScope.launch(Dispatchers.IO) {
            val args = arrayOf("iperf3", "-c", "10.0.2.2", "-p", "5201", "-t", "5")

            try {
                IperfRunner.runIperfLive(
                    args,
                    object : IperfCallback {
                        override fun onOutput(message: String) {
                            viewModelScope.launch(Dispatchers.Main) {
                                iperfLogs.add(message.trim())
                            }
                        }

                        override fun onError(error: String) {
                            viewModelScope.launch(Dispatchers.Main) {
                                iperfLogs.add("Error: $error")
                            }
                        }

                        override fun onComplete() {
                            viewModelScope.launch(Dispatchers.Main) {
                                iperfLogs.add("--- Test Complete ---")
                            }
                        }
                    },
                )
            } catch (e: Exception) {
                viewModelScope.launch(Dispatchers.Main) {
                    iperfLogs.add("Native implementation failed: ${e.message}")
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
                    val systemData = SystemInfoFetcher.fetchFullSystemInfo(context)

                    uiStateNetwork = networkData
                    uiStateLocation = locationData
                    uiStateSystem = systemData

                    val (lat, lon) = locationData
                    val request = networkData.toMeasurementsRequest(lat, lon, systemData)
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


}
