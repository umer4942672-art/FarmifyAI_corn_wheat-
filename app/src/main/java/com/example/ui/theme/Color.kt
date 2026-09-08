package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// App Theme Modes
/**
 * Only two themes are offered. GOLDEN and EARTH were extra light palettes that
 * differed only in accent colour, which is a decision a farmer has no reason to
 * make; light versus dark is the one that matters in a field at midday or indoors
 * at night.
 */
enum class AppThemeMode(
    val titleEn: String,
    val titleUr: String,
    val primaryColor: Color,
    val previewBg: Color
) {
    LIGHT(
        titleEn = "Light",
        titleUr = "روشن",
        primaryColor = Color(0xFF2E7D32),
        previewBg = Color(0xFFF3F6F2)
    ),
    DARK(
        titleEn = "Dark",
        titleUr = "گہرا",
        primaryColor = Color(0xFF4ADE80),
        previewBg = Color(0xFF121A16)
    )
}

// Sleek Interface Theme - Modern Pakistani Agriculture Palette
val ForestGreen = Color(0xFF1B5E20)
val DarkGreen = Color(0xFF1B3022)
val EmeraldGreen = Color(0xFF2E7D32)
val LightEmerald = Color(0xFF4CAF50)
val MintGreen = Color(0xFF81C784)
val VeryLightGreen = Color(0xFFE8F5E9)
val LightPaleGreenBg = Color(0xFFF3F6F2)
val LightCardGreenTint = Color(0xFFF7FAF7)

// Golden Harvest Colors
val GoldenHarvestPrimary = Color(0xFFB45309)
val GoldenHarvestSecondary = Color(0xFFD97706)
val GoldenHarvestBg = Color(0xFFFFFBEB)
val GoldenHarvestSurface = Color(0xFFFFFDF5)
val GoldenHarvestVariant = Color(0xFFFEF3C7)

// Fertile Earth Colors
val FertileEarthPrimary = Color(0xFF8D4B32)
val FertileEarthSecondary = Color(0xFFA0522D)
val FertileEarthBg = Color(0xFFFAF5F0)
val FertileEarthSurface = Color(0xFFFFFBF7)
val FertileEarthVariant = Color(0xFFF2E6DC)

// Sleek Accents & Badges
val GoldenWheat = Color(0xFFD4AF37)
val GoldenYellow = Color(0xFFF59E0B)
val AmberOrange = Color(0xFFEA580C)
val BadgeOrangeBg = Color(0xFFFFEDD5)
val BadgeBlueBg = Color(0xFFDBEAFE)
val BadgeBlueText = Color(0xFF2563EB)
val BadgeGreenBg = Color(0xFFD1FAE5)
val BadgeRedBg = Color(0xFFFEE2E2)
val EarthBrown = Color(0xFF6D4C41)
val WarmClay = Color(0xFF8D6E63)

// Surfaces & Neutral Colors
val LightSoftWhite = Color(0xFFFFFFFF)
val LightOffWhite = Color(0xFFF8FAF8)
val LightSurfaceVariant = Color(0xFFE8EFE9)
val LightBorderLight = Color(0xFFE2EBE2)
val LightBorderSlate = Color(0xFFE2E8F0)

// High-contrast Sleek Typography
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF94A3B8)

// Status colors
val SuccessGreen = Color(0xFF16A34A)
val ErrorRed = Color(0xFFDC2626)
val WarningAmber = Color(0xFFD97706)
val InfoBlue = Color(0xFF2563EB)
val SkyBlue = Color(0xFF0288D1)

// Glassmorphism & Sleek surface colors
val GlassSurface = Color(0xF2FFFFFF)
val GlassSurfaceDark = Color(0xCC1A3320)
val GlassBorder = Color(0x66FFFFFF)


// ---------------------------------------------------------------------------
// Theme-aware surface palette
//
// Screens reference SoftWhite, TextPrimary and friends by name in roughly three
// hundred places. Rather than rewriting every call site, those names are now
// composable getters that read the active palette. Call sites are unchanged; the
// values follow the selected theme.
//
// This is what makes dark mode actually usable. Previously the colour scheme
// flipped to dark while these surfaces stayed hardcoded white, so text went
// invisible.
// ---------------------------------------------------------------------------

@Immutable
data class FarmifySurfacePalette(
    val isDark: Boolean,
    val softWhite: Color,
    val offWhite: Color,
    val surfaceVariant: Color,
    val paleGreenBg: Color,
    val cardGreenTint: Color,
    val borderLight: Color,
    val borderSlate: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val LightSurfacePalette = FarmifySurfacePalette(
    isDark = false,
    softWhite = LightSoftWhite,
    offWhite = LightOffWhite,
    surfaceVariant = LightSurfaceVariant,
    paleGreenBg = LightPaleGreenBg,
    cardGreenTint = LightCardGreenTint,
    borderLight = LightBorderLight,
    borderSlate = LightBorderSlate,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted
)

val DarkSurfacePalette = FarmifySurfacePalette(
    isDark = true,
    // Cards sit slightly lighter than the page so elevation still reads.
    softWhite = Color(0xFF1B2420),
    offWhite = Color(0xFF223029),
    surfaceVariant = Color(0xFF2A3A32),
    paleGreenBg = Color(0xFF121A16),
    cardGreenTint = Color(0xFF1E2A24),
    borderLight = Color(0xFF33453B),
    borderSlate = Color(0xFF33453B),
    textPrimary = Color(0xFFECFDF3),
    textSecondary = Color(0xFFA7BDB0),
    textMuted = Color(0xFF7C9488)
)

val LocalFarmifySurfaces = staticCompositionLocalOf { LightSurfacePalette }

val SoftWhite: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.softWhite

val OffWhite: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.offWhite

val SurfaceVariant: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.surfaceVariant

val PaleGreenBg: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.paleGreenBg

val CardGreenTint: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.cardGreenTint

val BorderLight: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.borderLight

val BorderSlate: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.borderSlate

val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.textPrimary

val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.textSecondary

val TextMuted: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.textMuted
