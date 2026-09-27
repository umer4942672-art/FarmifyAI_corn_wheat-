package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * What actually needs the farmer's attention, drawn from real state.
 *
 * The reference design this borrows from shows a "Today's Tasks" list. This app
 * has no task feature, and inventing one would mean printing plausible-looking
 * rows that correspond to nothing. Instead the section reports things that are
 * genuinely outstanding: work waiting on a decision, unread messages, records
 * that never reached the cloud, and a spraying warning when rain is likely.
 *
 * When nothing is outstanding the whole section is hidden rather than showing
 * an empty state, because a farmer with nothing to do should see a shorter
 * page, not a box telling them so.
 */

private data class AttentionItem(
    val title: String,
    val detail: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
fun AttentionSection(
    isUrdu: Boolean,
    workNeedingAction: Int,
    unreadMessages: Int,
    unsyncedRecords: Int,
    rainProbability: Int,
    onOpenWork: () -> Unit,
    onOpenKhata: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = buildList {
        if (workNeedingAction > 0) {
            add(
                AttentionItem(
                    title = if (isUrdu) "کام آپ کے جواب کا منتظر" else "Work waiting on you",
                    detail = if (isUrdu) "$workNeedingAction کام" else "$workNeedingAction job(s) need a decision",
                    icon = Icons.Filled.Handshake,
                    accent = AmberOrange,
                    onClick = onOpenWork
                )
            )
        }
        if (unreadMessages > 0) {
            add(
                AttentionItem(
                    title = if (isUrdu) "نئے پیغام" else "New messages",
                    detail = if (isUrdu) "$unreadMessages پیغام" else "$unreadMessages unread",
                    icon = Icons.Filled.Handshake,
                    accent = EmeraldGreen,
                    onClick = onOpenWork
                )
            )
        }
        if (unsyncedRecords > 0) {
            add(
                AttentionItem(
                    title = if (isUrdu) "کلاؤڈ پر محفوظ نہیں" else "Not yet backed up",
                    detail = if (isUrdu)
                        "$unsyncedRecords اندراج، انٹرنیٹ آنے پر خود چلے جائیں گے"
                    else
                        "$unsyncedRecords record(s), will upload when online",
                    icon = Icons.Filled.CloudUpload,
                    accent = GoldenYellow,
                    onClick = onOpenKhata
                )
            )
        }
        // A spray washed off by rain is wasted money and a wasted day, which is
        // why this is worth a row of its own rather than a line in the weather
        // card the farmer may have scrolled past.
        if (rainProbability >= 50) {
            add(
                AttentionItem(
                    title = if (isUrdu) "آج سپرے نہ کریں" else "Hold off spraying",
                    detail = if (isUrdu)
                        "بارش کا امکان $rainProbability فیصد، دوا دھل سکتی ہے"
                    else
                        "$rainProbability% chance of rain could wash it off",
                    icon = Icons.Filled.Umbrella,
                    accent = ErrorRed,
                    onClick = {}
                )
            )
        } else if (rainProbability in 1..20) {
            add(
                AttentionItem(
                    title = if (isUrdu) "آبپاشی کے لیے موزوں" else "Good day to irrigate",
                    detail = if (isUrdu)
                        "بارش کا امکان کم ہے"
                    else
                        "Rain is unlikely today",
                    icon = Icons.Filled.WaterDrop,
                    accent = ForestGreen,
                    onClick = {}
                )
            )
        }
    }

    AnimatedVisibility(
        visible = items.isNotEmpty(),
        enter = fadeIn() + expandVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isUrdu) "آج کے کام" else "Needs your attention",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            items.forEach { item -> AttentionRow(item) }
        }
    }
}

@Composable
private fun AttentionRow(item: AttentionItem) {
    Surface(
        onClick = item.onClick,
        shape = RoundedCornerShape(16.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, item.accent.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(item.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1
                )
                Text(
                    text = item.detail,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp,
                    maxLines = 2
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
