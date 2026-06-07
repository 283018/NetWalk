package edu.pwr.zpi.netwalk.fetcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.ServiceState
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import edu.pwr.zpi.netwalk.iperf.IperfParsed
import edu.pwr.zpi.netwalk.iperf.RttPoint
import edu.pwr.zpi.netwalk.iperf.parseIperfJsonSafe
import edu.pwr.zpi.netwalk.system.SystemData
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume

@Serializable
data class MeasurementItem(
    val session_id: String,
    val android_id: String? = null,
    val cid: Long? = null, // Cell identity
    val measured_at: String, // using ISO 8901 as datetime
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val sinr: Int? = null,
    val network_type: String? = null,
    val tac: Int? = null,
    val cell_id: String? = null,
    val radio_frequency: Int? = null, // EARFCN for LTE, NR-ARFCN for 5G
    val band: Int? = null,
    val bandwidth: Int?,
    val battery_level: Int? = null,
    val battery_temp: Double? = null,
    val os_version: String? = null,
    //
    val ul_throughput_mbps: Double? = null,
    val ul_latency_ms: Double? = null,
    val ul_jitter_ms: Double? = null,
    val ul_mean_rtt: Double? = null,
    val ul_min_rtt: Double? = null,
    val ul_max_rtt: Double? = null,
    val ul_retransmits: Long? = null,
    val ul_lost_packets: Long? = null,
    val ul_lost_percent: Double? = null,
    //
    val dl_throughput_mbps: Double? = null,
    val dl_latency_ms: Double? = null,
    val dl_jitter_ms: Double? = null,
    val dl_mean_rtt: Double? = null,
    val dl_min_rtt: Double? = null,
    val dl_max_rtt: Double? = null,
    val dl_retransmits: Long? = null,
    val dl_lost_packets: Long? = null,
    val dl_lost_percent: Double? = null,
    //
    val test_start_time: String? = null,
    val test_duration: Double? = null,
    //
    val host_cpu: Double? = null, // we are taking max, since whole test must be repeated if load is too high
    val remote_cpu: Double? = null,
    //
    val protocol: String? = null,
    val iperf_ul_json: String? = null,
    val iperf_dl_json: String? = null,
) {
    constructor (
        sessionId: String,
        lat: Double?,
        lon: Double?,
        networkType: String,
        servingNr: NrNetworkInfo?,
        servingLte: LteNetworkInfo?,
        measuredAt: Long,
        system: SystemData,
        iperfUl: IperfParsed?,
        iperfDl: IperfParsed?,
        iperfUlRaw: String? = null,
        iperfDlRaw: String? = null,
    ) : this(
        session_id = sessionId,
        android_id = system.android_id,
        cid = servingNr?.cid ?: servingLte?.cid,
        measured_at = java.time.Instant
            .ofEpochMilli(measuredAt)
            .toString(),
        latitude = lat,
        longitude = lon,
        rsrp = servingNr?.ssRsrp ?: servingLte?.rsrp,
        rsrq = servingNr?.ssRsrq ?: servingLte?.rsrq,
        sinr = servingNr?.ssSinr ?: servingLte?.sinr,
        network_type = networkType,
        tac = servingNr?.tac ?: servingLte?.tac,
        cell_id = servingNr?.pci?.toString() ?: servingLte?.pci?.toString(),
        radio_frequency = servingNr?.nrarfcn ?: servingLte?.earfcn,
        band = servingNr?.bands?.firstOrNull() ?: servingLte?.bands?.firstOrNull(),
        bandwidth = servingNr?.bandwidth ?: servingLte?.bandwidth,
        battery_level = system.battery_level,
        battery_temp = system.battery_temp,
        os_version = system.os_version,
        //
        ul_throughput_mbps = iperfUl?.throughputMbps,
        ul_latency_ms = iperfUl?.meanRtt,
        ul_jitter_ms = iperfUl?.jitterMs ?: calculateTcpJitterMs(iperfUl?.rttTimeline),
        ul_mean_rtt = iperfUl?.meanRtt,
        ul_min_rtt = iperfUl?.minRtt,
        ul_max_rtt = iperfUl?.maxRtt,
        ul_retransmits = iperfUl?.retransmits,
        ul_lost_packets = iperfUl?.lostPackets,
        ul_lost_percent = iperfUl?.lostPercent,
        //
        dl_throughput_mbps = iperfDl?.throughputMbps,
        dl_latency_ms = iperfDl?.meanRtt,
        dl_jitter_ms = iperfDl?.jitterMs ?: calculateTcpJitterMs(iperfDl?.rttTimeline),
        dl_mean_rtt = iperfDl?.meanRtt,
        dl_min_rtt = iperfDl?.minRtt,
        dl_max_rtt = iperfDl?.maxRtt,
        dl_retransmits = iperfDl?.retransmits,
        dl_lost_packets = iperfDl?.lostPackets,
        dl_lost_percent = iperfDl?.lostPercent,
        //
        test_start_time = listOfNotNull(
            iperfDl?.startTime?.let { convertIperfTimeToIso(it) },
            iperfUl?.startTime?.let { convertIperfTimeToIso(it) },
        ).minOrNull(),
        test_duration = if (iperfUl?.testDuration != null || iperfDl?.testDuration != null) {
            (iperfUl?.testDuration ?: 0.0) + (iperfDl?.testDuration ?: 0.0)
        } else {
            null
        },
        //
        host_cpu = listOfNotNull(iperfUl?.hostCpuTotal, iperfDl?.hostCpuTotal).maxOrNull(),
        remote_cpu = listOfNotNull(iperfUl?.remoteCpuTotal, iperfDl?.remoteCpuTotal).maxOrNull(),
        //
        protocol = resolveProtocol(iperfUl?.protocol, iperfDl?.protocol),
        iperf_ul_json = iperfUlRaw, // optional, normally empty
        iperf_dl_json = iperfDlRaw, // optional, normally empty
    )

    companion object {
        private fun convertIperfTimeToIso(iperfTime: String): String? =
            try {
                val parsed = ZonedDateTime.parse(iperfTime, DateTimeFormatter.RFC_1123_DATE_TIME)
                parsed.toInstant().toString()
            } catch (e: Exception) {
                null
            }

        // calculating jitter according to RFC 3550
        private fun calculateTcpJitterMs(rttTimeline: List<RttPoint>?): Double? {
            if (rttTimeline.isNullOrEmpty() || rttTimeline.size < 2) return null

            var calculatedJitter = 0.0

            for (i in 1 until rttTimeline.size) {
                val currentRtt = rttTimeline[i].rtt
                val previousRtt = rttTimeline[i - 1].rtt

                val delta = kotlin.math.abs(currentRtt - previousRtt)
                calculatedJitter += (delta - calculatedJitter) / 16.0
            }

            // iperf3 RTT values are in microseconds; convert to milliseconds
            return calculatedJitter / 1000.0
        }

        private fun resolveProtocol(
            ulProto: String?,
            dlProto: String?,
        ): String? =
            when {
                ulProto != null && dlProto != null && ulProto == dlProto -> ulProto
                ulProto != null && dlProto == null -> ulProto
                dlProto != null && ulProto == null -> dlProto
                ulProto != null && dlProto != null && ulProto != dlProto -> "MIXED"
                else -> null
            }
    }
}

