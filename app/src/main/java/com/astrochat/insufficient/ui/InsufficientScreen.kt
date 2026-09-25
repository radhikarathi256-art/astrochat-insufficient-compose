package com.astrochat.insufficient.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astrochat.insufficient.data.BandType
import com.astrochat.insufficient.data.Pricing
import com.astrochat.insufficient.data.bandCopyFor
import com.astrochat.insufficient.ui.components.AlertBand
import com.astrochat.insufficient.ui.components.AmountTile
import com.astrochat.insufficient.ui.components.BackGlyph
import com.astrochat.insufficient.ui.components.BonusFlyer
import com.astrochat.insufficient.ui.components.CardArc
import com.astrochat.insufficient.ui.components.CongratsCard
import com.astrochat.insufficient.ui.components.OfferPopup
import com.astrochat.insufficient.ui.components.PayBar
import com.astrochat.insufficient.ui.components.PaymentSummaryRow
import com.astrochat.insufficient.ui.components.ReceiptCard
import com.astrochat.insufficient.ui.components.SeeMoreOptions
import com.astrochat.insufficient.ui.components.WalletGlyph
import com.astrochat.insufficient.ui.theme.Tokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val ASTROLOGER = "Astro Hemali"

/** The badge currently in the air, and how far along its flight it is. */
private data class Flight(val bonus: Int, val from: Rect, val progress: Float)

/**
 * Counts a figure from one value to another, easing out.
 *
 * The card COUNTS rather than cutting because the number is the reward — a value that lands
 * instantly is read as a label, and one that climbs is read as something being earned. Same
 * reason the prototype does it.
 */
private suspend fun countTo(from: Int, to: Int, durationMs: Int, set: (Int) -> Unit) {
    if (from == to) { set(to); return }
    animate(
        initialValue = from.toFloat(),
        targetValue = to.toFloat(),
        animationSpec = tween(durationMs, easing = Tokens.Motion.easeOut)
    ) { v, _ -> set(v.roundToInt()) }
    set(to)
}

/**
 * Reads a 0..1 run back as a multi-stage curve, which is what a CSS `@keyframes` block is and
 * what Compose has no direct equivalent of. Stops are (position, value) and it walks between
 * them linearly; the easing that makes it feel right lives on the tween driving [p].
 */
private fun stages(p: Float, vararg stops: Pair<Float, Float>): Float {
    for (i in 0 until stops.size - 1) {
        val (p0, v0) = stops[i]
        val (p1, v1) = stops[i + 1]
        if (p <= p1) return v0 + (v1 - v0) * ((p - p0) / (p1 - p0)).coerceIn(0f, 1f)
    }
    return stops.last().second
}

/** `cardTilt`: over in Z at 30%, a smaller counter-tip at 62%, flat by the end. */
private fun tiltCurve(p: Float) = stages(p, 0f to 0f, 0.30f to 1f, 0.62f to -0.39f, 1f to 0f)

/** `absorb`: the figure takes the hit, overshoots under, settles. */
private fun absorbCurve(p: Float) = stages(p, 0f to 0f, 0.24f to 0.14f, 0.58f to -0.025f, 1f to 0f)

/**
 * The insufficient-balance Add Money screen.
 *
 * State lives here rather than in a ViewModel on purpose: this module exists so a developer can
 * lift the components and the token file into the real app, and a ViewModel would just be one
 * more thing to unpick. Everything below the `remember`s is presentation.
 *
 * The selected amount drives the band, the tiles, the card figure and the pay total together —
 * there is no second source of truth for any of them. That is the whole point of [Pricing].
 */
