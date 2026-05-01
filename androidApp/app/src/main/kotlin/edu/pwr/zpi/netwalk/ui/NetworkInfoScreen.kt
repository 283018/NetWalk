package edu.pwr.zpi.netwalk.ui

import android.telephony.TelephonyManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.pwr.zpi.netwalk.fetcher.LteNetworkInfo
import edu.pwr.zpi.netwalk.fetcher.NetworkInfoFetcher
import edu.pwr.zpi.netwalk.fetcher.NrNetworkInfo
import androidx.lifecycle.viewmodel.compose.viewModel as _viewModel

@Composable
fun NetworkInfoScreen(
    tm: TelephonyManager,
    viewModel: NetworkViewModel = _viewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val networkData = viewModel.uiStateNetwork
    val (latitude, longitude) = viewModel.uiStateLocation
    val systemData = viewModel.uiStateSystem
    val context = androidx.compose.ui.platform.LocalContext.current
    val scrollState = rememberScrollState()

    var hasPermission by remember { mutableStateOf(NetworkInfoFetcher.hasRequiredPermissions(context)) }

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

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Netwalk Monitor",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }

        // Główna część ekranu
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hasPermission) {
                ErrorMessage("Missing permissions.")
            }

            HeaderCard(
                networkType = networkData?.networkType ?: "No data",
                batteryLevel = systemData?.battery_level ?: 0,
                batteryTemp = systemData?.battery_temp,
            )

            Text("Active connections", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            networkData?.nrCells?.filter { it.isServing }?.forEach { NrCellCard(it) }
            networkData?.lteCells?.filter { it.isServing }?.forEach { LteCellCard(it) }

            LocationCard(lat = latitude, lon = longitude)

            val neighborsCount = (networkData?.lteCells?.count { !it.isServing } ?: 0) +
                (networkData?.nrCells?.count { !it.isServing } ?: 0)

            if (neighborsCount > 0) {
                Text("Neighbouring stations ($neighborsCount)", style = MaterialTheme.typography.titleMedium)
                networkData?.nrCells?.filter { !it.isServing }?.forEach { NrCellCard(it) }
                networkData?.lteCells?.filter { !it.isServing }?.forEach { LteCellCard(it) }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        StatusFooter(status = viewModel.lastStatus)
    }
}

@Composable
fun NrCellCard(cell: NrNetworkInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (cell.isServing) Color(0xFF4CAF50) else Color.Gray,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        "5G",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("PCI: ${cell.pci}", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            ParameterGrid(
                listOf(
                    "NR-ARFCN(Band)" to "${cell.nrarfcn}(${cell.bands.joinToString()})",
                    "TAC" to "${cell.tac}",
                    "SS-RSRP" to "${cell.ssRsrp} dBm",
                    "SS-RSRQ" to "${cell.ssRsrq} dB",
                    "SS-SINR" to "${cell.ssSinr} dB",
                ),
            )
        }
    }
}

@Composable
fun LteCellCard(cell: LteNetworkInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (cell.isServing) Color(0xFF2196F3) else Color.Gray,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        "LTE",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("PCI: ${cell.pci}", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            ParameterGrid(
                listOf(
                    "EARFCN" to cell.earfcn.toString(),
                    "Band" to cell.bands.joinToString(),
                    "RSRP" to "${cell.rsrp} dBm",
                    "RSSI" to "${cell.rssi} dBm",
                    "SINR" to "${cell.sinr} dB",
                ),
            )
        }
    }
}

@Composable
fun HeaderCard(
    networkType: String,
    batteryLevel: Int,
    batteryTemp: Double?,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Technology", style = MaterialTheme.typography.labelMedium)
                Text(
                    networkType, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Battery state", fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 23.dp, vertical = 8.dp),
                )

                Text("Level: $batteryLevel%  Temp: $batteryTemp°C", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun ParameterGrid(params: List<Pair<String, String>>) {
    Column {
        params.chunked(2).forEach { rowParams ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowParams.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                        Text(
                            label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LocationCard(
    lat: Double?,
    lon: Double?,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (lat != null) {
                    "Latitude: %.5f, Longitude: %.5f".format(lat, lon)
                } else {
                    "Waiting for GPS..."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun StatusFooter(status: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(8.dp).navigationBarsPadding(),
            style = MaterialTheme.typography.labelSmall,
            color = if (status.startsWith("Error")) Color.Red else Color(0xFF388E3C),
            maxLines = 1,
        )
    }
}

@Composable
fun ErrorMessage(msg: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFFFEBEE), RoundedCornerShape(8.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(msg, color = Color.Red, fontSize = 11.sp)
    }
}
