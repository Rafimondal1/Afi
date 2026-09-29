package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MarketRepository
import com.example.model.PositionType
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    private val repository = MarketRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(repository = repository)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(repository: MarketRepository = remember { MarketRepository() }) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    var showExportGuideDialog by rememberSaveable { mutableStateOf(false) }

    val assets by repository.assets.collectAsStateWithLifecycle()
    val selectedAsset by repository.selectedAsset.collectAsStateWithLifecycle()
    val selectedTimeframe by repository.selectedTimeframe.collectAsStateWithLifecycle()
    val currentCandles by repository.currentCandles.collectAsStateWithLifecycle()
    val paperBalance by repository.paperBalance.collectAsStateWithLifecycle()
    val openPositions by repository.openPositions.collectAsStateWithLifecycle()
    val tradeHistory by repository.tradeHistory.collectAsStateWithLifecycle()
    val alerts by repository.alerts.collectAsStateWithLifecycle()
    val triggeredAlert by repository.triggeredAlertEvent.collectAsStateWithLifecycle()
    val ideas by repository.ideas.collectAsStateWithLifecycle()
    val news by repository.news.collectAsStateWithLifecycle()

    // Bar Replay / Backtest Engine state
    val isReplayMode by repository.isReplayMode.collectAsStateWithLifecycle()
    val isReplayPlaying by repository.isReplayPlaying.collectAsStateWithLifecycle()
    val isReplayTotalBars by repository.replayTotalBars.collectAsStateWithLifecycle()
    val replayIndex by repository.replayIndex.collectAsStateWithLifecycle()
    val replayTotalBars by repository.replayTotalBars.collectAsStateWithLifecycle()
    val replaySpeed by repository.replaySpeed.collectAsStateWithLifecycle()

    // Real-time live market telemetry
    val isLiveMarketConnected by repository.isLiveMarketConnected.collectAsStateWithLifecycle()
    val liveDataFeedSource by repository.liveDataFeedSource.collectAsStateWithLifecycle()
    val lastLiveSyncTimestamp by repository.lastLiveSyncTimestamp.collectAsStateWithLifecycle()
    val isLoadingCandles by repository.isLoadingCandles.collectAsStateWithLifecycle()

    // Broker Integration (Paper Trading vs Dhan Broker vs Multi-Broker)
    val activeBroker by repository.activeBroker.collectAsStateWithLifecycle()
    val dhanAccount by repository.dhanAccount.collectAsStateWithLifecycle()
    val connectedBrokers by repository.connectedBrokers.collectAsStateWithLifecycle()

    val technicalAnalysis = remember(selectedAsset.symbol, currentCandles) {
        repository.getTechnicalAnalysis(selectedAsset.symbol)
    }

    val navItems = listOf(
        NavigationItem("Watchlist", Icons.Default.FormatListBulleted, "tab_watchlist"),
        NavigationItem("Chart", Icons.Default.ShowChart, "tab_chart"),
        NavigationItem("Trade", Icons.Default.AccountBalanceWallet, "tab_trade"),
        NavigationItem("Alerts", Icons.Default.NotificationsNone, "tab_alerts"),
        NavigationItem("Pro+", Icons.Default.Stars, "tab_premium")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_navigation_bar"),
                containerColor = TvDarkSurface,
                contentColor = Color.White,
                tonalElevation = 8.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) TvBrandBlueLight else TvTextSecondary
                            )
                        },
                        label = {
                            Text(
                                item.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TvBrandBlueLight else TvTextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TvBrandBlueLight,
                            selectedTextColor = TvBrandBlueLight,
                            indicatorColor = TvBrandBlueBg,
                            unselectedIconColor = TvTextSecondary,
                            unselectedTextColor = TvTextSecondary
                        ),
                        modifier = Modifier.testTag(item.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = selectedTab, label = "tab_transition") { tab ->
                when (tab) {
                    0 -> WatchlistScreen(
                        assets = assets,
                        selectedAsset = selectedAsset,
                        onSelectAsset = { repository.selectAsset(it) },
                        onToggleFavorite = { repository.toggleFavorite(it) },
                        onNavigateToChart = { selectedTab = 1 },
                        isLiveMarketConnected = isLiveMarketConnected,
                        lastLiveSyncTimestamp = lastLiveSyncTimestamp,
                        onRefreshMarketData = { repository.refreshMarketData() },
                        onOpenExportGuide = { showExportGuideDialog = true }
                    )
                    1 -> ChartScreen(
                        asset = selectedAsset,
                        allAssets = assets,
                        candles = currentCandles,
                        selectedTimeframe = selectedTimeframe,
                        onSelectTimeframe = { repository.selectTimeframe(it) },
                        onSelectAsset = { repository.selectAsset(it) },
                        technicalAnalysis = technicalAnalysis,
                        onQuickTrade = { type, qty ->
                            repository.placePaperOrder(
                                symbol = selectedAsset.symbol,
                                type = type,
                                quantity = qty,
                                leverage = 1
                            )
                        },
                        isReplayMode = isReplayMode,
                        isReplayPlaying = isReplayPlaying,
                        replayIndex = replayIndex,
                        replayTotalBars = replayTotalBars,
                        replaySpeed = replaySpeed,
                        onStartReplay = { repository.startReplay() },
                        onExitReplay = { repository.exitReplay() },
                        onToggleReplayPlay = { repository.toggleReplayPlay() },
                        onStepReplayForward = { repository.stepReplayForward() },
                        onStepReplayBackward = { repository.stepReplayBackward() },
                        onSetReplaySpeed = { repository.setReplaySpeed(it) },
                        onSeekReplay = { repository.jumpReplayTo(it) },
                        isLiveMarketConnected = isLiveMarketConnected,
                        liveDataFeedSource = liveDataFeedSource,
                        lastLiveSyncTimestamp = lastLiveSyncTimestamp,
                        isLoadingCandles = isLoadingCandles,
                        onRefreshMarketData = { repository.refreshMarketData() },
                        onOpenExportGuide = { showExportGuideDialog = true }
                    )
                    2 -> PaperTradingScreen(
                        balance = paperBalance,
                        openPositions = openPositions,
                        tradeHistory = tradeHistory,
                        assets = assets,
                        selectedAsset = selectedAsset,
                        activeBroker = activeBroker,
                        dhanAccount = dhanAccount,
                        connectedBrokers = connectedBrokers,
                        onSelectBroker = { repository.selectBroker(it) },
                        onConnectDhan = { id, pin -> repository.connectDhanAccount(id, pin) },
                        onDisconnectDhan = { repository.disconnectDhanAccount() },
                        onConnectBroker = { type, id, token -> repository.connectBroker(type, id, token) },
                        onDisconnectBroker = { repository.disconnectBroker(it) },
                        onPlaceOrder = { sym, type, qty, lev, sl, tp ->
                            repository.placePaperOrder(sym, type, qty, lev, sl, tp)
                        },
                        onClosePosition = { repository.closePaperPosition(it) },
                        onResetAccount = { repository.resetPaperAccount() }
                    )
                    3 -> AlertsAndIdeasScreen(
                        alerts = alerts,
                        ideas = ideas,
                        news = news,
                        assets = assets,
                        selectedAsset = selectedAsset,
                        onAddAlert = { sym, target, cond, note ->
                            repository.addAlert(sym, target, cond, note)
                        },
                        onToggleAlert = { repository.toggleAlert(it) },
                        onRemoveAlert = { repository.removeAlert(it) }
                    )
                    4 -> PremiumScreen(
                        onNavigateToChart = { selectedTab = 1 },
                        onOpenExportGuide = { showExportGuideDialog = true }
                    )
                    else -> WatchlistScreen(
                        assets = assets,
                        selectedAsset = selectedAsset,
                        onSelectAsset = { repository.selectAsset(it) },
                        onToggleFavorite = { repository.toggleFavorite(it) },
                        onNavigateToChart = { selectedTab = 1 },
                        onOpenExportGuide = { showExportGuideDialog = true }
                    )
                }
            }

            // APK Export Guide Dialog
            if (showExportGuideDialog) {
                com.example.ui.components.ApkExportGuideDialog(
                    onDismiss = { showExportGuideDialog = false }
                )
            }

            // Real-time Triggered Alert Popup
            val activeAlert = triggeredAlert
            if (activeAlert != null) {
                AlertDialog(
                    onDismissRequest = { repository.dismissTriggeredAlert() },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = TvGold) },
                    title = { Text("Price Alert Triggered!", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("${activeAlert.symbol} has reached your target of $${activeAlert.targetPrice}", color = Color.White)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(activeAlert.note, color = TvTextSecondary, fontSize = 12.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                repository.selectAsset(activeAlert.symbol)
                                repository.dismissTriggeredAlert()
                                selectedTab = 1 // Go to chart
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TvBrandBlue)
                        ) {
                            Text("Open Chart")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { repository.dismissTriggeredAlert() }) {
                            Text("Dismiss", color = TvTextSecondary)
                        }
                    },
                    containerColor = TvDarkSurface
                )
            }
        }
    }
}

data class NavigationItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tag: String
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