@Serializable
data class MeasurementRequest(
    val measurements: List<MeasurementItem>,
)

@Serializable
data class LteNetworkInfo(
    val isServing: Boolean,
    val pci: Int,
    val cid: Long?,
    val earfcn: Int,
    val tac: Int?,
    val bands: List<Int>,
    val bandwidth: Int?,
    val rsrp: Int?,
    val rsrq: Int?,
    val rssi: Int?,
    val sinr: Int?,
    val frequencies: Pair<Double, Double>?,
    val duplexMode: String,
)

@Serializable
data class NrNetworkInfo(
    val isServing: Boolean,
    val pci: Int,
    val cid: Long?,
    val nrarfcn: Int,
    val tac: Int?,
    val bands: List<Int>,
    val bandwidth: Int?,
    val ssRsrp: Int?,
    val ssRsrq: Int?,
    val ssSinr: Int?,
    val frequencies: Pair<Double, Double>?,
    val duplexMode: String,
)

@Serializable
data class NetworkInfoData(
    val networkType: String,
    val lteCells: List<LteNetworkInfo>,
    val nrCells: List<NrNetworkInfo>,
)

// dodanie metody conversi "on runtime" bezpośredni do dataclass'u
fun NetworkInfoData.toMeasurementsRequest(
    sessionId: String,
    latitude: Double?,
    longitude: Double?,
    systemData: SystemData,
    iperfUlRaw: String?,
    iperfDlRaw: String?,
    measuredAtNow: Long? = null,
): MeasurementRequest {
    val servingLte = lteCells.find { it.isServing }
    val servingNr = nrCells.find { it.isServing }

    val iperfUlParsed = iperfUlRaw?.let { parseIperfJsonSafe(it) }
    val iperfDlParsed = iperfDlRaw?.let { parseIperfJsonSafe(it) }

    val item = MeasurementItem(
        sessionId = sessionId,
        lat = latitude,
        lon = longitude,
        networkType = this.networkType,
        servingNr = servingNr,
        servingLte = servingLte,
        measuredAt = measuredAtNow ?: System.currentTimeMillis(),
        system = systemData,
        iperfUl = iperfUlParsed,
        iperfDl = iperfDlParsed,
        // iperfUlRaw = iperfUlRaw, // debug leftover
        // iperfDlRaw = iperfDlRaw, // debug leftover
    )

    return MeasurementRequest(measurements = listOf(item))
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE])
fun getLteInfo(
    cell: CellInfoLte,
    currentServiceState: ServiceState?,
): LteNetworkInfo {
    val id = cell.cellIdentity
    val band = id.bands.maxOrNull()
    val signal = cell.cellSignalStrength

    val duplexMode = NetworkConverter.duplexModetoString(currentServiceState?.duplexMode, band)

    return LteNetworkInfo(
        isServing = cell.isRegistered,
        pci = id.pci,
        cid = unavailableToNull(id.ci)?.toLong(),
        earfcn = id.earfcn,
        tac = unavailableToNull(id.tac),
        bands = id.bands.toList(),
        bandwidth = unavailableToNull(id.bandwidth)?.div(1000), // MHz
        rsrp = unavailableToNull(signal.rsrp),
        rsrq = unavailableToNull(signal.rsrq),
        rssi = unavailableToNull(signal.rssi),
        sinr = unavailableToNull(signal.rssnr),
        frequencies = NetworkConverter.calculateLteMhz(id.earfcn, id.bands.firstOrNull()),
        duplexMode = duplexMode,
    )
}

