package com.astrochat.insufficient.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.astrochat.insufficient.ui.theme.Tokens
import kotlinx.coroutines.launch

/**
 * One amount in the SKU row — Figma component `4803:2363` (89 x 67, r14), plus the bonus badge
 * `4803:2372` hanging 11dp off its bottom edge.
 *
 * The badge is part of THIS composable rather than a sibling because its negative offset has to
 * be measured against the tile it belongs to; pulled out, every caller has to re-derive the
 * overlap and they drift.
 *
 * The badge figure is the wallet credit the amount actually earns, which under an applied coupon
 * is the standing bonus PLUS the flat ₹50 — callers pass [bonus] already resolved. Deriving it
 * here from the SKU table alone would reintroduce the original bug.
 */
@Composable
fun AmountTile(
    amount: Int,
    bonus: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBadgePlaced: (Rect) -> Unit = {},
    badgeHidden: Boolean = false,
    coins: Boolean = false,
    sweep: Boolean = false,
    sweepDelayMs: Int = 0
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "press")

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                // 1 : 0.81, which is what the prototype's `.box` actually sets. The Figma
                // component is 89 x 67 and this used to use that ratio, but 89/67 draws the tile
                // 7% SHORT of the running screen — at the stretched width of 90.7 it is 68.3
                // tall against the prototype's 73.4, and three tiles that size read as a
                // different row. Expressed as a ratio so the row can stretch on wider phones
                // without the tiles going off-spec relative to each other.
                //
                // It still clears the 96dp row: 73.4 tile + 26.5 badge - 11 overlap = 88.9.
                .aspectRatio(1f / 0.81f)
                .scale(press)
                .shadow(
                    elevation = if (selected) 6.dp else 4.dp,
                    shape = RoundedCornerShape(Tokens.Dimens.tileRadius)
                )
                .clip(RoundedCornerShape(Tokens.Dimens.tileRadius))
                .background(Tokens.Palette.white)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) Tokens.Palette.brand else Tokens.Palette.gray300,
                    shape = RoundedCornerShape(Tokens.Dimens.tileRadius)
                )
                // No ripple: the tile answers a tap with the scale above, and an ink ripple
                // on top of that reads as noise at this size.
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            // Under the figure, and only on the tile the prototype marks as the hero. It stops
            // dead once that tile is the selected one — the shower is an invitation, and leaving
            // it running after the tap makes the tile look like it is still asking to be picked.
            if (coins && !selected) {
                CoinShower(Modifier.fillMaxSize().padding(bottom = 11.dp))
            }

            // The rupee sign sits on the figure's cap height, not its baseline — hence the
            // fixed width and the taller line height on the small glyph.
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "₹",
                    fontSize = 12.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Tokens.Palette.gray700,
                    modifier = Modifier.width(7.dp)
                )
                Text(
                    text = amount.toString(),
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Tokens.Palette.gray700
                )
            }

            if (selected) {
                SelectedCorner(Modifier.align(Alignment.TopEnd))
            }
        }

        if (bonus > 0) {
            BonusBadge(
                bonus = bonus,
                sweep = sweep,
                sweepDelayMs = sweepDelayMs,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = -Tokens.Dimens.badgeOverlap.toPx()
                        // The badge is the thing in flight, so while the flyer is up the
                        // original has to be gone — two of them on screen at once is the
                        // giveaway that the flyer is a copy.
                        alpha = if (badgeHidden) 0f else 1f
                    }
                    .onGloballyPositioned { onBadgePlaced(it.boundsInRoot()) }
            )
        }
    }
}

/**
 * Coins raining down the hero tile — the shipped `AstroChat falling coins` dotLottie, the same
 * file and the same URL the web prototype plays (`COIN_LOTTIE` in its template).
 *
 * This used to be five ovals drawn on a Canvas, reverse-engineered from the prototype's CSS
 * *fallback* rather than from the animation itself. The real one is six coins at 192x154 over a
 * 4s loop at 60fps, and each coin is a precomp with a face, a round side and an edge — a spinning
 * disc with a rim, not a squashed ellipse. Nothing hand-drawn was going to converge on that.
 *
 * Bundled as an asset rather than streamed from lottie.host: the tile is what sells the bonus,
 * and on a bad connection a network fetch leaves it empty. There are no image assets inside, only
 * shapes, so the 4.9KB file is the whole animation.
 *
 * Crop, not Fit — `layout:{fit:'cover'}` in the prototype. The tile is 89x67 (1.33) against the
 * animation's 1.25, so Fit would letterbox it and the coins would fall short of the edges.
 */
@Composable
private fun CoinShower(modifier: Modifier = Modifier) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("coin.lottie"))
    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(Tokens.Dimens.tileRadius))
    )
}

