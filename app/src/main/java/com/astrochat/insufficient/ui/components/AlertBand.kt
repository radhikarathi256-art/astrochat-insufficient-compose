package com.astrochat.insufficient.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.astrochat.insufficient.R
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

    // Line colours are per-variant in Figma, not a single "on-surface" token. Both lines of a
    // band share one ink — measured off the prototype, which sets .l1 and .l2 to the same value.
    val ink = when (type) {
        BandType.NOT_ENOUGH -> Tokens.Palette.error800
        BandType.OFFER, BandType.COUPON -> Tokens.Palette.white
        BandType.MISSING_OUT -> Tokens.Palette.warning800
        BandType.BONUS, BandType.BONUS_DARK -> Tokens.Palette.success800
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(Tokens.Dimens.bandHeight)
            .clip(RoundedCornerShape(topStart = Tokens.Dimens.cardRadius, topEnd = Tokens.Dimens.cardRadius))
            // White first. The Bonus and Missing out fills are translucent washes of one hue —
            // that is how the design specifies them — so without a base they would composite
            // against the app's grey chrome and come out muddy instead of tinting white.
            .background(Tokens.Palette.white)
            .background(brush)
            .bandSweep(enabled = type == BandType.COUPON)
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
                        text = emphasise(copy.line1),
                        style = Tokens.Type.bodyXs,
                        fontWeight = FontWeight.Medium,
                        color = ink
                    )
                    if (copy.chip != null) {
                        Spacer(Modifier.width(5.dp))
                        CountdownChip(copy.chip)
                    }
                }
                Spacer(Modifier.height(1.dp))
                androidx.compose.material3.Text(
                    text = emphasise(copy.line2),
                    style = Tokens.Type.bodyXs,
                    fontWeight = FontWeight.Medium,
                    color = ink
                )
            }
        }
    }
}

/**
 * The coupon band's light sweep — the prototype's `.sweep`, which until now had no Compose
 * equivalent at all. On the ASTRO50 band it is the only thing moving apart from the seal, and
 * the seal holds dead still for two seconds and then snaps: with nothing else running, that snap
 * is all you see, and what it reads as is a blink rather than a band that is alive.
 *
 * Coupon ONLY, which is the prototype's own rule (`.am .coupon .sweep`) — the Offer band has a
 * running clock in it and deliberately goes without, because a light crossing a countdown reads
 * as the countdown flickering.
 *
 * Geometry off the CSS: a bar 40% of the band wide starting at -60%, travelling 190% of the
 * band's width, over the top 62dp only — below that the band is behind the receipt card and a
 * sweep down there is light with nothing to light.
 *
 * Timing: 2.8s, still for the first 40%, crossing over the next 40%, still again to the end.
 * The crossing is LINEAR, and that is a deliberate departure from the CSS's `ease-in-out`. The
 * run is 1.9x the width you can actually see, so under ease-in-out the entire visible crossing
 * falls inside the fastest part of the curve — about 0.4s of a 2.8s cycle — and the sweep
 * flashes instead of travelling. Constant speed puts 0.6s of visible movement on screen and is
 * what the badge sweep next to it already does.
 */
private fun Modifier.bandSweep(enabled: Boolean): Modifier = composed {
    if (!enabled) return@composed this
    val clock = rememberInfiniteTransition(label = "bandSweep")
    val t by clock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(Tokens.Motion.bandSweepMs, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "bandSweepClock"
    )
    drawWithContent {
        drawContent()
        val run = (((t - 0.40f) / 0.40f)).coerceIn(0f, 1f)
        drawSweepBar(
            left = size.width * (-0.60f + 1.90f * run),
            width = size.width * 0.40f,
            stops = Tokens.BandBrush.bandSweepStops,
            height = SWEEP_VISIBLE_HEIGHT.toPx()
        )
    }
}

