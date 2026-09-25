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

    /**
     * The astrologer's list rate in this flow, in rupees per minute, and the shortest chat the
     * flow will start.
     *
     * 15, not 12. The two have to agree or the screen contradicts itself: the Not enough band
     * says ₹50 is short of the 4-minute minimum, and at ₹12/min the minimum is ₹48, which ₹50
     * clears. At ₹15 the minimum is ₹60 and the band is telling the truth. It is also what makes
     * the Bonus band's minute counts match the prototype (₹100 credits ₹250, which is 16 mins).
     */
    const val ASTRO_RATE = 15
    const val MIN_MINUTES = 4

    fun bonusFor(amount: Int): Int = skus.firstOrNull { it.amount == amount }?.bonus ?: 0

    fun creditFor(amount: Int, couponApplied: Boolean): Int =
        amount + bonusFor(amount) + if (couponApplied) COUPON_FLAT else 0

    /** GST is 18% of the amount only — never of the bonus. */
    fun gstFor(amount: Int): Int = (amount * GST_RATE).roundToInt()

    fun minutesFor(amount: Int, couponApplied: Boolean): Int =
        creditFor(amount, couponApplied) / ASTRO_RATE

    /**
     * Which alert band each amount shows.
     *
     * THIS IS A DEMO MAPPING, NOT PRODUCT LOGIC. In the real app the band is chosen by offer
     * eligibility, coupon state and whether a countdown is running — never by the amount. It is
     * wired to the amount here for one reason: a developer integrating this module can tap along
     * the SKU row and see every band variant render, in situ, without six builds or a debug menu.
     * Open "See More Options" and all ten tiles are reachable.
     *
     *   ₹50    -> Not enough   ₹500   -> Missing out   ₹3000   -> Offer
     *   ₹100   -> Offer        ₹1000  -> Bonus         ₹4000   -> Coupon
     *   ₹250   -> Coupon       ₹2000  -> Bonus dark    ₹5000   -> Missing out
     *                                                  ₹10000  -> Bonus
     *
     * Two deliberate choices in that table:
     *
     *  - Not enough appears on ₹50 and NOWHERE else. Its copy names a concrete shortfall
     *    ("need wallet balance of 4 min (₹60)"), and ₹50 is the only amount on this table that
     *    is actually short of it. Putting that band on ₹3000 would render a sentence that
     *    contradicts the tile it is attached to, which is worse than one fewer sample.
     *  - The other five cycle in a fixed order, so the first three tiles — the only ones visible
     *    before the row is expanded — are three DIFFERENT bands rather than three of a kind.
     *
     * Replacing this with the real rule should mean deleting the function, not editing it.
     */
    fun bandFor(amount: Int): BandType {
        if (amount <= skus.first().amount) return BandType.NOT_ENOUGH
        val cycle = listOf(
            BandType.OFFER, BandType.COUPON, BandType.MISSING_OUT,
            BandType.BONUS, BandType.BONUS_DARK
        )
        val i = skus.indexOfFirst { it.amount == amount }
        // Unknown amounts fall to Bonus, the screen's resting band, rather than throwing.
        if (i <= 0) return BandType.BONUS
        return cycle[(i - 1) % cycle.size]
    }
}

data class Sku(val amount: Int, val bonus: Int)

/** Mirrors the Figma variant names on component set 4804:2417 ("Alert band"). */
enum class BandType { BONUS, BONUS_DARK, NOT_ENOUGH, OFFER, COUPON, MISSING_OUT }

/**
 * Resolved band copy. Two lines, plus an optional chip that only the Offer and Coupon bands
 * carry (the countdown and "Expiring Today" respectively).
 *
 * The two lines carry `*…*` runs marking the words the design sets in bold. That marker exists
 * because the bold runs are NOT derivable — English bolds the bare figure ("₹150") while the
 * approved Hindi bolds the whole phrase ("₹150 extra"), so the emphasis has to travel with the
 * string. [AlertBand] is what turns the markers into spans.
 */
data class BandCopy(val line1: String, val chip: String?, val line2: String)

/**
 * The approved v3 copy table, ported from the prototype's `V3COPY`.
 *
 * Key is branch + bracket. Branch: `b` = wallet, `k` = ASTRO50 coupon, `c` = astrologer flow.
 * Bracket is which SKU is selected. Everything in {braces} is filled from the live pricing
 * table, so no figure here is hardcoded — change the SKU table and the copy follows.
 *
 * The two "ask" brackets (50 and 100) point UP at the next tile; from ₹250 the band stops asking
 * and states what you get.
 */
