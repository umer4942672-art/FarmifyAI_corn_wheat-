package com.example.ui.screens.mandi

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MandiRate
import com.example.data.model.MandiTrend
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MandiSort
import com.example.util.LocalAppLanguage
import com.example.util.str

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MandiRatesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val langState = LocalAppLanguage.current
    val mandiRates by viewModel.filteredMandiRates.collectAsStateWithLifecycle()
    val filter by viewModel.mandiFilter.collectAsStateWithLifecycle()
    val mandiIsLive by viewModel.mandiRepository.isLive.collectAsStateWithLifecycle()

    val categories = listOf("All", "Grain", "Cash Crop", "Vegetable", "Fruit", "Oilseed")
    val provinces = listOf("All", "Punjab", "Sindh", "KPK")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaleGreenBg)
            .testTag("mandi_rates_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        // 1. Header with Refresh Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = str("mandi_title"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = str("mandi_subtitle"),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (mandiIsLive) "● Live rates" else "○ Live rates unavailable — showing reference data",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mandiIsLive) EmeraldGreen else TextMuted
                    )
                }

                IconButton(
                    onClick = { viewModel.refreshMandiRates() },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(VeryLightGreen)
                        .testTag("mandi_refresh_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Rates",
                        tint = EmeraldGreen
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Search Bar
        item {
            OutlinedTextField(
                value = filter.searchQuery,
                onValueChange = { viewModel.updateMandiSearch(it) },
                placeholder = { Text(str("search_crop")) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                },
                trailingIcon = {
                    if (filter.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateMandiSearch("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = EmeraldGreen,
                    focusedBorderColor = EmeraldGreen,
                    unfocusedBorderColor = BorderLight,
                    focusedContainerColor = SoftWhite,
                    unfocusedContainerColor = SoftWhite
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mandi_search_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 3. Unit Toggle & Watchlist Switch Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unit Switcher (KG vs Mann)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Surface(
                            onClick = { if (filter.isPerMann) viewModel.toggleMandiUnit() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (!filter.isPerMann) EmeraldGreen else Color.Transparent,
                            modifier = Modifier.testTag("mandi_unit_kg")
                        ) {
                            Text(
                                text = "PKR / KG",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!filter.isPerMann) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            onClick = { if (!filter.isPerMann) viewModel.toggleMandiUnit() },
                            shape = RoundedCornerShape(8.dp),
                            color = if (filter.isPerMann) EmeraldGreen else Color.Transparent,
                            modifier = Modifier.testTag("mandi_unit_mann")
                        ) {
                            Text(
                                text = if (langState.isUrdu) "روپے / من" else "PKR / 40 KG",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (filter.isPerMann) Color.White else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Favorites Only Filter Button
                FilterChip(
                    selected = filter.showFavoritesOnly,
                    onClick = { viewModel.toggleMandiFavoriteFilter() },
                    label = { Text(if (langState.isUrdu) "پسندیدہ" else "Watchlist") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (filter.showFavoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (filter.showFavoritesOnly) GoldenYellow else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("mandi_watchlist_filter")
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. Category Filter Tabs Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = filter.selectedCategory.equals(category, ignoreCase = true),
                        onClick = { viewModel.updateMandiCategory(category) },
                        label = {
                            Text(
                                when (category) {
                                    "All" -> str("all_mandis")
                                    "Grain" -> if (langState.isUrdu) "غلہ / اناج" else "Grain"
                                    "Cash Crop" -> if (langState.isUrdu) "نقد آور فصلیں" else "Cash Crops"
                                    "Vegetable" -> if (langState.isUrdu) "سبزیاں" else "Vegetables"
                                    "Fruit" -> if (langState.isUrdu) "پھل" else "Fruits"
                                    "Oilseed" -> if (langState.isUrdu) "تیل دار بیج" else "Oilseeds"
                                    else -> category
                                }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 5. Province Filters Row
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(provinces) { province ->
                    FilterChip(
                        selected = filter.selectedProvince.equals(province, ignoreCase = true),
                        onClick = { viewModel.updateMandiProvince(province) },
                        label = {
                            Text(
                                when (province) {
                                    "All" -> if (langState.isUrdu) "تمام صوبے" else "All Pakistan"
                                    "Punjab" -> str("filter_punjab")
                                    "Sindh" -> str("filter_sindh")
                                    "KPK" -> str("filter_kpk")
                                    else -> province
                                }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 6. Rates List or Empty State
        if (mandiRates.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.SearchOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No mandi rates match your search or filter.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            items(mandiRates, key = { it.id }) { rate ->
                MandiRateCard(
                    rate = rate,
                    isPerMann = filter.isPerMann,
                    onToggleFavorite = { viewModel.toggleMandiFavorite(rate.id) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun MandiRateCard(
    rate: MandiRate,
    isPerMann: Boolean,
    onToggleFavorite: () -> Unit
) {
    val langState = LocalAppLanguage.current
    val displayPrice = if (isPerMann) rate.pricePerMann else rate.pricePerKg
    val unitLabel = if (isPerMann) "PKR / 40 KG" else "PKR / KG"

    val trendColor = when (rate.trend) {
        MandiTrend.UP -> SuccessGreen
        MandiTrend.DOWN -> ErrorRed
        MandiTrend.STABLE -> TextMuted
    }

    val trendText = when (rate.trend) {
        MandiTrend.UP -> str("trend_up")
        MandiTrend.DOWN -> str("trend_down")
        MandiTrend.STABLE -> str("trend_stable")
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mandi_card_${rate.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crop artwork so a farmer can find the right row by shape and
                // colour instead of reading every name.
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(cropTint(rate.cropNameEn).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    CropArt(
                        cropName = rate.cropNameEn,
                        accent = cropTint(rate.cropNameEn),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (langState.isUrdu) rate.cropNameUr else rate.cropNameEn,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = VeryLightGreen
                        ) {
                            Text(
                                text = rate.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreen,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${rate.mandiName} • ${rate.city}, ${rate.province}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (rate.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Watchlist",
                        tint = if (rate.isFavorite) GoldenYellow else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "PKR ${displayPrice.toInt()}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = EmeraldGreen
                    )
                    Text(
                        text = unitLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = trendColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (rate.trend) {
                                MandiTrend.UP -> Icons.Default.ArrowUpward
                                MandiTrend.DOWN -> Icons.Default.ArrowDownward
                                MandiTrend.STABLE -> Icons.Default.TrendingFlat
                            },
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (rate.changePercent >= 0) "+" else ""}${rate.changePercent}% • $trendText",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = trendColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Range: PKR ${(rate.minPricePerKg * (if (isPerMann) 40 else 1)).toInt()} - ${(rate.maxPricePerKg * (if (isPerMann) 40 else 1)).toInt()}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = rate.lastUpdated,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}


/** Colour for each crop's artwork, so rows are distinguishable at a glance. */
private fun cropTint(cropNameEn: String): Color = when {
    cropNameEn.contains("wheat", true) -> Color(0xFFC79A2E)
    cropNameEn.contains("rice", true) || cropNameEn.contains("paddy", true) -> Color(0xFF7B9E4C)
    cropNameEn.contains("maize", true) || cropNameEn.contains("corn", true) -> Color(0xFFE0A21B)
    cropNameEn.contains("cotton", true) -> Color(0xFF5C8AA8)
    cropNameEn.contains("sugarcane", true) || cropNameEn.contains("cane", true) -> Color(0xFF3F8F5E)
    cropNameEn.contains("potato", true) -> Color(0xFFA9793F)
    cropNameEn.contains("tomato", true) -> Color(0xFFD1483C)
    cropNameEn.contains("onion", true) -> Color(0xFFA85C8C)
    else -> Color(0xFF4C8C55)
}

/**
 * Simple crop illustrations drawn on a Canvas. Everything is a ratio of the
 * available size, so no drawable assets are added to the APK and the art stays
 * sharp at any density.
 */
@Composable
private fun CropArt(cropName: String, accent: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier) {
        val s = this.size.minDimension
        fun at(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(s * x, s * y)
        fun box(w: Float, h: Float) = androidx.compose.ui.geometry.Size(s * w, s * h)
        val line = androidx.compose.ui.graphics.drawscope.Stroke(
            width = s * 0.07f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
        val name = cropName.lowercase()

        when {
            name.contains("wheat") -> {
                drawLine(accent, at(0.50f, 0.95f), at(0.50f, 0.30f), strokeWidth = s * 0.07f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
                for (i in 0..3) {
                    val y = 0.10f + i * 0.17f
                    drawOval(accent, topLeft = at(0.24f, y), size = box(0.26f, 0.14f))
                    drawOval(accent, topLeft = at(0.50f, y), size = box(0.26f, 0.14f))
                }
            }

            name.contains("rice") || name.contains("paddy") -> {
                for (i in 0..2) {
                    val x = 0.22f + i * 0.28f
                    drawLine(accent.copy(alpha = 0.8f), at(x, 0.94f), at(x, 0.42f),
                        strokeWidth = s * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    drawOval(accent, topLeft = at(x - 0.09f, 0.20f), size = box(0.18f, 0.26f))
                }
                drawLine(accent.copy(alpha = 0.45f), at(0.06f, 0.94f), at(0.94f, 0.94f),
                    strokeWidth = s * 0.06f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            name.contains("maize") || name.contains("corn") -> {
                drawOval(accent.copy(alpha = 0.35f), topLeft = at(0.32f, 0.10f), size = box(0.36f, 0.72f))
                drawOval(accent, topLeft = at(0.32f, 0.10f), size = box(0.36f, 0.72f), style = line)
                for (i in 0..2) {
                    drawLine(accent.copy(alpha = 0.7f), at(0.36f, 0.28f + i * 0.18f),
                        at(0.64f, 0.28f + i * 0.18f), strokeWidth = s * 0.045f)
                }
                drawOval(accent.copy(alpha = 0.55f), topLeft = at(0.08f, 0.50f), size = box(0.26f, 0.34f))
                drawOval(accent.copy(alpha = 0.55f), topLeft = at(0.66f, 0.50f), size = box(0.26f, 0.34f))
            }

            name.contains("cotton") -> {
                drawCircle(accent.copy(alpha = 0.30f), radius = s * 0.19f, center = at(0.35f, 0.36f))
                drawCircle(accent.copy(alpha = 0.30f), radius = s * 0.19f, center = at(0.65f, 0.36f))
                drawCircle(accent.copy(alpha = 0.30f), radius = s * 0.21f, center = at(0.50f, 0.55f))
                drawCircle(accent, radius = s * 0.19f, center = at(0.35f, 0.36f), style = line)
                drawCircle(accent, radius = s * 0.19f, center = at(0.65f, 0.36f), style = line)
                drawLine(accent, at(0.50f, 0.76f), at(0.50f, 0.95f), strokeWidth = s * 0.06f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            name.contains("sugarcane") || name.contains("cane") -> {
                for (i in 0..1) {
                    val x = 0.34f + i * 0.28f
                    drawLine(accent, at(x, 0.95f), at(x, 0.22f), strokeWidth = s * 0.11f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    for (j in 0..2) {
                        drawLine(Color.White.copy(alpha = 0.75f), at(x - 0.055f, 0.38f + j * 0.19f),
                            at(x + 0.055f, 0.38f + j * 0.19f), strokeWidth = s * 0.035f)
                    }
                }
                drawOval(accent.copy(alpha = 0.6f), topLeft = at(0.08f, 0.06f), size = box(0.34f, 0.16f))
                drawOval(accent.copy(alpha = 0.6f), topLeft = at(0.58f, 0.06f), size = box(0.34f, 0.16f))
            }

            name.contains("potato") -> {
                drawOval(accent.copy(alpha = 0.35f), topLeft = at(0.10f, 0.28f), size = box(0.58f, 0.44f))
                drawOval(accent, topLeft = at(0.10f, 0.28f), size = box(0.58f, 0.44f), style = line)
                drawCircle(accent, radius = s * 0.045f, center = at(0.28f, 0.44f))
                drawCircle(accent, radius = s * 0.045f, center = at(0.48f, 0.58f))
                drawOval(accent.copy(alpha = 0.5f), topLeft = at(0.58f, 0.48f), size = box(0.34f, 0.28f))
            }

            name.contains("tomato") -> {
                drawCircle(accent.copy(alpha = 0.35f), radius = s * 0.32f, center = at(0.50f, 0.60f))
                drawCircle(accent, radius = s * 0.32f, center = at(0.50f, 0.60f), style = line)
                drawLine(Color(0xFF4C8C55), at(0.50f, 0.28f), at(0.50f, 0.10f), strokeWidth = s * 0.06f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawOval(Color(0xFF4C8C55), topLeft = at(0.24f, 0.16f), size = box(0.24f, 0.13f))
                drawOval(Color(0xFF4C8C55), topLeft = at(0.52f, 0.16f), size = box(0.24f, 0.13f))
            }

            name.contains("onion") -> {
                drawOval(accent.copy(alpha = 0.35f), topLeft = at(0.18f, 0.34f), size = box(0.64f, 0.58f))
                drawOval(accent, topLeft = at(0.18f, 0.34f), size = box(0.64f, 0.58f), style = line)
                drawLine(accent.copy(alpha = 0.6f), at(0.50f, 0.40f), at(0.50f, 0.88f), strokeWidth = s * 0.04f)
                drawLine(accent, at(0.44f, 0.32f), at(0.36f, 0.08f), strokeWidth = s * 0.05f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(accent, at(0.56f, 0.32f), at(0.66f, 0.10f), strokeWidth = s * 0.05f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            else -> {
                // Generic leafy crop for anything not listed above.
                drawLine(accent, at(0.50f, 0.95f), at(0.50f, 0.36f), strokeWidth = s * 0.07f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawOval(accent.copy(alpha = 0.55f), topLeft = at(0.14f, 0.28f), size = box(0.36f, 0.22f))
                drawOval(accent, topLeft = at(0.50f, 0.16f), size = box(0.36f, 0.22f))
            }
        }
    }
}
