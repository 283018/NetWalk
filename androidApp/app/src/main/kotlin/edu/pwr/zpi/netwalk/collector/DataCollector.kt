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
    private val getIperfCommand: () -> Array<String>,
    private val onStatusUpdate: (String) -> Unit,
    private val onPassiveDataUpdate: (NetworkInfoData?, Pair<Double?, Double?>, SystemData?) -> Unit,
    private val sendRequest: (MeasurementRequest) -> Unit,
) {
    private var job: Job? = null
    private var lastIperfTime = 0L

    fun start(
        tm: TelephonyManager,
        context: Context,
        passiveIntervalMs: Flow<Long>,
        iperfIntervalMs: Flow<Long>,
        isCollectionEnabled: () -> Boolean,
        getSessionId: () -> String,
    ) {
        if (job?.isActive == true) return

        job = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val currentPassiveInterval = passiveIntervalMs.first()
                val currentIperfInterval = iperfIntervalMs.first()

                if (NetworkInfoFetcher.hasRequiredPermissions(context)) {
                    val networkData = NetworkInfoFetcher.fetchNetworkInfo(tm, context)
                    val locationData = getCurrentLocation(context)
                    val systemData = SystemInfoFetcher.fetchFullSystemInfo(context)

                    onPassiveDataUpdate(networkData, locationData, systemData)

                    if (isCollectionEnabled()) {
                        val now = System.currentTimeMillis()
                        // TODO: add check for busy iperf server OR server-side connection manager / port rotation
                        val shouldRunIperf = now - lastIperfTime > currentIperfInterval

                        val iperfResult = if (shouldRunIperf) {
                            lastIperfTime = now
                            try {
                                withContext(Dispatchers.IO) {
                                    withTimeoutOrNull(6000) {
                                        IperfRunner.runIperfOnce(getIperfCommand())
                                    }
                                }
                            } catch (e: Exception) {
                                null
                            }
                        } else {
                            null
                        }

                        val request = networkData.toMeasurementsRequest(
                            sessionId = getSessionId(),
                            latitude = locationData.first,
                            longitude = locationData.second,
                            systemData = systemData,
                            iperfRaw = iperfResult,
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
