package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
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
import com.example.model.AssetClass
import com.example.model.MarketAsset
import com.example.model.TechnicalRating
import com.example.ui.components.MiniSparkline
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    assets: List<MarketAsset>,
    selectedAsset: MarketAsset,
    onSelectAsset: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onNavigateToChart: () -> Unit,
    isLiveMarketConnected: Boolean = true,
    lastLiveSyncTimestamp: String = "Just Now",
    onRefreshMarketData: () -> Unit = {},
    onOpenExportGuide: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AssetClass.ALL) }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val filteredAssets = remember(assets, searchQuery, selectedCategory, showOnlyFavorites) {
        assets.filter { asset ->
            val matchesCategory = (selectedCategory == AssetClass.ALL || asset.assetClass == selectedCategory)
            val matchesSearch = asset.symbol.contains(searchQuery, ignoreCase = true) ||
                    asset.name.contains(searchQuery, ignoreCase = true)
            val matchesFav = if (showOnlyFavorites) asset.isFavorite else true
            matchesCategory && matchesSearch && matchesFav
        }
    }

    Scaffold(
        containerColor = TvDarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar: Search & Quick Pro Screener Status
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WATCHLIST",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(TvBrandBlue, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRO+",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        // Real-time live status indicator
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(TvSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isLiveMarketConnected) TvBullGreen else TvBearRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLiveMarketConnected) "LIVE · $lastLiveSyncTimestamp" else "OFFLINE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiveMarketConnected) TvBullGreen else TvBearRed
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshMarketData,
                            modifier = Modifier.size(28.dp).testTag("watchlist_refresh_btn")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Sync Live Market",
                                tint = TvBrandBlueLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TvBullGreenBg)
                                .clickable { onOpenExportGuide() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("watchlist_export_apk_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = TvBullGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export APK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TvBullGreen)
                            }
                        }

                        // Favorites toggle filter
                        IconButton(
                            onClick = { showOnlyFavorites = !showOnlyFavorites },
                            modifier = Modifier.testTag("filter_favorites_btn")
                        ) {
                            Icon(
                                imageVector = if (showOnlyFavorites) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Filter Favorites",
                                tint = if (showOnlyFavorites) TvGold else TvTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_asset_input"),
                    placeholder = { Text("Search 10,000+ symbols, crypto, stocks...", color = TvTextMuted, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TvTextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TvTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TvDarkBackground,
                        unfocusedContainerColor = TvDarkBackground,
                        focusedBorderColor = TvBrandBlue,
                        unfocusedBorderColor = TvBorderColor,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = TvTextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Asset Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AssetClass.values()) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            label = {
                                Text(
                                    text = category.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TvBrandBlue,
                                selectedLabelColor = Color.White,
                                containerColor = TvDarkBackground,
                                labelColor = TvTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) TvBrandBlue else TvBorderColor
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Market Summary Ticker Strip
            MarketSummaryTickerStrip(assets = assets)

            // Watchlist Asset Rows
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("watchlist_list"),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredAssets, key = { it.symbol }) { asset ->
                    WatchlistRow(
                        asset = asset,
                        isSelected = asset.symbol == selectedAsset.symbol,
                        onClick = {
                            onSelectAsset(asset.symbol)
                            onNavigateToChart()
                        },
                        onToggleFavorite = { onToggleFavorite(asset.symbol) }
                    )
                    HorizontalDivider(color = TvBorderColor.copy(alpha = 0.4f), thickness = 0.8.dp)
                }

                if (filteredAssets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.FilterList, contentDescription = null, tint = TvTextMuted, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No symbols found matching filter", color = TvTextSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarketSummaryTickerStrip(assets: List<MarketAsset>) {
    val keySymbols = listOf("BTC/USDT", "ETH/USDT", "NVDA", "SPX 500", "XAU/USD")
    val tickerAssets = assets.filter { it.symbol in keySymbols }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(TvDarkBackground)
            .padding(vertical = 6.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(tickerAssets) { asset ->
            val isBull = asset.changePercent >= 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(TvDarkSurface, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = asset.symbol.substringBefore("/"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TvTextPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatAssetPrice(asset.price),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${if (isBull) "+" else ""}${String.format("%.1f%%", asset.changePercent)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isBull) TvBullGreen else TvBearRed
                )
            }
        }
    }
}

@Composable
private fun WatchlistRow(
    asset: MarketAsset,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isPositive = asset.changePercent >= 0

    // Animated background flash for real-time tick pulse
    val flashColor by animateColorAsState(
        targetValue = when (asset.lastUpdateDirection) {
            1 -> TvBullGreen.copy(alpha = 0.22f)
            -1 -> TvBearRed.copy(alpha = 0.22f)
            else -> if (isSelected) TvBrandBlue.copy(alpha = 0.12f) else Color.Transparent
        },
        animationSpec = tween(durationMillis = 600),
        label = "price_flash"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(flashColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("asset_row_${asset.symbol}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Star Button
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (asset.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = "Favorite",
                tint = if (asset.isFavorite) TvGold else TvTextMuted,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Asset Symbol, Name and Exchange
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = asset.symbol,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .background(TvSurfaceVariant, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = asset.exchange,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TvTextSecondary
                    )
                }
            }
            Text(
                text = asset.name,
                fontSize = 11.sp,
                color = TvTextSecondary,
                maxLines = 1
            )
        }

        // Mini Sparkline
        MiniSparkline(
            points = asset.sparkline,
            isPositive = isPositive,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Price and 24h % Change Pill
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.widthIn(min = 82.dp)
        ) {
            Text(
                text = formatAssetPrice(asset.price, asset.currency),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isPositive) TvBullGreen else TvBearRed)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f%%", asset.changePercent)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                )
            }
        }
    }
}

private fun formatAssetPrice(price: Double, currency: String = "USD"): String {
    val prefix = if (currency == "INR") "₹" else "$"
    val num = when {
        price >= 1000.0 -> String.format(Locale.US, "%,.2f", price)
        price >= 1.0 -> String.format(Locale.US, "%.2f", price)
        else -> String.format(Locale.US, "%.4f", price)
    }
    return "$prefix$num"
}
