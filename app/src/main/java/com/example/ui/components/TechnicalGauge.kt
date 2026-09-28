package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TechnicalAnalysis
import com.example.model.TechnicalRating
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TechnicalGauge(
    analysis: TechnicalAnalysis,
    modifier: Modifier = Modifier
) {
    // Map score -2 to +2 to angle 180° to 360° (or -180° to 0°)
    // Strong Sell = 180 deg (left), Neutral = 270 deg (top), Strong Buy = 360 deg (right)
    val targetAngle = when (analysis.overall) {
        TechnicalRating.STRONG_SELL -> 195f
        TechnicalRating.SELL -> 230f
        TechnicalRating.NEUTRAL -> 270f
        TechnicalRating.BUY -> 310f
        TechnicalRating.STRONG_BUY -> 345f
    }

    val animatedAngle by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = tween(durationMillis = 800),
        label = "gauge_needle"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TvDarkSurface, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TECHNICAL ANALYSIS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TvTextSecondary,
                letterSpacing = 1.sp
            )
            Box(
                modifier = Modifier
                    .background(
                        color = when (analysis.overall) {
                            TechnicalRating.STRONG_BUY, TechnicalRating.BUY -> TvBullGreenBg
                            TechnicalRating.STRONG_SELL, TechnicalRating.SELL -> TvBearRedBg
                            else -> TvBrandBlueBg
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = analysis.overall.label.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (analysis.overall) {
                        TechnicalRating.STRONG_BUY, TechnicalRating.BUY -> TvBullGreen
                        TechnicalRating.STRONG_SELL, TechnicalRating.SELL -> TvBearRed
                        else -> TvBrandBlueLight
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Gauge Arc and Needle
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(115.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 14.dp.toPx()
                val diameter = size.width - strokeW
                val arcTop = strokeW / 2
                val arcLeft = strokeW / 2
                val arcSize = Size(diameter, diameter)

                // 5 Colored Segments: Strong Sell (Red), Sell (Coral), Neutral (Gray), Buy (Mint), Strong Buy (Green)
                val segments = listOf(
                    Triple(180f, 36f, Color(0xFFF23645)),
                    Triple(216f, 36f, Color(0xFFFF6D00)),
                    Triple(252f, 36f, Color(0xFF787B86)),
                    Triple(288f, 36f, Color(0xFF26A69A)),
                    Triple(324f, 36f, Color(0xFF089981))
                )

                segments.forEach { (start, sweep, color) ->
                    drawArc(
                        color = color,
                        startAngle = start,
                        sweepAngle = sweep - 2f,
                        useCenter = false,
                        topLeft = Offset(arcLeft, arcTop),
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                // Needle center
                val centerX = size.width / 2
                val centerY = size.height - 4f
                val needleLength = diameter * 0.42f
                val rad = Math.toRadians(animatedAngle.toDouble())
                val needleX = (centerX + needleLength * cos(rad)).toFloat()
                val needleY = (centerY + needleLength * sin(rad)).toFloat()

                // Needle Line
                drawLine(
                    color = Color.White,
                    start = Offset(centerX, centerY),
                    end = Offset(needleX, needleY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center pivot circle
                drawCircle(
                    color = Color.White,
                    radius = 5.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = TvDarkSurface,
                    radius = 2.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Breakdown counters: Sell, Neutral, Buy
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatPill(count = analysis.sellSignals, label = "Sell", color = TvBearRed)
            StatPill(count = analysis.neutralSignals, label = "Neutral", color = TvTextSecondary)
            StatPill(count = analysis.buySignals, label = "Buy", color = TvBullGreen)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Key indicator metrics row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TvDarkBackground, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricItem(title = "RSI (14)", value = String.format("%.1f", analysis.rsi))
            MetricItem(title = "MACD (12,26)", value = String.format("%.2f", analysis.macd))
            MetricItem(title = "Stoch %K", value = String.format("%.1f", analysis.stochK))
            MetricItem(title = "ADX (14)", value = String.format("%.1f", analysis.adx))
        }
    }
}

@Composable
private fun StatPill(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = count.toString(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = TvTextSecondary)
    }
}

@Composable
private fun MetricItem(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, fontSize = 10.sp, color = TvTextMuted)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TvTextPrimary)
    }
}
