package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// App Theme Modes
enum class AppThemeMode(
    val titleEn: String,
    val titleUr: String,
    val primaryColor: Color,
    val previewBg: Color
) {
    EMERALD(
        titleEn = "Emerald Lush",
        titleUr = "زمردی ہریالی",
        primaryColor = Color(0xFF2E7D32),
        previewBg = Color(0xFFF3F6F2)
    ),
    GOLDEN(
        titleEn = "Golden Harvest",
        titleUr = "گندمی سونا",
        primaryColor = Color(0xFFB45309),
        previewBg = Color(0xFFFFFBEB)
    ),
    EARTH(
        titleEn = "Fertile Earth",
        titleUr = "مٹی براؤن",
        primaryColor = Color(0xFF8D4B32),
        previewBg = Color(0xFFFAF5F0)
    ),
    MIDNIGHT(
        titleEn = "Midnight Dark",
        titleUr = "رات کا موڈ",
        primaryColor = Color(0xFF4ADE80),
        previewBg = Color(0xFF0D1B12)
    )
}

// Sleek Interface Theme - Modern Pakistani Agriculture Palette
val ForestGreen = Color(0xFF1B5E20)
val DarkGreen = Color(0xFF1B3022)
val EmeraldGreen = Color(0xFF2E7D32)
val LightEmerald = Color(0xFF4CAF50)
val MintGreen = Color(0xFF81C784)
val VeryLightGreen = Color(0xFFE8F5E9)
val PaleGreenBg = Color(0xFFF3F6F2)
val CardGreenTint = Color(0xFFF7FAF7)

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
val SoftWhite = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFF8FAF8)
val SurfaceVariant = Color(0xFFE8EFE9)
val BorderLight = Color(0xFFE2EBE2)
val BorderSlate = Color(0xFFE2E8F0)

// High-contrast Sleek Typography
val TextPrimary = Color(0xFF0F172A)
val TextSecondary = Color(0xFF475569)
val TextMuted = Color(0xFF94A3B8)

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
