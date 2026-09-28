package com.astrochat.insufficient.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.astrochat.insufficient.data.BandType
import com.astrochat.insufficient.data.Pricing
import com.astrochat.insufficient.data.bandCopyFor
import com.astrochat.insufficient.ui.components.AlertBand
import com.astrochat.insufficient.ui.components.AmountTile
import com.astrochat.insufficient.ui.components.BackGlyph
import com.astrochat.insufficient.ui.components.BonusFlyer
import com.astrochat.insufficient.ui.components.CardArc
import com.astrochat.insufficient.ui.components.CloseGlyph
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

    // ---- where the page sits when the SKU grid opens ----
    // The extra rows grow the card DOWNWARDS, behind the pinned pay block, so on open the page has
    // to come up to meet them or "See Less Options" is off-screen. The prototype lands at the
    // BOTTOM of the scroller (setOpen -> scrollTop = scrollHeight - clientHeight) — her call — and
    // it does it as a plain assignment after a 400ms wait, not a smooth scroll. Both details
    // matter here too: `maxValue` is only final once the 360ms height animation has settled, and a
    // second, slower animation riding on top of the expand reads as drift. So: wait, then snap.
    val pageScroll = rememberScrollState()
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(400)
            pageScroll.scrollTo(pageScroll.maxValue)
        } else {
            pageScroll.animateScrollTo(0)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Tokens.Palette.appBackground)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Opening the summary softens EVERYTHING behind the sheet, header included (Figma
            // 5227:27592). The prototype needs two layers for it because its nav sits above the
            // pay block in z and one backdrop filter cannot reach both; here it is two `blur`s
            // for the same reason — the nav and the scroller are separate subtrees. 3 on the nav
            // and 4 on the body, not one value: the nav is 4dp of type on white and blurs
            // visibly softer than the coloured band does at the same radius.
            TopNav(blur = if (summaryOpen) 3.dp else 0.dp, walletVisible = !summaryOpen)

            Column(
                Modifier
                    .weight(1f)
                    .blur(if (summaryOpen) 4.dp else 0.dp)
                    .verticalScroll(pageScroll)
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
                                    flyingAmount = if (flight != null) selected else null,
                                    // `.lure` — the badges only catch the light while the
                                    // current pick earns nothing, which on this table is ₹50.
                                    sweep = goldCard
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

                // This tail IS the prototype's SAFE_ABOVE_CARD. Scrolled to the bottom with the
                // grid open, it is what holds the dashed card's bottom edge 80 clear of the
                // congrats card instead of letting it come to rest almost touching. 80 is her
                // number; 52 is what the spacer has to be to produce it, because the receipt
                // card is lifted by `bandHiddenBehindCard` and so already ends 28 short of the
                // scroller. Collapsed, nothing overflows and this is never seen.
                Spacer(Modifier.height(52.dp))
            }

            // ---- pay block: pinned, never scrolls ----
            // The veil rides on the block's own background rather than being a separate layer,
            // because everything in the block that must stay crisp — card, arc, summary, pay bar —
            // paints its own white on top of it. What the tint shows through is only the strip
            // beside the 268-wide card, which is exactly what it is for. The summary is NOT part
            // of that strip: the prototype gives it `.am .summary{z-index:2;background:#fff}`, so
            // it must paint white too or the row reads as tinted mint against Figma's white.
            val veilAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (expanded || summaryOpen) 1f else 0f,
                animationSpec = tween(Tokens.Motion.veilFadeMs),
                label = "veil"
            )
            Column(
                Modifier.drawBehind {
                    drawRect(Tokens.Palette.white)
                    if (veilAlpha > 0f) {
                        drawRect(
                            if (goldCard) Tokens.BandBrush.veilGold else Tokens.BandBrush.veilGreen,
                            alpha = veilAlpha
                        )
                    }
                }
            ) {
                // Card and arc OVERLAP — they are not stacked. The arc is bottom-aligned inside a
                // wrap only 2dp taller than the card, so it is painted across the card's bottom
                // 22dp and becomes its bottom edge. Stacked one under the other (which is what
                // this was) the arc is white-on-white and invisible, and the card reads as a
                // floating rounded rectangle with a drop shadow instead of sitting into a sweep.
                // Clipped, and that is the fix for the smudge under the arc. The card's shadow is
                // offset 6 down with a 14 blur, so it reaches about 12 below the card's bottom
                // edge — past the arc, which is only 24 tall and bottom-aligned in this 110 wrap
                // and so cannot cover its own tail. Clipping the wrap ends the shadow exactly
                // where the arc's curve does.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(Tokens.Dimens.cardWrapHeight)
                        .clipToBounds(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    CongratsCard(
                        credit = shown,
                        gold = goldCard,
                        tilt = tiltCurve(tiltRun.value),
                        absorb = absorbCurve(absorbRun.value),
                        onFigurePlaced = { figureAt = it }
                    )
                    CardArc(goldCard, Modifier.align(Alignment.BottomCenter))
                }
                // The row AND its breakdown share one white surface, because in the prototype they
                // are one element (`.summary` wraps `.sumrow` + `.ps-details`).
                Column(Modifier.background(Tokens.Palette.white)) {
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
                }
                PayBar(total = selected + gst, method = "PhonePe", onPay = {})
            }
        }

        // The close button lives at ROOT, not in the nav, which is the only way it can stay sharp
        // while the bar behind it is blurred — anything inside the nav row is blurred with it.
        // 42dp, 24 in from the right edge, centred on the nav row (Figma 5227:27602).
        SummaryClose(
            visible = summaryOpen,
            onClick = { summaryOpen = false },
            modifier = Modifier.align(Alignment.TopEnd).padding(end = 24.dp, top = 11.dp)
        )

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

