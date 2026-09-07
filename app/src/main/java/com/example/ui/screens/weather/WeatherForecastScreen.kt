package com.example.ui.screens.weather

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.WeatherMetricItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.LocalAppLanguage
import com.example.util.str

@Composable
fun WeatherForecastScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val langState = LocalAppLanguage.current
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val districts = viewModel.weatherRepository.districts

    var showDistrictMenu by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaleGreenBg)
            .testTag("weather_forecast_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 90.dp)
    ) {
        // 1. District Selector & Refresh Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = str("weather_title"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = if (langState.isUrdu) weatherState.selectedDistrict.nameUr else "${weatherState.selectedDistrict.nameEn}, ${weatherState.selectedDistrict.province}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = EmeraldGreen
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Change Location Chip
                    Surface(
                        onClick = { showDistrictMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        color = SoftWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
                        modifier = Modifier.testTag("weather_select_district_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (langState.isUrdu) "شہر منتخب کریں" else "Change City",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.refreshWeather() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VeryLightGreen)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // District Selection Dropdown / Horizontal chips
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(districts) { district ->
                    FilterChip(
                        selected = weatherState.selectedDistrict.id == district.id,
                        onClick = { viewModel.selectWeatherDistrict(district) },
                        label = {
                            Text(if (langState.isUrdu) district.nameUr else district.nameEn)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 2. Hero Weather Card
        item {
            val current = weatherState.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ForestGreen, EmeraldGreen, Color(0xFF43A047))
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (langState.isUrdu) current.locationNameUr else current.locationName,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${current.temperatureC.toInt()}°C",
                                color = Color.White,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (langState.isUrdu) current.conditionUr else current.conditionEn,
                                color = GoldenWheat,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = null,
                                tint = GoldenYellow,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HeroWeatherMetric(Icons.Outlined.WaterDrop, str("weather_humidity"), "${current.humidityPercent}%")
                        HeroWeatherMetric(Icons.Outlined.Air, str("weather_wind"), "${current.windSpeedKmh.toInt()} km/h")
                        HeroWeatherMetric(Icons.Outlined.Umbrella, str("weather_rain_prob"), "${current.rainProbability}%")
                        HeroWeatherMetric(Icons.Outlined.Thermostat, str("weather_feels_like"), "${current.feelsLikeC.toInt()}°C")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Farming Advisories Section (Irrigation, Spray, Harvest)
        item {
            SectionHeader(
                title = str("weather_advisory"),
                icon = Icons.Outlined.Psychology
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                weatherState.advisories.forEach { advisory ->
                    AdvisoryCard(advisory = advisory)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. 7-Day Day-by-Day Forecast
        item {
            SectionHeader(
                title = str("weather_view_7day"),
                icon = Icons.Outlined.CalendarMonth
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weatherState.forecast7Days.forEach { day ->
                    DailyForecastCard(day = day)
                }
            }
        }
    }
}

@Composable
fun HeroWeatherMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun AdvisoryCard(advisory: FarmAdvisory) {
    val langState = LocalAppLanguage.current

    val (bgColor, borderColor, iconColor, icon) = when (advisory.severity) {
        AdvisorySeverity.SUCCESS -> Quadruple(Color(0xFFF1F8F4), Color(0xFFC8E6C9), SuccessGreen, Icons.Default.CheckCircle)
        AdvisorySeverity.WARNING -> Quadruple(Color(0xFFFFF8E1), Color(0xFFFFECB3), WarningAmber, Icons.Default.Warning)
        AdvisorySeverity.ALERT -> Quadruple(Color(0xFFFFEBEE), Color(0xFFFFCDD2), ErrorRed, Icons.Default.Dangerous)
        AdvisorySeverity.INFO -> Quadruple(Color(0xFFE8F4FD), Color(0xFFBBDEFB), SkyBlue, Icons.Default.Info)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (langState.isUrdu) advisory.titleUr else advisory.titleEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (langState.isUrdu) advisory.adviceUr else advisory.adviceEn,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun DailyForecastCard(day: DailyForecast) {
    val langState = LocalAppLanguage.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SoftWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    text = if (langState.isUrdu) day.dayNameUr else day.dayNameEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Text(
                    text = day.dateFormatted,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when (day.iconType) {
                        "rainy", "thunderstorm" -> Icons.Default.WaterDrop
                        "partly_cloudy" -> Icons.Default.Cloud
                        else -> Icons.Default.WbSunny
                    },
                    contentDescription = null,
                    tint = if (day.rainProbability > 30) SkyBlue else GoldenYellow,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (langState.isUrdu) day.conditionUr else day.conditionEn,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (day.rainProbability > 0) {
                    Text(
                        text = "${day.rainProbability}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SkyBlue,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                }

                Text(
                    text = "${day.maxTempC.toInt()}° / ${day.minTempC.toInt()}°",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
