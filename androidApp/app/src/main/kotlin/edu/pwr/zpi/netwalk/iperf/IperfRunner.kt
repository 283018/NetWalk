package edu.pwr.zpi.netwalk.iperf

// disable android optimization for these functions names
import android.net.TrafficStats
import androidx.annotation.Keep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Keep
interface IperfCallback {
    fun onOutput(message: String)

    fun onError(error: String)

    fun onComplete()
}

@Keep
object IperfRunner {
    init {
        System.loadLibrary("netwalkIperf")
    }

    @JvmStatic
    external fun runIperfLive(
        arguments: Array<String>,
        callback: IperfCallback,
    )

    @JvmStatic
    external fun forceStopIperfTest(callback: IperfCallback)

    suspend fun runIperfOnce(command: Array<String>): String =
        withContext(Dispatchers.IO) {
            TrafficStats.setThreadStatsTag(0x00001000)

            try {
                suspendCancellableCoroutine { cont ->

                    val outputBuffer = StringBuilder()

                    val callback = object : IperfCallback {
                        override fun onOutput(message: String) {
                            outputBuffer.append(message)
                        }

                        override fun onError(error: String) {
                            if (cont.isActive) {
                                cont.resumeWithException(RuntimeException(error))
                            }
                        }

                        override fun onComplete() {
                            if (cont.isActive) {
                                cont.resume(outputBuffer.toString())
                            }
                        }
                    }

                    cont.invokeOnCancellation {
                        forceStopIperfTest(callback)
                    }

                    Thread {
                        try {
                            runIperfLive(command, callback)
                        } catch (e: Exception) {
                            if (cont.isActive) {
                                cont.resumeWithException(e)
                            }
                        }
                    }.start()
                }
            } finally {
                TrafficStats.clearThreadStatsTag()
            }
        }
}

data class ThroughputPoint(
    val seconds: Double,
    val throughputMbps: Double,
)

data class IperfParsed(
    val isUdp: Boolean,
    val throughputMbps: Double?,
    val meanRtt: Double?, // TCP specific
    val minRtt: Double?, // TCP specific
    val maxRtt: Double?,
    val jitterMs: Double?, // directly from UDP
    val lostPackets: Long?, // UDP specific
    val lostPercent: Double?,
    val hostCpuTotal: Double?,
    val remoteCpuTotal: Double?,
    val startTime: String?,
    val testDuration: Double?,
    val retransmits: Long?, // TCP specific
    val throughputTimeline: List<ThroughputPoint>,
)

fun parseIperfJsonSafe(jsonString: String): IperfParsed? =
    try {
        val json = Json.parseToJsonElement(jsonString).jsonObject

        val start = json["start"]?.jsonObject
        val end = json["end"]?.jsonObject
        val sumReceived = end?.get("sum_received")?.jsonObject
        val cpuUtil = end?.get("cpu_utilization_percent")?.jsonObject

        val streamData = end
            ?.get("streams")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
        val reciverStats = streamData?.get("receiver")?.jsonObject
        val senderStats = streamData?.get("sender")?.jsonObject

        val timestamp = start?.get("timestamp")?.jsonObject
        val testStart = start?.get("test_start")?.jsonObject
        val protocol = testStart?.get("protocol")?.jsonPrimitive?.content
        val isUdp = protocol?.equals("UDP", ignoreCase = true) == true

        val timelinePoints = mutableListOf<ThroughputPoint>()
        val intervals = json["intervals"]?.jsonArray

        intervals?.forEach { intervalElement ->
            val intervalObj = intervalElement.jsonObject

            val targetObj = intervalObj["sum"]?.jsonObject
                ?: intervalObj["streams"]?.jsonArray?.firstOrNull()?.jsonObject

            if (targetObj != null) {
                val timeEnd = targetObj["end"]?.jsonPrimitive?.doubleOrNull
                val bps = targetObj["bits_per_second"]?.jsonPrimitive?.doubleOrNull

                if (timeEnd != null && bps != null) {
                    val mbps = bps / 1_000_000.0
                    timelinePoints.add(ThroughputPoint(timeEnd, mbps))
                }
            }
        }

        IperfParsed(
            isUdp = isUdp,
            // Throughput mbps
            throughputMbps = sumReceived // f flag foes not affect json output
                ?.get("bits_per_second")
                ?.jsonPrimitive
                ?.doubleOrNull
                ?.div(1_000_000.0),
            // TCP latency and jitter
            meanRtt = senderStats?.get("mean_rtt")?.jsonPrimitive?.doubleOrNull,
            minRtt = senderStats?.get("min_rtt")?.jsonPrimitive?.doubleOrNull,
            maxRtt = senderStats?.get("max_rtt")?.jsonPrimitive?.doubleOrNull,
            // UDP jitter and packet loss
            jitterMs = sumReceived?.get("jitter_ms")?.jsonPrimitive?.doubleOrNull,
            lostPackets = sumReceived?.get("lost_packets")?.jsonPrimitive?.longOrNull,
            lostPercent = sumReceived?.get("lost_percent")?.jsonPrimitive?.doubleOrNull,
            // cpu utilization
            hostCpuTotal = cpuUtil?.get("host_total")?.jsonPrimitive?.doubleOrNull,
            remoteCpuTotal = cpuUtil?.get("remote_total")?.jsonPrimitive?.doubleOrNull,
            // timing
            startTime = timestamp
                ?.get("time")
                ?.jsonPrimitive
                ?.contentOrNull,
            testDuration = sumReceived?.get("seconds")?.jsonPrimitive?.doubleOrNull,
            // retransmits
            retransmits = end
                ?.get("sum_sent")
                ?.jsonObject
                ?.get("retransmits")
                ?.jsonPrimitive
                ?.longOrNull,
            throughputTimeline = timelinePoints,
        )
    } catch (_: Exception) {
        null
    }
