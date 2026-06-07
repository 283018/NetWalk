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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.pwr.zpi.netwalk.iperf.ThroughputPoint
import edu.pwr.zpi.netwalk.ui.IperfLogScreen
import edu.pwr.zpi.netwalk.ui.NetworkInfoScreen
import edu.pwr.zpi.netwalk.ui.NetworkViewModel
import edu.pwr.zpi.netwalk.ui.SettingsScreen

data class IperfLogEntry(
    val timestamp: String,
    val ulthroughputMbps: Double?,
    val dlthroughputMbps: Double?,
    val meanRtt: Double?,
    val retransmits: Long?,
)

val dlColor = Color(0xFF2196F3)

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

        val lastUlTimeline = viewModel.lastUlTimeline
        val lastDlTimeline = viewModel.lastDlTimeline

        if (lastDlTimeline.isNotEmpty() || lastUlTimeline.isNotEmpty()) {
            Text(
                text = "Ostatni test - przepustowość w czasie:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (lastUlTimeline.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Wysyłanie (Uplink) ↑", style = MaterialTheme.typography.labelSmall, color = Color.Green)
                        IperfMiniChart(
                            points = lastUlTimeline,
                            lineColor = Color.Green,
                            modifier = Modifier.fillMaxWidth().height(130.dp),
                        )
                    }
                }

                if (lastDlTimeline.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Pobieranie (Downlink) ↓", style = MaterialTheme.typography.labelSmall, color = dlColor)
                        IperfMiniChart(
                            points = lastDlTimeline,
                            lineColor = dlColor,
                            modifier = Modifier.fillMaxWidth().height(130.dp),
                        )
                    }
                }
            }
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
                            text = "↑ ${entry.ulthroughputMbps?.let { "%.2f Mbps".format(it) } ?: "-"}",
                            color = Color.Green,
                            fontSize = 12.sp,
                        )
                        Text(
                            text = "↓ ${entry.dlthroughputMbps?.let { "%.2f Mbps".format(it) } ?: "-"}",
                            color = dlColor,
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
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val maxSeconds = points.maxOf { it.seconds }.toFloat()
    val maxThroughput = (points.maxOf { it.throughputMbps } * 1.1f).toFloat().coerceAtLeast(1f)

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = Color.LightGray.copy(alpha = 0.7f),
        fontSize = 10.sp,
    )

    Canvas(
        modifier = modifier
            .background(Color.Black)
            .padding(8.dp),
    ) {
        val width = size.width
        val height = size.height

        val gridColor = Color.DarkGray.copy(alpha = 0.5f)

        val paddingLeft = 55.dp.toPx()
        val paddingBottom = 25.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val paddingRight = 10.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        for (i in 0..4) {
            val factor = i.toFloat() / 4
            val y = paddingTop + chartHeight - (factor * chartHeight)
            val valueMbps = factor * maxThroughput

            drawLine(gridColor, Offset(paddingLeft, y), Offset(paddingLeft + chartWidth, y), strokeWidth = 1f)

            val textLayoutResult = textMeasurer.measure(
                text = "${valueMbps.toInt()} Mbps",
                style = labelStyle,
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = paddingLeft - textLayoutResult.size.width - 6.dp.toPx(),
                    y = y - (textLayoutResult.size.height / 2),
                ),
            )
        }

        for (i in 0..maxSeconds.toInt()) {
            val factor = i.toFloat() / maxSeconds.toInt()
            val x = paddingLeft + (factor * chartWidth)
            val valueSeconds = factor * maxSeconds

            drawLine(gridColor, Offset(x, paddingTop), Offset(x, paddingTop + chartHeight), strokeWidth = 1f)

            val textLayoutResult = textMeasurer.measure(
                text = "${valueSeconds.toInt()}s",
                style = labelStyle,
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = x - (textLayoutResult.size.width / 2),
                    y = paddingTop + chartHeight + 4.dp.toPx(),
                ),
            )
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = paddingLeft + (point.seconds / maxSeconds) * chartWidth
            val y = paddingTop + chartHeight - ((point.throughputMbps / maxThroughput) * chartHeight)

            if (index == 0) {
                path.moveTo(x.toFloat(), y.toFloat())
            } else {
                path.lineTo(x.toFloat(), y.toFloat())
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f),
        )
    }
}
