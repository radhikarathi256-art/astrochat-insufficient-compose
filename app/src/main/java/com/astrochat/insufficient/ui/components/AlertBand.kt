package com.astrochat.insufficient.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrochat.insufficient.data.BandCopy
import com.astrochat.insufficient.data.BandType
import com.astrochat.insufficient.ui.theme.Tokens

/**
 * Alert band — Figma component set `4804:2417`, variants Bonus / Bonus dark / Not enough /
 * Offer / Coupon / Missing out.
 *
 * Two things about this component are load-bearing and look like mistakes if you don't know:
 *
 *  1. Its bottom 42dp is deliberately hidden behind the receipt card. The band is 89dp tall but
 *     only ~47dp of it is ever visible; the rest is what makes it read as rising out from
 *     behind the card. Do not "fix" the extra bottom padding.
 *  2. Only the top corners are rounded (32dp), because the card sits on the bottom ones.
 */
@Composable
fun AlertBand(
    type: BandType,
    copy: BandCopy,
    modifier: Modifier = Modifier,
    animateTag: Boolean = true
) {
    val brush = when (type) {
        BandType.BONUS -> Tokens.BandBrush.bonus
        BandType.BONUS_DARK -> Tokens.BandBrush.bonusDark
        BandType.NOT_ENOUGH -> Tokens.BandBrush.notEnough
        BandType.OFFER -> Tokens.BandBrush.offer
        BandType.COUPON -> Tokens.BandBrush.coupon
        BandType.MISSING_OUT -> Tokens.BandBrush.missingOut
    }

    // Line colours are per-variant in Figma, not a single "on-surface" token.
    val (line1Colour, line2Colour) = when (type) {
        BandType.NOT_ENOUGH -> Tokens.Palette.error800 to Tokens.Palette.error800
        BandType.OFFER, BandType.COUPON -> Tokens.Palette.white to Tokens.Palette.white
        BandType.MISSING_OUT -> Tokens.Palette.warning800 to Tokens.Palette.warning700
        BandType.BONUS, BandType.BONUS_DARK -> Tokens.Palette.success800 to Tokens.Palette.success700
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(Tokens.Dimens.bandHeight)
            .clip(RoundedCornerShape(topStart = Tokens.Dimens.cardRadius, topEnd = Tokens.Dimens.cardRadius))
            .background(brush)
    ) {
        Row(
            Modifier.padding(
                start = 16.dp, end = 16.dp, top = 12.dp,
                bottom = Tokens.Dimens.bandHiddenBehindCard
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BandIcon(type = type, animate = animateTag)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Text(
                        text = copy.line1,
                        style = Tokens.Type.bodyXs,
                        fontWeight = FontWeight.Medium,
                        color = line1Colour
                    )
                    if (copy.chip != null) {
                        Spacer(Modifier.width(5.dp))
                        CountdownChip(copy.chip)
                    }
                }
                Spacer(Modifier.height(1.dp))
                androidx.compose.material3.Text(
                    text = copy.line2,
                    style = Tokens.Type.bodyXs,
                    fontWeight = FontWeight.Medium,
                    color = line2Colour
                )
            }
        }
    }
}

/**
 * White pill on the deepest green in the set — the strongest contrast the band can offer, which
 * is the whole reason the timer is in a chip. It flows inline with line 1 rather than being
 * pinned to the band's right edge: anchored right it read as a detached clock next to a sentence
 * fragment, and it had to clear the band's fade-to-white tail.
 */
@Composable
private fun CountdownChip(text: String) {
    Box(
        Modifier
            .shadow(3.dp, RoundedCornerShape(999.dp), ambientColor = Tokens.Palette.success900)
            .clip(RoundedCornerShape(999.dp))
            .background(Tokens.Palette.white)
            .padding(horizontal = 8.dp, vertical = 1.dp)
    ) {
        androidx.compose.material3.Text(
            text = text,
            style = Tokens.Type.bodyXs,
            fontWeight = FontWeight.Bold,
            color = Tokens.Palette.success800
        )
    }
}

/**
 * The band's leading glyph, drawn as a real vector rather than shipped as a PNG so it takes the
 * band's colour and scales cleanly. It wiggles slowly — 3.4s on the calm bands, faster on the
 * promo ones, matching the prototype's `tagWiggle`.
 */
@Composable
private fun BandIcon(type: BandType, animate: Boolean) {
    val periodMs = if (type == BandType.OFFER || type == BandType.COUPON) 1500 else 3400
    val transition = rememberInfiniteTransition(label = "tag")
    val angle by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs / 2, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tagAngle"
    )

    val tint = when (type) {
        BandType.NOT_ENOUGH -> Tokens.Palette.error700
        BandType.OFFER, BandType.COUPON -> Tokens.Palette.white
        BandType.MISSING_OUT -> Tokens.Palette.warning600
        else -> Tokens.Palette.success600
    }

    Box(
        Modifier
            .size(26.dp)
            .rotate(if (animate) angle else 0f),
        contentAlignment = Alignment.Center
    ) {
        when (type) {
            BandType.NOT_ENOUGH -> WalletGlyph(tint)
            else -> TagGlyph(tint, Color.White.copy(alpha = 0.92f))
        }
    }
}