/**
 * The orange corner wedge with its tick — `tile-selected.svg`.
 *
 * The wedge is NOT square. In the shipped asset it runs from (59.2, 0) to (92.66, 35) on a
 * 92.66 x 70 tile, so it is 36.1% of the tile wide and 50% tall, and those two fractions are
 * what set the hypotenuse's angle. Drawing it in a square box — which is what this did — tilts
 * that diagonal by about 8°, and the tile reads as having a sticker stuck on it rather than a
 * folded corner. Keep both fractions or neither.
 *
 * It is clipped to the tile's own top-right radius so the hypotenuse is the only straight edge
 * you see.
 *
 * The tick draws itself on over 350ms rather than fading, which is what makes the selection
 * read as confirmation rather than as a colour change.
 */
@Composable
private fun SelectedCorner(modifier: Modifier = Modifier) {
    // Both run from 0 on first composition, so the wedge slides in from the corner and the tick
    // writes itself on 80ms behind it. Recomposing a NEW corner on each selection is what makes
    // that replay — there is no visible/gone state to drive.
    //
    // Animatable, NOT animateFloatAsState. `animateFloatAsState(1f)` only animates when the
    // TARGET changes; on first composition it initialises AT the target, so both of these were
    // 1f on the frame the corner appeared and the whole entrance was silently skipped. The
    // selection just snapped on. That is the bug this comment used to describe as working.
    val wedge = remember { Animatable(0f) }
    val tick = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { wedge.animateTo(1f, tween(280, easing = Tokens.Motion.spring)) }
        tick.animateTo(1f, tween(350, delayMillis = 80, easing = Tokens.Motion.spring))
    }
    val wedgeIn = wedge.value
    val progress = tick.value
    Box(
        modifier
            .fillMaxWidth(WEDGE_WIDTH_FRACTION)
            .fillMaxHeight(WEDGE_HEIGHT_FRACTION)
            .graphicsLayer {
                alpha = wedgeIn
                translationX = (1f - wedgeIn) * 8.dp.toPx()
                translationY = (1f - wedgeIn) * -8.dp.toPx()
            }
            .clip(RoundedCornerShape(topEnd = Tokens.Dimens.tileRadius))
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            val wedge = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(wedge, Tokens.Palette.brand)
        }
        // Placed by offset from the wedge's own top-right, because the tick in the asset is not
        // centred in the wedge — it sits high and tight into the corner. Centring it drops it
        // onto the hypotenuse, where the white stroke runs off the orange.
        TickGlyph(
            tint = Tokens.Palette.white,
            progress = progress,
            modifier = Modifier
                .size(12.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-4.8).dp, y = 4.8.dp)
        )
    }
}

/** From `tile-selected.svg`: the wedge is 33.46 of 92.66 wide and 35 of 70 tall. */
private const val WEDGE_WIDTH_FRACTION = 0.361f
private const val WEDGE_HEIGHT_FRACTION = 0.5f

/**
 * Bonus badge — Figma "Bonus badge / Style=Green" (`4803:2372`).
 *
 * The figure is 12sp ExtraBold while "+₹" and "more" stay 10sp/600: the number carries the
 * hierarchy on its own, and the line height is pinned so the larger inline type cannot grow the
 * badge. The card's whole vertical rhythm below is measured off this badge's height.
 *
 * [sweep] is the prototype's `chipSweep` — a band of light crossing the pill. It is NOT always
 * on: the prototype gates it behind `.lure`, so the badges only advertise themselves while the
 * PICKED amount earns nothing. Once the user is on an amount that already pays a bonus the
 * badges have made their point, and leaving the light running turns the whole row into a
 * carousel. [sweepDelayMs] staggers it 380ms a tile so the three do not pulse in unison.
 */
@Composable
fun BonusBadge(
    bonus: Int,
    modifier: Modifier = Modifier,
    elevated: Boolean = false,
    sweep: Boolean = false,
    sweepDelayMs: Int = 0
) {
    val sweepClock = rememberInfiniteTransition(label = "chipSweep")
    val sweepT by sweepClock.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(Tokens.Motion.chipSweepMs, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "chipSweepClock"
    )
    Row(
        modifier
            .then(
                if (elevated) Modifier.shadow(10.dp, RoundedCornerShape(Tokens.Dimens.badgeRadius))
                else Modifier
            )
            .clip(RoundedCornerShape(Tokens.Dimens.badgeRadius))
            // White first, then the 85% green on top of it — the badge's gradient is translucent
            // and is specified against a white base, not against whatever it happens to overlap.
            .background(Tokens.Palette.white)
            .background(Tokens.BandBrush.badgeGreen)
            // After the clip, so the band is cut off at the pill's rounded edge instead of
            // sliding out past it. The CSS equivalent is the `overflow:hidden` on `.chip`.
            .drawWithContent {
                drawContent()
                if (!sweep) return@drawWithContent
                // Light crosses in the first 42% of the cycle and then waits offscreen. The
                // pause is the point: a band that crosses continuously reads as a barber pole.
                val phase = (sweepT + sweepDelayMs / Tokens.Motion.chipSweepMs.toFloat()) % 1f
                val run = (phase / 0.42f).coerceAtMost(1f)
                val band = size.width * 0.38f
                drawSweepBar(
                    left = band * (-1.2f + 4.2f * run),
                    width = band,
                    stops = Tokens.BandBrush.chipSweepStops
                )
            }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("+₹", fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = Tokens.Palette.white)
        Text("$bonus", fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, color = Tokens.Palette.white)
        Text(" more", fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = Tokens.Palette.white)
    }
}

