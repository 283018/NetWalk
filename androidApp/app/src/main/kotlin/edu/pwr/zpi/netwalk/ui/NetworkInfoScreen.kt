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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

private val BackgroundColor = Color(0xFF121212)
private val SurfaceColor = Color(0xFF1E1E1E)
private val CardColor = Color(0xFF252525)
private val PrimaryColor = Color(0xFFD0BCFF)
private val OutlineColor = Color(0xFF938F99)

@Composable
fun NetworkInfoScreen(
    tm: TelephonyManager,
    viewModel: NetworkViewModel = _viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToIperf: () -> Unit = {},
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
    // LaunchedEffect(hasPermission) {
    //     if (hasPermission) {
    //         viewModel.startCollection(tm, context)
    //     }
    // }

    // Główny ekran
    Column(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceColor,
            shadowElevation = 8.dp,
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
                    color = Color.White,
                )
                IconButton(onClick = onNavigateToSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            OutlinedButton(onClick = onNavigateToIperf) {
                Text("iPerf")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { viewModel.startCollection(tm, context) },
                enabled = !viewModel.isCollecting,
                modifier = Modifier.weight(1f),
            ) {
                Text("Start")
            }
            Button(
                onClick = { viewModel.stopCollection() },
                enabled = viewModel.isCollecting,
                modifier = Modifier.weight(1f),
            ) {
                Text("Stop")
            }
        }

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

            Text(
                "Active connections",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            networkData?.nrCells?.filter { it.isServing }?.forEach { NrCellCard(it) }
            networkData?.lteCells?.filter { it.isServing }?.forEach { LteCellCard(it) }

            LocationCard(lat = latitude, lon = longitude)

            val neighborsCount = (networkData?.lteCells?.count { !it.isServing } ?: 0) +
                (networkData?.nrCells?.count { !it.isServing } ?: 0)

            if (neighborsCount > 0) {
                Text(
                    "Neighbouring stations ($neighborsCount)",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                )
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
    val freqs = cell.frequencies
    val dlDisplay = freqs?.first?.let { "%.1f MHz".format(it) } ?: "-"
    val ulDisplay = freqs?.second?.let { "%.1f MHz".format(it) } ?: "-"

    val rsrpDisplay = cell.ssRsrp?.let { "$it dBm" } ?: "-"
    val rsrqDisplay = cell.ssRsrq?.let { "$it dB" } ?: "-"
    val sinrDisplay = cell.ssSinr?.let { "$it dB" } ?: "-"
    val tacDisplay = cell.tac?.toString() ?: "-"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (cell.isServing) Color(0xFF388E3C) else Color(0xFF555555),
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
                Text("PCI: ${cell.pci}", fontWeight = FontWeight.Bold, color = Color.White)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.Gray)
            ParameterGrid(
                listOf(
                    "NR-ARFCN(Band)" to "${cell.nrarfcn} (${cell.bands.joinToString()})",
                    "Duplex mode" to cell.duplexMode,
                    "TAC" to tacDisplay,
                    "SS-RSRP" to rsrpDisplay,
                    "SS-RSRQ" to rsrqDisplay,
                    "SS-SINR" to sinrDisplay,
                    "Freq DL" to dlDisplay,
                    "Freq UL" to ulDisplay,
                ),
            )
        }
    }
}

@Composable
fun LteCellCard(cell: LteNetworkInfo) {
    val freqs = cell.frequencies
    val dlDisplay = freqs?.first?.let { "%.1f MHz".format(it) } ?: "-"
    val ulDisplay = freqs?.second?.let { "%.1f MHz".format(it) } ?: "-"

    val rsrpDisplay = cell.rsrp?.let { "$it dBm" } ?: "-"
    val rssiDisplay = cell.rssi?.let { "$it dBm" } ?: "-"
    val rsrqDisplay = cell.rsrq?.let { "$it dB" } ?: "-"
    val sinrDisplay = cell.sinr?.let { "$it dB" } ?: "-"
    val tacDisplay = cell.tac?.toString() ?: "-"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (cell.isServing) Color(0xFF1976D2) else Color(0xFF555555),
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
                Text("PCI: ${cell.pci}", fontWeight = FontWeight.Bold, color = Color.White)
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = Color.Gray)
            ParameterGrid(
                listOf(
                    "EARFCN" to cell.earfcn.toString(),
                    "Duplex mode" to cell.duplexMode,
                    "Band" to cell.bands.joinToString(),
                    "TAC" to tacDisplay,
                    "RSRP" to rsrpDisplay,
                    "RSSI" to rssiDisplay,
                    "RSRQ" to rsrqDisplay,
                    "SINR" to sinrDisplay,
                    "Freq DL" to dlDisplay,
                    "Freq UL" to ulDisplay,
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
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceColor),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Technology", style = MaterialTheme.typography.labelMedium, color = OutlineColor)
                Text(
                    networkType, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold,
                    color = PrimaryColor,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Battery state", fontWeight = FontWeight.Bold, color = Color.White,
                    modifier = Modifier.padding(horizontal = 23.dp, vertical = 8.dp),
                )
                Text(
                    "Level: $batteryLevel%  Temp: $batteryTemp°C", style = MaterialTheme.typography.bodySmall,
                    color = OutlineColor,
                )
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
                            color = OutlineColor,
                        )
                        Text(
                            value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
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
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineColor),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.LocationOn, contentDescription = null, tint = PrimaryColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (lat != null) {
                    "Latitude: %.5f, Longitude: %.5f".format(lat, lon)
                } else {
                    "Waiting for GPS..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
            )
        }
    }
}

@Composable
fun StatusFooter(status: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceColor,
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(8.dp).navigationBarsPadding(),
            style = MaterialTheme.typography.labelSmall,
            color = if (status.startsWith("Error")) Color(0xFFFFB4AB) else Color(0xFF81C784),
            maxLines = 1,
        )
    }
}

@Composable
fun ErrorMessage(msg: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF370001),
                RoundedCornerShape(8.dp),
            ).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB4AB),
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(msg, color = Color(0xFFFFB4AB), fontSize = 11.sp)
    }
}
