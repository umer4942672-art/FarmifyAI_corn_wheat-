package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalAppTheme = staticCompositionLocalOf { AppThemeMode.EMERALD }

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
    background = PaleGreenBg,
    onBackground = TextPrimary,
    surface = SoftWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    onError = Color.White,
    outline = BorderLight
)

// 2. Golden Harvest (Wheat / Amber)
private val GoldenLightScheme = lightColorScheme(
    primary = GoldenHarvestPrimary,
    onPrimary = Color.White,
    primaryContainer = GoldenHarvestVariant,
    onPrimaryContainer = Color(0xFF451A03),
    secondary = GoldenYellow,
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = AmberOrange,
    onTertiary = Color.White,
    background = GoldenHarvestBg,
    onBackground = Color(0xFF292524),
    surface = GoldenHarvestSurface,
    onSurface = Color(0xFF292524),
    surfaceVariant = GoldenHarvestVariant,
    onSurfaceVariant = Color(0xFF78716C),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFFFDE68A)
)

// 3. Fertile Earth (Terracotta & Clay)
private val EarthLightScheme = lightColorScheme(
    primary = FertileEarthPrimary,
    onPrimary = Color.White,
    primaryContainer = FertileEarthVariant,
    onPrimaryContainer = Color(0xFF3E1F16),
    secondary = WarmClay,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5EBE6),
    onSecondaryContainer = Color(0xFF4A281E),
    tertiary = EarthBrown,
    onTertiary = Color.White,
    background = FertileEarthBg,
    onBackground = Color(0xFF2C1810),
    surface = FertileEarthSurface,
    onSurface = Color(0xFF2C1810),
    surfaceVariant = FertileEarthVariant,
    onSurfaceVariant = Color(0xFF795548),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFFE0D0C5)
)

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
fun FarmifyTheme(
    themeMode: AppThemeMode = AppThemeMode.EMERALD,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        themeMode == AppThemeMode.MIDNIGHT -> MidnightDarkScheme
        darkTheme -> MidnightDarkScheme
        themeMode == AppThemeMode.GOLDEN -> GoldenLightScheme
        themeMode == AppThemeMode.EARTH -> EarthLightScheme
        else -> EmeraldLightScheme
    }

    CompositionLocalProvider(LocalAppTheme provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.EMERALD,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    FarmifyTheme(themeMode = themeMode, darkTheme = darkTheme, content = content)
}