/** `.am .coupon .sweep{height:62px}` — the part of the band the card does not cover. */
private val SWEEP_VISIBLE_HEIGHT = 62.dp

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
 * Turns the copy table's `*…*` runs into bold spans.
 *
 * The band's whole job is to put a number in front of someone in one glance, and the number is
 * what is bold. Setting both lines at one weight — which is what this screen did before — loses
 * that, and the band stops being scannable even though every word is right.
 */
private fun emphasise(marked: String): AnnotatedString = buildAnnotatedString {
    marked.split('*').forEachIndexed { i, part ->
        if (i % 2 == 1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
        } else {
            append(part)
        }
    }
}

/**
 * The band's leading glyph. These are the prototype's own SVGs converted into res/drawable, NOT
 * redrawn: each already carries its white disc and its own ink, so there is no tint here.
 *
 * Which glyph is a copy decision, not a colour one. The two alert bands take the BELL, because
 * they are interrupting; every other band takes the discount seal, because it is offering
 * something. The Not enough band used to draw a wallet, and a wallet reads as "top up" — the
 * neutral message — exactly where the screen needs to say the chat cannot start.
 *
 * It wiggles on the prototype's own `tagWiggle` timing, which is NOT a metronome: the seal holds
 * dead still for the first 62% of a 3.4s cycle and then snaps through -13 / +10 / -7 / +4 / -2
 * degrees, scaling up 8% on the two big swings. This used to rock smoothly between -6 and +6 for
 * the whole cycle, which at 26dp is close to invisible — a constant slow drift reads as a
 * rendering artefact, and the stillness is what makes the shake land when it comes.
 */
@Composable
private fun BandIcon(type: BandType, animate: Boolean) {
    val periodMs = if (type == BandType.OFFER || type == BandType.COUPON) 1500 else 3400
    val transition = rememberInfiniteTransition(label = "tag")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "tagClock"
    )
    val angle = stageValue(t, TAG_ANGLE)
    val bump = stageValue(t, TAG_SCALE)

    // A band's seal follows its green. Offer is the only one on the deep ramp, so it is the only
    // one that takes the #065F41 rosette; the plain #039855 one would disappear into that fill.
    val asset = when (type) {
        BandType.NOT_ENOUGH -> R.drawable.ic_band_alert_red
        BandType.MISSING_OUT -> R.drawable.ic_band_alert_amber
        BandType.OFFER -> R.drawable.ic_band_bonus_dark
        else -> R.drawable.ic_band_bonus
    }

    Image(
        painter = painterResource(asset),
        contentDescription = null,
        modifier = Modifier
            .size(26.dp)
            .graphicsLayer {
                rotationZ = if (animate) angle else 0f
                val s = if (animate) bump else 1f
                scaleX = s
                scaleY = s
            }
    )
}

/** `@keyframes tagWiggle` — stop position 0..1 to degrees. */
private val TAG_ANGLE = listOf(
    0f to 0f, 0.62f to 0f, 0.68f to -13f, 0.74f to 10f,
    0.80f to -7f, 0.86f to 4f, 0.92f to -2f, 1f to 0f
)

/** The same cycle's scale track — only the first two swings grow. */
private val TAG_SCALE = listOf(
    0f to 1f, 0.62f to 1f, 0.68f to 1.08f, 0.74f to 1.08f,
    0.80f to 1.04f, 0.86f to 1f, 1f to 1f
)

/**
 * Piecewise-linear read of a CSS keyframe track. Compose has no `@keyframes`, and the usual
 * workaround — one `animateFloat` per segment — cannot express "hold, then shake" without a
 * chain of delays that drifts out of phase with itself.
 */
internal fun stageValue(t: Float, stops: List<Pair<Float, Float>>): Float {
    val p = t.coerceIn(0f, 1f)
    for (i in 0 until stops.size - 1) {
        val (t0, v0) = stops[i]
        val (t1, v1) = stops[i + 1]
        if (p <= t1) {
            if (t1 == t0) return v1
            return v0 + (v1 - v0) * ((p - t0) / (t1 - t0))
        }
    }
    return stops.last().second
}
