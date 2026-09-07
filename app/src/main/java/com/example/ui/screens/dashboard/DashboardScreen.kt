package com.example.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MandiRate
import com.example.data.model.MandiTrend
import com.example.ui.components.CurrencyText
import com.example.ui.components.FarmerUserVectorAvatar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.LocalAppLanguage

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateToWeather: () -> Unit,
    onNavigateToKhata: () -> Unit,
    onNavigateToMandi: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToKisanChat: () -> Unit = {},
    onNavigateToCropsGuide: () -> Unit = {},
    onOpenAddIncome: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenAddFieldWork: () -> Unit,
    modifier: Modifier = Modifier
) {
    val langState = LocalAppLanguage.current
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val weatherState by viewModel.weatherState.collectAsStateWithLifecycle()
    val khataStats by viewModel.khataSummary.collectAsStateWithLifecycle()
    val mandiRates by viewModel.filteredMandiRates.collectAsStateWithLifecycle()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) viewModel.refreshWeatherForCurrentLocation()
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.refreshWeatherForCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaleGreenBg)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 100.dp, top = 10.dp)
    ) {
        // 1. Farmer Welcome Greeting & Farm Profile Header Card
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .shadow(2.dp, shape = RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Farmer Vector Avatar
                        FarmerUserVectorAvatar(
                            size = 50.dp,
                            showTickMark = false
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = if (langState.isUrdu) "السلام علیکم" else "Assalam-o-Alaikum,",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = userProfile.fullName.ifBlank { "Chaudhry Muhammad Aslam" },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "${userProfile.farmName} • ${userProfile.district}, ${userProfile.province} (${userProfile.totalAcres.toInt()} Acres)",
                                fontSize = 12.sp,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Season Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PaleGreenBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderLight)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (langState.isUrdu) "ربیع سیزن" else "Rabi 2026",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen
                            )
                            Text(
                                text = if (langState.isUrdu) "گندم و آلو" else "Wheat/Potato",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 2. High-Contrast Hero Weather Card with Emerald-to-Forest Gradient
        item {
            val currentW = weatherState.current
            Surface(
                onClick = onNavigateToWeather,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(4.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x331B5E20))
                    .testTag("dashboard_weather_hero_card"),
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1B5E20),
                                    Color(0xFF2E7D32),
                                    Color(0xFF144D18)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        // Location & Live Pulse Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = GoldenYellow,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${weatherState.selectedDistrict.nameEn}, ${weatherState.selectedDistrict.province}",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Weather Status Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WbSunny,
                                        contentDescription = null,
                                        tint = GoldenYellow,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (langState.isUrdu) "لائیو اپڈیٹ" else "LIVE",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Temperature & Condition
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "${currentW.temperatureC.toInt()}°C",
                                    color = Color.White,
                                    fontSize = 46.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.5).sp
                                )
                                Text(
                                    text = if (langState.isUrdu) currentW.conditionUr else currentW.conditionEn,
                                    color = GoldenYellow,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Forecast Advisory Micro Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.Black.copy(alpha = 0.22f),
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        text = if (langState.isUrdu) "زرعی مشورہ:" else "Agri Alert:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GoldenYellow
                                    )
                                    Text(
                                        text = if (langState.isUrdu) "فصلوں کو پانی لگانے کے لیے موزوں موسم" else "Ideal window for irrigation",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Clear high-contrast metrics row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WeatherMetricPill(
                                icon = Icons.Default.WaterDrop,
                                label = if (langState.isUrdu) "نمی" else "Humidity",
                                value = "${currentW.humidityPercent}%"
                            )
                            WeatherMetricPill(
                                icon = Icons.Default.Air,
                                label = if (langState.isUrdu) "ہوا" else "Wind",
                                value = "${currentW.windSpeedKmh.toInt()} km/h"
                            )
                            WeatherMetricPill(
                                icon = Icons.Default.Umbrella,
                                label = if (langState.isUrdu) "بارش" else "Rain",
                                value = "${currentW.rainProbability}%"
                            )
                        }
                    }
                }
            }
        }

        // 3. Smart Farm Financial Snapshot (Net Profit, Total Income, Expense)
        item {
            Surface(
                onClick = onNavigateToKhata,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, shape = RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = SoftWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BadgeOrangeBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = AmberOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (langState.isUrdu) "سمارٹ کھاتہ بک کا خلاصہ" else "SMART KHATA BALANCE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = if (langState.isUrdu) "کل خالص بچت / منافع" else "Net Farm Profit / Savings",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }

                        // Open Khata Arrow
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Khata",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Big Net Profit Value
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        CurrencyText(
                            amount = khataStats.netProfit,
                            prefix = "Rs. ",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 28.sp
                            ),
                            color = if (khataStats.netProfit >= 0) ForestGreen else ErrorRed
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (khataStats.netProfit >= 0) BadgeGreenBg else BadgeRedBg
                        ) {
                            Text(
                                text = if (khataStats.netProfit >= 0) "منافع بخش (Profitable)" else "خسارہ (Loss)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (khataStats.netProfit >= 0) SuccessGreen else ErrorRed,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Income & Expense Breakdown Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PaleGreenBg)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (langState.isUrdu) "کل آمدن (Income)" else "Total Income",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Rs. ${khataStats.totalIncome.toInt()}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderLight))

                        Column {
                            Text(
                                text = if (langState.isUrdu) "کل اخراجات (Expense)" else "Total Expense",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "Rs. ${khataStats.totalExpense.toInt()}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderLight))

                        Column {
                            Text(
                                text = if (langState.isUrdu) "بہترین فصل" else "Top Crop",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = khataStats.mostProfitableCrop,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 4. Quick Action 4-Button Grid (High Contrast & Visible)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    // One language at a time. The Urdu label used to carry the
                    // English name in brackets while the English one shouted in caps.
                    text = if (langState.isUrdu) "فوری کارروائیاں" else "Quick actions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = if (langState.isUrdu) "آمدن" else "Income",
                        subtitle = if (langState.isUrdu) "فصل فروخت" else "Crop sale",
                        art = QuickActionArtKind.INCOME,
                        accentColor = SuccessGreen,
                        onClick = onOpenAddIncome,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = if (langState.isUrdu) "خرچہ" else "Expense",
                        subtitle = if (langState.isUrdu) "کھاد، ڈیزل" else "Fertilizer, fuel",
                        art = QuickActionArtKind.EXPENSE,
                        accentColor = ErrorRed,
                        onClick = onOpenAddExpense,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = if (langState.isUrdu) "فیلڈ کام" else "Field work",
                        subtitle = if (langState.isUrdu) "ہل، گوڈی، سپرے" else "Tillage, spray",
                        art = QuickActionArtKind.FIELD_WORK,
                        accentColor = ForestGreen,
                        onClick = onOpenAddFieldWork,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = if (langState.isUrdu) "AI مشورہ" else "AI advisory",
                        subtitle = if (langState.isUrdu) "کسان دوست سے پوچھیں" else "Ask Kisan Dost",
                        art = QuickActionArtKind.ADVISORY,
                        accentColor = Color(0xFF673AB7),
                        onClick = onNavigateToKisanChat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. AI Disease Detection hero banner (offline, on-device TFLite)
        item {
            Surface(
                onClick = onNavigateToScan,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(3.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x331B5E20))
                    .testTag("dashboard_disease_ai_card"),
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1B5E20),
                                    Color(0xFF2E7D32),
                                    Color(0xFF00897B)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = "AI DISEASE DETECTION",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GoldenYellow)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (langState.isUrdu) "پتے کی تصویر سے بیماری پہچانیں" else "Scan a leaf, find the disease",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (langState.isUrdu)
                                        "گندم اور مکئی کے پتوں کی تصویر لیں، بیماری اور علاج فوراً موبائل پر"
                                    else
                                        "Photograph a wheat or corn leaf. Diagnosis and treatment run on the phone, offline",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.92f),
                                    lineHeight = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = GoldenYellow,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Prompt Question Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PromptChip(
                                text = if (langState.isUrdu) "گندم کا پتہ" else "Wheat leaf",
                                onClick = onNavigateToScan,
                                modifier = Modifier.weight(1f)
                            )
                            PromptChip(
                                text = if (langState.isUrdu) "مکئی کا پتہ" else "Corn leaf",
                                onClick = onNavigateToScan,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // 6. Crops & Diseases Encyclopedia Direct Card
        item {
            Surface(
                onClick = onNavigateToCropsGuide,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(2.dp, shape = RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(VeryLightGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Spa,
                                contentDescription = null,
                                tint = EmeraldGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (langState.isUrdu) "فصلوں اور بیماریوں کی گائیڈ" else "Crops & Disease Encyclopedia",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (langState.isUrdu) "گندم، کپاس، چاول کے شیڈول اور مصدقہ پاکستانی سپرے" else "Detailed agronomy, fertilizer schedules & spray brands",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = "Open Guide",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 7. Live Mandi Rates Carousel (Visible & High Contrast)
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SectionHeader(
                        title = if (langState.isUrdu) "تازہ ترین منڈی ریٹس (Live Mandi)" else "CURRENT MANDI RATES",
                        icon = Icons.Outlined.TrendingUp,
                        actionLabel = if (langState.isUrdu) "تمام منڈیاں" else "View All",
                        onActionClick = onNavigateToMandi
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(mandiRates.take(6)) { rate ->
                        DashboardMandiCard(
                            rate = rate,
                            onClick = onNavigateToMandi
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldenYellow,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                Text(text = label, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
                Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

enum class QuickActionArtKind { INCOME, EXPENSE, FIELD_WORK, CROP_SCAN, ADVISORY }

/**
 * Hand-drawn vector art for the quick action tiles.
 *
 * Material icons all shared the same generic look, so the four actions were hard
 * to tell apart at a glance. These are drawn on a Canvas from ratios of the
 * available size, so they stay crisp at any density and need no extra drawables.
 */
@Composable
private fun QuickActionArt(
    kind: QuickActionArtKind,
    accent: Color,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier) {
        val s = this.size.minDimension
        fun at(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(s * x, s * y)
        fun box(w: Float, h: Float) = androidx.compose.ui.geometry.Size(s * w, s * h)
        val line = androidx.compose.ui.graphics.drawscope.Stroke(
            width = s * 0.075f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
        val soft = accent.copy(alpha = 0.25f)

        when (kind) {
            QuickActionArtKind.INCOME, QuickActionArtKind.EXPENSE -> {
                // Stack of coins with a direction arrow beside it.
                drawOval(soft, topLeft = at(0.04f, 0.62f), size = box(0.50f, 0.19f))
                drawOval(soft, topLeft = at(0.04f, 0.47f), size = box(0.50f, 0.19f))
                drawOval(accent.copy(alpha = 0.40f), topLeft = at(0.04f, 0.32f), size = box(0.50f, 0.19f))
                drawOval(accent, topLeft = at(0.04f, 0.32f), size = box(0.50f, 0.19f), style = line)

                val rising = kind == QuickActionArtKind.INCOME
                val arrow = androidx.compose.ui.graphics.Path().apply {
                    if (rising) {
                        moveTo(s * 0.66f, s * 0.42f); lineTo(s * 0.80f, s * 0.24f); lineTo(s * 0.94f, s * 0.42f)
                        moveTo(s * 0.80f, s * 0.24f); lineTo(s * 0.80f, s * 0.80f)
                    } else {
                        moveTo(s * 0.66f, s * 0.62f); lineTo(s * 0.80f, s * 0.80f); lineTo(s * 0.94f, s * 0.62f)
                        moveTo(s * 0.80f, s * 0.80f); lineTo(s * 0.80f, s * 0.24f)
                    }
                }
                drawPath(arrow, accent, style = line)
            }

            QuickActionArtKind.FIELD_WORK -> {
                // Sun over ploughed furrows, with a young sprout in front.
                drawCircle(accent.copy(alpha = 0.35f), radius = s * 0.11f, center = at(0.80f, 0.17f))
                for (i in 0..2) {
                    val y = 0.60f + i * 0.15f
                    val furrow = androidx.compose.ui.graphics.Path().apply {
                        moveTo(s * 0.04f, s * y)
                        quadraticBezierTo(s * 0.50f, s * (y - 0.13f), s * 0.96f, s * y)
                    }
                    drawPath(furrow, accent.copy(alpha = 0.55f), style = line)
                }
                drawLine(accent, at(0.36f, 0.56f), at(0.36f, 0.24f), strokeWidth = s * 0.065f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawOval(accent, topLeft = at(0.12f, 0.22f), size = box(0.24f, 0.15f))
                drawOval(accent.copy(alpha = 0.65f), topLeft = at(0.36f, 0.30f), size = box(0.24f, 0.15f))
            }

            QuickActionArtKind.ADVISORY -> {
                // Speech bubble with a wheat ear inside: farming advice, in words.
                val bubble = androidx.compose.ui.graphics.Path().apply {
                    moveTo(s * 0.10f, s * 0.14f)
                    lineTo(s * 0.90f, s * 0.14f)
                    lineTo(s * 0.90f, s * 0.66f)
                    lineTo(s * 0.40f, s * 0.66f)
                    lineTo(s * 0.24f, s * 0.90f)
                    lineTo(s * 0.24f, s * 0.66f)
                    lineTo(s * 0.10f, s * 0.66f)
                    close()
                }
                drawPath(bubble, accent.copy(alpha = 0.22f))
                drawPath(bubble, accent, style = line)
                drawLine(accent, at(0.50f, 0.55f), at(0.50f, 0.25f), strokeWidth = s * 0.055f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawOval(accent, topLeft = at(0.32f, 0.24f), size = box(0.18f, 0.11f))
                drawOval(accent, topLeft = at(0.50f, 0.24f), size = box(0.18f, 0.11f))
                drawOval(accent.copy(alpha = 0.7f), topLeft = at(0.32f, 0.38f), size = box(0.18f, 0.11f))
                drawOval(accent.copy(alpha = 0.7f), topLeft = at(0.50f, 0.38f), size = box(0.18f, 0.11f))
            }

            QuickActionArtKind.CROP_SCAN -> {
                // Leaf under a magnifier.
                val leaf = androidx.compose.ui.graphics.Path().apply {
                    moveTo(s * 0.10f, s * 0.78f)
                    quadraticBezierTo(s * 0.06f, s * 0.18f, s * 0.72f, s * 0.12f)
                    quadraticBezierTo(s * 0.74f, s * 0.66f, s * 0.10f, s * 0.78f)
                    close()
                }
                drawPath(leaf, accent.copy(alpha = 0.28f))
                drawPath(leaf, accent, style = line)
                drawLine(accent.copy(alpha = 0.7f), at(0.14f, 0.76f), at(0.62f, 0.24f),
                    strokeWidth = s * 0.05f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawCircle(accent, radius = s * 0.19f, center = at(0.66f, 0.62f), style = line)
                drawLine(accent, at(0.79f, 0.75f), at(0.95f, 0.92f), strokeWidth = s * 0.09f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    art: QuickActionArtKind,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = SoftWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
        shadowElevation = 2.dp,
        modifier = modifier.height(112.dp).testTag("quick_tile_${title.take(5)}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                QuickActionArt(
                    kind = art,
                    accent = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PromptChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DashboardMandiCard(
    rate: MandiRate,
    onClick: () -> Unit
) {
    val langState = LocalAppLanguage.current

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = SoftWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
        shadowElevation = 2.dp,
        modifier = Modifier
            .width(205.dp)
            .testTag("dashboard_mandi_card_${rate.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (langState.isUrdu) rate.cropNameUr else rate.cropNameEn.uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val trendColor = when (rate.trend) {
                    MandiTrend.UP -> SuccessGreen
                    MandiTrend.DOWN -> ErrorRed
                    MandiTrend.STABLE -> TextMuted
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (rate.trend) {
                        MandiTrend.UP -> BadgeGreenBg
                        MandiTrend.DOWN -> BadgeRedBg
                        MandiTrend.STABLE -> VeryLightGreen
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (rate.trend) {
                                MandiTrend.UP -> Icons.Default.TrendingUp
                                MandiTrend.DOWN -> Icons.Default.TrendingDown
                                MandiTrend.STABLE -> Icons.Default.TrendingFlat
                            },
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${if (rate.changePercent >= 0) "+" else ""}${rate.changePercent}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = trendColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Price per KG & Mann
            Text(
                text = "Rs. ${rate.pricePerKg.toInt()} / kg",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ForestGreen
            )

            Text(
                text = "Rs. ${(rate.pricePerKg * 40).toInt()} / من (40kg)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PaleGreenBg
                ) {
                    Text(
                        text = "${rate.city} Mandi",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "آج کا ریٹ",
                    fontSize = 10.sp,
                    color = TextMuted
                )
            }
        }
    }
}
