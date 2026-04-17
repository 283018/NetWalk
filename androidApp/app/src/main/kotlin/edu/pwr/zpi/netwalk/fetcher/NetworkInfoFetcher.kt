package edu.pwr.zpi.netwalk.fetcher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class NetworkInfoData(
    val networkType: String,
    val cellInfo: String,
)

object NetworkInfoFetcher {
    private val REQUIRED_PERMISSIONS =
        arrayOf(
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
        )

    fun hasRequiredPermissions(context: Context): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    fun getRequiredPermissions(): Array<String> = REQUIRED_PERMISSIONS

    fun fetchNetworkInfo(tm: TelephonyManager): NetworkInfoData {
        val networkType =
            when (tm.dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                else -> "${tm.dataNetworkType}"
            }
        val cellInfo = tm.allCellInfo?.toString() ?: "null"
        return NetworkInfoData(networkType, cellInfo)
    }
}
