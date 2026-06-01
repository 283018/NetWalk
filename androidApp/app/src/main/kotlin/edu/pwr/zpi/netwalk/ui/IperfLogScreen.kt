package edu.pwr.zpi.netwalk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.pwr.zpi.netwalk.iperf.ThroughputPoint
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
fun IperfLogScreen(viewModel: NetworkViewModel) {
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
    ) {
        Text(
            text = "iPerf Logs",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Button(
            onClick = { viewModel.requestIperfNow() },
            enabled = viewModel.isCollecting,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            Text("Run Test Now")
        }

        val lastTimeline = viewModel.lastTestTimeline

        if (lastTimeline.isNotEmpty()) {
            Text(
                text = "Ostatni test - przepustowość w czasie:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            IperfMiniChart(
                points = lastTimeline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(bottom = 16.dp),
            )
        }

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
    }
}

@Composable
fun IperfMiniChart(
    points: List<ThroughputPoint>,
    modifier: Modifier = Modifier,
) {
    val maxThroughput = points.maxOfOrNull { it.throughputMbps }?.takeIf { it > 0 } ?: 1.0
    val maxSeconds = points.maxOfOrNull { it.seconds }?.takeIf { it > 0 } ?: 1.0

    Canvas(
        modifier = modifier
            .background(Color.Black)
            .padding(8.dp),
    ) {
        val width = size.width
        val height = size.height

        val gridColor = Color.DarkGray.copy(alpha = 0.5f)
        drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, height / 2), Offset(width, height / 2), strokeWidth = 1f)
        drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = (point.seconds / maxSeconds) * width
            val y = height - ((point.throughputMbps / maxThroughput) * height)

            if (index == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }

        drawPath(
            path = path,
            color = Color.Green,
            style = Stroke(width = 4f),
        )
    }
}
