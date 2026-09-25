package com.astrochat.insufficient.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrochat.insufficient.R
import com.astrochat.insufficient.ui.theme.Tokens

/**
 * "CONGRATULATIONS! / ₹N / will be added to your wallet" — the foil card above the pay bar.
 *
 * The foil is a sweep gradient under a radial highlight, with a very fine rotating ray pattern
 * on top at 8.5% opacity. That last layer is what makes it read as foil rather than as a green
 * gradient; drop it and the card goes flat, which is the usual reason this gets rebuilt wrong.
 *
 * TWO foils, and which one shows is not decorative. When the amount earns nothing at all — no
 * bonus and no coupon, which on this table is ₹50 alone — the card turns GOLD and drops the
 * "CONGRATULATIONS!" eyebrow, because there is nothing to congratulate. Keeping it green would
 * have the card celebrating a plain top-up.
 *
 * Only the TOP corners are rounded — the card's bottom edge is covered by the white arc below.
 */
@Composable
fun CongratsCard(credit: Int, gold: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(268.dp)
            .height(108.dp)
            .shadow(6.dp, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            // The 1.5dp rim, on a 150deg axis. Green is White 95% · Success/700 45% · White 85%.
            // Gold has NO token: the Warning ramp is a saturated amber and no opacity of it
            // reaches an antique gold, so that middle stop is a literal.
            .background(
                Brush.linearGradient(
                    listOf(
                        Tokens.Palette.white.copy(alpha = 0.95f),
                        if (gold) Color(0xFF966E19).copy(alpha = 0.42f)
                        else Tokens.Palette.success700.copy(alpha = 0.45f),
                        Tokens.Palette.white.copy(alpha = if (gold) 0.88f else 0.85f)
                    )
                )
            )
            .padding(1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(105.dp)
                .clip(RoundedCornerShape(topStart = 16.5.dp, topEnd = 16.5.dp)),
            contentAlignment = Alignment.Center
        ) {
            FoilSkin(gold)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                if (!gold) {
                    Text(
                        "CONGRATULATIONS!",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.7.sp,
                        color = Tokens.Palette.success800
                    )
                }
                val ink = if (gold) GoldCardInk else CardInk
                Row(
                    Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text("₹", fontSize = 17.sp, lineHeight = 17.sp, fontWeight = FontWeight.Bold, color = ink)
                    Text(
                        "$credit",
                        fontSize = 31.sp, lineHeight = 31.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                        color = ink
                    )
                }
                Text(
                    "will be added to your wallet",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Tokens.Palette.gray800,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private val CardInk = Color(0xFF04301F)
private val GoldCardInk = Color(0xFF3F3417)

/**
 * The foil itself. Three layers, bottom to top: the sweep, a slowly rotating ray comb, and a
 * radial highlight that lifts the middle where the figure sits.
 *
 * Compose's sweep gradient starts at 3 o'clock while CSS's conic starts at 12, and the
 * prototype's starts at 190deg on top of that — so every stop here is the CSS angle mapped
 * through `((100 + a) mod 360) / 360`. Re-derive with that formula if a stop ever moves.
 */
@Composable
private fun FoilSkin(gold: Boolean) {
    val spin = rememberInfiniteTransition(label = "foil")
    val angle by spin.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(22000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "foilAngle"
    )

    // Same eleven stop POSITIONS for both foils — only the hues differ, so the two cards catch
    // the light identically and swapping one for the other reads as a change of metal.
    val sweep = if (gold) listOf(
        0.0000f to Color(0xFFEAD9AE), 0.0778f to Color(0xFFF4EBD3), 0.1778f to Color(0xFFE6D3A2),
        0.2778f to Color(0xFFF3E8CC), 0.3611f to Color(0xFFDEC88F), 0.4444f to Color(0xFFFBF4E4),
        0.5444f to Color(0xFFE2CE99), 0.6444f to Color(0xFFF7EEDA), 0.7444f to Color(0xFFD8C085),
        0.8444f to Color(0xFFFBF4E4), 0.9556f to Color(0xFFE0CB94), 1.0000f to Color(0xFFEAD9AE)
    ) else listOf(
        0.0000f to Color(0xFFA0DCC2), 0.0778f to Color(0xFFD2F0E2), 0.1778f to Color(0xFF8FD7B7),
        0.2778f to Color(0xFFCDEEDE), 0.3611f to Color(0xFF86D3B2), 0.4444f to Color(0xFFE6F8EF),
        0.5444f to Color(0xFF8AD4B4), 0.6444f to Color(0xFFD9F3E8), 0.7444f to Color(0xFF7FCDAC),
        0.8444f to Color(0xFFE6F8EF), 0.9556f to Color(0xFF84D1B0), 1.0000f to Color(0xFFA0DCC2)
    )
    val ray = if (gold) Color(0xFF60460A) else Color(0xFF00462C)
    val highlight = if (gold) 0.68f else 0.62f

    Canvas(Modifier.fillMaxWidth().height(105.dp)) {
        val centre = Offset(size.width * 0.5f, size.height * 0.56f)
        drawRect(Brush.sweepGradient(*sweep.toTypedArray(), center = centre))

        // The ray comb. 180 hairlines struck from the same centre, turning once every 22s.
        rotate(degrees = angle, pivot = centre) {
            val reach = size.maxDimension * 1.6f
            repeat(180) { i ->
                val a = Math.toRadians(i * 2.0)
                drawLine(
                    color = ray.copy(alpha = 0.085f),
                    start = centre,
                    end = Offset(
                        centre.x + (reach * kotlin.math.cos(a)).toFloat(),
                        centre.y + (reach * kotlin.math.sin(a)).toFloat()
                    ),
                    strokeWidth = 1f
                )
            }
        }

        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    Tokens.Palette.white.copy(alpha = highlight),
                    Tokens.Palette.white.copy(alpha = 0f)
                ),
                center = centre,
                radius = size.width * 0.6f
            )
        )
    }
}

/**
 * The white arc that the congratulations card sits into. It is the card's bottom edge: the card
 * is pulled 22dp down into it so the two overlap, which is why the card has no bottom radius.
 * The bright line along the arc is a left-to-right white/mint/white gradient.
 *
 * That line is MINT, so it goes with the green card and is dropped entirely under the gold one
 * rather than being recoloured — a gold card with a green seam under it is the tell that the
 * two were built separately.
 */
@Composable
fun CardArc(gold: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(24.dp)) {
        val w = size.width
        val h = size.height
        fun y(v: Float) = v / 24f * h
        val curve = Path().apply {
            moveTo(0f, y(1.5f))
            cubicTo(w / 3f, y(28f), w * 2f / 3f, y(28f), w, y(1.5f))
        }
        val filled = Path().apply {
            addPath(curve)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(filled, Tokens.Palette.white)
        if (!gold) {
            drawPath(
                curve,
                Brush.horizontalGradient(
                    0f to Tokens.Palette.white,
                    0.48f to Color(0xFF78F8BE),
                    1f to Tokens.Palette.white
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Butt)
            )
        }
    }
}

/**
 * Payment summary row. Collapsed it is a one-line "₹100 + ₹18 GST"; the chevron is the only
 * affordance, so it rotates on expand exactly like See More's does.
 */
@Composable
fun PaymentSummaryRow(
    amount: Int,
    gst: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val angle by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300, easing = Tokens.Motion.easeOut),
        label = "sumChevron"
    )
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Payment Summary", fontSize = 13.5.sp, lineHeight = 18.sp, color = Tokens.Palette.gray500)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("₹$amount + ₹$gst GST", fontSize = 13.5.sp, lineHeight = 18.sp, color = Tokens.Palette.gray500)
            ChevronGlyph(Tokens.Palette.gray500, Modifier.size(18.dp).rotate(angle))
        }
    }
}

