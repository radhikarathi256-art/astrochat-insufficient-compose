package com.astrochat.insufficient.data

import kotlin.math.roundToInt

/**
 * Pricing and band-state rules for the insufficient-balance Add Money screen.
 *
 * These are the prototype's live numbers, not illustrative ones. Two rules matter and are easy
 * to break:
 *
 *  1. The bonus PERCENTAGE is what moves the per-minute rate. On this table the percentage falls
 *     as the amount rises (150% at ₹100 down to 20% at ₹10,000), so a bigger recharge buys a
 *     worse per-minute rate. That was flagged and chosen anyway — do not "fix" it in code.
 *  2. The ASTRO 50 coupon is itemised at checkout, NOT folded into the bonus. So `bonusFor`
 *     never includes it, and anything showing a wallet TOTAL must use `creditFor`, which adds
 *     both. Deriving one from the other is the original bug.
 */
object Pricing {

    /** amount to standing bonus. Wallet credit = amount + bonus. */
    val skus: List<Sku> = listOf(
        Sku(50, 0), Sku(100, 150), Sku(250, 500), Sku(500, 300), Sku(1000, 500),
        Sku(2000, 800), Sku(3000, 1000), Sku(4000, 1200), Sku(5000, 1500), Sku(10000, 2000)
    )

    /** The three shown before "See More Options". */
    val collapsedSkus: List<Sku> get() = skus.take(3)

    const val COUPON_FLAT = 50
    const val GST_RATE = 0.18

    /** The astrologer's list rate in this flow, in rupees per minute. */
    const val ASTRO_RATE = 12

    fun bonusFor(amount: Int): Int = skus.firstOrNull { it.amount == amount }?.bonus ?: 0

    fun creditFor(amount: Int, couponApplied: Boolean): Int =
        amount + bonusFor(amount) + if (couponApplied) COUPON_FLAT else 0

    /** GST is 18% of the amount only — never of the bonus. */
    fun gstFor(amount: Int): Int = (amount * GST_RATE).roundToInt()

    fun minutesFor(amount: Int, couponApplied: Boolean): Int =
        creditFor(amount, couponApplied) / ASTRO_RATE

    /**
     * Which alert band each amount shows. This is the demo mapping Radhika specified so a
     * developer sees all four bands without needing four builds:
     *
     *   ₹50   -> Not enough     (the insufficient state)
     *   ₹100  -> Offer          (live countdown, white-on-dark-green)
     *   ₹250  -> Coupon         (what Offer becomes once the timer expires)
     *   rest  -> Bonus          (the insufficient screen's default band)
     *
     * In production this is driven by offer eligibility and the countdown, not by amount.
     */
    fun bandFor(amount: Int): BandType = when (amount) {
        50 -> BandType.NOT_ENOUGH
        100 -> BandType.OFFER
        250 -> BandType.COUPON
        else -> BandType.BONUS
    }
}

data class Sku(val amount: Int, val bonus: Int)

/** Mirrors the Figma variant names on component set 4804:2417 ("Alert band"). */
enum class BandType { BONUS, BONUS_DARK, NOT_ENOUGH, OFFER, COUPON, MISSING_OUT }

/**
 * Resolved band copy. Two lines, plus an optional chip that only the Offer and Coupon bands
 * carry (the countdown and "Expiring Today" respectively).
 */
data class BandCopy(val line1: String, val chip: String?, val line2: String)

/**
 * Copy is derived from the selected amount so the band stays truthful as tiles are tapped.
 * Strings match the Figma variants; the numbers are computed rather than hardcoded.
 */
fun bandCopyFor(amount: Int, astrologer: String, secondsLeft: Int): BandCopy {
    val credit = Pricing.creditFor(amount, couponApplied = false)
    val mins = Pricing.minutesFor(amount, couponApplied = false)
    val next = Pricing.skus.firstOrNull { it.amount > amount }

    return when (Pricing.bandFor(amount)) {
        BandType.NOT_ENOUGH -> BandCopy(
            line1 = "To connect with $astrologer,",
            chip = null,
            line2 = "need wallet balance of 4 min (₹60)"
        )
        BandType.OFFER -> BandCopy(
            line1 = "Offer Applied",
            chip = formatCountdown(secondsLeft),
            line2 = "Bonus on first recharge over ₹100"
        )
        BandType.COUPON -> BandCopy(
            line1 = "ASTRO 50 coupon",
            chip = "Expiring Today",
            line2 = "Get ₹50 extra on your first recharge"
        )
        BandType.MISSING_OUT -> BandCopy(
            line1 = "You're missing out on ₹${next?.bonus ?: 0} extra",
            chip = null,
            line2 = "Pick ₹${next?.amount ?: amount}, get ₹${Pricing.creditFor(next?.amount ?: amount, false)} in your wallet"
        )
        BandType.BONUS, BandType.BONUS_DARK -> BandCopy(
            line1 = "$mins mins with $astrologer",
            chip = null,
            line2 = "Pick ₹$amount, get ₹$credit in your wallet"
        )
    }
}

fun formatCountdown(totalSeconds: Int): String {
    val m = (totalSeconds / 60).coerceAtLeast(0)
    val s = (totalSeconds % 60).coerceAtLeast(0)
    return "%02d min : %02d s".format(m, s)
}
