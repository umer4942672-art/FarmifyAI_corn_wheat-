package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val titleEn: String,
    val titleUr: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
) {
    data object Splash : Screen(
        route = "splash",
        titleEn = "Splash",
        titleUr = "شروع",
        iconFilled = Icons.Filled.Spa,
        iconOutlined = Icons.Outlined.Spa
    )

    data object Auth : Screen(
        route = "auth",
        titleEn = "Login & Signup",
        titleUr = "لاگ ان و رجسٹریشن",
        iconFilled = Icons.Filled.Person,
        iconOutlined = Icons.Outlined.Person
    )

    data object Dashboard : Screen(
        route = "dashboard",
        titleEn = "Home",
        titleUr = "ہوم",
        iconFilled = Icons.Filled.Home,
        iconOutlined = Icons.Outlined.Home
    )

    data object CropsGuide : Screen(
        route = "crops_guide",
        titleEn = "Fasal Guide",
        titleUr = "فصل گائیڈ",
        iconFilled = Icons.Filled.Spa,
        iconOutlined = Icons.Outlined.Spa
    )

    data object KisanChat : Screen(
        route = "kisan_chat",
        titleEn = "Kisan AI",
        titleUr = "کسان دوست",
        iconFilled = Icons.Filled.SmartToy,
        iconOutlined = Icons.Outlined.SmartToy
    )

    data object Mandi : Screen(
        route = "mandi",
        titleEn = "Mandi",
        titleUr = "منڈی",
        iconFilled = Icons.Filled.TrendingUp,
        iconOutlined = Icons.Outlined.TrendingUp
    )

    data object Khata : Screen(
        route = "khata",
        titleEn = "Khata",
        titleUr = "کھاتہ",
        iconFilled = Icons.Filled.AccountBalanceWallet,
        iconOutlined = Icons.Outlined.AccountBalanceWallet
    )

    data object Weather : Screen(
        route = "weather",
        titleEn = "Weather",
        titleUr = "موسم",
        iconFilled = Icons.Filled.WbSunny,
        iconOutlined = Icons.Outlined.WbSunny
    )

    data object DiseaseScan : Screen(
        route = "disease_scan",
        titleEn = "AI Doctor",
        titleUr = "پودا سکین",
        iconFilled = Icons.Filled.DocumentScanner,
        iconOutlined = Icons.Outlined.DocumentScanner
    )

    data object Settings : Screen(
        route = "settings",
        titleEn = "Settings",
        titleUr = "ترتیبات",
        iconFilled = Icons.Filled.Settings,
        iconOutlined = Icons.Outlined.Settings
    )

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(
                Dashboard,
                CropsGuide,
                KisanChat,
                Mandi,
                Khata
            )
    }
}
