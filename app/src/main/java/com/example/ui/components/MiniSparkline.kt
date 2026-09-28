package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.TvBearRed
import com.example.ui.theme.TvBullGreen

@Composable
fun MiniSparkline(
    points: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
        .width(72.dp)
        .height(34.dp)
) {
    if (points.size < 2) return

    val strokeColor = if (isPositive) TvBullGreen else TvBearRed
    val fillColor = strokeColor.copy(alpha = 0.15f)

    Canvas(modifier = modifier) {
        val minVal = points.minOrNull() ?: 0.0
        val maxVal = points.maxOrNull() ?: 1.0
        val range = (maxVal - minVal).coerceAtLeast(0.0001)

        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { index, value ->
            val x = index * stepX
            val normalized = ((value - minVal) / range).toFloat()
            // Invert Y so high values are at top
            val y = h - (normalized * (h - 8f)) - 4f

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevVal = points[index - 1]
                val prevNorm = ((prevVal - minVal) / range).toFloat()
                val prevY = h - (prevNorm * (h - 8f)) - 4f

                // Cubic bezier smooth curve
                val cx1 = (prevX + x) / 2f
                val cy1 = prevY
                val cx2 = (prevX + x) / 2f
                val cy2 = y
                path.cubicTo(cx1, cy1, cx2, cy2, x, y)
                fillPath.cubicTo(cx1, cy1, cx2, cy2, x, y)
            }
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        // Draw soft gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = h
            )
        )

        // Draw sparkline stroke
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
