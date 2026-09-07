package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.FarmifyTopAppBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.chat.KisanChatScreen
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.disease.AIDiseaseScreen
import com.example.ui.screens.guide.CropsGuideScreen
import com.example.ui.screens.khata.AddExpenseDialog
import com.example.ui.screens.khata.AddFieldWorkDialog
import com.example.ui.screens.khata.AddIncomeDialog
import com.example.ui.screens.khata.SmartKhataScreen
import com.example.ui.screens.mandi.MandiRatesScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.splash.SplashScreen
import com.example.ui.screens.weather.WeatherForecastScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.util.LanguageState
import com.example.util.LocalAppLanguage
import com.example.util.str
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val languageState = remember { LanguageState() }
            val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalAppLanguage provides languageState) {
                FarmifyTheme(themeMode = currentTheme) {
                    FarmifyApp(viewModel = viewModel, languageState = languageState)
                }
            }
        }
    }
}

@Composable
fun FarmifyApp(
    viewModel: MainViewModel,
    languageState: LanguageState
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    val snackbarHostState = remember { SnackbarHostState() }
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddFieldWorkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.userFeedback.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val isFullScreen = currentScreen == Screen.Splash || currentScreen == Screen.Auth

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("farmify_main_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isFullScreen) {
                FarmifyTopAppBar(
                    title = str("app_title"),
                    subtitle = str("tagline"),
                    onLanguageToggle = {
                        languageState.toggleLanguage()
                        viewModel.toggleLanguage()
                    },
                    onProfileClick = {
                        currentScreen = Screen.Settings
                    }
                )
            }
        },
        bottomBar = {
            if (!isFullScreen) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        windowInsets = NavigationBarDefaults.windowInsets,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("farmify_bottom_bar")
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.iconFilled else screen.iconOutlined,
                                        contentDescription = screen.titleEn,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = if (languageState.isUrdu) screen.titleUr else screen.titleEn,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.route}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullScreen) PaddingValues(0.dp) else innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    Screen.Splash -> SplashScreen(
                        onSplashFinished = {
                            currentScreen = if (userProfile.isAuthenticated) {
                                Screen.Dashboard
                            } else {
                                Screen.Auth
                            }
                        }
                    )
                    Screen.Auth -> AuthScreen(
                        viewModel = viewModel,
                        onAuthSuccess = {
                            currentScreen = Screen.Dashboard
                        }
                    )
                    Screen.Dashboard -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToWeather = { currentScreen = Screen.Weather },
                        onNavigateToKhata = { currentScreen = Screen.Khata },
                        onNavigateToMandi = { currentScreen = Screen.Mandi },
                        onNavigateToScan = { currentScreen = Screen.DiseaseScan },
                        onNavigateToKisanChat = { currentScreen = Screen.KisanChat },
                        onNavigateToCropsGuide = { currentScreen = Screen.CropsGuide },
                        onOpenAddIncome = { showAddIncomeDialog = true },
                        onOpenAddExpense = { showAddExpenseDialog = true },
                        onOpenAddFieldWork = { showAddFieldWorkDialog = true }
                    )
                    Screen.CropsGuide -> CropsGuideScreen(
                        viewModel = viewModel,
                        onNavigateToScan = { currentScreen = Screen.DiseaseScan },
                        onAskAiQuestion = { question ->
                            viewModel.sendChatMessage(question)
                            currentScreen = Screen.KisanChat
                        }
                    )
                    Screen.KisanChat -> KisanChatScreen(
                        viewModel = viewModel,
                        onNavigateToCropGuide = { _ ->
                            currentScreen = Screen.CropsGuide
                        },
                        onNavigateToDiseaseScan = { currentScreen = Screen.DiseaseScan }
                    )
                    Screen.Khata -> SmartKhataScreen(viewModel = viewModel)
                    Screen.Mandi -> MandiRatesScreen(viewModel = viewModel)
                    Screen.Weather -> WeatherForecastScreen(viewModel = viewModel)
                    Screen.DiseaseScan -> AIDiseaseScreen(viewModel = viewModel)
                    Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onLogout = {
                            currentScreen = Screen.Auth
                        }
                    )
                }
            }
        }
    }

    // Global Floating Dialogs
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            onDismiss = { showAddIncomeDialog = false },
            onSave = { crop, qty, unit, price, buyer, field, desc ->
                viewModel.addIncome(crop, qty, unit, price, buyer, field, desc)
                showAddIncomeDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSave = { crop, field, cat, amt, desc ->
                viewModel.addExpense(crop, field, cat, amt, desc)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddFieldWorkDialog) {
        AddFieldWorkDialog(
            onDismiss = { showAddFieldWorkDialog = false },
            onSave = { crop, field, acres, act, desc, labor, seed, fert, pest, irrig, mach, trans, other ->
                viewModel.addFieldWork(crop, field, acres, act, desc, labor, seed, fert, pest, irrig, mach, trans, other)
                showAddFieldWorkDialog = false
            }
        )
    }
}
