package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTradingScreen(
    balance: Double,
    openPositions: List<PaperTrade>,
    tradeHistory: List<PaperTrade>,
    assets: List<MarketAsset>,
    selectedAsset: MarketAsset,
    activeBroker: BrokerType = BrokerType.PAPER_TRADING,
    dhanAccount: DhanAccount = DhanAccount(),
    connectedBrokers: Map<BrokerType, ConnectedBrokerAccount> = emptyMap(),
    onSelectBroker: (BrokerType) -> Unit = {},
    onConnectDhan: (String, String) -> Boolean = { _, _ -> true },
    onDisconnectDhan: () -> Unit = {},
    onConnectBroker: (BrokerType, String, String) -> Boolean = { _, _, _ -> true },
    onDisconnectBroker: (BrokerType) -> Unit = {},
    onPlaceOrder: (String, PositionType, Double, Int, Double?, Double?) -> Boolean,
    onClosePosition: (String) -> Unit,
    onResetAccount: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Terminal/Order, 1: Positions, 2: History
    var tradeType by remember { mutableStateOf(PositionType.LONG) }
    var chosenSymbol by remember { mutableStateOf(selectedAsset.symbol) }
    var quantityText by remember { mutableStateOf(if (selectedAsset.symbol.contains("NIFTY 50")) "25" else if (selectedAsset.symbol.contains("BANK NIFTY")) "15" else "1.0") }
    var leverage by remember { mutableIntStateOf(1) }
    var stopLossText by remember { mutableStateOf("") }
    var takeProfitText by remember { mutableStateOf("") }
    var orderStatusMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showBrokerModal by remember { mutableStateOf<BrokerType?>(null) }

    // Dynamic inputs for broker connect
    var brokerInputAccountId by remember { mutableStateOf("DHAN108429") }
    var brokerInputToken by remember { mutableStateOf("dhan_live_jwt_v2_active") }

    val currentAsset = assets.find { it.symbol == chosenSymbol } ?: selectedAsset
    val isIndianOrDhan = currentAsset.currency == "INR" || activeBroker == BrokerType.DHAN_BROKER
    val currencyPrefix = if (isIndianOrDhan) "₹" else "$"

    val totalUnrealizedPnl = openPositions.sumOf { it.pnl }
    val totalMarginUsed = openPositions.sumOf { it.margin }
    val currentBalance = if (activeBroker == BrokerType.DHAN_BROKER) dhanAccount.availableCash else balance
    val equity = currentBalance + totalMarginUsed + totalUnrealizedPnl

    Scaffold(
        containerColor = TvDarkBackground,
        snackbarHost = {
            if (orderStatusMessage != null) {
                Snackbar(
                    containerColor = TvDarkSurface,
                    contentColor = Color.White,
                    action = {
                        TextButton(onClick = { orderStatusMessage = null }) {
                            Text("OK", color = TvBrandBlueLight)
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(orderStatusMessage ?: "")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // --- 1. Multi-Broker Switcher Strip (TradingView Pro Style) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scrollable brokers row
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(BrokerType.values()) { broker ->
                        val isSelected = activeBroker == broker
                        val isConnected = when (broker) {
                            BrokerType.PAPER_TRADING -> true
                            BrokerType.DHAN_BROKER -> dhanAccount.isConnected || (connectedBrokers[broker]?.isConnected == true)
                            else -> connectedBrokers[broker]?.isConnected == true
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isSelected && broker == BrokerType.DHAN_BROKER -> TvBullGreen
                                        isSelected -> TvBrandBlue
                                        else -> TvSurfaceVariant
                                    }
                                )
                                .clickable {
                                    if (isConnected) {
                                        onSelectBroker(broker)
                                    } else {
                                        brokerInputAccountId = when (broker) {
                                            BrokerType.DHAN_BROKER -> "DHAN108429"
                                            BrokerType.ZERODHA_BROKER -> "ZR9482"
                                            BrokerType.ANGEL_ONE -> "ANG83910"
                                            BrokerType.BINANCE_BROKER -> "BN-API-994"
                                            BrokerType.INTERACTIVE_BROKERS -> "U849201"
                                            BrokerType.PAPER_TRADING -> "TV-SIM"
                                        }
                                        showBrokerModal = broker
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("broker_pill_${broker.name}")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = broker.shortName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TvTextSecondary
                                )
                                if (broker.isOfficialPartner) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("⚡", fontSize = 10.sp)
                                }
                                if (isConnected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else TvBullGreen)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Account Reset / Manage button
                if (activeBroker == BrokerType.PAPER_TRADING) {
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.size(28.dp).testTag("reset_account_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Demo", tint = TvTextSecondary)
                    }
                } else {
                    TextButton(
                        onClick = {
                            brokerInputAccountId = if (activeBroker == BrokerType.DHAN_BROKER) dhanAccount.clientId else "DHAN108429"
                            showBrokerModal = activeBroker
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Manage",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeBroker == BrokerType.DHAN_BROKER) TvBullGreen else TvBrandBlueLight
                        )
                    }
                }
            }

            HorizontalDivider(color = TvBorderColor, thickness = 1.dp)

            // --- 2. Active Portfolio Summary Card ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (activeBroker == BrokerType.DHAN_BROKER) "DHAN BROKERAGE" else "PAPER TRADING",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(if (activeBroker == BrokerType.DHAN_BROKER) TvBullGreenBg else TvBrandBlueBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (activeBroker == BrokerType.DHAN_BROKER) (if (dhanAccount.isConnected) "LIVE LINKED" else "UNLINKED") else "DEMO ACCOUNT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeBroker == BrokerType.DHAN_BROKER) TvBullGreen else TvBrandBlueLight
                                )
                            }
                        }
                        if (activeBroker == BrokerType.DHAN_BROKER && dhanAccount.isConnected) {
                            Text(
                                text = "Client ID: ${dhanAccount.clientId} · ${dhanAccount.brokerStatus}",
                                fontSize = 10.sp,
                                color = TvTextSecondary
                            )
                        }
                    }

                    if (activeBroker == BrokerType.DHAN_BROKER && !dhanAccount.isConnected) {
                        Button(
                            onClick = { showBrokerModal = BrokerType.DHAN_BROKER },
                            colors = ButtonDefaults.buttonColors(containerColor = TvBullGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Connect Dhan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Account Equity
                Text("TOTAL PORTFOLIO VALUE", fontSize = 10.sp, color = TvTextSecondary, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$currencyPrefix${String.format(Locale.US, "%,.2f", equity)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${if (totalUnrealizedPnl >= 0) "+" else ""}$currencyPrefix${String.format(Locale.US, "%,.2f", totalUnrealizedPnl)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (totalUnrealizedPnl >= 0) TvBullGreen else TvBearRed
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Balance & Margin breakdown
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvDarkBackground)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Available Margin", fontSize = 10.sp, color = TvTextSecondary)
                        Text(
                            text = "$currencyPrefix${String.format(Locale.US, "%,.2f", currentBalance)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                    Column {
                        Text("Used Margin", fontSize = 10.sp, color = TvTextSecondary)
                        Text(
                            text = "$currencyPrefix${String.format(Locale.US, "%,.2f", totalMarginUsed)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TvTextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Open Positions", fontSize = 10.sp, color = TvTextSecondary)
                        Text(
                            text = "${openPositions.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvBrandBlueLight
                        )
                    }
                }
            }

            HorizontalDivider(color = TvBorderColor, thickness = 1.dp)

            // --- 3. Tabs: Order Ticket / Open Positions / History ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TvDarkSurface,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TvBrandBlueLight,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Order Ticket", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Positions (${openPositions.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (openPositions.isNotEmpty()) TvBullGreen else TvTextSecondary
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History (${tradeHistory.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            HorizontalDivider(color = TvBorderColor, thickness = 1.dp)

            // --- 4. Tab Content ---
            when (selectedTab) {
                0 -> OrderTicketContent(
                    assets = assets,
                    selectedAsset = currentAsset,
                    tradeType = tradeType,
                    onTradeTypeChange = { tradeType = it },
                    chosenSymbol = chosenSymbol,
                    onSymbolChange = { chosenSymbol = it },
                    quantityText = quantityText,
                    onQuantityChange = { quantityText = it },
                    leverage = leverage,
                    onLeverageChange = { leverage = it },
                    stopLossText = stopLossText,
                    onStopLossChange = { stopLossText = it },
                    takeProfitText = takeProfitText,
                    onTakeProfitChange = { takeProfitText = it },
                    availableBalance = currentBalance,
                    activeBroker = activeBroker,
                    currencyPrefix = currencyPrefix,
                    onSubmitOrder = {
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        val sl = stopLossText.toDoubleOrNull()
                        val tp = takeProfitText.toDoubleOrNull()
                        val success = onPlaceOrder(chosenSymbol, tradeType, qty, leverage, sl, tp)
                        if (success) {
                            orderStatusMessage = "Order executed: ${tradeType.label} $qty $chosenSymbol via ${if (activeBroker == BrokerType.DHAN_BROKER) "Dhan" else "Paper"}"
                            selectedTab = 1
                        } else {
                            orderStatusMessage = "Order failed: Insufficient margin available in account"
                        }
                    }
                )
                1 -> OpenPositionsList(
                    positions = openPositions,
                    currencyPrefix = currencyPrefix,
                    onClosePosition = onClosePosition
                )
                2 -> TradeHistoryList(
                    history = tradeHistory,
                    currencyPrefix = currencyPrefix
                )
            }
        }
    }

    // Reset Account Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Demo Trading Account?") },
            text = { Text("This will close all open positions and reset your virtual balance to $100,000 USD.") },
            confirmButton = {
                TextButton(onClick = {
                    onResetAccount()
                    showResetDialog = false
                    orderStatusMessage = "Account reset to $100,000 USD"
                }) {
                    Text("Reset", color = TvBearRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TvTextSecondary)
                }
            }
        )
    }

    // Multi-Broker Connect Dialog Modal
    val modalBroker = showBrokerModal
    if (modalBroker != null) {
        val isDhan = modalBroker == BrokerType.DHAN_BROKER
        val isCurrentlyConnected = if (isDhan) dhanAccount.isConnected else connectedBrokers[modalBroker]?.isConnected == true

        AlertDialog(
            onDismissRequest = { showBrokerModal = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDhan) TvBullGreenBg else TvBrandBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isDhan) "⚡" else "🏦", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Connect ${modalBroker.title}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (isDhan) "Official TradingView Broker Partner in India" else modalBroker.subtitle,
                            fontSize = 10.sp,
                            color = TvTextSecondary
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isDhan) {
                            "Link your Dhan account to trade directly on TradingView charts with NSE, BSE, and MCX execution (Dhan HQ API v2)."
                        } else {
                            "Connect your ${modalBroker.shortName} trading account to execute real-time orders directly from charts."
                        },
                        fontSize = 12.sp,
                        color = TvTextPrimary
                    )

                    OutlinedTextField(
                        value = brokerInputAccountId,
                        onValueChange = { brokerInputAccountId = it },
                        label = { Text(if (isDhan) "Dhan Client ID (e.g. DHAN108429)" else "Account / Client ID", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("broker_client_id_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = TvTextPrimary,
                            focusedBorderColor = if (isDhan) TvBullGreen else TvBrandBlue,
                            unfocusedBorderColor = TvBorderColor
                        )
                    )

                    OutlinedTextField(
                        value = brokerInputToken,
                        onValueChange = { brokerInputToken = it },
                        label = { Text(if (isDhan) "Dhan HQ API Access Token / PIN" else "API Key / Token", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("broker_token_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = TvTextPrimary,
                            focusedBorderColor = if (isDhan) TvBullGreen else TvBrandBlue,
                            unfocusedBorderColor = TvBorderColor
                        )
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(TvDarkBackground)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (isDhan) TvBullGreen else TvBrandBlueLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDhan) "Verified Dhan HQ REST API & WebSocket Feed" else "Secure OAuth2 / API Key Gateway",
                            fontSize = 11.sp,
                            color = TvTextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isDhan) {
                            onConnectDhan(brokerInputAccountId, brokerInputToken)
                        } else {
                            onConnectBroker(modalBroker, brokerInputAccountId, brokerInputToken)
                        }
                        showBrokerModal = null
                        orderStatusMessage = "Connected to ${modalBroker.shortName} (${brokerInputAccountId})"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDhan) TvBullGreen else TvBrandBlue),
                    modifier = Modifier.testTag("confirm_broker_connect_btn")
                ) {
                    Text(if (isCurrentlyConnected) "Update Connection" else "Connect ${modalBroker.shortName}", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (isCurrentlyConnected) {
                    TextButton(onClick = {
                        if (isDhan) {
                            onDisconnectDhan()
                        } else {
                            onDisconnectBroker(modalBroker)
                        }
                        showBrokerModal = null
                        orderStatusMessage = "${modalBroker.shortName} disconnected. Reverted to Paper Trading."
                    }) {
                        Text("Disconnect", color = TvBearRed)
                    }
                } else {
                    TextButton(onClick = { showBrokerModal = null }) {
                        Text("Cancel", color = TvTextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun OrderTicketContent(
    assets: List<MarketAsset>,
    selectedAsset: MarketAsset,
    tradeType: PositionType,
    onTradeTypeChange: (PositionType) -> Unit,
    chosenSymbol: String,
    onSymbolChange: (String) -> Unit,
    quantityText: String,
    onQuantityChange: (String) -> Unit,
    leverage: Int,
    onLeverageChange: (Int) -> Unit,
    stopLossText: String,
    onStopLossChange: (String) -> Unit,
    takeProfitText: String,
    onTakeProfitChange: (String) -> Unit,
    availableBalance: Double,
    activeBroker: BrokerType,
    currencyPrefix: String,
    onSubmitOrder: () -> Unit
) {
    val qty = quantityText.toDoubleOrNull() ?: 1.0
    val totalOrderValue = selectedAsset.price * qty
    val marginRequired = totalOrderValue / leverage
    val hasEnoughFunds = availableBalance >= marginRequired

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Long / Short Switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvDarkSurface)
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { onTradeTypeChange(PositionType.LONG) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tradeType == PositionType.LONG) TvBullGreen else Color.Transparent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).testTag("order_type_long")
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BUY / LONG", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = { onTradeTypeChange(PositionType.SHORT) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (tradeType == PositionType.SHORT) TvBearRed else Color.Transparent
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).testTag("order_type_short")
                ) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SELL / SHORT", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Symbol Quick Picker
        item {
            Column {
                Text("INSTRUMENT", fontSize = 11.sp, color = TvTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvDarkSurface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedAsset.symbol, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(TvSurfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(selectedAsset.exchange, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TvTextSecondary)
                            }
                        }
                        Text(selectedAsset.name, fontSize = 11.sp, color = TvTextSecondary)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$currencyPrefix${String.format(Locale.US, "%,.2f", selectedAsset.price)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                        Text(
                            text = "${if (selectedAsset.changePercent >= 0) "+" else ""}${String.format("%.2f%%", selectedAsset.changePercent)}",
                            fontSize = 11.sp,
                            color = if (selectedAsset.changePercent >= 0) TvBullGreen else TvBearRed
                        )
                    }
                }
            }
        }

        // Quantity Input
        item {
            Column {
                Text("ORDER QUANTITY / CONTRACTS", fontSize = 11.sp, color = TvTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.fillMaxWidth().testTag("order_quantity_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TvDarkSurface,
                        unfocusedContainerColor = TvDarkSurface,
                        focusedBorderColor = TvBrandBlue,
                        unfocusedBorderColor = TvBorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = TvTextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))
                // Quick Lot / Qty presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = when {
                        selectedAsset.symbol.contains("NIFTY 50") -> listOf("25", "50", "75", "100")
                        selectedAsset.symbol.contains("BANK NIFTY") -> listOf("15", "30", "45", "60")
                        selectedAsset.currency == "INR" -> listOf("10", "25", "50", "100")
                        selectedAsset.price > 1000 -> listOf("0.1", "0.5", "1.0", "2.0")
                        else -> listOf("1", "5", "10", "25")
                    }
                    presets.forEach { preset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (quantityText == preset) TvBrandBlue else TvSurfaceVariant)
                                .clickable { onQuantityChange(preset) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedAsset.symbol.contains("NIFTY 50") && preset == "25") "25 (1 Lot)"
                                else if (selectedAsset.symbol.contains("BANK NIFTY") && preset == "15") "15 (1 Lot)"
                                else preset,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (quantityText == preset) Color.White else TvTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Leverage Selector
        item {
            Column {
                Text("LEVERAGE", fontSize = 11.sp, color = TvTextSecondary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 5, 10, 20).forEach { lev ->
                        val isSelected = leverage == lev
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) TvBrandBlue else TvDarkSurface)
                                .border(1.dp, if (isSelected) TvBrandBlue else TvBorderColor, RoundedCornerShape(8.dp))
                                .clickable { onLeverageChange(lev) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${lev}x",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) Color.White else TvTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Stop Loss & Take Profit (Optional)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("STOP LOSS (SL)", fontSize = 11.sp, color = TvTextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = onStopLossChange,
                        placeholder = { Text("Optional", fontSize = 12.sp, color = TvTextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("order_stop_loss_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TvDarkSurface,
                            unfocusedContainerColor = TvDarkSurface,
                            focusedBorderColor = TvBearRed,
                            unfocusedBorderColor = TvBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = TvTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("TAKE PROFIT (TP)", fontSize = 11.sp, color = TvTextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = takeProfitText,
                        onValueChange = onTakeProfitChange,
                        placeholder = { Text("Optional", fontSize = 12.sp, color = TvTextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("order_take_profit_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TvDarkSurface,
                            unfocusedContainerColor = TvDarkSurface,
                            focusedBorderColor = TvBullGreen,
                            unfocusedBorderColor = TvBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = TvTextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Order Summary Ticket
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvDarkSurface)
                    .border(1.dp, TvBorderColor, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Order Value", fontSize = 12.sp, color = TvTextSecondary)
                    Text("$currencyPrefix${String.format(Locale.US, "%,.2f", totalOrderValue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Margin Required", fontSize = 12.sp, color = TvTextSecondary)
                    Text("$currencyPrefix${String.format(Locale.US, "%,.2f", marginRequired)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Execution Gateway", fontSize = 12.sp, color = TvTextSecondary)
                    Text(
                        if (activeBroker == BrokerType.DHAN_BROKER) "Dhan Live Brokerage" else "Paper Trading Engine",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeBroker == BrokerType.DHAN_BROKER) TvBullGreen else TvBrandBlueLight
                    )
                }
            }
        }

        // Submit Button
        item {
            Button(
                onClick = onSubmitOrder,
                enabled = hasEnoughFunds && qty > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tradeType == PositionType.LONG) TvBullGreen else TvBearRed
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_order_btn")
            ) {
                Text(
                    text = "${tradeType.label.uppercase()} ${selectedAsset.symbol}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun OpenPositionsList(
    positions: List<PaperTrade>,
    currencyPrefix: String,
    onClosePosition: (String) -> Unit
) {
    if (positions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TvTextMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No Open Positions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Place a trade using the Order Ticket above to start tracking live P&L.", fontSize = 12.sp, color = TvTextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(positions, key = { it.id }) { pos ->
                val isProfit = pos.pnl >= 0
                val posCurrency = if (pos.currency == "INR") "₹" else currencyPrefix

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvDarkSurface)
                        .border(1.dp, if (isProfit) TvBullGreen.copy(alpha = 0.3f) else TvBearRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .testTag("position_item_${pos.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(pos.symbol, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(if (pos.type == PositionType.LONG) TvBullGreenBg else TvBearRedBg, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${pos.type.name} ${pos.leverage}X",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pos.type == PositionType.LONG) TvBullGreen else TvBearRed
                                )
                            }
                        }

                        // Floating PnL
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (isProfit) "+" else ""}$posCurrency${String.format(Locale.US, "%,.2f", pos.pnl)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isProfit) TvBullGreen else TvBearRed
                            )
                            Text(
                                text = "${if (isProfit) "+" else ""}${String.format("%.2f%%", pos.pnlPercent)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isProfit) TvBullGreen else TvBearRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Entry Price", fontSize = 10.sp, color = TvTextSecondary)
                            Text("$posCurrency${String.format(Locale.US, "%,.2f", pos.entryPrice)}", fontSize = 12.sp, color = TvTextPrimary)
                        }
                        Column {
                            Text("Mark Price", fontSize = 10.sp, color = TvTextSecondary)
                            Text("$posCurrency${String.format(Locale.US, "%,.2f", pos.currentPrice)}", fontSize = 12.sp, color = TvTextPrimary)
                        }
                        Column {
                            Text("Size", fontSize = 10.sp, color = TvTextSecondary)
                            Text("${pos.quantity}", fontSize = 12.sp, color = TvTextPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Button(
                                onClick = { onClosePosition(pos.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceVariant),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp).testTag("close_pos_${pos.id}")
                            ) {
                                Text("Close", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeHistoryList(
    history: List<PaperTrade>,
    currencyPrefix: String
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, contentDescription = null, tint = TvTextMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No Trade History Yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Closed trades and executed orders will be recorded here.", fontSize = 12.sp, color = TvTextSecondary)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { trade ->
                val isProfit = trade.pnl >= 0
                val posCurrency = if (trade.currency == "INR") "₹" else currencyPrefix

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvDarkSurface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(trade.symbol, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = trade.type.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (trade.type == PositionType.LONG) TvBullGreen else TvBearRed
                            )
                        }
                        Text(
                            text = "Size: ${trade.quantity} · Close: $posCurrency${trade.closePrice ?: trade.currentPrice}",
                            fontSize = 11.sp,
                            color = TvTextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (isProfit) "+" else ""}$posCurrency${String.format(Locale.US, "%,.2f", trade.pnl)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isProfit) TvBullGreen else TvBearRed
                        )
                        Text(
                            text = "${if (isProfit) "+" else ""}${String.format("%.2f%%", trade.pnlPercent)}",
                            fontSize = 10.sp,
                            color = if (isProfit) TvBullGreen else TvBearRed
                        )
                    }
                }
            }
        }
    }
}
