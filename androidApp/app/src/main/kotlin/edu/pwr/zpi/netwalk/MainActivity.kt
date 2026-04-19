package edu.pwr.zpi.netwalk

import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import edu.pwr.zpi.netwalk.ui.NetworkInfoScreen

class MainActivity : ComponentActivity() {
    lateinit var tm: TelephonyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        setContent {
            MaterialTheme {
                NetworkInfoScreen(tm)
            }
        }
    }
}
