package edu.pwr.zpi.netwalk.collector

import edu.pwr.zpi.netwalk.fetcher.MeasurementItem

data class MeasurementConditionParams(
    val hostCpuThreshold: Double,
    val hostCpuScheduleDelayCycles: Int,
    val remoteCpuThreshold: Double,
    val remoteCpuScheduleDelayCycles: Int,
    // add new here if needed
)

class MeasurementConditionChecker {
    fun check(
        measurements: List<MeasurementItem>,
        params: MeasurementConditionParams,
        onHighCpuDetected: (MeasurementItem, Double) -> Unit,
        onScheduleIperfInCycles: (Int) -> Unit,
    ) {
        for (item in measurements) {
            val hostCpu = item.host_cpu ?: continue
            val remoteCpu = item.remote_cpu ?: continue

            if (hostCpu > params.hostCpuThreshold) {
                onHighCpuDetected(item, hostCpu)
                onScheduleIperfInCycles(params.hostCpuScheduleDelayCycles)
                return
            }
            if (remoteCpu > params.remoteCpuThreshold) {
                onHighCpuDetected(item, remoteCpu)
                onScheduleIperfInCycles(params.remoteCpuScheduleDelayCycles)
                return
            }
        }
    }
}
