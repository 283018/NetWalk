package edu.pwr.zpi.netwalk.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BackgroundColor = Color(0xFF121212)
private val SurfaceColor = Color(0xFF1E1E1E)
private val CardColor = Color(0xFF252525)

@Composable
fun ChartsScreen(viewModel: NetworkViewModel) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = SurfaceColor,
            shadowElevation = 8.dp,
        ) {
            Text(
                text = "Signal Analysis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChartContainer(title = "RSRP", currentValue = viewModel.rsrpHistory.lastOrNull(), unit = "dBm") {
                SignalChart(
                    history = viewModel.rsrpHistory, unit = "dBm", lineColor = Color.Magenta,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
            }

            ChartContainer(title = "RSRQ", currentValue = viewModel.rsrqHistory.lastOrNull(), unit = "dB") {
                SignalChart(
                    history = viewModel.rsrqHistory, unit = "dB", lineColor = Color.White,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
            }

            ChartContainer(title = "SINR", currentValue = viewModel.sinrHistory.lastOrNull(), unit = "dB") {
                SignalChart(
                    history = viewModel.sinrHistory, unit = "dB", lineColor = Color.Green,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ChartContainer(
    title: String,
    currentValue: Float?,
    unit: String,
    chartBlock: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val valueText = currentValue?.let { "${it.toInt()} $unit" } ?: "No data"
            Text(
                text = "$title: $valueText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp)),
            ) {
                chartBlock()
            }
        }
    }
}

@Composable
fun SignalChart(
    history: List<Float>,
    unit: String,
    lineColor: Color,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = Color.White.copy(alpha = 0.6f),
        fontSize = 9.sp,
    )

    val maxVal = history.maxOf { it }
    val minVal = history.minOf { it }

    val currentValue = history.last()

    val (yMin, yMax) =
        if (maxVal == minVal) {
            currentValue - 4f to currentValue + 4f
        } else {
            val range = maxVal - minVal
            (minVal - range * 0.1f) to (maxVal + range * 0.1f)
        }

    Canvas(
        modifier = modifier
            .background(Color.Black)
            .padding(4.dp),
    ) {
        val totalWidth = size.width
        val totalHeight = size.height

        val paddingLeft = 60.dp.toPx()
        val paddingBottom = 20.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val paddingRight = 15.dp.toPx()

        val chartWidth = totalWidth - paddingLeft - paddingRight
        val chartHeight = totalHeight - paddingTop - paddingBottom
        val gridColor = Color.DarkGray.copy(alpha = 0.3f)

        for (i in 0..4) {
            val factor = i.toFloat() / 4
            val y = paddingTop + chartHeight - (factor * chartHeight)
            val currentVal = yMin + (factor * (yMax - yMin))

            drawLine(gridColor, Offset(paddingLeft, y), Offset(paddingLeft + chartWidth, y), strokeWidth = 1f)

            val textLayout = textMeasurer.measure("${currentVal.toInt()} $unit", style = labelStyle)
            drawText(
                textLayout,
                topLeft = Offset(
                    x = paddingLeft - textLayout.size.width - 6.dp.toPx(),
                    y = y - (textLayout.size.height / 2),
                ),
            )
        }

        val totalPoints = history.size

        val path = Path()
        history.forEachIndexed { index, point ->
            val xFactor = index.toFloat() / (totalPoints - 1).coerceAtLeast(1)
            val x = paddingLeft + (xFactor * chartWidth)

            val yFactor = (point - yMin) / (yMax - yMin)
            val y = paddingTop + chartHeight - (yFactor * chartHeight)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f),
        )
    }
}
