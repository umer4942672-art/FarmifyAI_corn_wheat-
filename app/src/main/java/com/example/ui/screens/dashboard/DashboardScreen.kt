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
                    text = if (langState.isUrdu) "فوری کارروائیاں (Quick Actions)" else "QUICK ACTIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        title = if (langState.isUrdu) "+ آمدن" else "+ Income",
                        subtitle = if (langState.isUrdu) "فصل فروخت" else "Crop Sale",
                        icon = Icons.Default.TrendingUp,
                        accentColor = SuccessGreen,
                        onClick = onOpenAddIncome,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = if (langState.isUrdu) "+ خرچہ" else "+ Expense",
                        subtitle = if (langState.isUrdu) "کھاد، ڈیزل" else "Fertilizer/Fuel",
                        icon = Icons.Default.TrendingDown,
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
                        title = if (langState.isUrdu) "🌱 فیلڈ کام" else "🌱 Field Work",
                        subtitle = if (langState.isUrdu) "ہل، گوڈی، سپرے" else "Tillage, Spray",
                        icon = Icons.Default.Engineering,
                        accentColor = ForestGreen,
                        onClick = onOpenAddFieldWork,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        title = if (langState.isUrdu) "📷 پودا سکین" else "📷 AI Doctor",
                        subtitle = if (langState.isUrdu) "بیماری تشخیص" else "Crop Scan",
                        icon = Icons.Default.DocumentScanner,
                        accentColor = Color(0xFF673AB7),
                        onClick = onNavigateToScan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Kisan Dost AI Voice Assistant Interactive Hero Banner
        item {
            Surface(
                onClick = onNavigateToKisanChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .shadow(3.dp, shape = RoundedCornerShape(26.dp), spotColor = Color(0x331B5E20))
                    .testTag("dashboard_kisan_ai_card"),
                shape = RoundedCornerShape(26.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF0D47A1),
                                    Color(0xFF1976D2),
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
                                            text = "KISAN AI ADVISOR",
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
                                    text = if (langState.isUrdu) "کسان دوست AI سے مشورہ لیں" else "Ask Kisan Dost AI Advisor",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = if (langState.isUrdu)
                                        "اردو آواز یا چیٹ میں فصل، کھاد اور بیماریوں کے فوری مشورے حاصل کریں"
                                    else
                                        "Bilingual voice & chat assistant for fertilizer, pests & mandi rates",
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
                                    imageVector = Icons.Filled.RecordVoiceOver,
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
                                text = if (langState.isUrdu) "🌾 گندم کھاد شیڈول" else "🌾 Wheat Fertilizer",
                                onClick = onNavigateToKisanChat,
                                modifier = Modifier.weight(1f)
                            )
                            PromptChip(
                                text = if (langState.isUrdu) "🐛 سنڈی کا سپرے" else "🐛 Pest Remedies",
                                onClick = onNavigateToKisanChat,
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

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
        modifier = modifier.height(68.dp).testTag("quick_tile_${title.take(5)}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
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
