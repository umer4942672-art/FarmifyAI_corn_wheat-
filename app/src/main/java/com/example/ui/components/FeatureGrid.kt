package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * The four things a farmer opens the app to do, as a grid near the top.
 *
 * These used to be spread down the page behind the weather card, the financial
 * snapshot and a disease banner, so reaching the ledger or the scanner meant
 * scrolling past three full-height cards. They are now the first thing under
 * the weather.
 *
 * Icons are 26dp in a 52dp tile rather than the 20dp used elsewhere, because
 * the people using this often cannot read the labels quickly and navigate by
 * the shape instead.
 */

data class DashboardFeature(
    val titleEn: String,
    val titleUr: String,
    val subtitleEn: String,
    val subtitleUr: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit,
    /** Draws attention to one tile, used for the scanner. */
    val pulse: Boolean = false
)

@Composable
fun FeatureGrid(
    features: List<DashboardFeature>,
    isUrdu: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        features.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { feature ->
                    FeatureCard(feature, isUrdu, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: DashboardFeature,
    isUrdu: Boolean,
    modifier: Modifier = Modifier
) {
    val surfaces = LocalFarmifySurfaces.current

    // A slow breath on the scanner tile. Fast or bouncy motion is tiring on a
    // screen a farmer checks many times a day, so this is barely perceptible.
    val transition = rememberInfiniteTransition(label = "feature_pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (feature.pulse) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // On the pale themes the tile takes a wash of its own accent, which is what
    // makes the four read as different things at a glance. On the dark themes a
    // wash would muddy them, so the surface colour is used instead.
    val tileColor = if (surfaces.isDark) surfaces.softWhite else feature.accent.copy(alpha = 0.10f)

    Surface(
        onClick = feature.onClick,
        shape = RoundedCornerShape(20.dp),
        color = tileColor,
        border = BorderStroke(1.dp, feature.accent.copy(alpha = if (surfaces.isDark) 0.35f else 0.22f)),
        modifier = modifier
            .scale(pulse)
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = feature.accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(feature.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (isUrdu) feature.titleUr else feature.titleEn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
            Text(
                text = if (isUrdu) feature.subtitleUr else feature.subtitleEn,
                fontSize = 11.5.sp,
                color = TextSecondary,
                lineHeight = 15.sp,
                maxLines = 2
            )
        }
    }
}
