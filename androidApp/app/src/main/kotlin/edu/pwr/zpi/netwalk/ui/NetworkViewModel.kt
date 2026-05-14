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
import edu.pwr.zpi.netwalk.collector.DataCollector
import edu.pwr.zpi.netwalk.fetcher.MeasurementItem
import edu.pwr.zpi.netwalk.fetcher.MeasurementRequest
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import edu.pwr.zpi.netwalk.network.NetworkClient
import edu.pwr.zpi.netwalk.settings.SettingsRepository
import edu.pwr.zpi.netwalk.system.SystemData
import edu.pwr.zpi.netwalk.ui.IperfLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.time.LocalTime
import java.util.UUID
import java.util.zip.GZIPOutputStream
import kotlin.Double
import kotlin.Pair

data class NetworkSettingsState(
    val serverUrl: String,
    val iperfIp: String,
    val iperfPort: String,
    val iperfTime: String,
    val iperfParallel: String,
    val sendImmediately: Boolean,
)

class NetworkViewModel(
    private val settings: SettingsRepository,
) : ViewModel() {
    var uiStateNetwork by mutableStateOf<NetworkInfoData?>(null)
        private set
    var uiStateLocation by mutableStateOf<Pair<Double?, Double?>>(null to null)
        private set

    var uiStateSystem by mutableStateOf<SystemData?>(null)
        private set

    var lastStatus by mutableStateOf("Waiting for first fetch...")
        private set
    var isCollecting by mutableStateOf(false)
        private set

    var forceIperfNow by mutableStateOf(false)
        private set

    private var sessionId: String = "" // only passive collection for ui displaying
    private var passiveJobStarted = false

    // na razie jest host dostosowany do android emulator któty jest dostępny razem z android sdk
    private var client: NetworkClient? = null
    private var currentServerUrl: String? = null

    val iperfLogEntries = mutableStateListOf<IperfLogEntry>()

    private val queuedMeasurements = mutableListOf<MeasurementItem>()

    fun requestIperfNow() {
        forceIperfNow = true
    }

    // exposing specific settings in viewModel as flows
    // I know its ugly, but I dint know it will turn out like this :c
    val uiSettingsState: StateFlow<NetworkSettingsState> = combine(
        settings.serverUrl.flow,
        settings.iperfIp.flow,
        settings.iperfPort.flow,
        settings.iperfTime.flow,
        settings.iperfParallel.flow,
        settings.sendImmediately.flow,
    ) { values: Array<Any?> ->
        NetworkSettingsState(
            serverUrl = values[0] as String,
            iperfIp = values[1] as String,
            iperfPort = values[2] as String,
            iperfTime = values[3] as String,
            iperfParallel = values[4] as String,
            sendImmediately = values[5] as Boolean,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NetworkSettingsState(
            settings.serverUrl.defaultValue,
            settings.iperfIp.defaultValue,
            settings.iperfPort.defaultValue,
            settings.iperfTime.defaultValue,
            settings.iperfParallel.defaultValue,
            settings.sendImmediately.defaultValue,
        ),
    )

    fun saveAllSettings(state: NetworkSettingsState) {
        viewModelScope.launch {
            settings.serverUrl.update(state.serverUrl)
            settings.iperfIp.update(state.iperfIp)
            settings.iperfPort.update(state.iperfPort)
            settings.iperfTime.update(state.iperfTime)
            settings.iperfParallel.update(state.iperfParallel)
            settings.sendImmediately.update(state.sendImmediately)
        }
    }

    val defaults = NetworkSettingsState(
        settings.serverUrl.defaultValue,
        settings.iperfIp.defaultValue,
        settings.iperfPort.defaultValue,
        settings.iperfTime.defaultValue,
        settings.iperfParallel.defaultValue,
        settings.sendImmediately.defaultValue,
    )

    private suspend fun sendMeasurementRequest(request: MeasurementRequest) {
        client
            ?.sendFullUpdate(request)
            ?.onSuccess {
                lastStatus = "Last send: Success (${LocalTime.now()})"
            }?.onFailure {
                lastStatus = "Error: ${it.localizedMessage}"
            } ?: run {
            lastStatus = "Error: NetworkClient not initialized"
        }
    }

    private suspend fun sendGzippedBatch(batchRequest: MeasurementRequest) {
        val jsonBytes = Json
            .encodeToString(MeasurementRequest.serializer(), batchRequest)
            .toByteArray(Charsets.UTF_8)
        val gzippedBytes = ByteArrayOutputStream().use { bos ->
            GZIPOutputStream(bos).use { gzip -> gzip.write(jsonBytes) }
            bos.toByteArray()
        }

        client
            ?.sendGzippedUpdate(gzippedBytes)
            ?.onSuccess {
                lastStatus = "Batch sent (compressed) successfully (${LocalTime.now()})"
            }?.onFailure {
                lastStatus = "Batch error: ${it.localizedMessage}"
            } ?: run {
            lastStatus = "Error: NetworkClient not initialized"
        }
    }

    private val collector = DataCollector(
        scope = viewModelScope,
        getIperfCommand = { iperfCommand.value },
        onStatusUpdate = { status -> lastStatus = status },
        onPassiveDataUpdate = { network, location, system ->
            uiStateNetwork = network
            uiStateLocation = location
            uiStateSystem = system
        },
        sendRequest = { request ->

            request.measurements.forEach { item ->
                if (item.throughput_mbps != null || item.mean_rtt != null || item.retransmits != null) {
                    iperfLogEntries.add(
                        IperfLogEntry(
                            timestamp = item.measured_at,
                            throughputMbps = item.throughput_mbps,
                            meanRtt = item.mean_rtt,
                            retransmits = item.retransmits,
                        ),
                    )
                }
            }

            if (settings.sendImmediately.flow.first()) {
                sendMeasurementRequest(request)
            } else {
                queuedMeasurements.addAll(request.measurements)
                lastStatus = "Queued: ${queuedMeasurements.size} measurements"
            }
        },
        shouldForceIperf = { forceIperfNow },
        onForceIperfHandled = { forceIperfNow = false },
    )

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

    fun startPassiveCollection(
        tm: TelephonyManager,
        context: Context,
    ) {
        if (passiveJobStarted) return
        passiveJobStarted = true

        val iperfTimeoutMsFlow: Flow<Long> = settings.iperfTime.flow.map { timeStr ->
            val testSeconds = timeStr.toDoubleOrNull() ?: 10.0
            ((testSeconds + 5.0) * 1000).toLong()
        }

        collector.start(
            tm = tm,
            context = context,
            passiveIntervalMs = settings.passiveInterval.flow,
            iperfIntervalMs = settings.iperfInterval.flow,
            iperfTimeoutMs = iperfTimeoutMsFlow,
            isCollectionEnabled = { isCollecting },
            getSessionId = { sessionId },
        )
    }

    @SuppressLint("MissingPermission")
    fun startCollection(
        tm: TelephonyManager,
        context: Context,
    ) {
        if (!isCollecting) {
            sessionId = UUID.randomUUID().toString()
            isCollecting = true
            queuedMeasurements.clear() // just to be sure
        }
    }

    fun stopCollection() {
        if (isCollecting) {
            isCollecting = false
            if (queuedMeasurements.isNotEmpty()) {
                viewModelScope.launch {
                    val batchRequest = MeasurementRequest(measurements = queuedMeasurements.toList())
                    queuedMeasurements.clear()
                    lastStatus = "Sending batch of ${batchRequest.measurements.size} measurements"

                    sendGzippedBatch(batchRequest)
                    // sendMeasurementRequest(batchRequest)
                }
            }
        }
    }

    val iperfCommand = combine(
        settings.iperfIp.flow,
        settings.iperfPort.flow,
        settings.iperfTime.flow,
        settings.iperfParallel.flow,
    ) { ip, port, time, parallel ->
        buildList {
            add("iperf3")
            add("-c")
            add(ip)

            if (port.isNotBlank()) {
                add("-p")
                add(port)
            }

            add("-t")
            add(time)

            if (parallel.isNotBlank()) {
                add("-P")
                add(parallel)
            }

            add("--json")
        }.toTypedArray()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = arrayOf("iperf3"), // temporary fallback on init, reconstructed form default arguments later
    )
}