@RequiresPermission(allOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE])
fun getNrInfo(
    cell: CellInfoNr,
    currentServiceState: ServiceState?,
): NrNetworkInfo {
    val id = cell.cellIdentity as CellIdentityNr
    val band = id.bands.maxOrNull()
    val signal = cell.cellSignalStrength as CellSignalStrengthNr

    val cid = if (id.nci != Long.MAX_VALUE && id.nci != 0L) id.nci else null

    val bandwidth = if (cell.isRegistered) {
        NetworkConverter.calculateNrBandwidth(currentServiceState?.cellBandwidths)
    } else {
        null
    }

    val duplexMode = NetworkConverter.duplexModetoString(currentServiceState?.duplexMode, band)

    return NrNetworkInfo(
        isServing = cell.isRegistered,
        pci = id.pci,
        cid = cid,
        nrarfcn = id.nrarfcn,
        tac = unavailableToNull(id.tac),
        bands = id.bands.toList(),
        bandwidth = bandwidth,
        ssRsrp = unavailableToNull(signal.ssRsrp),
        ssRsrq = unavailableToNull(signal.ssRsrq),
        ssSinr = unavailableToNull(signal.ssSinr),
        frequencies = NetworkConverter.calculateNrMhz(id.nrarfcn, id.bands.firstOrNull(), duplexMode),
        duplexMode = duplexMode,
    )
}

fun unavailableToNull(value: Int?): Int? =
    if (value != Int.MAX_VALUE) {
        value
    } else {
        null
    }

object NetworkInfoFetcher {
    private val REQUIRED_PERMISSIONS =
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

    fun hasRequiredPermissions(context: Context): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun getRequiredPermissions(): Array<String> = REQUIRED_PERMISSIONS

    @RequiresPermission(allOf = [Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION])
    suspend fun fetchNetworkInfo(
        tm: TelephonyManager,
        context: Context,
    ): NetworkInfoData =
        suspendCancellableCoroutine { continuation ->

            fetchNetworkInfoSafe(tm, context) { data ->
                if (continuation.isActive) {
                    continuation.resume(data)
                }
            }
        }

    @RequiresPermission(
        anyOf = [Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION],
    )
    fun fetchNetworkInfoSafe(
        tm: TelephonyManager,
        context: Context,
        onResult: (NetworkInfoData) -> Unit,
    ) {
        tm.requestCellInfoUpdate(
            context.mainExecutor,
            object : TelephonyManager.CellInfoCallback() {
                @RequiresPermission(
                    allOf = [
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.READ_PHONE_STATE,
                    ],
                )
                override fun onCellInfo(activeCellInfo: MutableList<CellInfo>) {
                    val lteCells = mutableListOf<LteNetworkInfo>()
                    val nrCells = mutableListOf<NrNetworkInfo>()
                    val serviceState = tm.serviceState

                    for (cell in activeCellInfo) {
                        when (cell) {
                            is CellInfoLte -> lteCells.add(getLteInfo(cell, serviceState))
                            is CellInfoNr -> nrCells.add(getNrInfo(cell, serviceState))
                        }
                    }

                    val networkType =
                        when (tm.dataNetworkType) {
                            TelephonyManager.NETWORK_TYPE_NR -> "5G"
                            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                            else -> "${tm.dataNetworkType}"
                        }

                    onResult(NetworkInfoData(networkType, lteCells, nrCells))
                }

                override fun onError(
                    errorCode: Int,
                    detail: Throwable?,
                ) {
                    onResult(
                        NetworkInfoData(
                            "Cell info error $errorCode:" +
                                " ${detail?.message}",
                            lteCells = emptyList(),
                            nrCells = emptyList(),
                        ),
                    )
                }
            },
        )
    }
}
