package edu.pwr.zpi.netwalk.ui

import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoFetcher

@Composable
fun NetworkInfoScreen(tm: TelephonyManager) {
    var networkText by remember { mutableStateOf("Checking permissions...") }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Żadanie pozwoleń
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            val allGranted = NetworkInfoFetcher.getRequiredPermissions().all { results[it] == true }
            networkText =
                if (allGranted) {
                    val info = NetworkInfoFetcher.fetchNetworkInfo(tm)
                    formatNetworkInfo(info)
                } else {
                    "Permission denied"
                }
        }

    if (!NetworkInfoFetcher.hasRequiredPermissions(context)) {
        SideEffect {
            permissionLauncher.launch(NetworkInfoFetcher.getRequiredPermissions())
        }
    } else {
        val info = NetworkInfoFetcher.fetchNetworkInfo(tm)
        networkText = formatNetworkInfo(info)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = networkText,
            color = Color.White,
        )
    }
}

private fun formatNetworkInfo(data: NetworkInfoData): String = "Network type: ${data.networkType} \nCell info: ${data.cellInfo}"
