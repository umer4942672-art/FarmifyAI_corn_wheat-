package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.AppLanguage
import com.example.util.LocalAppLanguage
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FarmifyTopAppBar(
    title: String,
    subtitle: String? = null,
    onLanguageToggle: () -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val langState = LocalAppLanguage.current
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("farmify_top_app_bar"),
        color = Color.White.copy(alpha = 0.85f),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Farmer Vector Avatar + Assalam-o-Alaikum Greeting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    FarmerUserVectorAvatar(
                        size = 46.dp,
                        showTickMark = true,
                        onClick = onProfileClick
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (langState.isUrdu) "السلام علیکم" else "ASSALAM-O-ALAIKUM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp
                                ),
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = SuccessGreen,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = if (subtitle != null && subtitle.isNotBlank()) subtitle else "Muhammad!",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            ),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Sleek Action Buttons: Language Pill & Notification Bell
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sleek Language Pill
                    Surface(
                        onClick = onLanguageToggle,
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
                        shadowElevation = 1.dp,
                        modifier = Modifier.testTag("language_toggle_btn")
                    ) {
                        Text(
                            text = if (langState.isUrdu) "English" else "اردو",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Notification Bell / Profile Squircle
                    Surface(
                        onClick = onProfileClick,
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSlate),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("profile_top_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SoftWhite,
    borderColor: Color = BorderLight,
    elevation: Dp = 1.dp,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, shape = shape, spotColor = Color(0x0F1B3022))
            .border(1.dp, borderColor, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(VeryLightGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    fontSize = 13.sp
                ),
                color = Color(0xFF334155)
            )
        }

        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.testTag("section_action_${title.take(6)}")
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = EmeraldGreen
                    )
                )
            }
        }
    }
}

@Composable
fun CurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    prefix: String = "Rs.",
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = TextPrimary
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    val formatted = formatter.format(amount.toLong())
    Text(
        text = "$prefix $formatted",
        style = style,
        color = color,
        modifier = modifier
    )
}

@Composable
fun WeatherMetricItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EmeraldGreen,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

@Composable
fun OfflineStatusPill(
    isOnline: Boolean = true,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isOnline) BadgeGreenBg else BadgeRedBg,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) SuccessGreen else ErrorRed)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isOnline) "Live Sync" else "Offline Khata",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isOnline) SuccessGreen else ErrorRed
            )
        }
    }
}

/**
 * Scalable Vector Image of Farmer Profile with optional Green Verified Checkmark
 */
@Composable
fun FarmerUserVectorAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    showTickMark: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Box(modifier = modifier.size(size).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize().clip(CircleShape)) {
            val w = size.width; val h = size.height
            drawCircle(brush = Brush.linearGradient(listOf(Color(0xFFFFF3D6), Color(0xFFDCEFD8))))
            drawCircle(Color(0xFF2E7D32), style = androidx.compose.ui.graphics.drawscope.Stroke(w * .035f))
            // Clean Pakistani farmer avatar: straw cap, face, beard and blue kurta.
            drawRoundRect(Color(0xFF355C7D), topLeft = androidx.compose.ui.geometry.Offset(w*.16f,h*.68f), size = androidx.compose.ui.geometry.Size(w*.68f,h*.38f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w*.18f))
            drawCircle(Color(0xFFC98962), radius=w*.23f, center=androidx.compose.ui.geometry.Offset(w*.5f,h*.47f))
            drawArc(Color(0xFF5A3A28), 0f, 180f, false, androidx.compose.ui.geometry.Rect(w*.28f,h*.38f,w*.72f,h*.72f), style=androidx.compose.ui.graphics.drawscope.Stroke(w*.07f))
            drawRoundRect(Color(0xFFC89B3C), topLeft=androidx.compose.ui.geometry.Offset(w*.25f,h*.20f), size=androidx.compose.ui.geometry.Size(w*.5f,h*.12f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.05f))
            drawCircle(Color(0xFF5A3A28), radius=w*.035f, center=androidx.compose.ui.geometry.Offset(w*.41f,h*.47f)); drawCircle(Color(0xFF5A3A28), radius=w*.035f, center=androidx.compose.ui.geometry.Offset(w*.59f,h*.47f))
        }
        if (showTickMark) Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.align(Alignment.BottomEnd).size(size*.30f))
    }
}
