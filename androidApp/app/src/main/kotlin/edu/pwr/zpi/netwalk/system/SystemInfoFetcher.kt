package edu.pwr.zpi.netwalk.system

import android.annotation.SuppressLint
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import edu.pwr.zpi.netwalk.logD

data class SystemData(
    val android_id: String?,
    val battery_level: Int,
    val battery_temp: Double?,
    val os_version: String = "Android ${Build.VERSION.RELEASE}",
)

object SystemInfoFetcher {
    fun fetchFullSystemInfo(context: Context): SystemData {
        val data = SystemData(
            android_id = getAndroidId(context),
            battery_level = getBatteryLevel(context),
            battery_temp = getBatteryTemp(context),
        )
        logD(
            """
            [SystemInfoFetcher: fetchFullSystemInfo] android_id=${data.android_id}, battery_level=${data.battery_level}, battery_temp=${data.battery_temp}
            """.trimIndent(),
        )
        return data
    }

    /* Android ID - 64 bitowy identyfikator unikalny dla każdego użytkownika i każdej aplikacji - może się zmienić
       jedynie, jeśli użytkownik zresetuje telefon do ustawień fabrycznych

       IMSI i IMEI są niedostępne bez specjalnych uprawnień, które mogą otrzymać:
              - aplikacje preinstalowane przez producenta telefonu
              - aplikacje z tym samym kluczem co system operacyjny
              - aplikacje operatora komórkowego
     */
    @SuppressLint("HardwareIds")
    fun getAndroidId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "Unknown"

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
