package com.astrochat.insufficient.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.astrochat.insufficient.R

/**
 * Design tokens for the Add Money / insufficient-balance surface.
 *
 * Source of truth: the Payments-Android Figma file, page "Bonuus higlight". Every value here
 * was read off that file or measured on the running prototype — none of it is eyeballed.
 * (The file key is deliberately not in this repo; ask the design owner for the link.)
 *
 * This file is the one to copy first: everything else in the module reads from it, so
 * re-pointing these values at the production theme re-skins the whole screen.
 */
object Tokens {

    /** Ramps are Untitled UI. Names match the Figma paint styles 1:1. */
    object Palette {
        val brand = Color(0xFFEF6939)
        val brand500 = Color(0xFFF28761)
        val brand900 = Color(0xFF560000)

        val gray25 = Color(0xFFFCFCFD)
        val gray50 = Color(0xFFF9FAFB)
        val gray100 = Color(0xFFF2F4F7)
        val gray200 = Color(0xFFEAECF0)
        val gray300 = Color(0xFFD0D5DD)
        val gray400 = Color(0xFF98A2B3)
        val gray500 = Color(0xFF667085)
        val gray600 = Color(0xFF475467)
        val gray700 = Color(0xFF344054)
        val gray800 = Color(0xFF1D2939)
        val gray900 = Color(0xFF101828)

        val success50 = Color(0xFFECFDF3)
        val success100 = Color(0xFFD1FADF)
        val success200 = Color(0xFFA6F4C5)
        val success400 = Color(0xFF32D583)
        val success500 = Color(0xFF12B76A)
        val success600 = Color(0xFF039855)
        val success700 = Color(0xFF027A48)
        val success800 = Color(0xFF05603A)
        val success900 = Color(0xFF054F31)
        val success950 = Color(0xFF04452F)

        val warning50 = Color(0xFFFFFAEB)
        val warning100 = Color(0xFFFEF0C7)
        val warning300 = Color(0xFFFEC84B)
        val warning400 = Color(0xFFFDB022)
        val warning600 = Color(0xFFDC6803)
        val warning700 = Color(0xFFB54708)
        val warning800 = Color(0xFF93370D)

        val error700 = Color(0xFFB42318)
        val error800 = Color(0xFF912018)

        val white = Color(0xFFFFFFFF)
        val black = Color(0xFF000000)

        /** Phone chrome behind the sheet. Deliberately has NO Figma style — it is not a product colour. */
        val appBackground = Color(0xFFEEF0F3)
    }

    /**
     * Type scale. Figma names are in the trailing comment so a spec review can be done by eye.
     * Inter everywhere, letter-spacing 0.
     *
     * This is REAL Inter (res/font), not FontFamily.Default. It used to be the default, and that
     * one line was the single biggest reason the screen read as "not the design": Roboto's wider
     * apertures and taller x-height throw every measured width on this screen — the tile figures,
     * the badge, the band's two lines. Do not swap it back to a system family to save 2MB.
     */
    object Type {
        private val inter = FontFamily(
            Font(R.font.inter_regular, FontWeight.Normal),
            Font(R.font.inter_medium, FontWeight.Medium),
            Font(R.font.inter_semibold, FontWeight.SemiBold),
            Font(R.font.inter_bold, FontWeight.Bold),
            Font(R.font.inter_extrabold, FontWeight.ExtraBold)
        )

        /** Exposed so call sites that set a raw fontSize still get Inter rather than Roboto. */
        val family = inter

        val bodyXs = TextStyle(fontFamily = inter, fontSize = 12.sp, lineHeight = 18.sp)   // Body X Small 12
        val bodySm = TextStyle(fontFamily = inter, fontSize = 14.sp, lineHeight = 20.sp)   // Body Medium 14
        val bodyMd = TextStyle(fontFamily = inter, fontSize = 16.sp, lineHeight = 24.sp)   // Body large 16
        val bodyLg = TextStyle(fontFamily = inter, fontSize = 18.sp, lineHeight = 28.sp)   // Body XL 18
        val bodyXl = TextStyle(fontFamily = inter, fontSize = 20.sp, lineHeight = 30.sp)   // Body 2XL 20

        val medium = FontWeight.Medium       // 500
        val semiBold = FontWeight.SemiBold   // 600
        val bold = FontWeight.Bold           // 700
        val extraBold = FontWeight.ExtraBold // 800
    }

    /**
     * Geometry measured on the running prototype at the 360 x 768 design size, then
     * cross-checked against the Figma frames. Where the two disagreed the prototype won —
     * it is what the user actually sees.
     */
    object Dimens {
        val screenWidth = 360.dp
        val screenHeight = 768.dp

        val gutter = 16.dp            // card edge inset
        val tileGutter = 32.dp        // SKU row inset (the card is inset a further 16)

        val cardRadius = 32.dp        // receipt card top corners
        val tileRadius = 14.dp
        val badgeRadius = 12.dp
        val pillRadius = 12.dp
        val popupRadius = 16.dp

        val bandWidth = 328.dp
        val bandHeight = 89.dp
        /** The band's bottom 42dp sits BEHIND the receipt card — it is padding, not empty space. */
        val bandHiddenBehindCard = 42.dp

        val receiptTop = 139.dp
        val receiptHeight = 224.8f.dp
        val skuRowTop = 28.dp         // relative to the receipt card
        /** One SKU row. Adding a row moves the button, the notch AND the card height by this. */
        val skuRowHeight = 96.dp
        val notchTop = 128.dp         // relative to the receipt card
        val tileWidth = 89.dp
        val tileHeight = 67.dp
        val badgeHeight = 26.dp
        /** Badge overlaps the tile by this much — a negative margin on web, an offset here. */
        val badgeOverlap = 11.dp
        val tileGap = 12.dp

