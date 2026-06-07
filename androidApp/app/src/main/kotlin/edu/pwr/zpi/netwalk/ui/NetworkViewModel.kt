package edu.pwr.zpi.netwalk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
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
import edu.pwr.zpi.netwalk.iperf.ThroughputPoint
import edu.pwr.zpi.netwalk.iperf.parseIperfJsonSafe
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    val packageSize: String,
    val bufferLength: String,
    val targetBandwidth: String,
    val useUdp: Boolean,
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

    var lastTestTimeline by mutableStateOf<List<ThroughputPoint>>(emptyList())
        private set

    private var sessionId: String = "" // only passive collection for ui displaying
    private var passiveJobStarted = false

    // na razie jest host dostosowany do android emulator któty jest dostępny razem z android sdk
    private var client: NetworkClient? = null
    private var currentServerUrl: String? = null

    val iperfLogEntries = mutableStateListOf<IperfLogEntry>()

    private val queuedMeasurements = mutableListOf<MeasurementItem>()

    val rsrpHistory = mutableStateListOf<Float>()
    val rsrqHistory = mutableStateListOf<Float>()
    val sinrHistory = mutableStateListOf<Float>()
    private val signalPointLimit = 50

    fun requestIperfNow() {
        forceIperfNow = true
        lastTestTimeline = emptyList()
    }

    private val flushMutex = Mutex()

    // exposing specific settings in viewModel as flows
    // I know its ugly, but I dint know it will turn out like this :c
    val uiSettingsState: StateFlow<NetworkSettingsState> = combine(
        settings.serverUrl.flow,
        settings.iperfIp.flow,
        settings.iperfPort.flow,
        settings.iperfTime.flow,
        settings.iperfParallel.flow,
        settings.useUdp.flow,
        settings.packageSize.flow,
        settings.targetBandwidth.flow,
        settings.bufferLength.flow,
        settings.sendImmediately.flow,
    ) { values: Array<Any?> ->
        NetworkSettingsState(
            serverUrl = values[0] as String,
            iperfIp = values[1] as String,
            iperfPort = values[2] as String,
            iperfTime = values[3] as String,
            iperfParallel = values[4] as String,
            useUdp = values[5] as Boolean,
            packageSize = values[6] as String,
            targetBandwidth = values[7] as String,
            bufferLength = values[8] as String,
            sendImmediately = values[9] as Boolean,
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
            settings.packageSize.defaultValue,
            settings.targetBandwidth.defaultValue,
            settings.bufferLength.defaultValue,
            settings.useUdp.defaultValue,
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
            settings.packageSize.update(state.packageSize)
            settings.targetBandwidth.update(state.targetBandwidth)
            settings.bufferLength.update(state.bufferLength)
            settings.useUdp.update(state.useUdp)
            settings.sendImmediately.update(state.sendImmediately)
        }
    }

    val defaults = NetworkSettingsState(
        settings.serverUrl.defaultValue,
        settings.iperfIp.defaultValue,
        settings.iperfPort.defaultValue,
        settings.iperfTime.defaultValue,
        settings.iperfParallel.defaultValue,
        settings.packageSize.defaultValue,
        settings.targetBandwidth.defaultValue,
        settings.bufferLength.defaultValue,
        settings.useUdp.defaultValue,
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

    private fun updateSignalHistory(networkData: NetworkInfoData?) {
        val nrServing = networkData?.nrCells?.firstOrNull { it.isServing }
        val lteServing = networkData?.lteCells?.firstOrNull { it.isServing }

        val currentRsrp = nrServing?.ssRsrp?.toFloat() ?: lteServing?.rsrp?.toFloat()
        if (currentRsrp != null) {
            if (rsrpHistory.size >= signalPointLimit) rsrpHistory.removeAt(0)
            rsrpHistory.add(currentRsrp)
        }

        val currentRsrq = nrServing?.ssRsrq?.toFloat() ?: lteServing?.rsrq?.toFloat()
        if (currentRsrq != null) {
            if (rsrqHistory.size >= signalPointLimit) rsrqHistory.removeAt(0)
            rsrqHistory.add(currentRsrq)
        }

        val currentSinr = nrServing?.ssSinr?.toFloat() ?: lteServing?.sinr?.toFloat()
        if (currentSinr != null) {
            if (sinrHistory.size >= signalPointLimit) sinrHistory.removeAt(0)
            sinrHistory.add(currentSinr)
        }
    }

    private val collector = DataCollector(
        scope = viewModelScope,
        getIperfCommand = { isDownload -> iperfCommand(isDownload) },
        onStatusUpdate = { status -> lastStatus = status },
        onPassiveDataUpdate = { network, location, system ->
            uiStateNetwork = network
            uiStateLocation = location
            uiStateSystem = system
            updateSignalHistory(network)
        },
        sendRequest = { request ->

            request.measurements.forEach { item ->
                if (item.test_duration != null || item.protocol != null) {
                    iperfLogEntries.add(
                        IperfLogEntry(
                            timestamp = item.measured_at,
                            throughputMbps = item.dl_throughput_mbps,
                            meanRtt = item.dl_mean_rtt,
                            retransmits = item.dl_retransmits,
                        ),
                    )
                }
            }

            if (settings.sendImmediately.flow.first()) {
                sendMeasurementRequest(request)
            } else {
                queuedMeasurements.addAll(request.measurements)
                lastStatus = "Queued: ${queuedMeasurements.size} measurements"

                // immediate send then queue exceed some size
                val threshold = settings.maxQueueSize.flow.first()
                if (queuedMeasurements.size >= threshold) {
                    val batchList = queuedMeasurements.toList()
                    queuedMeasurements.clear()
                    viewModelScope.launch {
                        flushMutex.withLock {
                            val batchRequest = MeasurementRequest(measurements = batchList)
                            sendGzippedBatch(batchRequest)
                        }
                    }
                }
            }
        },
        shouldForceIperf = { forceIperfNow },
        onForceIperfHandled = { forceIperfNow = false },
        onIperfRawResult = { ulRawJson, dlRawJson ->

            // TODO: update plot to use both
            val activeJson = dlRawJson ?: ulRawJson // for now using dl by default

            if (activeJson != null) {
                parseIperfJsonSafe(activeJson)?.let { parsedData ->
                    lastTestTimeline = parsedData.throughputTimeline
                }
            }
        },
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

    private fun iperfCommand(isDownload: Boolean): Array<String> {
        val state = uiSettingsState.value

        val commandArray = buildList {
            add("iperf3")
            add("-c")
            add(state.iperfIp)

            if (state.iperfPort.isNotBlank()) {
                add("-p")
                add(state.iperfPort)
            }

            if (state.useUdp) {
                add("-u")

                add("-l")
                add(state.bufferLength)

                add("-b")
                add(state.targetBandwidth)
            } else {
                add("-M")
                add(state.packageSize)
            }

            add("-t")
            add(state.iperfTime)

            if (state.iperfParallel.isNotBlank()) {
                add("-P")
                add(state.iperfParallel)
            }

            if (isDownload) {
                add("-R")
            }

            add("--json")
        }.toTypedArray()

        // temporary fallback on init, reconstructed form default arguments later
        val finalArray = if (state.iperfIp.isBlank()) arrayOf("iperf3") else commandArray
        Log.d("NetWalk", "Iperf command array: ${commandArray.joinToString(" ")}")

        return finalArray
    }
}
