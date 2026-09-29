package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.InteractiveTradingChart
import com.example.ui.components.TechnicalGauge
import com.example.ui.components.BarReplayControlBar
import com.example.ui.components.ApkExportGuideDialog
import com.example.ui.theme.*
import java.util.Locale

enum class MultiChartLayout(
    val label: String,
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    SINGLE("1x", "1 Chart (Single)", Icons.Default.CropSquare),
    SPLIT_V("2x V", "2 Charts (Split Vertical)", Icons.Default.ViewAgenda),
    SPLIT_H("2x H", "2 Charts (Split Horizontal)", Icons.Default.ViewWeek),
    QUAD("4x", "4 Charts (Quad Grid Pro)", Icons.Default.GridView)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    asset: MarketAsset,
    allAssets: List<MarketAsset>,
    candles: List<Candle>,
    selectedTimeframe: Timeframe,
    onSelectTimeframe: (Timeframe) -> Unit,
    onSelectAsset: (String) -> Unit,
    technicalAnalysis: TechnicalAnalysis,
    onQuickTrade: (PositionType, Double) -> Unit,
    isReplayMode: Boolean = false,
    isReplayPlaying: Boolean = false,
    replayIndex: Int = 0,
    replayTotalBars: Int = 0,
    replaySpeed: Float = 1f,
    onStartReplay: () -> Unit = {},
    onExitReplay: () -> Unit = {},
    onToggleReplayPlay: () -> Unit = {},
    onStepReplayForward: () -> Unit = {},
    onStepReplayBackward: () -> Unit = {},
    onSetReplaySpeed: (Float) -> Unit = {},
    onSeekReplay: (Int) -> Unit = {},
    isLiveMarketConnected: Boolean = true,
    liveDataFeedSource: String = "BINANCE SPOT (REAL-TIME)",
    lastLiveSyncTimestamp: String = "Just Now",
    isLoadingCandles: Boolean = false,
    onRefreshMarketData: () -> Unit = {},
    onOpenExportGuide: () -> Unit = {}
) {
    var chartType by remember { mutableStateOf(ChartType.CANDLESTICK) }
    var indicators by remember { mutableStateOf(IndicatorConfig()) }
    var activeDrawingTool by remember { mutableStateOf(DrawingTool.NONE) }
    var drawnLines by remember { mutableStateOf<List<DrawnHorizontalLine>>(emptyList()) }

    var showSymbolSelectorDialog by remember { mutableStateOf(false) }
    var showIndicatorsDialog by remember { mutableStateOf(false) }
    var showChartTypeMenu by remember { mutableStateOf(false) }
    var isDualChartLayout by remember { mutableStateOf(false) }
    var showTechnicalStatsSheet by remember { mutableStateOf(false) }
    var tradeExecutionFeedback by remember { mutableStateOf<String?>(null) }
    var orderModalSide by remember { mutableStateOf<PositionType?>(null) }

    val isPositive = asset.changePercent >= 0

    Scaffold(
        containerColor = TvDarkBackground,
        snackbarHost = {
            if (tradeExecutionFeedback != null) {
                Snackbar(
                    containerColor = TvDarkSurface,
                    contentColor = Color.White,
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { tradeExecutionFeedback = null }) {
                            Text("OK", color = TvBrandBlueLight)
                        }
                    }
                ) {
                    Text(tradeExecutionFeedback ?: "")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- 1. Pro Header: Symbol, Real-time Price, Quick Buy/Sell & Pro Controls ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Symbol selector pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSymbolSelectorDialog = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("symbol_selector_btn")
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = asset.symbol,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Symbol",
                                tint = TvTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isLiveMarketConnected) TvBullGreen else TvBearRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${asset.exchange} · ${selectedTimeframe.label} · ${if (isLiveMarketConnected) "LIVE" else "OFFLINE"}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLiveMarketConnected) TvBullGreen else TvTextSecondary
                            )
                        }
                    }
                }

                // Price and nominal change
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatPrice(asset.price, asset.currency),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f%%", asset.changePercent)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPositive) TvBullGreen else TvBearRed
                    )
                }

                // Quick Long / Short Execution Pills
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val qty = when {
                                asset.symbol.contains("NIFTY 50") -> 25.0
                                asset.symbol.contains("BANK NIFTY") -> 15.0
                                asset.price > 1000 -> 0.1
                                else -> 1.0
                            }
                            onQuickTrade(PositionType.LONG, qty)
                            tradeExecutionFeedback = "Bought Long $qty ${asset.symbol} @ ${formatPrice(asset.price, asset.currency)}"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TvBullGreen),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("quick_buy_btn")
                    ) {
                        Text("BUY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val qty = when {
                                asset.symbol.contains("NIFTY 50") -> 25.0
                                asset.symbol.contains("BANK NIFTY") -> 15.0
                                asset.price > 1000 -> 0.1
                                else -> 1.0
                            }
                            onQuickTrade(PositionType.SHORT, qty)
                            tradeExecutionFeedback = "Sold Short $qty ${asset.symbol} @ ${formatPrice(asset.price, asset.currency)}"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TvBearRed),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("quick_sell_btn")
                    ) {
                        Text("SELL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = TvBorderColor, thickness = 1.dp)

            // --- 2. Chart Toolbar: Timeframes, Indicators (fx), Chart Type, Split Layout ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Timeframe Chips
                Timeframe.values().forEach { tf ->
                    val isSelected = selectedTimeframe == tf
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) TvBrandBlue else Color.Transparent)
                            .clickable { onSelectTimeframe(tf) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("tf_${tf.label}")
                    ) {
                        Text(
                            text = tf.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TvTextSecondary
                        )
                    }
                }

                VerticalDivider(modifier = Modifier.height(18.dp), color = TvBorderColor)

                // Chart Style Selector
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TvSurfaceVariant)
                            .clickable { showChartTypeMenu = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("chart_type_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (chartType) {
                                ChartType.CANDLESTICK -> Icons.Default.BarChart
                                ChartType.LINE -> Icons.Default.ShowChart
                                ChartType.AREA -> Icons.Default.WaterfallChart
                                ChartType.HEIKIN_ASHI -> Icons.Default.CandlestickChart
                                ChartType.HOLLOW -> Icons.Default.AlignVerticalBottom
                            },
                            contentDescription = "Chart Type",
                            tint = TvBrandBlueLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(chartType.displayName, fontSize = 11.sp, color = TvTextPrimary)
                    }

                    DropdownMenu(
                        expanded = showChartTypeMenu,
                        onDismissRequest = { showChartTypeMenu = false },
                        modifier = Modifier.background(TvDarkSurface)
                    ) {
                        ChartType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName, color = Color.White) },
                                onClick = {
                                    chartType = type
                                    showChartTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Indicators Dialog Trigger (fx button)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TvBrandBlueBg)
                        .clickable { showIndicatorsDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("indicators_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("fx", fontSize = 12.sp, fontWeight = FontWeight.Black, color = TvBrandBlueLight)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Indicators", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TvBrandBlueLight)
                    }
                }

                // Replay / Backtest Mode Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isReplayMode) TvOrange.copy(alpha = 0.25f) else TvSurfaceVariant)
                        .clickable {
                            if (isReplayMode) onExitReplay() else onStartReplay()
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("toolbar_replay_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "Bar Replay",
                            tint = if (isReplayMode) TvOrange else TvTextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isReplayMode) "Replaying" else "Replay",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isReplayMode) TvOrange else TvTextPrimary
                        )
                    }
                }

                // Dual-Split View (Premium Feature)
                IconButton(
                    onClick = { isDualChartLayout = !isDualChartLayout },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isDualChartLayout) Icons.Default.ViewAgenda else Icons.Default.CropPortrait,
                        contentDescription = "Split Layout",
                        tint = if (isDualChartLayout) TvBrandBlueLight else TvTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Technical Analysis Gauge toggle
                IconButton(
                    onClick = { showTechnicalStatsSheet = !showTechnicalStatsSheet },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Technical Gauge",
                        tint = if (showTechnicalStatsSheet) TvGold else TvTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Export APK Guide trigger
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(TvBullGreenBg)
                        .clickable { onOpenExportGuide() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("toolbar_export_apk_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Export APK",
                            tint = TvBullGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Export APK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvBullGreen
                        )
                    }
                }

                // Live Refresh / Sync button
                IconButton(
                    onClick = onRefreshMarketData,
                    modifier = Modifier.size(28.dp).testTag("chart_live_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Live Exchange",
                        tint = if (isLoadingCandles) TvBrandBlueLight else TvTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Real exchange candle fetch indicator
            AnimatedVisibility(visible = isLoadingCandles) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = TvBrandBlueLight,
                    trackColor = TvSurfaceVariant
                )
            }

            // --- 3. Drawing Tools Floating Toolbar Strip ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkBackground)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("TOOLS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TvTextMuted)

                DrawingToolChip(
                    label = "Cursor",
                    icon = Icons.Default.NearMe,
                    isActive = activeDrawingTool == DrawingTool.NONE,
                    onClick = { activeDrawingTool = DrawingTool.NONE }
                )

                DrawingToolChip(
                    label = "+ Support / Resistance",
                    icon = Icons.Default.HorizontalRule,
                    isActive = activeDrawingTool == DrawingTool.HORIZONTAL_LINE,
                    onClick = {
                        activeDrawingTool = if (activeDrawingTool == DrawingTool.HORIZONTAL_LINE) DrawingTool.NONE else DrawingTool.HORIZONTAL_LINE
                    }
                )

                if (drawnLines.isNotEmpty()) {
                    TextButton(
                        onClick = { drawnLines = emptyList() },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Clear (${drawnLines.size})", fontSize = 11.sp, color = TvBearRed)
                    }
                }
            }

            // --- 4. Main Chart Canvas Area ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (!isDualChartLayout) {
                    InteractiveTradingChart(
                        candles = candles,
                        chartType = chartType,
                        indicators = indicators,
                        activeDrawingTool = activeDrawingTool,
                        drawnLines = drawnLines,
                        onAddHorizontalLine = { price ->
                            drawnLines = drawnLines + DrawnHorizontalLine(price, "Level ${drawnLines.size + 1}")
                            activeDrawingTool = DrawingTool.NONE
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Split screen 2-chart layout (TradingView Premium Pro feature)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            InteractiveTradingChart(
                                candles = candles,
                                chartType = chartType,
                                indicators = indicators.copy(showRsi = false, showMacd = false),
                                activeDrawingTool = activeDrawingTool,
                                drawnLines = drawnLines,
                                onAddHorizontalLine = { price ->
                                    drawnLines = drawnLines + DrawnHorizontalLine(price, "Level ${drawnLines.size + 1}")
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        HorizontalDivider(color = TvBorderColor, thickness = 2.dp)
                        Box(modifier = Modifier.weight(1f)) {
                            // Sub-chart with Area representation
                            InteractiveTradingChart(
                                candles = candles,
                                chartType = ChartType.AREA,
                                indicators = indicators.copy(showRsi = true, showBollinger = false),
                                activeDrawingTool = DrawingTool.NONE,
                                drawnLines = emptyList(),
                                onAddHorizontalLine = {},
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Bar Replay Control Bar overlay (Floating on bottom of chart)
                if (isReplayMode) {
                    BarReplayControlBar(
                        isReplayActive = true,
                        isPlaying = isReplayPlaying,
                        currentBarIndex = replayIndex,
                        totalBars = replayTotalBars,
                        currentSpeed = replaySpeed,
                        onTogglePlay = onToggleReplayPlay,
                        onStepForward = onStepReplayForward,
                        onStepBackward = onStepReplayBackward,
                        onSetSpeed = onSetReplaySpeed,
                        onSeek = onSeekReplay,
                        onExitReplay = onExitReplay,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 36.dp)
                    )
                }
            }

            // --- 5. Expandable Technical Analysis & Key Metrics Panel ---
            AnimatedVisibility(
                visible = showTechnicalStatsSheet,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TvDarkSurface)
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    TechnicalGauge(analysis = technicalAnalysis)

                    Spacer(modifier = Modifier.height(10.dp))

                    // 24h Statistics Grid
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TvDarkBackground, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("24h High", fontSize = 10.sp, color = TvTextMuted)
                            Text(formatPrice(asset.high24h), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvBullGreen)
                        }
                        Column {
                            Text("24h Low", fontSize = 10.sp, color = TvTextMuted)
                            Text(formatPrice(asset.low24h), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvBearRed)
                        }
                        Column {
                            Text("24h Volume", fontSize = 10.sp, color = TvTextMuted)
                            Text(asset.volume24h, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvTextPrimary)
                        }
                        Column {
                            Text("Market Cap", fontSize = 10.sp, color = TvTextMuted)
                            Text(asset.marketCap, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvTextPrimary)
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Symbol Selector Dialog
    if (showSymbolSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showSymbolSelectorDialog = false },
            title = { Text("Select Symbol", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.heightIn(max = 350.dp).verticalScroll(rememberScrollState())) {
                    allAssets.forEach { a ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectAsset(a.symbol)
                                    showSymbolSelectorDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(a.symbol, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(a.name, fontSize = 11.sp, color = TvTextSecondary)
                            }
                            Text(
                                formatPrice(a.price),
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = if (a.changePercent >= 0) TvBullGreen else TvBearRed
                            )
                        }
                        HorizontalDivider(color = TvBorderColor)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSymbolSelectorDialog = false }) {
                    Text("Close", color = TvBrandBlueLight)
                }
            },
            containerColor = TvDarkSurface
        )
    }

    // 2. Indicators (fx) Dialog
    if (showIndicatorsDialog) {
        AlertDialog(
            onDismissRequest = { showIndicatorsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Technical Indicators", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.background(TvBrandBlue, RoundedCornerShape(3.dp)).padding(4.dp)) {
                        Text("PRO+ UNLIMITED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    IndicatorToggleRow("Volume Bars", indicators.showVolume) { indicators = indicators.copy(showVolume = it) }
                    IndicatorToggleRow("EMA 20 (Exponential Moving Avg)", indicators.showEma20) { indicators = indicators.copy(showEma20 = it) }
                    IndicatorToggleRow("EMA 50 (Trend Line)", indicators.showEma50) { indicators = indicators.copy(showEma50 = it) }
                    IndicatorToggleRow("EMA 200 (Long Term Baseline)", indicators.showEma200) { indicators = indicators.copy(showEma200 = it) }
                    IndicatorToggleRow("Bollinger Bands (20, 2.0)", indicators.showBollinger) { indicators = indicators.copy(showBollinger = it) }
                    IndicatorToggleRow("RSI (Relative Strength Index 14)", indicators.showRsi) { indicators = indicators.copy(showRsi = it) }
                    IndicatorToggleRow("MACD (12, 26, 9 Histogram)", indicators.showMacd) { indicators = indicators.copy(showMacd = it) }
                    IndicatorToggleRow("VWAP (Volume Weighted Avg)", indicators.showVwap) { indicators = indicators.copy(showVwap = it) }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showIndicatorsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TvBrandBlue)
                ) {
                    Text("Apply Indicators")
                }
            },
            containerColor = TvDarkSurface
        )
    }
}

@Composable
private fun DrawingToolChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) TvBrandBlue else TvDarkSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) Color.White else TvTextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) Color.White else TvTextSecondary
        )
    }
}

@Composable
private fun IndicatorToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, color = TvTextPrimary)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TvBrandBlue,
                uncheckedThumbColor = TvTextMuted,
                uncheckedTrackColor = TvSurfaceVariant
            )
        )
    }
}

private fun formatPrice(price: Double, currency: String = "USD"): String {
    val prefix = if (currency == "INR") "₹" else "$"
    val num = when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.2f", price)
        else -> String.format(Locale.US, "%.4f", price)
    }
    return "$prefix$num"
}
