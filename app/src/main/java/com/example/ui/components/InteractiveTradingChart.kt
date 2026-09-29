package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.*

@Composable
fun InteractiveTradingChart(
    candles: List<Candle>,
    chartType: ChartType,
    indicators: IndicatorConfig,
    activeDrawingTool: DrawingTool,
    drawnLines: List<DrawnHorizontalLine>,
    onAddHorizontalLine: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(TvDarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = TvBrandBlue)
        }
        return
    }

    val textMeasurer = rememberTextMeasurer()

    // Viewport control: offset (panning) and visible candle count (zoom)
    var visibleCount by remember { mutableIntStateOf(50) }
    var scrollOffsetFloat by remember { mutableFloatStateOf(0f) }

    // Mode: Pan/Zoom or Crosshair Inspect
    var isCrosshairMode by remember { mutableStateOf(false) }

    // Crosshair state
    var isScrubbing by remember { mutableStateOf(false) }
    var crosshairOffset by remember { mutableStateOf(Offset.Zero) }

    // Selected / scrubbed candle
    var selectedCandle by remember { mutableStateOf<Candle?>(null) }

    // Clamp visible range
    val totalCandles = candles.size
    val safeVisibleCount = visibleCount.coerceIn(15, totalCandles.coerceAtLeast(15))
    val maxScroll = (totalCandles - safeVisibleCount).coerceAtLeast(0)
    val safeScroll = scrollOffsetFloat.roundToInt().coerceIn(0, maxScroll)

    // Window of candles to render: from (total - safeVisibleCount - safeScroll) to (total - safeScroll)
    val startIndex = (totalCandles - safeVisibleCount - safeScroll).coerceAtLeast(0)
    val endIndex = (totalCandles - safeScroll).coerceAtMost(totalCandles)
    val visibleCandles = candles.subList(startIndex, endIndex)

    val currentDisplayCandle = selectedCandle ?: visibleCandles.lastOrNull() ?: candles.last()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvDarkBackground)
    ) {
        // --- 1. Top HUD / Legend Bar (OHLCV & Indicator Values) ---
        ChartHeaderLegend(
            candle = currentDisplayCandle,
            indicators = indicators,
            isHistorical = selectedCandle != null,
            onDismissSelection = {
                selectedCandle = null
                isScrubbing = false
            }
        )

        // --- 2. Main Interactive Chart Canvas Area ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("interactive_chart_canvas")
                    .pointerInput(visibleCandles.size, activeDrawingTool, isCrosshairMode) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startOffset = down.position
                            var currentOffset = down.position
                            var isTransforming = false
                            var isMultiTouch = false
                            val startTime = System.currentTimeMillis()

                            val canvasW = size.width
                            val canvasH = size.height
                            val priceScaleW = 60.dp.toPx()
                            val timeScaleH = 24.dp.toPx()
                            val chartW = canvasW - priceScaleW
                            val chartH = canvasH - timeScaleH
                            val subPaneCount = (if (indicators.showRsi) 1 else 0) + (if (indicators.showMacd) 1 else 0)
                            val subPaneH = if (subPaneCount > 0) chartH * (0.16f * subPaneCount) else 0f
                            val mainChartH = chartH - subPaneH
                            val barW = (chartW / visibleCandles.size).coerceAtLeast(1f)

                            while (true) {
                                val event = awaitPointerEvent()
                                val pressedPointers = event.changes.filter { it.pressed }
                                if (pressedPointers.isEmpty()) {
                                    // Pointer released
                                    val totalDuration = System.currentTimeMillis() - startTime
                                    val totalDistance = (currentOffset - startOffset).getDistance()

                                    if (!isTransforming && !isMultiTouch && totalDistance < 15f && totalDuration < 400L) {
                                        if (activeDrawingTool == DrawingTool.HORIZONTAL_LINE) {
                                            val (minP, maxP) = calculatePriceRange(visibleCandles)
                                            val pricePadding = (maxP - minP) * 0.08
                                            val plotMinP = minP - pricePadding
                                            val plotMaxP = maxP + pricePadding
                                            val price = yToPrice(startOffset.y, mainChartH, plotMinP, plotMaxP)
                                            onAddHorizontalLine(price)
                                        } else if (startOffset.x in 0f..chartW && startOffset.y in 0f..chartH) {
                                            val idx = (startOffset.x / barW).toInt().coerceIn(0, visibleCandles.size - 1)
                                            if (isScrubbing && selectedCandle == visibleCandles[idx]) {
                                                isScrubbing = false
                                                selectedCandle = null
                                            } else {
                                                isScrubbing = true
                                                selectedCandle = visibleCandles[idx]
                                                crosshairOffset = Offset(idx * barW + barW / 2, startOffset.y.coerceIn(0f, mainChartH))
                                            }
                                        }
                                    }
                                    break
                                }

                                if (pressedPointers.size >= 2) {
                                    isMultiTouch = true
                                    isTransforming = true
                                    val p0 = pressedPointers[0].position
                                    val p1 = pressedPointers[1].position
                                    val prevP0 = pressedPointers[0].previousPosition
                                    val prevP1 = pressedPointers[1].previousPosition
                                    val prevDist = (prevP0 - prevP1).getDistance()
                                    val currentDist = (p0 - p1).getDistance()
                                    if (prevDist > 1f) {
                                        val zoom = currentDist / prevDist
                                        if (zoom > 0.05f && zoom < 20f) {
                                            visibleCount = (visibleCount / zoom).roundToInt().coerceIn(15, totalCandles.coerceAtLeast(15))
                                        }
                                    }
                                    event.changes.forEach { it.consume() }
                                } else if (pressedPointers.size == 1 && !isMultiTouch) {
                                    val pointer = pressedPointers[0]
                                    val prevPos = pointer.previousPosition
                                    currentOffset = pointer.position
                                    val dx = currentOffset.x - prevPos.x
                                    val distFromStart = (currentOffset - startOffset).getDistance()

                                    if (distFromStart > 10f) {
                                        isTransforming = true
                                        if (isCrosshairMode || isScrubbing) {
                                            isScrubbing = true
                                            val newX = currentOffset.x.coerceIn(0f, chartW)
                                            val newY = currentOffset.y.coerceIn(0f, chartH)
                                            val idx = (newX / barW).toInt().coerceIn(0, visibleCandles.size - 1)
                                            selectedCandle = visibleCandles[idx]
                                            crosshairOffset = Offset(idx * barW + barW / 2, newY)
                                        } else {
                                            // Smooth horizontal pan
                                            val deltaBars = dx / barW
                                            scrollOffsetFloat = (scrollOffsetFloat + deltaBars).coerceIn(0f, maxScroll.toFloat())
                                            if (isScrubbing) {
                                                isScrubbing = false
                                                selectedCandle = null
                                            }
                                        }
                                        pointer.consume()
                                    } else if (!isTransforming && (System.currentTimeMillis() - startTime > 380L)) {
                                        // Long press triggered
                                        if (startOffset.x in 0f..chartW && startOffset.y in 0f..chartH) {
                                            isScrubbing = true
                                            val idx = (startOffset.x / barW).toInt().coerceIn(0, visibleCandles.size - 1)
                                            selectedCandle = visibleCandles[idx]
                                            crosshairOffset = Offset(idx * barW + barW / 2, startOffset.y.coerceIn(0f, mainChartH))
                                        }
                                    }
                                }
                            }
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height

                // Reserve right 60.dp for Y-axis price scale, bottom 24.dp for X-axis time scale
                val priceScaleW = 60.dp.toPx()
                val timeScaleH = 24.dp.toPx()

                val chartW = canvasW - priceScaleW
                val chartH = canvasH - timeScaleH

                // Sub-pane vertical split
                val subPaneCount = (if (indicators.showRsi) 1 else 0) + (if (indicators.showMacd) 1 else 0)
                val subPaneH = if (subPaneCount > 0) chartH * (0.16f * subPaneCount) else 0f
                val mainChartH = chartH - subPaneH

                // Price range calculations
                val (minP, maxP) = calculatePriceRange(visibleCandles)
                val pricePadding = (maxP - minP) * 0.08
                val plotMinP = minP - pricePadding
                val plotMaxP = maxP + pricePadding
                val priceRange = (plotMaxP - plotMinP).coerceAtLeast(0.0001)

                val n = visibleCandles.size
                val barW = chartW / n
                val candleBodyW = (barW * 0.72f).coerceIn(2f, 26f)

                // 1. Draw Grid Lines
                drawGrid(
                    chartW = chartW,
                    chartH = chartH,
                    minPrice = plotMinP,
                    maxPrice = plotMaxP,
                    visibleCandles = visibleCandles,
                    barW = barW,
                    textMeasurer = textMeasurer
                )

                // 2. Draw Volume Bars in bottom portion of main chart
                if (indicators.showVolume) {
                    val maxVol = visibleCandles.maxOfOrNull { it.volume } ?: 1.0
                    val volAreaH = mainChartH * 0.22f
                    visibleCandles.forEachIndexed { i, c ->
                        val x = i * barW + barW / 2
                        val volHeight = ((c.volume / maxVol) * volAreaH).toFloat()
                        val volTop = mainChartH - volHeight
                        val isBull = c.close >= c.open
                        drawRect(
                            color = if (isBull) TvBullGreen.copy(alpha = 0.35f) else TvBearRed.copy(alpha = 0.35f),
                            topLeft = Offset(x - candleBodyW / 2, volTop),
                            size = Size(candleBodyW, volHeight)
                        )
                    }
                }

                // 3. Draw Bollinger Bands fill and lines
                if (indicators.showBollinger) {
                    drawBollingerBands(
                        candles = visibleCandles,
                        barW = barW,
                        mainH = mainChartH,
                        minP = plotMinP,
                        range = priceRange
                    )
                }

                // 4. Draw Moving Averages (EMA 20, 50, 200, VWAP)
                if (indicators.showEma20) {
                    drawEmaLine(visibleCandles, { it.ema20 }, TvBrandBlue, 2.dp.toPx(), barW, mainChartH, plotMinP, priceRange)
                }
                if (indicators.showEma50) {
                    drawEmaLine(visibleCandles, { it.ema50 }, TvOrange, 2.dp.toPx(), barW, mainChartH, plotMinP, priceRange)
                }
                if (indicators.showEma200) {
                    drawEmaLine(visibleCandles, { it.ema200 }, TvPurple, 2.dp.toPx(), barW, mainChartH, plotMinP, priceRange)
                }
                if (indicators.showVwap) {
                    drawEmaLine(visibleCandles, { it.vwap }, TvGold, 2.dp.toPx(), barW, mainChartH, plotMinP, priceRange)
                }

                // 5. Draw Primary Chart Elements (Candles or Line/Area)
                when (chartType) {
                    ChartType.CANDLESTICK, ChartType.HOLLOW -> {
                        drawCandlesticks(
                            candles = visibleCandles,
                            barW = barW,
                            candleBodyW = candleBodyW,
                            mainH = mainChartH,
                            minP = plotMinP,
                            range = priceRange,
                            isHollow = chartType == ChartType.HOLLOW
                        )
                    }
                    ChartType.HEIKIN_ASHI -> {
                        val haCandles = computeHeikinAshi(visibleCandles)
                        drawCandlesticks(
                            candles = haCandles,
                            barW = barW,
                            candleBodyW = candleBodyW,
                            mainH = mainChartH,
                            minP = plotMinP,
                            range = priceRange,
                            isHollow = false
                        )
                    }
                    ChartType.LINE -> {
                        drawLineChart(
                            candles = visibleCandles,
                            barW = barW,
                            mainH = mainChartH,
                            minP = plotMinP,
                            range = priceRange,
                            isArea = false
                        )
                    }
                    ChartType.AREA -> {
                        drawLineChart(
                            candles = visibleCandles,
                            barW = barW,
                            mainH = mainChartH,
                            minP = plotMinP,
                            range = priceRange,
                            isArea = true
                        )
                    }
                }

                // 6. Draw User Horizontal Support/Resistance Lines
                drawnLines.forEach { line ->
                    val y = priceToY(line.price, mainChartH, plotMinP, priceRange)
                    if (y in 0f..mainChartH) {
                        drawLine(
                            color = TvGold,
                            start = Offset(0f, y),
                            end = Offset(chartW, y),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                        // Label badge
                        drawText(
                            textMeasurer = textMeasurer,
                            text = "${line.label} ${formatPrice(line.price)}",
                            topLeft = Offset(8.dp.toPx(), y - 18.dp.toPx()),
                            style = TextStyle(color = TvGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // 7. Draw Sub-Panes (RSI / MACD)
                var currentSubPaneTop = mainChartH
                if (indicators.showRsi) {
                    val rsiH = chartH * 0.16f
                    drawRsiPane(
                        candles = visibleCandles,
                        barW = barW,
                        top = currentSubPaneTop,
                        height = rsiH,
                        chartW = chartW,
                        textMeasurer = textMeasurer
                    )
                    currentSubPaneTop += rsiH
                }

                if (indicators.showMacd) {
                    val macdH = chartH * 0.16f
                    drawMacdPane(
                        candles = visibleCandles,
                        barW = barW,
                        candleBodyW = candleBodyW,
                        top = currentSubPaneTop,
                        height = macdH,
                        chartW = chartW,
                        textMeasurer = textMeasurer
                    )
                }

                // 8. Draw Y-Axis Price Scale Labels on Right Rail & Live Price Horizontal Line
                drawPriceScaleRail(
                    chartW = chartW,
                    priceScaleW = priceScaleW,
                    mainChartH = mainChartH,
                    minPrice = plotMinP,
                    maxPrice = plotMaxP,
                    lastPrice = visibleCandles.last().close,
                    isBullish = visibleCandles.last().close >= visibleCandles.last().open,
                    textMeasurer = textMeasurer
                )

                // 9. Draw X-Axis Time Scale at Bottom
                drawTimeScale(
                    candles = visibleCandles,
                    chartW = chartW,
                    timeScaleY = chartH,
                    timeScaleH = timeScaleH,
                    barW = barW,
                    textMeasurer = textMeasurer
                )

                // 10. Draw Interactive Crosshair if scrubbing / inspecting
                if (isScrubbing && crosshairOffset.x in 0f..chartW && crosshairOffset.y in 0f..chartH) {
                    val candleIdx = (crosshairOffset.x / barW).toInt().coerceIn(0, visibleCandles.size - 1)
                    val candle = visibleCandles[candleIdx]
                    selectedCandle = candle

                    val snappedX = candleIdx * barW + barW / 2
                    val snappedY = crosshairOffset.y.coerceIn(0f, mainChartH)
                    val cursorPrice = yToPrice(snappedY, mainChartH, plotMinP, plotMaxP)

                    // Vertical dashed line
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(snappedX, 0f),
                        end = Offset(snappedX, chartH),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )

                    // Horizontal dashed line
                    drawLine(
                        color = Color.White.copy(alpha = 0.5f),
                        start = Offset(0f, snappedY),
                        end = Offset(chartW, snappedY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )

                    // Pinned Price Tag on right scale
                    drawRect(
                        color = TvBrandBlue,
                        topLeft = Offset(chartW, snappedY - 11.dp.toPx()),
                        size = Size(priceScaleW, 22.dp.toPx())
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = formatPrice(cursorPrice),
                        topLeft = Offset(chartW + 4.dp.toPx(), snappedY - 7.dp.toPx()),
                        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )

                    // Pinned Time Tag on bottom scale
                    val timeW = 60.dp.toPx()
                    drawRect(
                        color = TvDarkSurface,
                        topLeft = Offset(snappedX - timeW / 2, chartH),
                        size = Size(timeW, timeScaleH)
                    )
                    drawText(
                        textMeasurer = textMeasurer,
                        text = candle.timeLabel,
                        topLeft = Offset(snappedX - timeW / 2 + 8.dp.toPx(), chartH + 4.dp.toPx()),
                        style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }

            // Quick Zoom & Interactive Mode Controls in Floating Corner
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 28.dp)
                    .background(TvDarkSurface.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mode toggle: Pan vs Crosshair
                IconButton(
                    onClick = {
                        isCrosshairMode = !isCrosshairMode
                        if (!isCrosshairMode) {
                            isScrubbing = false
                            selectedCandle = null
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isCrosshairMode) Icons.Default.FilterCenterFocus else Icons.Default.PanTool,
                        contentDescription = if (isCrosshairMode) "Crosshair Mode" else "Pan Mode",
                        tint = if (isCrosshairMode) TvBrandBlueLight else TvTextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                VerticalDivider(modifier = Modifier.height(16.dp).padding(horizontal = 2.dp), color = TvBorderColor)

                IconButton(
                    onClick = { visibleCount = (visibleCount - 10).coerceAtLeast(15) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = TvTextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = { visibleCount = (visibleCount + 10).coerceAtMost(candles.size) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = TvTextPrimary, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {
                        scrollOffsetFloat = 0f
                        visibleCount = 50
                        selectedCandle = null
                        isScrubbing = false
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = "Reset Chart", tint = TvTextPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // "Jump to Live Bar" Floating Button when user has panned back into history
            if (safeScroll > 0) {
                Button(
                    onClick = {
                        scrollOffsetFloat = 0f
                        selectedCandle = null
                        isScrubbing = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TvBrandBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 68.dp, bottom = 28.dp)
                        .height(30.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Jump to Live", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LIVE ($safeScroll)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// --- Drawing Helper Functions ---

private fun DrawScope.drawGrid(
    chartW: Float,
    chartH: Float,
    minPrice: Double,
    maxPrice: Double,
    visibleCandles: List<Candle>,
    barW: Float,
    textMeasurer: TextMeasurer
) {
    val gridColor = TvSurfaceVariant.copy(alpha = 0.45f)
    val stroke = Stroke(width = 0.8.dp.toPx())

    // 5 Horizontal price grid lines
    val priceSteps = 5
    val pRange = maxPrice - minPrice
    for (i in 0..priceSteps) {
        val y = (chartH / priceSteps) * i
        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(chartW, y), strokeWidth = stroke.width)
    }

    // Vertical time grid lines (every 10 candles)
    for (i in visibleCandles.indices step 10) {
        val x = i * barW + barW / 2
        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, chartH), strokeWidth = stroke.width)
    }
}

private fun DrawScope.drawCandlesticks(
    candles: List<Candle>,
    barW: Float,
    candleBodyW: Float,
    mainH: Float,
    minP: Double,
    range: Double,
    isHollow: Boolean
) {
    candles.forEachIndexed { i, c ->
        val x = i * barW + barW / 2
        val isBull = c.close >= c.open
        val color = if (isBull) TvBullGreen else TvBearRed

        val openY = priceToY(c.open, mainH, minP, range)
        val closeY = priceToY(c.close, mainH, minP, range)
        val highY = priceToY(c.high, mainH, minP, range)
        val lowY = priceToY(c.low, mainH, minP, range)

        // Draw upper and lower wicks
        drawLine(
            color = color,
            start = Offset(x, highY),
            end = Offset(x, lowY),
            strokeWidth = 1.4.dp.toPx()
        )

        // Draw Candle Body
        val bodyTop = min(openY, closeY)
        val bodyHeight = max(abs(closeY - openY), 2f)

        if (isHollow && isBull) {
            drawRect(
                color = color,
                topLeft = Offset(x - candleBodyW / 2, bodyTop),
                size = Size(candleBodyW, bodyHeight),
                style = Stroke(width = 1.5.dp.toPx())
            )
        } else {
            drawRect(
                color = color,
                topLeft = Offset(x - candleBodyW / 2, bodyTop),
                size = Size(candleBodyW, bodyHeight)
            )
        }
    }
}

private fun DrawScope.drawLineChart(
    candles: List<Candle>,
    barW: Float,
    mainH: Float,
    minP: Double,
    range: Double,
    isArea: Boolean
) {
    if (candles.isEmpty()) return
    val path = Path()
    val fillPath = Path()

    candles.forEachIndexed { i, c ->
        val x = i * barW + barW / 2
        val y = priceToY(c.close, mainH, minP, range)
        if (i == 0) {
            path.moveTo(x, y)
            fillPath.moveTo(x, mainH)
            fillPath.lineTo(x, y)
        } else {
            val prevX = (i - 1) * barW + barW / 2
            val prevY = priceToY(candles[i - 1].close, mainH, minP, range)
            val cx = (prevX + x) / 2f
            path.cubicTo(cx, prevY, cx, y, x, y)
            fillPath.cubicTo(cx, prevY, cx, y, x, y)
        }
    }

    if (isArea) {
        val lastX = (candles.size - 1) * barW + barW / 2
        fillPath.lineTo(lastX, mainH)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(TvBrandBlue.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = mainH
            )
        )
    }

    drawPath(
        path = path,
        color = TvBrandBlueLight,
        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawBollingerBands(
    candles: List<Candle>,
    barW: Float,
    mainH: Float,
    minP: Double,
    range: Double
) {
    val upperPoints = mutableListOf<Offset>()
    val lowerPoints = mutableListOf<Offset>()

    candles.forEachIndexed { i, c ->
        val x = i * barW + barW / 2
        val u = c.bbUpper
        val l = c.bbLower
        if (u != null && l != null) {
            upperPoints.add(Offset(x, priceToY(u, mainH, minP, range)))
            lowerPoints.add(Offset(x, priceToY(l, mainH, minP, range)))
        }
    }

    if (upperPoints.size < 2) return

    val fillPath = Path()
    fillPath.moveTo(upperPoints.first().x, upperPoints.first().y)
    upperPoints.forEach { fillPath.lineTo(it.x, it.y) }
    lowerPoints.reversed().forEach { fillPath.lineTo(it.x, it.y) }
    fillPath.close()

    // Shaded envelope fill
    drawPath(path = fillPath, color = TvOrange.copy(alpha = 0.08f))

    // Upper and lower boundary lines
    for (i in 1 until upperPoints.size) {
        drawLine(
            color = TvOrange.copy(alpha = 0.5f),
            start = upperPoints[i - 1],
            end = upperPoints[i],
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = TvOrange.copy(alpha = 0.5f),
            start = lowerPoints[i - 1],
            end = lowerPoints[i],
            strokeWidth = 1.2.dp.toPx()
        )
    }
}

private fun DrawScope.drawEmaLine(
    candles: List<Candle>,
    selector: (Candle) -> Double?,
    color: Color,
    strokeW: Float,
    barW: Float,
    mainH: Float,
    minP: Double,
    range: Double
) {
    var prevOffset: Offset? = null
    candles.forEachIndexed { i, c ->
        val v = selector(c)
        if (v != null) {
            val currentOffset = Offset(i * barW + barW / 2, priceToY(v, mainH, minP, range))
            if (prevOffset != null) {
                drawLine(
                    color = color,
                    start = prevOffset!!,
                    end = currentOffset,
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
            prevOffset = currentOffset
        }
    }
}

private fun DrawScope.drawRsiPane(
    candles: List<Candle>,
    barW: Float,
    top: Float,
    height: Float,
    chartW: Float,
    textMeasurer: TextMeasurer
) {
    val bottom = top + height
    // Background and border
    drawRect(color = TvDarkSurface.copy(alpha = 0.4f), topLeft = Offset(0f, top), size = Size(chartW, height))
    drawLine(color = TvSurfaceVariant, start = Offset(0f, top), end = Offset(chartW, top), strokeWidth = 1.dp.toPx())

    // 70 and 30 guidelines
    val y70 = top + height * (1f - 0.70f)
    val y30 = top + height * (1f - 0.30f)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

    drawLine(color = TvBearRed.copy(alpha = 0.6f), start = Offset(0f, y70), end = Offset(chartW, y70), strokeWidth = 1f, pathEffect = dashEffect)
    drawLine(color = TvBullGreen.copy(alpha = 0.6f), start = Offset(0f, y30), end = Offset(chartW, y30), strokeWidth = 1f, pathEffect = dashEffect)

    // Shaded middle 30-70 band
    drawRect(color = TvPurple.copy(alpha = 0.06f), topLeft = Offset(0f, y70), size = Size(chartW, y30 - y70))

    // Label
    val latestRsi = candles.lastOrNull()?.rsi ?: 50.0
    drawText(
        textMeasurer = textMeasurer,
        text = "RSI(14) ${String.format("%.1f", latestRsi)}",
        topLeft = Offset(8.dp.toPx(), top + 4.dp.toPx()),
        style = TextStyle(color = TvPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    )

    // RSI Line
    var prevOffset: Offset? = null
    candles.forEachIndexed { i, c ->
        val rsi = c.rsi
        if (rsi != null) {
            val clamped = rsi.coerceIn(0.0, 100.0)
            val currentOffset = Offset(i * barW + barW / 2, (top + height * (1.0 - (clamped / 100.0))).toFloat())
            if (prevOffset != null) {
                drawLine(color = Color(0xFFBA68C8), start = prevOffset!!, end = currentOffset, strokeWidth = 1.8.dp.toPx())
            }
            prevOffset = currentOffset
        }
    }
}

private fun DrawScope.drawMacdPane(
    candles: List<Candle>,
    barW: Float,
    candleBodyW: Float,
    top: Float,
    height: Float,
    chartW: Float,
    textMeasurer: TextMeasurer
) {
    drawRect(color = TvDarkSurface.copy(alpha = 0.4f), topLeft = Offset(0f, top), size = Size(chartW, height))
    drawLine(color = TvSurfaceVariant, start = Offset(0f, top), end = Offset(chartW, top), strokeWidth = 1.dp.toPx())

    val zeroY = top + height / 2f
    drawLine(color = TvTextMuted.copy(alpha = 0.4f), start = Offset(0f, zeroY), end = Offset(chartW, zeroY), strokeWidth = 1f)

    val maxAbsHist = candles.mapNotNull { it.macdHist?.let { abs(it) } }.maxOrNull()?.coerceAtLeast(0.1) ?: 1.0

    // Draw Histogram
    candles.forEachIndexed { i, c ->
        val hist = c.macdHist
        if (hist != null) {
            val x = i * barW + barW / 2
            val hHeight = ((hist / maxAbsHist) * (height * 0.4f)).toFloat()
            val isPos = hist >= 0
            val color = if (isPos) TvBullGreen else TvBearRed

            val barTop = if (isPos) zeroY - hHeight else zeroY
            drawRect(
                color = color.copy(alpha = 0.7f),
                topLeft = Offset(x - candleBodyW / 3, barTop),
                size = Size((candleBodyW * 0.66f).coerceAtLeast(1.5f), abs(hHeight))
            )
        }
    }

    // MACD line (cyan) and Signal line (orange)
    val maxAbsMacd = candles.mapNotNull { it.macd?.let { abs(it) } }.maxOrNull()?.coerceAtLeast(0.1) ?: 1.0
    var prevMacd: Offset? = null
    var prevSignal: Offset? = null

    candles.forEachIndexed { i, c ->
        val x = i * barW + barW / 2
        val m = c.macd
        val s = c.macdSignal
        if (m != null) {
            val my = zeroY - ((m / maxAbsMacd) * (height * 0.42f)).toFloat()
            val mOffset = Offset(x, my)
            if (prevMacd != null) {
                drawLine(color = TvCyan, start = prevMacd!!, end = mOffset, strokeWidth = 1.8.dp.toPx())
            }
            prevMacd = mOffset
        }
        if (s != null) {
            val sy = zeroY - ((s / maxAbsMacd) * (height * 0.42f)).toFloat()
            val sOffset = Offset(x, sy)
            if (prevSignal != null) {
                drawLine(color = TvOrange, start = prevSignal!!, end = sOffset, strokeWidth = 1.8.dp.toPx())
            }
            prevSignal = sOffset
        }
    }

    drawText(
        textMeasurer = textMeasurer,
        text = "MACD(12,26,9)",
        topLeft = Offset(8.dp.toPx(), top + 4.dp.toPx()),
        style = TextStyle(color = TvCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    )
}

private fun DrawScope.drawPriceScaleRail(
    chartW: Float,
    priceScaleW: Float,
    mainChartH: Float,
    minPrice: Double,
    maxPrice: Double,
    lastPrice: Double,
    isBullish: Boolean,
    textMeasurer: TextMeasurer
) {
    // Rail separator line
    drawLine(color = TvSurfaceVariant, start = Offset(chartW, 0f), end = Offset(chartW, mainChartH), strokeWidth = 1.dp.toPx())

    // 5 Price labels along Y rail
    val steps = 5
    val range = maxPrice - minPrice
    for (i in 0..steps) {
        val y = (mainChartH / steps) * i
        val price = maxPrice - (range / steps) * i
        drawText(
            textMeasurer = textMeasurer,
            text = formatPrice(price),
            topLeft = Offset(chartW + 6.dp.toPx(), y - 7.dp.toPx()),
            style = TextStyle(color = TvTextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        )
    }

    // Active Last Price Level & Badge
    val lastY = priceToY(lastPrice, mainChartH, minPrice, range).coerceIn(10f, mainChartH - 10f)
    val badgeColor = if (isBullish) TvBullGreen else TvBearRed

    // Horizontal dashed live price line across the whole main chart canvas (TradingView signature)
    drawLine(
        color = badgeColor.copy(alpha = 0.75f),
        start = Offset(0f, lastY),
        end = Offset(chartW, lastY),
        strokeWidth = 1.2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )

    // Pinned Badge on right rail
    drawRect(
        color = badgeColor,
        topLeft = Offset(chartW, lastY - 11.dp.toPx()),
        size = Size(priceScaleW, 22.dp.toPx())
    )
    // Pulsing live tick dot inside badge
    drawCircle(
        color = Color.White,
        radius = 2.dp.toPx(),
        center = Offset(chartW + 6.dp.toPx(), lastY)
    )
    drawText(
        textMeasurer = textMeasurer,
        text = formatPrice(lastPrice),
        topLeft = Offset(chartW + 11.dp.toPx(), lastY - 7.dp.toPx()),
        style = TextStyle(color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    )
}

private fun DrawScope.drawTimeScale(
    candles: List<Candle>,
    chartW: Float,
    timeScaleY: Float,
    timeScaleH: Float,
    barW: Float,
    textMeasurer: TextMeasurer
) {
    drawLine(color = TvSurfaceVariant, start = Offset(0f, timeScaleY), end = Offset(chartW, timeScaleY), strokeWidth = 1.dp.toPx())

    // Draw timestamps every 8 candles
    for (i in candles.indices step 8) {
        val c = candles[i]
        val x = i * barW + barW / 2
        drawText(
            textMeasurer = textMeasurer,
            text = c.timeLabel,
            topLeft = Offset(x - 16.dp.toPx(), timeScaleY + 4.dp.toPx()),
            style = TextStyle(color = TvTextMuted, fontSize = 9.sp)
        )
    }
}

@Composable
private fun ChartHeaderLegend(
    candle: Candle,
    indicators: IndicatorConfig,
    isHistorical: Boolean = false,
    onDismissSelection: (() -> Unit)? = null
) {
    val isBull = candle.close >= candle.open
    val color = if (isBull) TvBullGreen else TvBearRed
    val change = candle.close - candle.open
    val changePct = if (candle.open > 0) (change / candle.open) * 100.0 else 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TvDarkSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // If inspecting a historical bar: show clear banner & dismiss action
        if (isHistorical) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(TvBrandBlue.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "INSPECTING: ${candle.timeLabel}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvBrandBlueLight
                        )
                    }
                }
                if (onDismissSelection != null) {
                    Text(
                        text = "✕ Back to Live",
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onDismissSelection() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // OHLCV Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendPill("O", formatPrice(candle.open), TvTextPrimary)
            LegendPill("H", formatPrice(candle.high), TvBullGreen)
            LegendPill("L", formatPrice(candle.low), TvBearRed)
            LegendPill("C", formatPrice(candle.close), color)
            LegendPill("Vol", String.format("%.0f", candle.volume), TvTextSecondary)

            Text(
                text = "${if (change >= 0) "+" else ""}${String.format("%.2f%%", changePct)}",
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Active Indicators Value summary
        if (indicators.showEma20 || indicators.showEma50 || indicators.showBollinger || indicators.showVwap) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (indicators.showEma20 && candle.ema20 != null) {
                    Text("EMA 20: ${formatPrice(candle.ema20)}", color = TvBrandBlueLight, fontSize = 10.sp)
                }
                if (indicators.showEma50 && candle.ema50 != null) {
                    Text("EMA 50: ${formatPrice(candle.ema50)}", color = TvOrange, fontSize = 10.sp)
                }
                if (indicators.showBollinger && candle.bbUpper != null) {
                    Text("BB(20,2): ${formatPrice(candle.bbUpper)}", color = TvOrange.copy(alpha = 0.8f), fontSize = 10.sp)
                }
                if (indicators.showVwap && candle.vwap != null) {
                    Text("VWAP: ${formatPrice(candle.vwap)}", color = TvGold, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun LegendPill(label: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label ", color = TvTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(text = value, color = valueColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

// --- Math & Coordinate Helpers ---

private fun calculatePriceRange(candles: List<Candle>): Pair<Double, Double> {
    if (candles.isEmpty()) return Pair(0.0, 1.0)
    var min = Double.MAX_VALUE
    var max = Double.MIN_VALUE
    candles.forEach {
        if (it.low < min) min = it.low
        if (it.high > max) max = it.high
        it.bbUpper?.let { bb -> if (bb > max) max = bb }
        it.bbLower?.let { bb -> if (bb < min) min = bb }
    }
    return Pair(min, max)
}

private fun priceToY(price: Double, height: Float, minP: Double, range: Double): Float {
    val norm = ((price - minP) / range).toFloat()
    return height - (norm * height)
}

private fun yToPrice(y: Float, height: Float, minP: Double, maxP: Double): Double {
    val norm = ((height - y) / height).coerceIn(0f, 1f)
    return minP + (norm * (maxP - minP))
}

private fun computeHeikinAshi(candles: List<Candle>): List<Candle> {
    if (candles.isEmpty()) return candles
    val list = mutableListOf<Candle>()
    var prevHaOpen = candles.first().open
    var prevHaClose = candles.first().close

    candles.forEach { c ->
        val haClose = (c.open + c.high + c.low + c.close) / 4.0
        val haOpen = (prevHaOpen + prevHaClose) / 2.0
        val haHigh = max(c.high, max(haOpen, haClose))
        val haLow = min(c.low, min(haOpen, haClose))

        list.add(c.copy(open = haOpen, high = haHigh, low = haLow, close = haClose))
        prevHaOpen = haOpen
        prevHaClose = haClose
    }
    return list
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.2f", price)
        else -> String.format(Locale.US, "%.4f", price)
    }
}
