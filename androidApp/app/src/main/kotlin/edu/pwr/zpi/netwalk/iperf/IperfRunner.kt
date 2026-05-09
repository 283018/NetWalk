package edu.pwr.zpi.netwalk.iperf

// disable android optimization for these functions names
import androidx.annotation.Keep
import kotlinx.coroutines.suspendCancellableCoroutine
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

            runIperfLive(command, callback)

            cont.invokeOnCancellation {
                forceStopIperfTest(callback)
            }
        }
}

data class IperfParsed(
    val throughputMbps: Double?,
    val meanRtt: Double?,
    val minRtt: Double?,
    val maxRtt: Double?,
    val hostCpuTotal: Double?,
    val remoteCpuTotal: Double?,
    val startTime: String?,
    val testDuration: Double?,
    val retransmits: Long?,
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

        IperfParsed(
            // Throughput mbps
            throughputMbps = sumReceived // f flag foes not affect json output
                ?.get("bits_per_second")
                ?.jsonPrimitive
                ?.doubleOrNull
                ?.div(1_000_000.0),
            // latency and jitter
            meanRtt = senderStats?.get("mean_rtt")?.jsonPrimitive?.doubleOrNull,
            minRtt = senderStats?.get("min_rtt")?.jsonPrimitive?.doubleOrNull,
            maxRtt = senderStats?.get("max_rtt")?.jsonPrimitive?.doubleOrNull,
            // cpu utilization
            hostCpuTotal = cpuUtil?.get("host_total")?.jsonPrimitive?.doubleOrNull,
            remoteCpuTotal = cpuUtil?.get("remote_total")?.jsonPrimitive?.doubleOrNull,
            // timing
            startTime = start
                ?.get("timestamp")
                ?.jsonObject
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
        )
    } catch (_: Exception) {
        null
    }
