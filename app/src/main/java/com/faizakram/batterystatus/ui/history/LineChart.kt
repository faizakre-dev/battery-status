package com.faizakram.batterystatus.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Minimal dependency-free time-series line chart. Draws horizontal gridlines, a
 * soft area fill and the line itself; y is mapped from [ChartSeries.minY]..[ChartSeries.maxY].
 */
@Composable
fun LineChart(
    series: ChartSeries,
    lineColor: Color,
    modifier: Modifier = Modifier,
    gridColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
) {
    if (series.isEmpty) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text(
                text = "No data for this range",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Canvas(modifier) {
        val inset = 6.dp.toPx()
        val width = (size.width - inset * 2).coerceAtLeast(1f)
        val height = (size.height - inset * 2).coerceAtLeast(1f)

        val minX = series.points.first().x
        val maxX = series.points.last().x
        val spanX = (maxX - minX).coerceAtLeast(1L).toFloat()
        val spanY = (series.maxY - series.minY).coerceAtLeast(1f)

        fun toOffset(point: ChartPoint): Offset = Offset(
            x = inset + ((point.x - minX) / spanX) * width,
            y = inset + (1f - ((point.y - series.minY) / spanY)) * height,
        )

        for (i in 0..3) {
            val y = inset + height * i / 3f
            drawLine(gridColor, Offset(inset, y), Offset(inset + width, y), strokeWidth = 1.dp.toPx())
        }

        if (series.points.size == 1) {
            drawCircle(lineColor, radius = 4.dp.toPx(), center = toOffset(series.points.first()))
            return@Canvas
        }

        val line = Path()
        val fill = Path()
        series.points.forEachIndexed { index, point ->
            val offset = toOffset(point)
            if (index == 0) {
                line.moveTo(offset.x, offset.y)
                fill.moveTo(offset.x, inset + height)
                fill.lineTo(offset.x, offset.y)
            } else {
                line.lineTo(offset.x, offset.y)
                fill.lineTo(offset.x, offset.y)
            }
        }
        fill.lineTo(inset + width, inset + height)
        fill.close()

        drawPath(fill, lineColor.copy(alpha = 0.12f))
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}
