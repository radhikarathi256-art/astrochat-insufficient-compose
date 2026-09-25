package com.astrochat.insufficient.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrochat.insufficient.ui.theme.Tokens

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
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "press")

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                // 89 x 67 expressed as a ratio so the row can stretch on wider phones without
                // the tiles going off-spec relative to each other.
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
                modifier = Modifier
                    .padding(top = 0.dp)
                    .graphicsLayer { translationY = -Tokens.Dimens.badgeOverlap.toPx() }
            )
        }
    }
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
    // Both start at 0 on first composition, so the wedge slides in from the corner and the tick
    // writes itself on 80ms behind it. Recomposing a NEW corner on each selection is what makes
    // that replay — there is no visible/gone state to drive.
    val wedgeIn by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(280, easing = Tokens.Motion.spring),
        label = "wedge"
    )
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(350, delayMillis = 80, easing = Tokens.Motion.spring),
        label = "tick"
    )
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
 */
@Composable
fun BonusBadge(bonus: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(Tokens.Dimens.badgeRadius))
            // White first, then the 75% green on top of it — the badge's gradient is translucent
            // and is specified against a white base, not against whatever it happens to overlap.
            .background(Tokens.Palette.white)
            .background(Tokens.BandBrush.badgeGreen)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("+₹", fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = Tokens.Palette.white)
        Text("$bonus", fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold, color = Tokens.Palette.white)
        Text(" more", fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold, color = Tokens.Palette.white)
    }
}
