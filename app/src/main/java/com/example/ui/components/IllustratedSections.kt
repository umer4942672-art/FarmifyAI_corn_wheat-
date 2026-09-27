package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Illustrated layout pieces, used by the Harvest and Meadow themes.
 *
 * The designs this borrows from lean on stock photography for most of their
 * warmth. Photographs would add megabytes to the APK, need licensing, and be
 * the same generic fields every other farming app uses, so the imagery here is
 * drawn instead. It costs nothing to ship, recolours with the theme, and stays
 * sharp at any screen density.
 */

// ---------------------------------------------------------------------------
// Crop chips
// ---------------------------------------------------------------------------

private data class IllustratedCrop(
    val nameEn: String,
    val nameUr: String,
    val warmTint: Color,
    val freshTint: Color,
    val kind: String
)

// Two tints per crop. The warm set belongs to Harvest's clay surfaces; on a
// near-white background those browns look muddy, so Meadow gets a fresher set.
private val illustratedCrops = listOf(
    IllustratedCrop("Wheat", "گندم", Color(0xFFD9A441), Color(0xFFCA8A04), "wheat"),
    IllustratedCrop("Rice", "چاول", Color(0xFFCBB68B), Color(0xFF65A30D), "rice"),
    IllustratedCrop("Maize", "مکئی", Color(0xFFE0A72E), Color(0xFFEAB308), "maize"),
    IllustratedCrop("Cotton", "کپاس", Color(0xFFBFA98C), Color(0xFF78909C), "cotton"),
    IllustratedCrop("Potato", "آلو", Color(0xFFB07C4A), Color(0xFF8D6E63), "potato"),
    IllustratedCrop("Onion", "پیاز", Color(0xFFC08457), Color(0xFFC2410C), "onion")
)

/** A scrollable row of crops, each drawn into its own circle. */
@Composable
fun IllustratedCropChips(
    isUrdu: Boolean,
    onCropClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (isUrdu) "اجناس اور فصلیں" else "Crops and commodities",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 16.dp, bottom = 10.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val warm = LocalAppTheme.current == AppThemeMode.HARVEST
            illustratedCrops.forEach { c ->
                val crop = c
                val tint = if (warm) c.warmTint else c.freshTint
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(64.dp)
                ) {
                    Surface(
                        onClick = { onCropClick(crop.nameEn) },
                        shape = CircleShape,
                        color = SoftWhite,
                        border = BorderStroke(1.dp, BorderLight),
                        modifier = Modifier
                            .size(62.dp)
                            .shadow(5.dp, CircleShape, spotColor = Color(0x33795548))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.16f))
                            )
                            Canvas(modifier = Modifier.size(32.dp)) {
                                drawCropMark(crop.kind, tint)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isUrdu) crop.nameUr else crop.nameEn,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** Simple crop marks. Recognisable at 32dp, which detail would not survive. */