@Composable
fun InsufficientScreen() {
    var selected by remember { mutableIntStateOf(50) }
    var expanded by remember { mutableStateOf(false) }
    var summaryOpen by remember { mutableStateOf(false) }
    var popupVisible by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableIntStateOf(14 * 60 + 59) }

    // The offer countdown. It runs whatever band is showing, because tapping ₹100 has to find
    // the clock already where it would have been rather than restarting it.
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
        }
    }

    // The popup shows on every load and then gets out of the way on its own.
    LaunchedEffect(Unit) {
        delay(Tokens.Motion.popupHoldMs.toLong())
        popupVisible = false
    }

    val band = Pricing.bandFor(selected)
    val copy = bandCopyFor(selected, ASTROLOGER, secondsLeft)
    val credit = Pricing.creditFor(selected, couponApplied = band == BandType.COUPON)
    val gst = Pricing.gstFor(selected)
    // Gold whenever the amount earns nothing. The COUPON counts towards this even though it is
    // itemised separately from the bonus, so a ₹50 under ASTRO50 stays green — it is credit the
    // user gained, which is the only thing the card is about.
    val goldCard = credit == selected

    // ---- the pick choreography ----
    // Where each tile's badge is sitting, and where the card's figure is, both in ROOT
    // coordinates. Measured rather than assumed: the tiles move when the row expands and the
    // card moves when the summary opens, so any hardcoded path would be wrong half the time.
    val badgeBounds = remember { mutableStateMapOf<Int, Rect>() }
    var figureAt by remember { mutableStateOf(Offset.Zero) }

    var flight by remember { mutableStateOf<Flight?>(null) }
    // Counted separately from `credit` so the card can show a figure mid-count.
    var shown by remember { mutableIntStateOf(credit) }
    // Both run 0..1 and are read back through a curve, rather than being the tilt itself. A
    // keyframed Animatable whose start and end are both 0 is liable to be optimised away.
    val tiltRun = remember { Animatable(1f) }
    val absorbRun = remember { Animatable(1f) }
    // Each amount's chip drops ONCE per visit. Pick ₹100 and it flies; come back to ₹100 later
    // and only the figure refreshes. A chip that replays on every tap stops reading as a reward.
    val dropped = remember { mutableSetOf<Int>() }

    LaunchedEffect(selected) {
        val bonus = credit - selected
        val from = badgeBounds[selected]

        launch {
            tiltRun.snapTo(0f)
            tiltRun.animateTo(1f, tween(620, easing = Tokens.Motion.easeOut))
        }

        if (bonus <= 0 || from == null || figureAt == Offset.Zero || !dropped.add(selected)) {
            countTo(shown, credit, 360) { shown = it }
            return@LaunchedEffect
        }

        // 1. count up to the recharge amount, 2. throw the chip, 3. count the bonus on as it
        // lands. Sequential on purpose: the bonus must not start moving before the thing that
        // earns it has arrived.
        countTo(shown, selected, 360) { shown = it }

        flight = Flight(bonus = bonus, from = from, progress = 0f)
        animate(0f, 1f, animationSpec = tween(640, easing = LinearEasing)) { p, _ ->
            flight = flight?.copy(progress = p)
        }
        flight = null

        launch {
            absorbRun.snapTo(0f)
            absorbRun.animateTo(1f, tween(520, easing = LinearEasing))
        }
        countTo(selected, credit, 700) { shown = it }
    }

    // Anything that changes the figure WITHOUT a pick — the countdown flipping the band onto a
    // coupon, say — still has to keep the card honest.
    LaunchedEffect(credit) { if (flight == null) shown = credit }

    Box(
        Modifier
            .fillMaxSize()
            .background(Tokens.Palette.appBackground)
    ) {
        Column(Modifier.fillMaxSize()) {
            TopNav()

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(16.dp))

                // Band and card are one unit: the band's bottom 42dp is UNDER the card, which
                // is why the card is pulled back up by exactly that much instead of the two
                // being spaced apart.
                Box(Modifier.fillMaxWidth()) {
                    Column {
                        // Cross-faded, not swapped. The six variants are different colours AND
                        // different sentences, so a cut makes the strip flicker on every tile
                        // tap; over 420ms the same strip reads as changing its mind.
                        //
                        // Keyed on the TYPE alone, so the countdown ticking inside the Offer
                        // band updates in place instead of restarting the fade every second.
                        AnimatedContent(
                            targetState = band,
                            transitionSpec = {
                                fadeIn(tween(Tokens.Motion.bandSwapMs)) togetherWith
                                    fadeOut(tween(Tokens.Motion.bandSwapMs))
                            },
                            label = "band"
                        ) { type ->
                            AlertBand(
                                type = type,
                                copy = copy,
                                modifier = Modifier.padding(horizontal = Tokens.Dimens.gutter)
                            )
                        }
                        // Every extra SKU row is worth exactly 96dp, and it moves the button,
                        // the notch and the card's own height by that same 96 — which is why
                        // one row and four rows can share these three expressions.
                        //
                        // Animated, and deliberately ASYMMETRIC: opening is the house ease-out
                        // at 360ms, closing is the gentler curve over 520. Opening answers a
                        // tap and should feel immediate; closing is the sheet settling, and at
                        // the same speed it reads as the rows being snatched away.
                        val extra by animateDpAsState(
                            targetValue = if (expanded) Tokens.Dimens.skuRowHeight * 3f else 0.dp,
                            animationSpec = tween(
                                durationMillis = if (expanded) Tokens.Motion.expandMs else Tokens.Motion.collapseMs,
                                easing = if (expanded) Tokens.Motion.easeOut else Tokens.Motion.easeGentle
                            ),
                            label = "skuRows"
                        )

                        ReceiptCard(
                            modifier = Modifier
                                .padding(horizontal = Tokens.Dimens.gutter)
                                .graphicsLayer {
                                    translationY = -Tokens.Dimens.bandHiddenBehindCard.toPx()
                                }
                                .fillMaxWidth()
                                .height(Tokens.Dimens.receiptHeight + extra),
                            notchFromTop = Tokens.Dimens.notchTop + extra
                        ) {
                            // Offsets, not a Column: the button's distance from the card top is
                            // the measurement the design is specified in, and a flow layout
                            // re-derives it from the tiles' real height every time the type or
                            // the badge changes. It drifted once already.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    // The tile block is inset a further 16 inside the card.
                                    .padding(horizontal = Tokens.Dimens.gutter)
                                    .padding(top = Tokens.Dimens.skuRowTop)
                            ) {
                                TileBlock(
                                    expanded = expanded,
                                    // The extra rows are CLIPPED into view rather than faded:
                                    // they are already laid out at their final positions, so
                                    // the ones above never move while the row opens.
                                    revealHeight = Tokens.Dimens.skuRowHeight + extra,
                                    selected = selected,
                                    couponApplied = band == BandType.COUPON,
                                    onSelect = { selected = it },
                                    onBadgePlaced = { amount, r -> badgeBounds[amount] = r },
                                    flyingAmount = if (flight != null) selected else null
                                )
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = Tokens.Dimens.seeButtonTop + extra),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                SeeMoreOptions(
                                    expanded = expanded,
                                    onToggle = { expanded = !expanded }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }

            // ---- pay block: pinned, never scrolls ----
            Column(Modifier.background(Tokens.Palette.white)) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CongratsCard(
                            credit = shown,
                            gold = goldCard,
                            tilt = tiltCurve(tiltRun.value),
                            absorb = absorbCurve(absorbRun.value),
                            onFigurePlaced = { figureAt = it },
                            // Pulled into the arc below so the arc IS its bottom edge.
                            modifier = Modifier.graphicsLayer { translationY = 22.dp.toPx() }
                        )
                        CardArc(goldCard, Modifier.graphicsLayer { translationY = 22.dp.toPx() })
                    }
                }
                Spacer(Modifier.height(22.dp))
                PaymentSummaryRow(
                    amount = selected,
                    gst = gst,
                    expanded = summaryOpen,
                    onToggle = { summaryOpen = !summaryOpen }
                )
                SummaryDetail(
                    visible = summaryOpen,
                    amount = selected,
                    bonus = Pricing.bonusFor(selected),
                    coupon = if (band == BandType.COUPON) Pricing.COUPON_FLAT else 0,
                    gst = gst
                )
                PayBar(total = selected + gst, method = "PhonePe", onPay = {})
            }
        }

        // Drawn LAST and at root level so it passes over the receipt card and the pay block
        // instead of being clipped by either on the way down.
        flight?.let {
            BonusFlyer(bonus = it.bonus, from = it.from, to = figureAt, progress = it.progress)
        }

        OfferPopup(
            visible = popupVisible,
            title = if (band == BandType.COUPON) "Coupon code Applied !" else "First recharge offer !",
            body = if (band == BandType.COUPON) {
                "You'll get ₹50 extra on your first wallet recharge"
            } else {
                "You'll get a bonus on your first recharge over ₹100"
            },
            onDismiss = { popupVisible = false }
        )
    }
}

