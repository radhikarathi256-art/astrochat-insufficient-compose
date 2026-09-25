package com.astrochat.insufficient.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrochat.insufficient.data.BandType
import com.astrochat.insufficient.data.Pricing
import com.astrochat.insufficient.data.bandCopyFor
import com.astrochat.insufficient.ui.components.AlertBand
import com.astrochat.insufficient.ui.components.AmountTile
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

private const val ASTROLOGER = "Astro Hemali"

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
                        AlertBand(
                            type = band,
                            copy = copy,
                            modifier = Modifier.padding(horizontal = Tokens.Dimens.gutter)
                        )
                        // Every extra SKU row is worth exactly 96dp, and it moves the button,
                        // the notch and the card's own height by that same 96 — which is why
                        // one row and four rows can share these three expressions.
                        val rows = if (expanded) 4 else 1
                        val extra = ((rows - 1) * Tokens.Dimens.skuRowHeight.value).dp

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
                                    selected = selected,
                                    couponApplied = band == BandType.COUPON,
                                    onSelect = { selected = it }
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
                            credit = credit,
                            // Pulled into the arc below so the arc IS its bottom edge.
                            modifier = Modifier.graphicsLayer { translationY = 22.dp.toPx() }
                        )
                        CardArc(Modifier.graphicsLayer { translationY = 22.dp.toPx() })
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
                PayBar(total = selected + gst, method = "UPI", onPay = {})
            }
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
        Text(
            "Add Money",
            style = Tokens.Type.bodyLg,
            fontWeight = FontWeight.SemiBold,
            color = Tokens.Palette.gray700,
            modifier = Modifier.weight(1f)
        )
        Row(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Tokens.Palette.gray100)
                .padding(horizontal = 8.dp, vertical = 4.dp),
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
    selected: Int,
    couponApplied: Boolean,
    onSelect: (Int) -> Unit
) {
    val shown = if (expanded) {
        Pricing.skus
    } else {
        val head = Pricing.collapsedSkus
        if (head.any { it.amount == selected }) head
        else head.drop(1) + Pricing.skus.first { it.amount == selected }
    }

    Column {
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
