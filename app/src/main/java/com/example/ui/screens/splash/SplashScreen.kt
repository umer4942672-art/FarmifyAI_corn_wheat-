package com.example.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.LocalAppLanguage
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val langState = LocalAppLanguage.current
    var currentStepIndex by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0f) }

    // Pulsing animation for outer ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val loadingStepsEn = listOf(
        "Loading Live Mandi Rates...",
        "Connecting Local Weather Radar...",
        "Initializing Kisan Dost AI...",
        "Welcome to FarmifyAI!"
    )

    val loadingStepsUr = listOf(
        "تازہ ترین منڈی ریٹس لوڈ ہو رہے ہیں...",
        "موسمی راڈار رابطہ قائم کر رہا ہے...",
        "کسان دوست AI تیار کیا جا رہا ہے...",
        "خوش آمدید فارمی فائی AI!"
    )

    LaunchedEffect(Unit) {
        // Step 1
        progress = 0.25f
        currentStepIndex = 0
        delay(600)

        // Step 2
        progress = 0.60f
        currentStepIndex = 1
        delay(600)

        // Step 3
        progress = 0.90f
        currentStepIndex = 2
        delay(600)

        // Step 4
        progress = 1.0f
        currentStepIndex = 3
        delay(500)

        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F3816),
                        Color(0xFF1B5E20),
                        Color(0xFF0A290E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .testTag("splash_screen")
    ) {
        // Subtle background decorative circles
        Box(
            modifier = Modifier
                .size(350.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-80).dp)
                .alpha(0.08f)
                .clip(CircleShape)
                .background(Color.White)
        )

        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 80.dp)
                .alpha(0.08f)
                .clip(CircleShape)
                .background(GoldenYellow)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Language Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = { langState.toggleLanguage() },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    modifier = Modifier.testTag("splash_lang_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = GoldenYellow,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (langState.isUrdu) "English" else "اردو",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Center: App Branding & Emblem
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 20.dp)
            ) {
                // Pulsing Halo & Logo
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Glowing Ring
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(GoldenYellow.copy(alpha = glowAlpha * 0.4f))
                    )

                    // Inner Emblem Card
                    Surface(
                        shape = RoundedCornerShape(36.dp),
                        color = Color.White,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .size(96.dp)
                            .border(2.dp, GoldenYellow, RoundedCornerShape(36.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF2E7D32),
                                            Color(0xFF1B5E20)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Spa,
                                contentDescription = "Farmify Logo",
                                tint = GoldenYellow,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Farmify",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "AI",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldenYellow,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Urdu Subtitle
                Text(
                    text = "پاکستان کا جدید ترین ڈیجیٹل کسان پلیٹ فارم",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // English Subtitle
                Text(
                    text = "Smart Agricultural Intelligence & Farm Management",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Badge Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureBadge(text = if (langState.isUrdu) "🌾 منڈی ریٹس" else "🌾 Mandi Rates")
                    FeatureBadge(text = if (langState.isUrdu) "🤖 کسان AI" else "🤖 Kisan AI")
                    FeatureBadge(text = if (langState.isUrdu) "💰 کھاتہ بک" else "💰 Smart Khata")
                }
            }

            // Bottom: Loading Status & Skip Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Loading Status Text
                Text(
                    text = if (langState.isUrdu) {
                        loadingStepsUr.getOrElse(currentStepIndex) { loadingStepsUr.last() }
                    } else {
                        loadingStepsEn.getOrElse(currentStepIndex) { loadingStepsEn.last() }
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Linear Progress Bar
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GoldenYellow,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Continue / Skip Button
                OutlinedButton(
                    onClick = onSplashFinished,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(44.dp)
                        .testTag("splash_continue_btn")
                ) {
                    Text(
                        text = if (langState.isUrdu) "شروع کریں →" else "Get Started →",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