/**
 * The pay bar. "Pay ₹118" and "(GST incl.)" are ONE line — the two-line variant with the rupee
 * figure of GST spelled out was tried and rejected, so don't split them again.
 */
@Composable
fun PayBar(total: Int, method: String, onPay: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    Row(
        modifier
            .fillMaxWidth()
            .background(Tokens.Palette.white)
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pay with", style = Tokens.Type.bodyXs, color = Tokens.Palette.gray600)
                ChevronGlyph(Tokens.Palette.gray600, Modifier.size(16.dp))
            }
            // The method is its MARK plus its name, never the name alone. At a glance down here
            // the logo is what the user actually recognises, and a bare "UPI" is not even the
            // same claim — UPI is the rail, PhonePe is the app the money leaves from.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_phonepe),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(method, style = Tokens.Type.bodySm, fontWeight = FontWeight.SemiBold, color = Tokens.Palette.gray800)
            }
        }
        Row(
            Modifier
                .width(200.dp)
                .scale(if (pressed) 0.98f else 1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Tokens.Palette.brand)
                .clickable(interactionSource = interaction, indication = null, onClick = onPay)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pay ₹$total",
                style = Tokens.Type.bodyMd,
                fontWeight = FontWeight.SemiBold,
                color = Tokens.Palette.white
            )
            Text(
                " (GST incl.)",
                style = Tokens.Type.bodyXs,
                color = Tokens.Palette.white.copy(alpha = 0.95f)
            )
        }
    }
}
