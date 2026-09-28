package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun BarReplayControlBar(
    isReplayActive: Boolean,
    isPlaying: Boolean,
    currentBarIndex: Int,
    totalBars: Int,
    currentSpeed: Float,
    onTogglePlay: () -> Unit,
    onStepForward: () -> Unit,
    onStepBackward: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onSeek: (Int) -> Unit,
    onExitReplay: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isReplayActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(TvDarkSurface.copy(alpha = 0.95f))
                .border(1.dp, TvBrandBlue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Header: Status Badge & Exit
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(TvBrandBlue, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "BACKTEST REPLAY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bar $currentBarIndex / $totalBars",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onExitReplay,
                    modifier = Modifier.size(24.dp).testTag("exit_replay_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Replay",
                        tint = TvTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Scrub Slider
            Slider(
                value = currentBarIndex.toFloat(),
                onValueChange = { onSeek(it.toInt()) },
                valueRange = 10f..totalBars.coerceAtLeast(11).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = TvBrandBlueLight,
                    activeTrackColor = TvBrandBlue,
                    inactiveTrackColor = TvSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().height(24.dp)
            )

            // Playback controls row: Step Back, Play/Pause, Step Forward, Speed Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Playback Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Step backward
                    IconButton(
                        onClick = onStepBackward,
                        modifier = Modifier
                            .size(34.dp)
                            .background(TvSurfaceVariant, CircleShape)
                            .testTag("replay_step_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Step Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Play / Pause
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .size(38.dp)
                            .background(if (isPlaying) TvOrange else TvBrandBlue, CircleShape)
                            .testTag("replay_toggle_play_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Step forward (Next Candle Forward Test)
                    IconButton(
                        onClick = onStepForward,
                        modifier = Modifier
                            .size(34.dp)
                            .background(TvSurfaceVariant, CircleShape)
                            .testTag("replay_step_forward_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Step Forward",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Speed Selector Pills (1x, 2x, 5x)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speed:", fontSize = 11.sp, color = TvTextSecondary)
                    listOf(1f, 2f, 5f).forEach { speed ->
                        val isSelected = currentSpeed == speed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) TvBrandBlue else TvDarkBackground)
                                .clickable { onSetSpeed(speed) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${speed.toInt()}x",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TvTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
