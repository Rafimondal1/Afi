package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TradingViewDarkColorScheme = darkColorScheme(
    primary = TvBrandBlue,
    onPrimary = Color.White,
    primaryContainer = TvBrandBlueBg,
    onPrimaryContainer = TvBrandBlueLight,
    secondary = TvBullGreen,
    onSecondary = Color.White,
    secondaryContainer = TvBullGreenBg,
    onSecondaryContainer = TvBullGreenHover,
    tertiary = TvBearRed,
    onTertiary = Color.White,
    tertiaryContainer = TvBearRedBg,
    onTertiaryContainer = TvBearRedHover,
    background = TvDarkBackground,
    onBackground = TvTextPrimary,
    surface = TvDarkSurface,
    onSurface = TvTextPrimary,
    surfaceVariant = TvSurfaceVariant,
    onSurfaceVariant = TvTextSecondary,
    outline = TvBorderColor
)

private val TradingViewLightColorScheme = lightColorScheme(
    primary = TvBrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F0FE),
    onPrimaryContainer = TvBrandBlue,
    secondary = TvBullGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6F4EA),
    onSecondaryContainer = TvBullGreen,
    tertiary = TvBearRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE8E6),
    onTertiaryContainer = TvBearRed,
    background = TvLightBackground,
    onBackground = TvLightTextPrimary,
    surface = TvLightSurface,
    onSurface = TvLightTextPrimary,
    surfaceVariant = TvLightSurfaceVariant,
    onSurfaceVariant = TvLightTextSecondary,
    outline = Color(0xFFD0D5DD)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to iconic TradingView dark theme
    dynamicColor: Boolean = false, // Keep signature TradingView look
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) TradingViewDarkColorScheme else TradingViewLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