@Composable
private fun TopNav() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Tokens.Palette.white)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back arrow and title are ONE control — the whole "← Add Money" group is the back
        // target, which is why they sit in a row at a 2dp gap rather than the title being
        // centred with a separate icon button.
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                BackGlyph(Tokens.Palette.gray700, Modifier.size(18.dp))
            }
            Text(
                "Add Money",
                style = Tokens.Type.bodyLg,
                fontWeight = FontWeight.SemiBold,
                color = Tokens.Palette.gray700
            )
        }
        Row(
            Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Tokens.Palette.gray100)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WalletGlyph(Tokens.Palette.gray600, Modifier.size(20.dp))
            Text("₹0", style = Tokens.Type.bodyXs, fontWeight = FontWeight.SemiBold, color = Tokens.Palette.gray600)
        }
    }
}

/**
 * Collapsed the row is three tiles; expanded it is all ten in a 3-wide grid.
 *
 * The collapsed set is NOT simply the first three. If the user has picked something bigger,
 * ₹50 is dropped off the front and the picked amount is pushed onto the end, so whatever is
 * selected is always visible without opening the sheet.
 */
@Composable
private fun TileBlock(
    expanded: Boolean,
    revealHeight: Dp,
    selected: Int,
    couponApplied: Boolean,
    onSelect: (Int) -> Unit,
    onBadgePlaced: (Int, Rect) -> Unit,
    flyingAmount: Int?
) {
    // The full grid is laid out the whole time the row is any way open, so nothing re-flows
    // mid-animation; `revealHeight` is the window onto it. Only once it is fully shut does it
    // fall back to the three-tile set.
    val open = expanded || revealHeight > Tokens.Dimens.skuRowHeight
    val shown = if (open) {
        Pricing.skus
    } else {
        val head = Pricing.collapsedSkus
        if (head.any { it.amount == selected }) head
        else head.drop(1) + Pricing.skus.first { it.amount == selected }
    }

    Column(Modifier.height(revealHeight).clipToBounds()) {
        shown.chunked(3).forEach { row ->
            Row(
                // Pinned to 96 so row 1 lands pixel-identical whether there is one row or four,
                // and every row below is a clean +96 on the card, the notch and the button.
                Modifier
                    .fillMaxWidth()
                    .height(Tokens.Dimens.skuRowHeight),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Dimens.tileGap),
                verticalAlignment = Alignment.Top
            ) {
                row.forEach { sku ->
                    AmountTile(
                        amount = sku.amount,
                        // Under an applied coupon the badge must show what the wallet is
                        // ACTUALLY credited — the standing bonus plus the flat ₹50.
                        bonus = sku.bonus + if (couponApplied && sku.bonus > 0) Pricing.COUPON_FLAT else 0,
                        selected = sku.amount == selected,
                        onClick = { onSelect(sku.amount) },
                        onBadgePlaced = { onBadgePlaced(sku.amount, it) },
                        badgeHidden = sku.amount == flyingAmount,
                        // Tile + badge is ~99 tall against a 96 slot, and the badge's last 3dp
                        // is exactly what the negative overlap is meant to eat. Without
                        // unbounded height the row clamps the column and squashes the badge
                        // into a bar with no text in it.
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(Alignment.Top, unbounded = true)
                    )
                }
                // Keeps a short last row aligned left instead of stretching its tiles.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * The expanded summary. It itemises the coupon SEPARATELY from the bonus — folding the two
 * together is the bug this screen was rebuilt to avoid.
 */
@Composable
private fun SummaryDetail(visible: Boolean, amount: Int, bonus: Int, coupon: Int, gst: Int) {
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            fadeIn(tween(Tokens.Motion.fadeMs)) togetherWith fadeOut(tween(Tokens.Motion.fadeMs))
        },
        label = "summary"
    ) { open ->
        if (!open) {
            Spacer(Modifier.height(0.dp))
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SummaryLine("Recharge amount", "₹$amount")
                if (bonus > 0) SummaryLine("Bonus credit", "+₹$bonus")
                if (coupon > 0) SummaryLine("ASTRO 50 coupon", "+₹$coupon")
                SummaryLine("GST (18%)", "₹$gst")
                SummaryLine("Wallet credit", "₹${amount + bonus + coupon}", strong = true)
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, strong: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            style = Tokens.Type.bodyXs,
            color = if (strong) Tokens.Palette.gray800 else Tokens.Palette.gray500,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            value,
            style = Tokens.Type.bodyXs,
            color = if (strong) Tokens.Palette.gray800 else Tokens.Palette.gray600,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
