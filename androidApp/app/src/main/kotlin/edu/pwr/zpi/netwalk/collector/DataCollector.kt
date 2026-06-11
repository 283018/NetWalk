package edu.pwr.zpi.netwalk.collector

import android.content.Context
import android.telephony.TelephonyManager
import edu.pwr.zpi.netwalk.fetcher.MeasurementRequest
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoFetcher
import edu.pwr.zpi.netwalk.fetcher.toMeasurementsRequest
import edu.pwr.zpi.netwalk.iperf.IperfRunner
import edu.pwr.zpi.netwalk.location.getCurrentLocation
import edu.pwr.zpi.netwalk.system.SystemData
import edu.pwr.zpi.netwalk.system.SystemInfoFetcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class DataCollector(
    private val scope: CoroutineScope,
    private val getIperfCommand: (isDownload: Boolean) -> Array<String>,
    private val onStatusUpdate: (String) -> Unit,
    private val onPassiveDataUpdate: (NetworkInfoData?, Pair<Double?, Double?>, SystemData?) -> Unit,
    private val sendRequest: suspend (MeasurementRequest) -> Unit,
    private val shouldForceIperf: () -> Boolean,
    private val onForceIperfHandled: () -> Unit,
    private val onIperfRawResult: (String?, String?) -> Unit,
) {
    private var job: Job? = null
    private var lastIperfTime = 0L

    private var lastSessionId: String? = null

    private val delayedIperfRequests = mutableListOf<Int>() // each item - how many cycles until iperf should run
    private var readyForcedRuns = 0 // if several requests comes at single cycle, keep them queued

    fun scheduleIperfInCycles(cycles: Int) {
        delayedIperfRequests.add(cycles.coerceAtLeast(1))
    }

    private fun tickIperfSchedule() {
        if (delayedIperfRequests.isEmpty()) return

        for (i in delayedIperfRequests.indices) {
            delayedIperfRequests[i] -= 1
        }

        val dueCount = delayedIperfRequests.count { it <= 0 }
        delayedIperfRequests.removeAll { it <= 0 }
        readyForcedRuns += dueCount
    }

    fun start(
        tm: TelephonyManager,
        context: Context,
        passiveIntervalMs: Flow<Long>,
        iperfIntervalMs: Flow<Long>,
        iperfTimeoutMs: Flow<Long>,
        isCollectionEnabled: () -> Boolean,
        getSessionId: () -> String,
    ) {
        if (job?.isActive == true) return

        job = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val currentPassiveInterval = passiveIntervalMs.first()
                val currentIperfInterval = iperfIntervalMs.first()
                val currentTimout = iperfTimeoutMs.first()

                if (NetworkInfoFetcher.hasRequiredPermissions(context)) {
                    val networkData = NetworkInfoFetcher.fetchNetworkInfo(tm, context)
                    val locationData = getCurrentLocation(context)
                    val systemData = SystemInfoFetcher.fetchFullSystemInfo(context)

                    onPassiveDataUpdate(networkData, locationData, systemData)

                    if (isCollectionEnabled()) {
                        // re-sets iperf timer in new session
                        val currentSId = getSessionId()
                        if (currentSId.isNotEmpty() && currentSId != lastSessionId) {
                            lastIperfTime = 0
                            lastSessionId = currentSId
                        }

                        val now = System.currentTimeMillis()

                        tickIperfSchedule()

                        // TODO: add check for busy iperf server OR server-side connection manager / port rotation
                        // TODO: add repeat with delay if cpu is too high
                        var regularDue = now - lastIperfTime > currentIperfInterval
                        val forcedDue = readyForcedRuns > 0

                        var iperfUploadResult: String? = null
                        var iperfDownloadResult: String? = null

                        if (regularDue || forcedDue) {
                            if (forcedDue) {
                                readyForcedRuns -= 1
                            }

                            lastIperfTime = now

                            val (ul, dl) = try {
                                withContext(Dispatchers.IO) {
                                    val ulResuls = withTimeoutOrNull(currentTimout) {
                                        // named args are prohibited in labdas
                                        IperfRunner.runIperfOnce(getIperfCommand(false)) // isDownload=false
                                    }
                                    val dlResult = withTimeoutOrNull(currentTimout) {
                                        IperfRunner.runIperfOnce(getIperfCommand(true)) // isDownload=true
                                    }
                                    Pair(ulResuls, dlResult)
                                }
                            } catch (e: Exception) {
                                Pair(null, null)
                            }

                            iperfUploadResult = ul
                            iperfDownloadResult = dl

                            if (iperfUploadResult != null || iperfDownloadResult != null) {
                                onIperfRawResult(iperfUploadResult, iperfDownloadResult)
                            }
                        }

                        val request = networkData.toMeasurementsRequest(
                            sessionId = getSessionId(),
                            latitude = locationData.first,
                            longitude = locationData.second,
                            systemData = systemData,
                            iperfUlRaw = iperfUploadResult,
                            iperfDlRaw = iperfDownloadResult,
                            measuredAtNow = now,
                        )

                        sendRequest(request)
                    }
                } else {
                    onStatusUpdate("Permissions missing - cannot fetch data.")
                }

                delay(currentPassiveInterval)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