/**
 * The nav bar. [blur] is applied to its CONTENTS but not to its white ground or its rule, so the
 * bar keeps its edge while the title behind the close button goes soft — the prototype does the
 * same thing by putting `filter` on `.topnav` and leaving the close button a sibling of it.
 */
@Composable
private fun TopNav(blur: Dp = 0.dp, walletVisible: Boolean = true) {
    val walletAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (walletVisible) 1f else 0f,
        animationSpec = tween(200),
        label = "walletPill"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Tokens.Palette.white)
            // The 1dp Gray/300 rule under the nav. It is the only thing separating the bar from
            // the sheet — both are white — so without it the title floats on the banner.
            .drawBehind {
                drawLine(
                    color = Tokens.Palette.gray300,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(48.dp)
            .blur(blur),
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
                .graphicsLayer { alpha = walletAlpha }
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
 * The button that shuts the open summary. It scales up from 88% as it fades in, which is what
 * makes it read as having ARRIVED in the wallet pill's place rather than the pill having changed
 * into it.
 */
@Composable
private fun SummaryClose(visible: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val t by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(if (visible) 280 else 200, easing = Tokens.Motion.easeOut),
        label = "sumClose"
    )
    if (t <= 0f) return
    Box(
        modifier
            .graphicsLayer {
                alpha = t
                val s = 0.88f + 0.12f * t
                scaleX = s; scaleY = s
            }
            .size(42.dp)
            .clip(CircleShape)
            .background(Tokens.Palette.white)
            .border(1.dp, Tokens.Palette.gray300, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CloseGlyph(Tokens.Palette.gray700, Modifier.size(16.dp))
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
    flyingAmount: Int?,
    sweep: Boolean
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
        shown.chunked(3).forEachIndexed { rowIndex, row ->
            Row(
                // Pinned to 96 so row 1 lands pixel-identical whether there is one row or four,
                // and every row below is a clean +96 on the card, the notch and the button.
                Modifier
                    .fillMaxWidth()
                    .height(Tokens.Dimens.skuRowHeight),
                horizontalArrangement = Arrangement.spacedBy(Tokens.Dimens.tileGap),
                verticalAlignment = Alignment.Top
            ) {
                row.forEachIndexed { col, sku ->
                    AmountTile(
                        amount = sku.amount,
                        // Under an applied coupon the badge must show what the wallet is
                        // ACTUALLY credited — the standing bonus plus the flat ₹50.
                        bonus = sku.bonus + if (couponApplied && sku.bonus > 0) Pricing.COUPON_FLAT else 0,
                        selected = sku.amount == selected,
                        onClick = { onSelect(sku.amount) },
                        onBadgePlaced = { onBadgePlaced(sku.amount, it) },
                        badgeHidden = sku.amount == flyingAmount,
                        // One hero tile carries the coin shower, and it is a fixed amount, not
                        // "the biggest one showing" — the prototype keeps the coins on ₹250 even
                        // once the sheet is open and ₹5000 is on screen.
                        coins = sku.amount == Pricing.COIN_SKU,
                        sweep = sweep,
                        // 380ms a tile, the prototype's own `sweepDelay(i)`, counted across the
                        // whole grid rather than per row so opening the sheet does not put three
                        // rows of badges in lockstep.
                        sweepDelayMs = (rowIndex * 3 + col) * 380,
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
 * The expanded summary — Figma "Payment summary / State=Open" (`4815:2493`).
 *
 * WHAT YOU GET LEADS, WHAT YOU PAY FOLLOWS. That order is the design's, and it is the whole
 * argument of the sheet: the outlined box states the wallet credit, the grey panel under it shows
 * where that figure came from, and only then does the GST calculation appear. Flipped — GST
 * first — the sheet reads as an invoice and the bonus looks like a footnote.
 *
 * Two structural details that look like mistakes:
 *
 *  - The grey panel is pulled UP 12 under the outlined box, and its top padding is 24 to
 *    compensate. That is what makes the box look seated into the panel rather than stacked on it;
 *    the box is z-above so its border stays unbroken across the seam.
 *  - The panel is dropped entirely when the amount earns nothing, and so is the GST calc block
 *    when there is no GST. An empty "+₹0" line is worse than no line.
 *
 * The coupon is itemised SEPARATELY from the bonus — folding the two together is the bug this
 * screen was rebuilt to avoid.
 */
@Composable
private fun SummaryDetail(visible: Boolean, amount: Int, bonus: Int, coupon: Int, gst: Int) {
    val total = amount + bonus + coupon
    val payable = amount + gst

    // grid-template-rows 0fr -> 1fr over 400ms on the house curve, with the contents fading in
    // 100ms behind the height. The delay matters: fading in on the same frame as the open makes
    // the text look like it is being stretched rather than revealed.
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(tween(Tokens.Motion.summaryOpenMs, easing = Tokens.Motion.easeOut)) +
            fadeIn(tween(Tokens.Motion.fadeMs, delayMillis = 100)),
        exit = shrinkVertically(tween(Tokens.Motion.summaryOpenMs, easing = Tokens.Motion.easeOut)) +
            fadeOut(tween(200))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Tokens.Palette.white)
                .padding(horizontal = 16.dp)
                .padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column {
                // Outlined in Success/700 only while there is something to celebrate; a plain
                // top-up gets the grey version, because a green box round "₹50" is a promise of
                // a bonus that is not there.
                val earned = bonus + coupon > 0
                Row(
                    Modifier
                        .zIndex(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Tokens.Palette.white)
                        .border(
                            1.dp,
                            if (earned) Tokens.Palette.success700 else Tokens.Palette.gray300,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val ink = if (earned) Tokens.Palette.success700 else Tokens.Palette.gray600
                    Text(
                        "To be added in your wallet",
                        style = Tokens.Type.bodyXs, fontWeight = FontWeight.SemiBold, color = ink
                    )
                    Text(
                        "₹$total",
                        style = Tokens.Type.bodySm, fontWeight = FontWeight.Bold, color = ink
                    )
                }
                if (earned) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationY = -12.dp.toPx() }
                            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                            .background(Tokens.Palette.gray100)
                            .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryLine("Recharge Added", "₹$amount", valueInk = Tokens.Palette.gray800)
                        if (bonus > 0) SummaryLine(
                            "Extra Bonus", "+₹$bonus",
                            labelInk = Tokens.Palette.success700, valueInk = Tokens.Palette.success700
                        )
                        if (coupon > 0) SummaryLine(
                            "Coupon Code Applied", "+₹$coupon",
                            labelInk = Tokens.Palette.success700, valueInk = Tokens.Palette.success700
                        )
                        DashRule(Tokens.Palette.gray300)
                        SummaryLine(
                            "Total", "₹$total",
                            labelInk = Tokens.Palette.gray600, valueInk = Tokens.Palette.gray700
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DashRule(Tokens.Palette.gray200)
                if (gst > 0) {
                    Column(
                        Modifier.padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SummaryLine("Recharge amount", "₹$amount", valueInk = Tokens.Palette.gray700)
                        SummaryLine("GST (18%)", "₹$gst", valueInk = Tokens.Palette.gray800)
                    }
                    DashRule(Tokens.Palette.gray200)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total payable", style = Tokens.Type.bodySm, fontWeight = FontWeight.SemiBold, color = Tokens.Palette.gray800)
                    Text("₹$payable", style = Tokens.Type.bodySm, fontWeight = FontWeight.SemiBold, color = Tokens.Palette.gray800)
                }
                DashRule(Tokens.Palette.gray200)
            }
        }
    }
}

/**
 * One summary row. The label is Body X Small and the value Body Small — the value is a size up,
 * which is what lets the eye run down the right-hand column without reading the labels.
 */
@Composable
private fun SummaryLine(
    label: String,
    value: String,
    labelInk: androidx.compose.ui.graphics.Color = Tokens.Palette.gray500,
    valueInk: androidx.compose.ui.graphics.Color = Tokens.Palette.gray700
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = Tokens.Type.bodyXs, color = labelInk)
        Text(value, style = Tokens.Type.bodySm, color = valueInk)
    }
}

/**
 * `repeating-linear-gradient(90deg, c 0 2px, transparent 2px 4px)` — a 2-on 2-off dotted rule.
 * A solid hairline here reads as a table border and boxes the numbers in; the dotted one reads
 * as a receipt, which is what the whole card is pretending to be.
 */
@Composable
private fun DashRule(color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        val step = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawRect(color, topLeft = Offset(x, 0f), size = Size(2.dp.toPx(), size.height))
            x += step
        }
    }
}
