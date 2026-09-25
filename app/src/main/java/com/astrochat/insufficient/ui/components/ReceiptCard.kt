package com.astrochat.insufficient.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.astrochat.insufficient.ui.theme.Tokens

/** Stroke colour of the receipt outline. Deliberately NOT gray300 — it is a hair cooler. */
private val ReceiptStroke = Color(0xFFC6CCD5)

/**
 * The receipt card: rounded top corners, straight sides, and a bottom edge that bows down in
 * the middle like a torn stub, with a dashed outline and two punched notches.
 *
 * The shape is drawn rather than composed out of clips and borders, because the bow and the
 * dashes have to be the SAME path — a rounded-rect background with a separate bowed overlay
 * leaves a seam where the two meet at the sides.
 *
 * Geometry, so it can be re-derived rather than re-guessed:
 *   - top corners use [Tokens.Dimens.cardRadius] (32)
 *   - the sides run straight down to `H - bowDepth`; only the centre dips further, so making
 *     the bow deeper does NOT raise the card's shoulders
 *   - the two control handles are `bowDepth` vertically and 18% of the half-width horizontally,
 *     which is what stops the curve pinching at the sides
 *
 * [notchFromTop] is where the pair of bite marks sit. It must stay above `H - bowDepth` or a
 * notch lands on the curve and the bite stops reading as a straight tear line.
 */
@Composable
fun ReceiptCard(
    modifier: Modifier = Modifier,
    notchFromTop: Dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val o = 0.5f * density                  // half the 1dp stroke, kept inside the box
            val w = size.width - o
            val h = size.height - o
            val r = Tokens.Dimens.cardRadius.toPx()
            val drop = Tokens.Dimens.receiptBowDepth.toPx()
            val mid = (w + o) / 2f
            val b = h - drop
            val hh = (w - mid) * 0.18f

            val card = Path().apply {
                moveTo(o, r)
                arcTo(Rect(o, o, o + 2 * r, o + 2 * r), 180f, 90f, false)
                lineTo(w - r, o)
                arcTo(Rect(w - 2 * r, o, w, o + 2 * r), 270f, 90f, false)
                lineTo(w, b)
                cubicTo(w, b + drop, w - hh, h, mid, h)
                cubicTo(o + hh, h, o, b + drop, o, b)
                close()
            }

            val dashes = PathEffect.dashPathEffect(
                floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f
            )
            drawPath(card, Tokens.Palette.white)
            drawPath(
                card, ReceiptStroke,
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Butt, pathEffect = dashes)
            )

            // The notches. Each is a 20dp disc centred ON the card edge, so only its inner half
            // shows: white fill to punch the bite out, then a dashed ring to outline it. The
            // clip is what keeps the outer half from spilling onto the band behind the card.
            val notchR = Tokens.Dimens.notchSize.toPx() / 2f
            val cy = notchFromTop.toPx() + notchR
            clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height) {
                listOf(Offset(o, cy), Offset(w, cy)).forEach { c ->
                    drawCircle(Tokens.Palette.white, notchR, c)
                    drawCircle(
                        ReceiptStroke, notchR, c,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = dashes)
                    )
                }
            }
        }
        content()
    }
}
