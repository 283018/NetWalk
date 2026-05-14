package edu.pwr.zpi.netwalk.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.pwr.zpi.netwalk.ui.IperfLogScreen
import edu.pwr.zpi.netwalk.ui.NetworkInfoScreen
import edu.pwr.zpi.netwalk.ui.NetworkViewModel
import edu.pwr.zpi.netwalk.ui.SettingsScreen

data class IperfLogEntry(
    val timestamp: String,
    val throughputMbps: Double?,
    val meanRtt: Double?,
    val retransmits: Long?,
)

@Composable
fun IperfLogScreen(
    viewModel: NetworkViewModel,
    onNavigateBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "iPerf Logs",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Text(
            text = "Log entries: ${viewModel.iperfLogEntries.size}",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            items(viewModel.iperfLogEntries.reversed()) { entry ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "↓ ${entry.throughputMbps?.let { "%.2f Mbps".format(it) } ?: "-"}",
                            color = Color.Green,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "RTT ${entry.meanRtt?.let { "%.1f ms".format(it) } ?: "-"}",
                            color = Color.Cyan,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "Retx ${entry.retransmits ?: 0}",
                            color = Color.Yellow,
                            fontSize = 12.sp,
                        )
                    }
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
                }
            }
        }

        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text("<- Back to Network Info")
        }
    }
}