private fun DrawScope.drawCropMark(kind: String, tint: Color) {
    val s = size.minDimension
    fun at(x: Float, y: Float) = Offset(s * x, s * y)
    val thick = s * 0.085f

    when (kind) {
        "wheat", "rice" -> {
            drawLine(tint, at(0.5f, 0.95f), at(0.5f, 0.28f), thick, StrokeCap.Round)
            for (i in 0..3) {
                val y = 0.22f + i * 0.16f
                drawLine(tint, at(0.5f, y + 0.09f), at(0.26f, y), thick * 0.85f, StrokeCap.Round)
                drawLine(tint, at(0.5f, y + 0.09f), at(0.74f, y), thick * 0.85f, StrokeCap.Round)
            }
        }
        "maize" -> {
            drawRoundRect(
                color = tint,
                topLeft = at(0.34f, 0.16f),
                size = Size(s * 0.32f, s * 0.62f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(s * 0.16f)
            )
            drawLine(tint, at(0.5f, 0.78f), at(0.5f, 0.95f), thick, StrokeCap.Round)
            for (i in 0..2) {
                val y = 0.28f + i * 0.18f
                drawLine(Color.White.copy(alpha = 0.55f), at(0.38f, y), at(0.62f, y), s * 0.045f)
            }
        }
        "cotton" -> {
            listOf(0.36f to 0.38f, 0.64f to 0.38f, 0.5f to 0.22f, 0.5f to 0.52f).forEach { (x, y) ->
                drawCircle(tint, radius = s * 0.16f, center = at(x, y))
            }
            drawLine(tint, at(0.5f, 0.62f), at(0.5f, 0.95f), thick, StrokeCap.Round)
        }
        "potato", "onion" -> {
            drawOval(tint, topLeft = at(0.18f, 0.34f), size = Size(s * 0.64f, s * 0.5f))
            drawLine(tint, at(0.5f, 0.34f), at(0.5f, 0.1f), thick * 0.8f, StrokeCap.Round)
            drawLine(tint, at(0.5f, 0.2f), at(0.68f, 0.12f), thick * 0.7f, StrokeCap.Round)
        }
        else -> drawCircle(tint, radius = s * 0.34f, center = at(0.5f, 0.5f))
    }
}

// ---------------------------------------------------------------------------
// Field card
// ---------------------------------------------------------------------------

/**
 * A field card with a drawn landscape where these designs would place a photo.
 *
 * Drawn rather than photographed, for the reasons above, and because a real
 * photograph of someone else's farm would misrepresent the farmer's own field.
 */
@Composable
fun IllustratedFieldCard(
    fieldName: String,
    areaLabel: String,
    isUrdu: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = SoftWhite,
        border = BorderStroke(1.dp, BorderLight),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0x33795548))
    ) {
        Column {
            // Resolved outside the Canvas: its onDraw is a DrawScope, which
            // cannot read a CompositionLocal.
            val warmField = LocalAppTheme.current == AppThemeMode.HARVEST
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
            ) {
                drawField(warm = warmField)
            }
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUrdu) "میرے کھیت" else "My fields",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = fieldName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
                Surface(shape = RoundedCornerShape(11.dp), color = GoldenYellow.copy(alpha = 0.18f)) {
                    Text(
                        text = areaLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenYellow,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sky, sun, distant hills and rows of crop.
 *
 * Harvest gets ripened gold; the cooler themes get a green field, since a
 * golden landscape on a near-white green page reads as a mistake rather than a
 * choice.
 */
private fun DrawScope.drawField(warm: Boolean) {
    val w = size.width
    val h = size.height

    val sky = if (warm) listOf(Color(0xFFFCE7B8), Color(0xFFF6D089))
              else listOf(Color(0xFFDDF3FB), Color(0xFFBEE8D4))
    val hillNear = if (warm) Color(0xFFD8B26A) else Color(0xFF8FCB9B)
    val hillFar = if (warm) Color(0xFFCFA557) else Color(0xFF6DBA85)
    val ground = if (warm) listOf(Color(0xFFE3B865), Color(0xFFC9923F))
                 else listOf(Color(0xFF86C97F), Color(0xFF4E9E55))
    val stalk = if (warm) Color(0xFFA8712B) else Color(0xFF2F7A3E)

    drawRect(
        brush = Brush.verticalGradient(sky, endY = h * 0.55f),
        size = Size(w, h * 0.55f)
    )
    drawCircle(
        color = Color(0xFFF7B733).copy(alpha = 0.85f),
        radius = h * 0.13f,
        center = Offset(w * 0.78f, h * 0.2f)
    )

    // Distant hills
    drawOval(
        color = hillNear.copy(alpha = 0.55f),
        topLeft = Offset(-w * 0.1f, h * 0.34f),
        size = Size(w * 0.7f, h * 0.3f)
    )
    drawOval(
        color = hillFar.copy(alpha = 0.5f),
        topLeft = Offset(w * 0.45f, h * 0.36f),
        size = Size(w * 0.75f, h * 0.28f)
    )

    // Ground
    drawRect(
        brush = Brush.verticalGradient(ground),
        topLeft = Offset(0f, h * 0.52f),
        size = Size(w, h * 0.48f)
    )

    // Rows of wheat, shorter towards the horizon so the field has depth
    val stalkColor = stalk
    var y = h * 0.6f
    var step = h * 0.055f
    while (y < h) {
        var x = 0f
        val height = (y - h * 0.5f) * 0.55f
        while (x < w) {
            drawLine(
                color = stalkColor.copy(alpha = 0.55f),
                start = Offset(x, y),
                end = Offset(x - height * 0.2f, y - height),
                strokeWidth = h * 0.012f,
                cap = StrokeCap.Round
            )
            x += w * 0.045f
        }
        y += step
        step *= 1.12f
    }
}
