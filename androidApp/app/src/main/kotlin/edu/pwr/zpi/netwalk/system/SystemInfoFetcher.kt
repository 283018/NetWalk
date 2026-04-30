package edu.pwr.zpi.netwalk.system

import android.content.Context
import android.os.BatteryManager
import android.os.Build

data class SystemData(
    val battery_level: Int,
    val battery_temp: Double?,
    val os_version: String = "Android ${Build.VERSION.RELEASE}",
)

object SystemInfoFetcher {
    fun fetchFullSystemInfo(context: Context): SystemData =
        SystemData(
            battery_level = getBatteryLevel(context),
            battery_temp = getBatteryTemp(context),
        )

    /* Identifiers in sdk >29 are not easily acquireable
    fun getImei(context: Context): String? {

    }


    fun getImsi(context: Context): String {

    }*/

    fun getBatteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getBatteryTemp(context: Context): Double? {
        val intent = context.registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED),
        )

        val temp = intent?.getIntExtra(
            BatteryManager.EXTRA_TEMPERATURE,
            0,
        ) ?: 0
        return if (temp > 0) temp.toDouble() / 10 else null // Returns battery temperature in Celsius
    }
}
