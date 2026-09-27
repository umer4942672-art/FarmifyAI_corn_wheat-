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
    ),
    HARVEST(
        titleEn = "Harvest",
        titleUr = "فصل",
        primaryColor = Color(0xFFB45309),
        previewBg = Color(0xFFFBF3E3)
    ),
    MIDNIGHT(
        titleEn = "Midnight",
        titleUr = "رات",
        primaryColor = Color(0xFF22D3EE),
        previewBg = Color(0xFF0E151D)
    ),
    MEADOW(
        titleEn = "Meadow",
        titleUr = "سبزہ",
        primaryColor = Color(0xFF16A34A),
        previewBg = Color(0xFFF7FBF6)
    )
}

// Sleek Interface Theme - Modern Pakistani Agriculture Palette
val GreenForest = Color(0xFF1B5E20)
val DarkGreen = Color(0xFF1B3022)
val GreenEmerald = Color(0xFF2E7D32)
val GreenLightEmerald = Color(0xFF4CAF50)
val GreenMint = Color(0xFF81C784)
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
val AccentGoldenYellow = Color(0xFFF59E0B)
val AccentAmberOrange = Color(0xFFEA580C)
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
    val textMuted: Color,
    // Accents. Holding these in the palette is what lets a theme change the
    // whole look rather than only the background, since every screen reaches
    // its greens and golds through these.
    val forestGreen: Color,
    val emeraldGreen: Color,
    val lightEmerald: Color,
    val mintGreen: Color,
    val goldenYellow: Color,
    val amberOrange: Color,
    /** Gradient used by the weather card and other hero surfaces. */
    val heroGradient: List<Color>
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
    textMuted = LightTextMuted,
    forestGreen = GreenForest,
    emeraldGreen = GreenEmerald,
    lightEmerald = GreenLightEmerald,
    mintGreen = GreenMint,
    goldenYellow = AccentGoldenYellow,
    amberOrange = AccentAmberOrange,
    heroGradient = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF144D18))
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
    textMuted = Color(0xFF7C9488),
    // Accents lift in dark mode. The light-theme forest green is too close to
    // the background here to read as an accent at all.
    forestGreen = Color(0xFF4ADE80),
    emeraldGreen = Color(0xFF34D399),
    lightEmerald = Color(0xFF6EE7B7),
    mintGreen = Color(0xFF9DECC4),
    goldenYellow = Color(0xFFFBBF24),
    amberOrange = Color(0xFFFB923C),
    heroGradient = listOf(Color(0xFF14361F), Color(0xFF1B5E20), Color(0xFF0E2A16))
)

/**
 * Harvest: wheat and clay, for farmers who find the green too cool.
 *
 * The reference designs this draws on use pale cream on cream, which looks
 * settled indoors and disappears outdoors. The surfaces here stay warm but the
 * text and accents keep the same contrast as the green theme, so the screen is
 * still readable in a field at midday.
 */
val HarvestSurfacePalette = FarmifySurfacePalette(
    isDark = false,
    softWhite = Color(0xFFFFFDF7),
    offWhite = Color(0xFFFDF6E8),
    surfaceVariant = Color(0xFFF7EAD1),
    paleGreenBg = Color(0xFFFBF3E3),
    cardGreenTint = Color(0xFFFFFAF0),
    borderLight = Color(0xFFEEDFC4),
    borderSlate = Color(0xFFE4D5BB),
    textPrimary = Color(0xFF2E1F0B),
    textSecondary = Color(0xFF6B5433),
    textMuted = Color(0xFF9C8560),
    forestGreen = Color(0xFF8A4B12),
    emeraldGreen = Color(0xFFB45309),
    lightEmerald = Color(0xFFD97706),
    mintGreen = Color(0xFFE8B563),
    goldenYellow = Color(0xFF8A4B12),
    amberOrange = Color(0xFFC2410C),
    heroGradient = listOf(Color(0xFF7C3D0A), Color(0xFFB45309), Color(0xFF6B3208))
)

/**
 * Meadow: bright and airy, almost white, with saturated accents.
 *
 * Where Light is a deep forest green and Harvest is warm clay, this is the
 * clean pale look of a modern consumer app. The accents are strong enough that
 * the feature tiles still separate at a glance on a near-white background,
 * which is the failure mode of pale designs.
 */
val MeadowSurfacePalette = FarmifySurfacePalette(
    isDark = false,
    softWhite = Color(0xFFFFFFFF),
    offWhite = Color(0xFFF4F8F3),
    surfaceVariant = Color(0xFFEAF3E9),
    paleGreenBg = Color(0xFFF7FBF6),
    cardGreenTint = Color(0xFFF0F8EF),
    borderLight = Color(0xFFE2EBE0),
    borderSlate = Color(0xFFD5E0D3),
    textPrimary = Color(0xFF16261B),
    textSecondary = Color(0xFF5A6B5E),
    textMuted = Color(0xFF8B9A8E),
    forestGreen = Color(0xFF15803D),
    emeraldGreen = Color(0xFF16A34A),
    lightEmerald = Color(0xFF4ADE80),
    mintGreen = Color(0xFF86EFAC),
    goldenYellow = Color(0xFFEA9A12),
    amberOrange = Color(0xFFEA7317),
    heroGradient = listOf(Color(0xFF16A34A), Color(0xFF22C55E), Color(0xFF15803D))
)

/**
 * Midnight: a deep blue-teal dark theme, cooler than the green dark mode.
 */
val MidnightSurfacePalette = FarmifySurfacePalette(
    isDark = true,
    softWhite = Color(0xFF16202B),
    offWhite = Color(0xFF1B2836),
    surfaceVariant = Color(0xFF243546),
    paleGreenBg = Color(0xFF0E151D),
    cardGreenTint = Color(0xFF18232F),
    borderLight = Color(0xFF2C3E52),
    borderSlate = Color(0xFF2C3E52),
    textPrimary = Color(0xFFE8F4FB),
    textSecondary = Color(0xFF9FB6C9),
    textMuted = Color(0xFF7288A0),
    forestGreen = Color(0xFF38BDF8),
    emeraldGreen = Color(0xFF22D3EE),
    lightEmerald = Color(0xFF67E8F9),
    mintGreen = Color(0xFFA5F3FC),
    goldenYellow = Color(0xFFFCD34D),
    amberOrange = Color(0xFFFB923C),
    heroGradient = listOf(Color(0xFF0C3A52), Color(0xFF115E75), Color(0xFF082B3D))
)

val LocalFarmifySurfaces = staticCompositionLocalOf { LightSurfacePalette }

// Accents, read through the active palette. Screens keep using the familiar
// names; which colour arrives depends on the theme the farmer chose.
val ForestGreen: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.forestGreen

val EmeraldGreen: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.emeraldGreen

val LightEmerald: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.lightEmerald

val MintGreen: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.mintGreen

val GoldenYellow: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.goldenYellow

val AmberOrange: Color
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.amberOrange

/** Gradient for hero surfaces such as the weather card. */
val HeroGradient: List<Color>
    @Composable @ReadOnlyComposable get() = LocalFarmifySurfaces.current.heroGradient

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