        val seeButtonTop = 171.4f.dp  // relative to the receipt card
        val seeButtonHeight = 28.dp

        /** How deep the receipt's bottom edge bows. Drives the corner roundness too. */
        val receiptBowDepth = 72.dp
        val notchSize = 20.dp
        val notchFromBottom = 77.dp   // must move whenever receiptBowDepth does

        val popupWidth = 278.dp
        val popupCardHeight = 176.dp
        val popupBadge = 52.dp
    }

    /**
     * Motion. `easeOut` is the house curve, used for everything that answers a tap.
     * `easeGentle` is a softer ease-out used only where something is settling rather than
     * responding — the See Less collapse. Opening and closing are deliberately asymmetric.
     */
    object Motion {
        val easeOut: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
        val easeGentle: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
        val spring: Easing = CubicBezierEasing(0.2f, 1.3f, 0.4f, 1f)

        const val expandMs = 360
        const val collapseMs = 520
        const val fadeMs = 300
        const val tileSelectMs = 280
        const val popupInMs = 400
        const val popupHoldMs = 2300
        const val popupOutMs = 250
        const val bandSwapMs = 420
    }

    /**
     * Alert band fills, straight off Figma component set `4804:2417` ("Alert band").
     * Every one is a horizontal linear gradient that runs out to white on the right, so the
     * band reads as a wash rather than a block. Stops and offsets are the Figma values.
     */
    object BandBrush {
        private fun h(vararg stops: Pair<Float, Color>) = Brush.horizontalGradient(*stops)

        /**
         * Type=Bonus — the standing green bonus band.
         *
         * Alpha stops over Success/600, not opaque mixes, exactly as Figma 5075:5757 and the
         * prototype specify. Every band in this set is drawn on white, so the two are the same
         * pixels today; keeping the alpha means the band still behaves if it is ever placed on
         * a tinted surface, and it is what the design actually says.
         */
        val bonus = h(
            0f to Color(0xFF039855).copy(alpha = 0.18f),
            0.55f to Color(0xFF039855).copy(alpha = 0.03f),
            1f to Color(0xFF039855).copy(alpha = 0f)
        )

        /** Type=Bonus dark — the same band while a countdown is running. Opaque in the design. */
        val bonusDark = h(
            0f to Color(0xFFCDF8E0), 0.40f to Color(0xFFDEFBEB),
            0.78f to Color(0xFFF2FDF8), 1f to Color(0xFFFFFFFF)
        )

        /** Type=Not enough — the insufficient-balance band. */
        val notEnough = h(
            0f to Color(0xFFFCE7E7), 0.40f to Color(0xFFFDEEEE),
            0.78f to Color(0xFFFEF8F8), 1f to Color(0xFFFFFFFF)
        )

        /**
         * Type=Offer — dark green, holds white text and a live timer chip.
         *
         * It is a walk DOWN the success ramp (950 -> 800 -> 700 -> 600), not a fade to white.
         * That matters: this is the only band whose right edge stays saturated, which is why
         * the copy must not run past ~80% of the width — on the pale bands an overrun just gets
         * faint, here it lands on full-strength green and the white text keeps working, so the
         * mistake is invisible until someone reads it at 12px.
         */
        val offer = h(
            0f to Color(0xFF04452F), 0.42f to Color(0xFF05603A),
            0.78f to Color(0xFF027A48), 1f to Color(0xFF039855)
        )

        /**
         * Type=Coupon — the ASTRO 50 band the offer becomes once the timer runs out.
         * Same four ramp colours as Offer; only the stop positions move, so it reads as the
         * same band having brightened rather than as a different component.
         */
        val coupon = h(
            0f to Color(0xFF04452F), 0.25f to Color(0xFF05603A),
            0.663f to Color(0xFF027A48), 1f to Color(0xFF039855)
        )

        /** Type=Missing out — amber, shown when a better tier is one tap away. */
        val missingOut = h(
            0f to Color(0xFFFEF5DE).copy(alpha = 0.6f),
            0.40f to Color(0xFFFEF8E9).copy(alpha = 0.7f),
            0.78f to Color(0xFFFFFCF6), 1f to Color(0xFFFFFFFF)
        )

        /**
         * Bonus badge on the amount tile — Figma "Bonus badge / Style=Green".
         *
         * 85% opacity, not solid, and the last stop fades further. The badge hangs over the
         * tile's white AND its own shadow; at full strength it stops being a badge ON the tile
         * and reads as a separate green bar under it, and at Figma's own 75% the green went
         * milky. 85% is Radhika's call and is what the prototype ships. The tail stop tracks
         * it: Success/600 at 38.5% of the layer, i.e. .385 x .85 = .32725.
         */
        val badgeGreen = h(
            0f to Color(0xFF027A48).copy(alpha = 0.85f),
            0.10f to Color(0xFF027A48).copy(alpha = 0.85f),
            0.233f to Color(0xFF039855).copy(alpha = 0.85f),
            0.839f to Color(0xFF039855).copy(alpha = 0.85f),
            1f to Color(0xFF039855).copy(alpha = 0.32725f)
        )
    }

    /**
     * Scrim behind the popup. 80% black — taken from the prototype's `rgba(0,0,0,.8)` and then
     * confirmed by sampling the rendered pixel (80% black over white decodes to exactly 51,51,51).
     * Do not re-tune it because a thumbnail looks light.
     */
    val scrim = Color(red = 0f, green = 0f, blue = 0f, alpha = 0.8f)
}
