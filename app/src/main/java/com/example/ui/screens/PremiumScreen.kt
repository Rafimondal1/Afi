package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun PremiumScreen(
    onNavigateToChart: () -> Unit,
    onOpenExportGuide: () -> Unit = {}
) {
    var replayPlaying by remember { mutableStateOf(false) }
    var magnetMode by remember { mutableStateOf(true) }
    var extendedHours by remember { mutableStateOf(true) }
    var soundAlerts by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TvDarkBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. VIP Premium Status Card ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1E3A8A), Color(0xFF1E222D), Color(0xFF0F172A))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TvBrandBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("TradeView Pro+", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Text("PREMIUM VIP UNLOCKED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TvGold)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .background(TvBullGreen, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "You have full access to all professional features: unlimited chart indicators, real-time seconds timeframes, multi-chart splits, paper trading simulator, and cloud price alerts.",
                        fontSize = 12.sp,
                        color = TvTextPrimary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onNavigateToChart,
                        colors = ButtonDefaults.buttonColors(containerColor = TvBrandBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("launch_pro_charts_btn")
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Pro Charting Terminal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- 2. Premium Features Matrix ---
        item {
            Text("UNLOCKED PRO+ CAPABILITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvTextSecondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureRow(
                    icon = Icons.Default.AllInclusive,
                    title = "Unlimited Indicators Per Chart",
                    desc = "Apply EMA 20/50/200, Bollinger Bands, RSI, MACD & VWAP simultaneously"
                )
                HorizontalDivider(color = TvBorderColor)
                FeatureRow(
                    icon = Icons.Default.ViewAgenda,
                    title = "Multi-Chart Split Screen Layouts",
                    desc = "Compare multiple crypto assets or equity pairs side by side"
                )
                HorizontalDivider(color = TvBorderColor)
                FeatureRow(
                    icon = Icons.Default.Speed,
                    title = "Real-Time 1-Second Timeframes",
                    desc = "Ultra-low latency price tick simulation and market depth"
                )
                HorizontalDivider(color = TvBorderColor)
                FeatureRow(
                    icon = Icons.Default.NotificationsActive,
                    title = "Instant Server-Side Alerts",
                    desc = "Instant price crossing triggers with custom alert notifications"
                )
                HorizontalDivider(color = TvBorderColor)
                FeatureRow(
                    icon = Icons.Default.CurrencyExchange,
                    title = "$100,000 Paper Trading Account",
                    desc = "Virtual capital simulation with margin, stop-loss, and take-profit"
                )
            }
        }

        // --- 3. Bar Replay Simulation Feature ---
        item {
            Text("BAR REPLAY SIMULATOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvTextSecondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "Backtest trading setups candle-by-candle with historical bar replay.",
                    fontSize = 12.sp,
                    color = TvTextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { replayPlaying = !replayPlaying },
                        modifier = Modifier
                            .size(44.dp)
                            .background(TvBrandBlue, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (replayPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play Replay",
                            tint = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = { /* Step forward */ },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Step 1 Bar")
                    }

                    OutlinedButton(
                        onClick = { replayPlaying = false },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Real-Time")
                    }
                }
            }
        }

        // --- 4. Charting Preferences & Settings ---
        item {
            Text("CHART PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TvTextSecondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                PreferenceToggleRow(
                    title = "Magnet Mode",
                    subtitle = "Snap drawing lines to candle High and Low wick points",
                    checked = magnetMode,
                    onCheckedChange = { magnetMode = it }
                )
                HorizontalDivider(color = TvBorderColor)
                PreferenceToggleRow(
                    title = "Extended Trading Hours (ETH)",
                    subtitle = "Show pre-market and after-hours stock sessions",
                    checked = extendedHours,
                    onCheckedChange = { extendedHours = it }
                )
                HorizontalDivider(color = TvBorderColor)
                PreferenceToggleRow(
                    title = "Sound & Vibration Alerts",
                    subtitle = "Notify when price alert triggers are reached",
                    checked = soundAlerts,
                    onCheckedChange = { soundAlerts = it }
                )
            }
        }

        // --- 5. Direct APK Installation Guide ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvDarkSurface, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.InstallMobile, contentDescription = null, tint = TvBullGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Install on Android Phone as APK", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This application is a full native Android app. You can install it directly onto your phone as an APK file (not a zip file) by selecting 'Download APK' in the AI Studio platform menu.",
                    fontSize = 12.sp,
                    color = TvTextSecondary,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenExportGuide,
                    colors = ButtonDefaults.buttonColors(containerColor = TvBrandBlue),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("open_export_guide_modal_btn")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("APK কোথায় এক্সপোর্ট করবেন দেখুন (Guide)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(TvBrandBlueBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TvBrandBlueLight, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Text(desc, fontSize = 11.sp, color = TvTextSecondary)
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, fontSize = 11.sp, color = TvTextSecondary)
        }
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