private val V3COPY: Map<String, Pair<String, String>> = mapOf(
    "b50" to ("You're missing out on *₹{b1}* extra" to "Pick *₹{t1}*, get *₹{c1}* in your wallet"),
    "b100" to ("You're getting *₹{b1}* extra" to "Pick *₹{t2}*, get *₹{c2}* in your wallet"),
    "b250" to ("You're getting *₹{b2}* extra" to "Best deal · *₹{c2}* in your wallet"),
    "bhi" to ("You're getting *₹{bs}* extra" to "*₹{cs}* in your wallet"),
    "k50" to ("*ASTRO50* applied" to "Pick *₹{t1}*, get *₹{c1}* in your wallet"),
    "k100" to ("*ASTRO50* applied" to "Pick *₹{t2}*, get *₹{c2}* in your wallet"),
    "k250" to ("*ASTRO50* applied" to "Best deal · *₹{c2}* in your wallet"),
    "khi" to ("*ASTRO50* applied" to "*₹{cs}* in your wallet with ASTRO50"),
    "c50" to ("Only {M} with {astro}" to "Pick *₹{t1}*, chat for {M1}"),
    "c100" to ("{M} with {astro}" to "Pick *₹{t2}*, chat for {M2}"),
    "c250" to ("{M} with {astro}" to "Best deal · *₹{b2}* extra on ₹{t2}"),
    "chi" to ("{M} with {astro}" to "*₹{bs}* extra on ₹{s}")
)

/** Explicit comparisons, not endsWith("50") — "250" ends with "50" too. */
private fun bracketFor(amount: Int): String {
    val t1 = Pricing.skus[1].amount
    val t2 = Pricing.skus[2].amount
    return when {
        amount <= Pricing.skus[0].amount -> "50"
        amount < t2 -> "100"
        amount == t2 -> "250"
        else -> "hi"
    }
}

/**
 * Talk time, as markup.
 *
 * FLOOR, not round: the band is promising talk time, and rounding 49.6 up to "50 mins" is a
 * promise the wallet cannot keep on the last minute. Past 99 the count stops meaning anything —
 * "137 mins" is not a duration anyone pictures — so it collapses to "more than 1 hour", where
 * only "1 hour" is bold and the qualifier is not.
 */
private fun minutesMarkup(amount: Int, couponApplied: Boolean): String {
    val m = Pricing.minutesFor(amount, couponApplied)
    if (m > 99) return "more than *1 hour*"
    return "*$m ${if (m == 1) "min" else "mins"}*"
}

/** Plain digits, NO thousands separator: at 12px on a strip this narrow "₹5,200" reads as two numbers. */
private fun fillCopy(s: String, amount: Int, astrologer: String, couponApplied: Boolean): String {
    val t1 = Pricing.skus[1].amount
    val t2 = Pricing.skus[2].amount
    val tok = mapOf(
        "t1" to "$t1", "t2" to "$t2",
        "b1" to "${Pricing.bonusFor(t1)}", "b2" to "${Pricing.bonusFor(t2)}",
        "c1" to "${Pricing.creditFor(t1, couponApplied)}",
        "c2" to "${Pricing.creditFor(t2, couponApplied)}",
        "bs" to "${Pricing.bonusFor(amount)}",
        "cs" to "${Pricing.creditFor(amount, couponApplied)}",
        "s" to "$amount", "astro" to astrologer,
        "M" to minutesMarkup(amount, couponApplied),
        "M1" to minutesMarkup(t1, couponApplied),
        "M2" to minutesMarkup(t2, couponApplied)
    )
    // Both braces escaped. Android's regex engine is ICU, not the JVM's: a bare `}` compiles
    // fine on the desktop and throws PatternSyntaxException on device, so this only fails once
    // it is running on a phone.
    return Regex("\\{(\\w+)\\}").replace(s) { tok[it.groupValues[1]] ?: it.value }
}

/**
 * Copy is derived from the selected amount so the band stays truthful as tiles are tapped.
 *
 * Not enough and Offer own their sentences outright; the other three read the v3 table off the
 * branch their band belongs to. Not enough comes FIRST in the prototype for the same reason it
 * is special-cased here: on this flow ₹50 is a blocker, not a missed bonus, so it must not fall
 * through to the gold "you're missing out" copy.
 */
fun bandCopyFor(amount: Int, astrologer: String, secondsLeft: Int): BandCopy {
    val band = Pricing.bandFor(amount)
    val coupon = band == BandType.COUPON

    if (band == BandType.NOT_ENOUGH) return BandCopy(
        line1 = "To connect with *$astrologer*,",
        chip = null,
        line2 = "need wallet balance of *${Pricing.MIN_MINUTES} min (₹${Pricing.MIN_MINUTES * Pricing.ASTRO_RATE})*"
    )
    if (band == BandType.OFFER) return BandCopy(
        line1 = "Offer expires in",
        chip = formatCountdown(secondsLeft),
        line2 = "Bonus on first recharge over *₹${Pricing.skus[1].amount}*"
    )

    val branch = when (band) {
        BandType.COUPON -> "k"
        BandType.MISSING_OUT -> "b"
        else -> "c"
    }
    val (l1, l2) = V3COPY.getValue(branch + bracketFor(amount))
    return BandCopy(
        line1 = fillCopy(l1, amount, astrologer, coupon),
        // Only the coupon band carries a chip here, and it is a STATIC status, not a clock.
        chip = if (coupon) "Expiring Today" else null,
        line2 = fillCopy(l2, amount, astrologer, coupon)
    )
}

fun formatCountdown(totalSeconds: Int): String {
    val m = (totalSeconds / 60).coerceAtLeast(0)
    val s = (totalSeconds % 60).coerceAtLeast(0)
    return "%02d min : %02d s".format(m, s)
}
