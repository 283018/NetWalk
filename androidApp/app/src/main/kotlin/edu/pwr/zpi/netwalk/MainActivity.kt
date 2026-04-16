package edu.pwr.zpi.netwalk

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.TelephonyManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

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

@Composable
fun NetworkInfoScreen(tm: TelephonyManager) {
    var networkText by remember { mutableStateOf("Checking permissions...") }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Potrzebujemy pytać się o dwa zezwolenia (dla cellInfo) - READ_PHONE_STATE i ACCES_FINE_LOCATION
    val permissions =
        arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

    // Żadanie pozwoleń
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            // Jeżeli nie ma klucza (?: false) traktujemy prośbę jako odrzuconą
            val readGranted = results[Manifest.permission.READ_PHONE_STATE] ?: false
            val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            networkText =
                if (readGranted && locationGranted) {
                    val networkType = tm.dataNetworkType
                    val cellInfo = tm.allCellInfo
                    val typeText =
                        when (networkType) {
                            TelephonyManager.NETWORK_TYPE_NR -> "5G"
                            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                            else -> "$networkType"
                        }
                    "Network type: $typeText\nCell info: $cellInfo"
                } else {
                    "Permissions denied"
                }
        }

    val allGranted =
        permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    if (!allGranted) {
        SideEffect {
            permissionLauncher.launch(permissions)
        }
    } else {
        val networkType = tm.dataNetworkType
        val cellInfo = tm.allCellInfo
        val typeText =
            when (networkType) {
                TelephonyManager.NETWORK_TYPE_NR -> "5G"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                else -> "$networkType"
            }
        networkText = "Network type: $typeText\nCell info: $cellInfo"
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = networkText,
            color = Color.White,
        )
    }
}