/**
 * The badge in flight — the screen's signature move.
 *
 * Picking an amount lifts its "+₹150 more" pill off the tile, arcs it down and drops it into the
 * congratulations card, and the card starts counting the bonus on at the moment it lands. This
 * is what connects the two halves of the screen: without it the card's figure just changes and
 * nothing tells the user that the number came from the tile they tapped.
 *
 * It is rendered by the screen at ROOT level, not inside the card, because it has to travel
 * across both — anything parented lower gets clipped by the receipt card on the way down.
 *
 * [progress] runs 0..1 over the flight, and the path is the prototype's `flyBonus` keyframes
 * rather than a curve of my own: the velocity profile IS the brief, and it is fast, slow, fast.
 * The chip leaves the tile hard and decelerates into an apex 76 above it; hangs there through
 * the direction change (8 of travel over 12% of the flight); then falls away from the apex
 * ACCELERATING the whole way, fastest on impact, landing on the same frame the count starts.
 *
 * The obvious version — a sine lift over an ease-OUT fall — gets the shape roughly right and the
 * speed exactly backwards: it arrives slowly and stops, which reads as the pill being set down
 * instead of caught. That is the difference between this looking like the prototype and not.
 *
 * The fall is ONE interval. Every extra waypoint would restart its own easing from zero velocity
 * and put a visible hitch just before the card, which is why the 86% keyframe carries opacity
 * alone — opacity is interpolated separately and so does not split the transform.
 */
@Composable
fun BonusFlyer(bonus: Int, from: Rect, to: Offset, progress: Float, modifier: Modifier = Modifier) {
    val p = progress.coerceIn(0f, 1f)
    // Centre-to-centre, measured at call time, exactly as the prototype measures it.
    val dx = to.x - (from.left + from.width / 2f)
    val dy = to.y - (from.top + from.height / 2f)

    // Which interval we are in, and how far through it after that interval's own easing.
    val (leg, q) = when {
        p < 0.22f -> 0 to ARC_LAUNCH.transform(p / 0.22f)
        p < 0.34f -> 1 to ARC_HANG.transform((p - 0.22f) / 0.12f)
        else -> 2 to ARC_FALL.transform((p - 0.34f) / 0.66f)
    }
    fun lerp(a: Float, b: Float) = a + (b - a) * q

    Box(
        modifier.graphicsLayer {
            val apex = -76.dp.toPx()
            val hang = -68.dp.toPx()
            val tx: Float
            val ty: Float
            val s: Float
            val rot: Float
            when (leg) {
                0 -> { tx = lerp(0f, dx * 0.03f); ty = lerp(0f, apex); s = lerp(1f, 0.92f); rot = lerp(0f, -9f) }
                1 -> { tx = lerp(dx * 0.03f, dx * 0.12f); ty = lerp(apex, hang); s = lerp(0.92f, 0.86f); rot = lerp(-9f, -3f) }
                else -> { tx = lerp(dx * 0.12f, dx); ty = lerp(hang, dy); s = lerp(0.86f, 0.34f); rot = lerp(-3f, 5f) }
            }
            translationX = from.left + tx
            translationY = from.top + ty
            scaleX = s; scaleY = s
            rotationZ = rot
            // Scale and rotation pivot on the chip's middle, as CSS does; the translation above
            // still places its top-left, so the two are independent.
            transformOrigin = TransformOrigin.Center
            // Fades UP off the tile so the chip does not double the badge still sitting there,
            // then holds solid to 86% and goes out on the last 14% as the card takes it.
            alpha = when {
                p < 0.22f -> ARC_LAUNCH.transform(p / 0.22f)
                p < 0.86f -> 1f
                else -> 1f - (p - 0.86f) / 0.14f
            }
        }
    ) {
        BonusBadge(bonus = bonus, elevated = true)
    }
}

/** Off the tile: nearly all the speed in the first third, decelerating into the apex. */
private val ARC_LAUNCH = CubicBezierEasing(0.12f, 0.8f, 0.25f, 1f)

/** The direction change. Symmetric and slow — this is the hang. */
private val ARC_HANG = CubicBezierEasing(0.45f, 0.0f, 0.55f, 1f)

/**
 * The fall. Kept near free fall (distance proportional to t squared): it covers 8/30/62/85% of
 * the drop at quarter/half/three-quarter/nine-tenths time, against free fall's 6/25/56/81.
 * A steeper ease-in reads as hovering and then teleporting.
 */
private val ARC_FALL = CubicBezierEasing(0.4f, 0.0f, 0.8f, 0.7f)
