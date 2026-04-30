package edu.pwr.zpi.netwalk.system

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.HardwarePropertiesManager

data class SystemData(
    val battery_level: Int,
    val processor_temp: Double?,
    val os_version: String = "Android ${Build.VERSION.RELEASE}",
)

object SystemInfoFetcher {
    fun fetchFullSystemInfo(context: Context): SystemData =
        SystemData(
            battery_level = getBatteryLevel(context),
            processor_temp = getProcessorTemp(context),
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

    fun getProcessorTemp(context: Context): Double? {
        val hpm = context.getSystemService(Context.HARDWARE_PROPERTIES_SERVICE) as HardwarePropertiesManager
        val temps = hpm.getDeviceTemperatures(
            HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
            HardwarePropertiesManager.TEMPERATURE_CURRENT,
        )
        return if (temps.isNotEmpty()) {
            temps[0].toDouble()
        } else {
            null
        }
    }
}
