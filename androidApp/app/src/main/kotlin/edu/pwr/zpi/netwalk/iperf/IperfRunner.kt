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
    val startTime: String?,
    val endTime: String?,
)

fun parseIperfJsonSafe(jsonString: String): IperfParsed? =
    try {
        val json = Json.parseToJsonElement(jsonString).jsonObject

        val end = json["end"]?.jsonObject
        val sumReceived = end?.get("sum_received")?.jsonObject
        val bitsPerSecond = sumReceived?.get("bits_per_second")?.jsonPrimitive?.doubleOrNull

        val start = json["start"]?.jsonObject
        val timestamp = start?.get("timestamp")?.jsonObject

        val startTime = timestamp?.get("time")?.jsonPrimitive?.contentOrNull
        val endTime = json["end"]
            ?.jsonObject
            ?.get("streams")
            ?.jsonArray
            ?.lastOrNull()
            ?.jsonObject
            ?.get("receiver")
            ?.jsonObject
            ?.get("end")
            ?.jsonPrimitive
            ?.contentOrNull

        IperfParsed(
            throughputMbps = bitsPerSecond?.div(1_000_000.0), // -f flag foes not affect json output
            startTime = startTime,
            endTime = endTime,
        )
    } catch (_: Exception) {
        null
    }
