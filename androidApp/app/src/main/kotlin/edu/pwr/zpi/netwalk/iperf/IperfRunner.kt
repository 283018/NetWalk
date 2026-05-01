package edu.pwr.zpi.netwalk.iperf

// disable android optimization for these functions names
import androidx.annotation.Keep

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
}
