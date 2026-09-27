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
    primary = GreenEmerald,
    onPrimary = Color.White,
    primaryContainer = VeryLightGreen,
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = GoldenWheat,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFFFFF8E1),
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = GreenLightEmerald,
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
    tertiary = GreenMint,
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

// 2. Harvest (wheat and clay)
private val HarvestLightScheme = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF7C3D0A),
    secondary = Color(0xFF8A4B12),
    onSecondary = Color.White,
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFFBF3E3),
    onBackground = Color(0xFF2E1F0B),
    surface = Color(0xFFFFFDF7),
    onSurface = Color(0xFF2E1F0B),
    surfaceVariant = Color(0xFFF7EAD1),
    onSurfaceVariant = Color(0xFF6B5433),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFFEEDFC4)
)

// 3. Midnight blue (cooler dark)
private val MidnightBlueScheme = darkColorScheme(
    primary = Color(0xFF22D3EE),
    onPrimary = Color(0xFF042F3D),
    primaryContainer = Color(0xFF115E75),
    onPrimaryContainer = Color(0xFFA5F3FC),
    secondary = Color(0xFFFCD34D),
    onSecondary = Color(0xFF3B2A02),
    tertiary = Color(0xFF67E8F9),
    onTertiary = Color(0xFF042F3D),
    background = Color(0xFF0E151D),
    onBackground = Color(0xFFE8F4FB),
    surface = Color(0xFF16202B),
    onSurface = Color(0xFFE8F4FB),
    surfaceVariant = Color(0xFF243546),
    onSurfaceVariant = Color(0xFF9FB6C9),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    outline = Color(0xFF2C3E52)
)

// 4. Meadow (bright, near-white)
private val MeadowLightScheme = lightColorScheme(
    primary = Color(0xFF16A34A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = Color(0xFF15803D),
    onSecondary = Color.White,
    tertiary = Color(0xFFEA9A12),
    onTertiary = Color.White,
    background = Color(0xFFF7FBF6),
    onBackground = Color(0xFF16261B),
    surface = Color.White,
    onSurface = Color(0xFF16261B),
    surfaceVariant = Color(0xFFEAF3E9),
    onSurfaceVariant = Color(0xFF5A6B5E),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFFE2EBE0)
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
    //
    // Each theme supplies both a Material scheme and a Farmify palette. The
    // palette is what most of the app reads, since every screen reaches its
    // surfaces and accents through it rather than through hardcoded colours.
    val surfaces = when (themeMode) {
        AppThemeMode.LIGHT -> LightSurfacePalette
        AppThemeMode.DARK -> DarkSurfacePalette
        AppThemeMode.HARVEST -> HarvestSurfacePalette
        AppThemeMode.MIDNIGHT -> MidnightSurfacePalette
        AppThemeMode.MEADOW -> MeadowSurfacePalette
    }
    val colorScheme = when (themeMode) {
        AppThemeMode.LIGHT -> EmeraldLightScheme
        AppThemeMode.DARK -> MidnightDarkScheme
        AppThemeMode.HARVEST -> HarvestLightScheme
        AppThemeMode.MIDNIGHT -> MidnightBlueScheme
        AppThemeMode.MEADOW -> MeadowLightScheme
    }

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
