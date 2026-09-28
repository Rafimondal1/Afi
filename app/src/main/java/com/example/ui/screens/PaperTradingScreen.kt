package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.example.model.MarketAsset
import com.example.model.PaperTrade
import com.example.model.PositionType
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
    onPlaceOrder: (String, PositionType, Double, Int, Double?, Double?) -> Boolean,
    onClosePosition: (String) -> Unit,
    onResetAccount: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Terminal/Order, 1: Positions, 2: History
    var tradeType by remember { mutableStateOf(PositionType.LONG) }
    var chosenSymbol by remember { mutableStateOf(selectedAsset.symbol) }
    var quantityText by remember { mutableStateOf("1.0") }
    var leverage by remember { mutableIntStateOf(1) }
    var stopLossText by remember { mutableStateOf("") }
    var takeProfitText by remember { mutableStateOf("") }
    var orderStatusMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    val currentAsset = assets.find { it.symbol == chosenSymbol } ?: selectedAsset
    val totalUnrealizedPnl = openPositions.sumOf { it.pnl }
    val totalMarginUsed = openPositions.sumOf { it.margin }
    val equity = balance + totalMarginUsed + totalUnrealizedPnl

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
            // --- 1. Portfolio Header Card ---
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PAPER TRADING", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(TvBullGreenBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SIMULATED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TvBullGreen)
                        }
                    }

                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Account", tint = TvTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Account Equity
                Text("TOTAL EQUITY", fontSize = 11.sp, color = TvTextSecondary, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$${String.format(Locale.US, "%,.2f", equity)}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${if (totalUnrealizedPnl >= 0) "+" else ""}$${String.format(Locale.US, "%,.2f", totalUnrealizedPnl)}",
                        fontSize = 14.sp,
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
                        .background(TvDarkBackground, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Available Cash", fontSize = 10.sp, color = TvTextMuted)
                        Text("$${String.format(Locale.US, "%,.2f", balance)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvTextPrimary)
                    }
                    Column {
                        Text("Margin Used", fontSize = 10.sp, color = TvTextMuted)
                        Text("$${String.format(Locale.US, "%,.2f", totalMarginUsed)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvTextPrimary)
                    }
                    Column {
                        Text("Open Positions", fontSize = 10.sp, color = TvTextMuted)
                        Text("${openPositions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvBrandBlueLight)
                    }
                }
            }

            // --- 2. Terminal Tabs (New Order / Open Positions / Trade History) ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TvDarkSurface,
                contentColor = TvBrandBlueLight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TvBrandBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Order Ticket", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Positions (${openPositions.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History (${tradeHistory.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // --- 3. Tab Body ---
            when (selectedTab) {
                0 -> OrderTicketView(
                    asset = currentAsset,
                    allAssets = assets,
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
                    onSubmitOrder = {
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        val sl = stopLossText.toDoubleOrNull()
                        val tp = takeProfitText.toDoubleOrNull()
                        val success = onPlaceOrder(chosenSymbol, tradeType, qty, leverage, sl, tp)
                        if (success) {
                            orderStatusMessage = "Position Opened: $tradeType $qty $chosenSymbol @ $${formatPrice(currentAsset.price)}"
                            selectedTab = 1 // Switch to positions tab
                        } else {
                            orderStatusMessage = "Order Failed: Insufficient paper balance"
                        }
                    }
                )
                1 -> OpenPositionsList(
                    positions = openPositions,
                    onClosePosition = onClosePosition
                )
                2 -> TradeHistoryList(
                    history = tradeHistory
                )
            }
        }
    }

    // Reset Account Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Paper Trading Account?", color = Color.White) },
            text = { Text("This will reset your paper capital back to $100,000 USD and close all positions.", color = TvTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAccount()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TvBearRed)
                ) {
                    Text("Reset to $100,000")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TvTextSecondary)
                }
            },
            containerColor = TvDarkSurface
        )
    }
}

