package edu.pwr.zpi.netwalk.fetcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.coroutines.resume

@Serializable
data class MeasurementItem(
    val session_id: String,
    val imsi: String,
    val imei: String? = null,
    val measured_at: String, // using ISO 8901 as datetime
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rsrp: Int? = null,
    val sinr: Int? = null,
    val network_type: String? = null,
    val cell_id: String? = null,
    val battery_level: Int? = null,
    val processor_temp: Double? = null,
    val os_version: String? = "Android ${Build.VERSION.RELEASE}",
    val throughput_mbps: Double? = null,
    val test_start_time: String? = null,
    val test_end_time: String? = null,
)

@Serializable
data class MeasurementRequest(
    val measurements: List<MeasurementItem>,
)

@Serializable
data class LteNetworkInfo(
    val isServing: Boolean,
    val pci: Int,
    val earfcn: Int,
    val tac: Int,
    val bands: List<Int>,
    val rsrp: Int,
    val rsrq: Int,
    val rssi: Int,
    val sinr: Int,
)

@Serializable
data class NrNetworkInfo(
    val isServing: Boolean,
    val pci: Int,
    val nrarfcn: Int,
    val tac: Int,
    val bands: List<Int>,
    val ssRsrp: Int,
    val ssRsrq: Int,
    val ssSinr: Int,
)

@Serializable
data class NetworkInfoData(
    val networkType: String,
    val lteCells: List<LteNetworkInfo>,
    val nrCells: List<NrNetworkInfo>,
)

// dodanie metody conversi "on runtime" bezpośredni do dataclass'u
fun NetworkInfoData.toMeasurementsRequest(
    latitude: Double?,
    longitude: Double?,
): MeasurementRequest {
    val servingLte = lteCells.find { it.isServing }
    val servingNr = nrCells.find { it.isServing }

    val item = MeasurementItem(
        session_id = "550e8400-e29b-41d4-a716-446655440000", // hardcoded for tests
        imsi = "1234567890987654321",
        measured_at = Instant.now().toString(),
        latitude = latitude,
        longitude = longitude,
        network_type = this.networkType,
        rsrp = servingNr?.ssRsrp ?: servingLte?.rsrp,
        sinr = servingNr?.ssSinr ?: servingLte?.sinr,
        cell_id = servingLte?.pci?.toString() ?: servingNr?.pci?.toString(),
    )

    return MeasurementRequest(measurements = listOf(item))
}

fun getLteInfo(cell: CellInfoLte): LteNetworkInfo {
    val id = cell.cellIdentity
    val signal = cell.cellSignalStrength

    return LteNetworkInfo(
        isServing = cell.isRegistered,
        pci = id.pci,
        earfcn = id.earfcn,
        tac = id.tac,
        bands = id.bands.toList(),
        rsrp = signal.rsrp,
        rsrq = signal.rsrq,
        rssi = signal.rssi,
        sinr = signal.rssnr,
    )
}

fun getNrInfo(cell: CellInfoNr): NrNetworkInfo {
    val id = cell.cellIdentity as CellIdentityNr
    val signal = cell.cellSignalStrength as CellSignalStrengthNr

    return NrNetworkInfo(
        isServing = cell.isRegistered,
        pci = id.pci,
        nrarfcn = id.nrarfcn,
        tac = id.tac,
        bands = id.bands.toList(),
        ssRsrp = signal.ssRsrp,
        ssRsrq = signal.ssRsrq,
        ssSinr = signal.ssSinr,
    )
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
                @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
                override fun onCellInfo(activeCellInfo: MutableList<CellInfo>) {
                    val lteCells = mutableListOf<LteNetworkInfo>()
                    val nrCells = mutableListOf<NrNetworkInfo>()

                    for (cell in activeCellInfo) {
                        when (cell) {
                            is CellInfoLte -> lteCells.add(getLteInfo(cell))
                            is CellInfoNr -> nrCells.add(getNrInfo(cell))
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
