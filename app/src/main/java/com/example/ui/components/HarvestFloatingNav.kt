package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.Screen
import com.example.ui.theme.LocalFarmifySurfaces

/**
 * A floating pill navigation bar, used by the Harvest theme.
 *
 * The standard bar spans the full width and sits flush against the bottom
 * edge. This one is a rounded bar with a margin around it, which is what gives
 * the Harvest layout its distinct feel.
 *
 * Only the selected item shows its label. Five labelled items in a pill this
 * narrow would each be clipped, and a clipped Urdu label is worse than none;
 * the selected item expanding to show its own reads better and keeps the
 * icons large enough to hit.
 */
@Composable
fun HarvestFloatingNav(
    items: List<Screen>,
    current: Screen,
    isUrdu: Boolean,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalFarmifySurfaces.current

    // A deep bar against the pale Harvest surfaces, so it reads as a distinct
    // object floating above the page rather than part of it.
    val barColor = if (palette.isDark) palette.offWhite else Color(0xFF3B2412)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = barColor,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(14.dp, RoundedCornerShape(30.dp), spotColor = Color(0x59422A12))
                .testTag("farmify_bottom_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { screen ->
                    HarvestNavItem(
                        screen = screen,
                        selected = screen == current,
                        isUrdu = isUrdu,
                        accent = palette.goldenYellow,
                        onClick = { if (screen != current) onSelect(screen) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HarvestNavItem(
    screen: Screen,
    selected: Boolean,
    isUrdu: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val background by animateColorAsState(
        targetValue = if (selected) accent else Color.Transparent,
        label = "nav_bg"
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 14.dp else 11.dp,
        label = "nav_pad"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = background,
        modifier = Modifier.testTag("nav_item_${screen.route}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (selected) screen.iconFilled else screen.iconOutlined,
                contentDescription = screen.titleEn,
                tint = if (selected) Color(0xFF3B2412) else Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(21.dp)
            )
            if (selected) {
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = if (isUrdu) screen.titleUr else screen.titleEn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3B2412),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