@Composable
private fun OrderTicketView(
    asset: MarketAsset,
    allAssets: List<MarketAsset>,
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
    onSubmitOrder: () -> Unit
) {
    var showSymbolDropdown by remember { mutableStateOf(false) }
    val qty = quantityText.toDoubleOrNull() ?: 1.0
    val marginRequired = (asset.price * qty) / leverage

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Buy Long / Sell Short Tab Switch
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (tradeType == PositionType.LONG) TvBullGreen else Color.Transparent)
                        .clickable { onTradeTypeChange(PositionType.LONG) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("BUY / LONG", fontWeight = FontWeight.Bold, color = if (tradeType == PositionType.LONG) Color.White else TvTextSecondary)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (tradeType == PositionType.SHORT) TvBearRed else Color.Transparent)
                        .clickable { onTradeTypeChange(PositionType.SHORT) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SELL / SHORT", fontWeight = FontWeight.Bold, color = if (tradeType == PositionType.SHORT) Color.White else TvTextSecondary)
                }
            }
        }

        // Symbol Picker & Market Price
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(8.dp))
                    .clickable { showSymbolDropdown = true }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ASSET", fontSize = 10.sp, color = TvTextMuted)
                    Text(chosenSymbol, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MARKET PRICE", fontSize = 10.sp, color = TvTextMuted)
                    Text(
                        "$${formatPrice(asset.price)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }

            DropdownMenu(
                expanded = showSymbolDropdown,
                onDismissRequest = { showSymbolDropdown = false },
                modifier = Modifier.background(TvDarkSurface)
            ) {
                allAssets.forEach { a ->
                    DropdownMenuItem(
                        text = { Text("${a.symbol} ($${formatPrice(a.price)})", color = Color.White) },
                        onClick = {
                            onSymbolChange(a.symbol)
                            showSymbolDropdown = false
                        }
                    )
                }
            }
        }

        // Quantity Selector
        item {
            Text("CONTRACT SIZE / QUANTITY", fontSize = 11.sp, color = TvTextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
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
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )

            // Quick quantity pills
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("0.1", "0.5", "1.0", "5.0", "10.0").forEach { q ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (quantityText == q) TvBrandBlue else TvDarkSurface)
                            .clickable { onQuantityChange(q) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(q, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Leverage Slider (1x - 50x)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LEVERAGE", fontSize = 11.sp, color = TvTextSecondary, fontWeight = FontWeight.Bold)
                Text("${leverage}x", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TvBrandBlueLight)
            }
            Slider(
                value = leverage.toFloat(),
                onValueChange = { onLeverageChange(it.toInt()) },
                valueRange = 1f..50f,
                steps = 48,
                colors = SliderDefaults.colors(
                    thumbColor = TvBrandBlue,
                    activeTrackColor = TvBrandBlue,
                    inactiveTrackColor = TvSurfaceVariant
                )
            )
        }

        // Risk Management (Take Profit & Stop Loss)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TAKE PROFIT (TP)", fontSize = 11.sp, color = TvBullGreen, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = takeProfitText,
                        onValueChange = onTakeProfitChange,
                        placeholder = { Text("Optional", color = TvTextMuted, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TvDarkSurface,
                            unfocusedContainerColor = TvDarkSurface,
                            focusedBorderColor = TvBullGreen,
                            unfocusedBorderColor = TvBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("STOP LOSS (SL)", fontSize = 11.sp, color = TvBearRed, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = onStopLossChange,
                        placeholder = { Text("Optional", color = TvTextMuted, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TvDarkSurface,
                            unfocusedContainerColor = TvDarkSurface,
                            focusedBorderColor = TvBearRed,
                            unfocusedBorderColor = TvBorderColor,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Margin & Order Summary
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Required Margin:", color = TvTextSecondary, fontSize = 12.sp)
                Text(
                    "$$String.format(Locale.US, \"%,.2f\", marginRequired)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        }

        // Submit Button
        item {
            Button(
                onClick = onSubmitOrder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("submit_order_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tradeType == PositionType.LONG) TvBullGreen else TvBearRed
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "EXECUTE ${if (tradeType == PositionType.LONG) "BUY" else "SELL"} ORDER",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun OpenPositionsList(
    positions: List<PaperTrade>,
    onClosePosition: (String) -> Unit
) {
    if (positions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TvTextMuted, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No open positions", color = TvTextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Use the Order Ticket to start paper trading", color = TvTextMuted, fontSize = 12.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(positions, key = { it.id }) { pos ->
            val isProfit = pos.pnl >= 0
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(10.dp))
                    .padding(14.dp)
                    .testTag("position_card_${pos.id}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (pos.type == PositionType.LONG) TvBullGreenBg else TvBearRedBg,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (pos.type == PositionType.LONG) "LONG" else "SHORT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (pos.type == PositionType.LONG) TvBullGreen else TvBearRed
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(pos.symbol, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${pos.leverage}x", fontSize = 11.sp, color = TvTextSecondary)
                    }

                    // PnL in USD and %
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${if (isProfit) "+" else ""}$${String.format(Locale.US, "%,.2f", pos.pnl)}",
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

                Spacer(modifier = Modifier.height(10.dp))

                // Entry Price vs Current Price
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TvDarkBackground, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Entry Price", fontSize = 10.sp, color = TvTextMuted)
                        Text("$${formatPrice(pos.entryPrice)}", fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("Current Price", fontSize = 10.sp, color = TvTextMuted)
                        Text("$${formatPrice(pos.currentPrice)}", fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("Size", fontSize = 10.sp, color = TvTextMuted)
                        Text("${pos.quantity}", fontSize = 12.sp, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onClosePosition(pos.id) },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceVariant),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text("Close Position", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TradeHistoryList(
    history: List<PaperTrade>
) {
    if (history.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No trade history recorded yet", color = TvTextSecondary, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(history) { trade ->
            val isProfit = trade.pnl >= 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (trade.type == PositionType.LONG) "BUY" else "SELL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (trade.type == PositionType.LONG) TvBullGreen else TvBearRed
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(trade.symbol, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                    }
                    Text(
                        text = "Closed @ $${formatPrice(trade.closePrice ?: trade.currentPrice)}",
                        fontSize = 11.sp,
                        color = TvTextSecondary
                    )
                }

                Text(
                    text = "${if (isProfit) "+" else ""}$${String.format(Locale.US, "%,.2f", trade.pnl)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isProfit) TvBullGreen else TvBearRed
                )
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.2f", price)
        else -> String.format(Locale.US, "%.4f", price)
    }
}
