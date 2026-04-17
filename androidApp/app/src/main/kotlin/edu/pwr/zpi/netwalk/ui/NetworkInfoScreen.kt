package edu.pwr.zpi.netwalk.ui

import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import edu.pwr.zpi.netwalk.fetcher.LteNetworkInfo
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoData
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoFetcher
import edu.pwr.zpi.netwalk.fetcher.NrNetworkInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun NetworkInfoScreen(tm: TelephonyManager) {
    var data by remember { mutableStateOf<NetworkInfoData?>(null) }
    var errorText by remember {mutableStateOf<String?>(null)}
    var hasPermission by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    // Żadanie pozwoleń
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            hasPermission = NetworkInfoFetcher
                .getRequiredPermissions()
                .all { results[it] == true }

            if (!hasPermission) {
                errorText = "Permission denied"
            }
        }

    // LaunchedEffect potrzebny zamiast SideEffect - SideEffect wywołuje się po każdej rekompozycji
    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            permissionLauncher.launch(NetworkInfoFetcher.getRequiredPermissions())
            return@LaunchedEffect
        }

        while (isActive) {

            NetworkInfoFetcher.fetchNetworkInfo(tm, context) {
                data = it
            }

            delay(5000)
        }
    }


    Column(modifier = Modifier.padding(16.dp)) {

        errorText?.let {
            Text(text = it, color = Color.Red)
        }

        Text(
            text = "Network: ${data?.networkType ?: "..."}",
            color = Color.White
        )

        Text("  5G NR  ", color = Color.White)

        data?.nrCells?.forEach {
            NrCellView(it)
            Spacer(modifier = Modifier.height(8.dp))
        }


        Text(" LTE ", color = Color.White)

        data?.lteCells?.forEach {
            LteCellView(it)
            Spacer(modifier = Modifier.height(8.dp))
        }

    }
}

@Composable
fun LteCellView(cell: LteNetworkInfo, color: Color = Color.White) {
    Text(
        text = buildString {
            append(if (cell.isServing) "LTE (Serving)\n" else "LTE (Neighbor)\n")
            append("PCI: ${cell.pci}\n")
            append("EARFCN: ${cell.earfcn}\n")
            append("Bandwith: ${cell.bandwidth}\n")
            append("RSRP: ${cell.rsrp} dBm\n")
            append("RSRQ: ${cell.rsrq} dB\n")
            append("SINR: ${cell.sinr} dB\n")
        },
        color = color
    )
}

@Composable
fun NrCellView(cell: NrNetworkInfo, color: Color = Color.White) {
    Text(
        text = buildString {
            append(if (cell.isServing) "5G (Serving)\n" else "5G (Neighbor)\n")
            append("PCI: ${cell.pci}\n")
            append("NR-ARFCN: ${cell.nrarfcn}\n")
            append("Band: ${cell.bands.joinToString()}\n")
            append("RSRP: ${cell.ssRsrp}\n")
            append("RSRQ: ${cell.ssRsrq}\n")
            append("SINR: ${cell.ssSinr}\n")
        },
        color = color
    )
}