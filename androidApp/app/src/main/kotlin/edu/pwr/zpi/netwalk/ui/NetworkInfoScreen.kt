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
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.pwr.zpi.netwalk.fetcher.LteNetworkInfo
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoFetcher
import edu.pwr.zpi.netwalk.fetcher.NrNetworkInfo
import edu.pwr.zpi.netwalk.ui.NetworkViewModel

@Composable
fun NetworkInfoScreen(
    tm: TelephonyManager,
    viewModel: NetworkViewModel = androidx.lifecycle.viewmodel.compose
        .viewModel(),
) {
    var data = viewModel.uiState
    val context = androidx.compose.ui.platform.LocalContext.current

    var hasPermission by remember {
        mutableStateOf(NetworkInfoFetcher.hasRequiredPermissions(context))
    }

    // Żadanie pozwoleń
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
        ) { results ->
            hasPermission = results.values.all { it }
        }

    // jednoktotne rządanie pozwoleń
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(NetworkInfoFetcher.getRequiredPermissions())
        }
    }

    // LaunchedEffect potrzebny zamiast SideEffect - SideEffect wywołuje się po każdej rekompozycji
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.startCollection(tm, context)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        // tworzymy text jeżeli występuje błąd w ui
        if (!hasPermission) {
            Text(text = "Permission denied", color = Color.Red)
        }

        Text(
            text = "Network: ${data?.networkType ?: "..."}",
            color = Color.White,
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

        // tekst poniżej jest tylko do sprawdzenia połączenia
        Text(
            text = viewModel.lastStatus,
            color = if (viewModel.lastStatus.startsWith("Error")) Color.Red else Color.Green,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun LteCellView(
    cell: LteNetworkInfo,
    color: Color = Color.White,
) {
    Text(
        text =
            buildString {
                append(if (cell.isServing) "LTE (Serving)\n" else "LTE (Neighbor)\n")
                append("PCI: ${cell.pci}\n")
                append("EARFCN: ${cell.earfcn}\n")
                append("TAC: ${cell.tac}\n")
                append("Band number: ${cell.bands.joinToString()}\n")
                append("RSRP: ${cell.rsrp} dBm\n")
                append("RSRQ: ${cell.rsrq} dB\n")
                append("RSSI: ${cell.rssi} dBm\n")
                append("SINR: ${cell.sinr} dB\n")
            },
        color = color,
    )
}

@Composable
fun NrCellView(
    cell: NrNetworkInfo,
    color: Color = Color.White,
) {
    Text(
        text =
            buildString {
                append(if (cell.isServing) "5G (Serving)\n" else "5G (Neighbor)\n")
                append("PCI: ${cell.pci}\n")
                append("NR-ARFCN: ${cell.nrarfcn}\n")
                append("TAC: ${cell.tac}\n")
                append("Band number: ${cell.bands.joinToString()}\n")
                append("RSRP: ${cell.ssRsrp}\n")
                append("RSRQ: ${cell.ssRsrq}\n")
                append("SINR: ${cell.ssSinr}\n")
            },
        color = color,
    )
}
