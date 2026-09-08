package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppTheme = staticCompositionLocalOf { AppThemeMode.LIGHT }

// 1. Emerald Lush (Default Green)
private val EmeraldLightScheme = lightColorScheme(
    primary = EmeraldGreen,
    onPrimary = Color.White,
    primaryContainer = VeryLightGreen,
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = GoldenWheat,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = LightEmerald,
    onTertiary = Color.White,
    background = LightPaleGreenBg,
    onBackground = LightTextPrimary,
    surface = LightSoftWhite,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = LightBorderLight
)

// 2. Golden Harvest (Wheat / Amber)

// 3. Fertile Earth (Terracotta & Clay)

// 4. Midnight Dark Mode
private val MidnightDarkScheme = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF052E16),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFF86EFAC),
    secondary = GoldenWheat,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF292524),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = MintGreen,
    onTertiary = Color(0xFF052E16),
    background = Color(0xFF0C160F),
    onBackground = Color(0xFFF0FDF4),
    surface = Color(0xFF132217),
    onSurface = Color(0xFFF0FDF4),
    surfaceVariant = Color(0xFF1C3122),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    outline = Color(0xFF23442C)
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun FarmifyTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // The system dark-mode flag is deliberately ignored. Theme is an explicit
    // choice in settings, so a farmer who wants the light UI outdoors keeps it
    // regardless of what the phone is set to.
    val isDark = themeMode == AppThemeMode.DARK
    val colorScheme = if (isDark) MidnightDarkScheme else EmeraldLightScheme
    val surfaces = if (isDark) DarkSurfacePalette else LightSurfacePalette

    CompositionLocalProvider(
        LocalAppTheme provides themeMode,
        LocalFarmifySurfaces provides surfaces
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FarmifyTheme(themeMode = themeMode, darkTheme = darkTheme, content = content)
}
