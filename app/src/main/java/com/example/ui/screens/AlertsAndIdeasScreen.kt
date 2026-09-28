package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
fun AlertsAndIdeasScreen(
    alerts: List<PriceAlert>,
    ideas: List<TradingIdea>,
    news: List<MarketNewsItem>,
    assets: List<MarketAsset>,
    selectedAsset: MarketAsset,
    onAddAlert: (String, Double, AlertCondition, String) -> Unit,
    onToggleAlert: (String) -> Unit,
    onRemoveAlert: (String) -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) } // 0: Price Alerts, 1: Ideas Stream, 2: News
    var showCreateAlertDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = TvDarkBackground,
        floatingActionButton = {
            if (activeSubTab == 0) {
                FloatingActionButton(
                    onClick = { showCreateAlertDialog = true },
                    containerColor = TvBrandBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("create_alert_fab")
                ) {
                    Icon(Icons.Default.AddAlert, contentDescription = "Create Alert")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header Tabs
            TabRow(
                selectedTabIndex = activeSubTab,
                containerColor = TvDarkSurface,
                contentColor = TvBrandBlueLight,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                        color = TvBrandBlue
                    )
                }
            ) {
                Tab(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    text = { Text("Alerts (${alerts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    text = { Text("Trading Ideas", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = activeSubTab == 2,
                    onClick = { activeSubTab = 2 },
                    text = { Text("Market News", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // Tab Content
            when (activeSubTab) {
                0 -> AlertsTabContent(
                    alerts = alerts,
                    onToggleAlert = onToggleAlert,
                    onRemoveAlert = onRemoveAlert
                )
                1 -> IdeasTabContent(ideas = ideas)
                2 -> NewsTabContent(news = news)
            }
        }
    }

    // Create Alert Dialog
    if (showCreateAlertDialog) {
        CreateAlertDialog(
            assets = assets,
            initialAsset = selectedAsset,
            onDismiss = { showCreateAlertDialog = false },
            onConfirm = { symbol, target, condition, note ->
                onAddAlert(symbol, target, condition, note)
                showCreateAlertDialog = false
            }
        )
    }
}

@Composable
private fun AlertsTabContent(
    alerts: List<PriceAlert>,
    onToggleAlert: (String) -> Unit,
    onRemoveAlert: (String) -> Unit
) {
    if (alerts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = TvTextMuted, modifier = Modifier.size(52.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text("No price alerts set", color = TvTextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Tap the + button to create a real-time price trigger", color = TvTextMuted, fontSize = 12.sp)
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
        items(alerts, key = { it.id }) { alert ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(alert.symbol, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(TvBrandBlueBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(alert.condition.label, fontSize = 10.sp, color = TvBrandBlueLight, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target: $${formatPrice(alert.targetPrice)}",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TvGold
                    )
                    Text(
                        text = alert.note,
                        fontSize = 11.sp,
                        color = TvTextSecondary,
                        maxLines = 1
                    )
                    if (alert.isTriggered) {
                        Text("TRIGGERED", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TvBullGreen)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = alert.isActive,
                        onCheckedChange = { onToggleAlert(alert.id) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TvBrandBlue,
                            uncheckedThumbColor = TvTextMuted,
                            uncheckedTrackColor = TvSurfaceVariant
                        )
                    )
                    IconButton(onClick = { onRemoveAlert(alert.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TvTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun IdeasTabContent(ideas: List<TradingIdea>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(ideas, key = { it.id }) { idea ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                // Author row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(TvBrandBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(idea.author.first().toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(idea.author, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(idea.authorBadge, fontSize = 10.sp, color = TvBrandBlueLight)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (idea.bias == PositionType.LONG) TvBullGreenBg else TvBearRedBg,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (idea.bias == PositionType.LONG) "BULLISH LONG" else "BEARISH SHORT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (idea.bias == PositionType.LONG) TvBullGreen else TvBearRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title & Symbol
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(TvSurfaceVariant, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(idea.symbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TvTextPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(idea.timeframe, fontSize = 11.sp, color = TvTextSecondary)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = idea.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = idea.content,
                    fontSize = 12.sp,
                    color = TvTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tags & Social stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(idea.tags.joinToString(" "), fontSize = 11.sp, color = TvBrandBlueLight)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, contentDescription = "Likes", tint = TvTextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${idea.likes}", fontSize = 11.sp, color = TvTextMuted)
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = TvTextMuted, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${idea.comments}", fontSize = 11.sp, color = TvTextMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsTabContent(news: List<MarketNewsItem>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(news, key = { it.id }) { item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item.source, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TvBrandBlueLight)
                    Box(
                        modifier = Modifier
                            .background(
                                when (item.sentiment) {
                                    NewsSentiment.BULLISH -> TvBullGreenBg
                                    NewsSentiment.BEARISH -> TvBearRedBg
                                    NewsSentiment.NEUTRAL -> TvSurfaceVariant
                                },
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.sentiment.name,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (item.sentiment) {
                                NewsSentiment.BULLISH -> TvBullGreen
                                NewsSentiment.BEARISH -> TvBearRed
                                NewsSentiment.NEUTRAL -> TvTextSecondary
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = item.headline,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item.category, fontSize = 10.sp, color = TvTextMuted)
                    Text(item.timeAgo, fontSize = 10.sp, color = TvTextMuted)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAlertDialog(
    assets: List<MarketAsset>,
    initialAsset: MarketAsset,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, AlertCondition, String) -> Unit
) {
    var symbol by remember { mutableStateOf(initialAsset.symbol) }
    var priceText by remember { mutableStateOf(formatPrice(initialAsset.price)) }
    var condition by remember { mutableStateOf(AlertCondition.CROSS_ABOVE) }
    var note by remember { mutableStateOf("") }
    var showConditionMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Price Alert", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Symbol: $symbol", color = TvBrandBlueLight, fontWeight = FontWeight.Bold)

                Text("Trigger Condition", fontSize = 11.sp, color = TvTextSecondary)
                Box {
                    Button(
                        onClick = { showConditionMenu = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TvSurfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(condition.label, color = Color.White)
                    }

                    DropdownMenu(
                        expanded = showConditionMenu,
                        onDismissRequest = { showConditionMenu = false },
                        modifier = Modifier.background(TvDarkSurface)
                    ) {
                        AlertCondition.values().forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.label, color = Color.White) },
                                onClick = {
                                    condition = c
                                    showConditionMenu = false
                                }
                            )
                        }
                    }
                }

                Text("Target Price ($)", fontSize = 11.sp, color = TvTextSecondary)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TvDarkBackground,
                        unfocusedContainerColor = TvDarkBackground,
                        focusedBorderColor = TvBrandBlue,
                        unfocusedBorderColor = TvBorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("alert_price_input")
                )

                Text("Note / Message", fontSize = 11.sp, color = TvTextSecondary)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("e.g. Breakout retest", color = TvTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TvDarkBackground,
                        unfocusedContainerColor = TvDarkBackground,
                        focusedBorderColor = TvBrandBlue,
                        unfocusedBorderColor = TvBorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = priceText.toDoubleOrNull() ?: initialAsset.price
                    onConfirm(symbol, target, condition, note)
                },
                colors = ButtonDefaults.buttonColors(containerColor = TvBrandBlue)
            ) {
                Text("Set Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TvTextSecondary)
            }
        },
        containerColor = TvDarkSurface
    )
}

private fun formatPrice(price: Double): String {
    return when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.2f", price)
        else -> String.format(Locale.US, "%.4f", price)
    }
}
