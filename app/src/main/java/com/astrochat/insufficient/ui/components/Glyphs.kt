package com.astrochat.insufficient.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Every glyph on this screen is drawn here as a real path rather than shipped as a drawable.
 *
 * Two reasons, both of which matter for the hand-off:
 *
 *  1. They take a `tint` at the call site, so one glyph serves all six band variants. A PNG
 *     would need six exports and would still be wrong the next time a ramp moves.
 *  2. They are animatable. The chevron flips, the tick draws itself on, the tag wiggles —
 *     none of that is possible with a bitmap, and all of it is in the prototype.
 *
 * All of them are authored against a 24 x 24 viewport and scaled to whatever box they are
 * given, so `Modifier.size(...)` is the only thing that changes between call sites.
 */

/** Maps a coordinate in the 24x24 authoring grid onto the actual canvas. */
private fun DrawScope.p(x: Float, y: Float): Offset =
    Offset(x / 24f * size.width, y / 24f * size.height)

private fun DrawScope.u(v: Float): Float = v / 24f * size.minDimension

/**
 * Price tag — the leading glyph on every band except Not enough. A rounded pentagon with a
 * punched hole, the same shape Figma uses on the Bonus / Offer / Coupon variants.
 *
 * The hole is drawn as a second filled circle in [holeColor] rather than an even-odd
 * subtraction: at 26dp the subtraction rounds to a ragged edge, a redrawn dot does not.
 */
@Composable
fun TagGlyph(tint: Color, holeColor: Color, modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val body = Path().apply {
                moveTo(p(3f, 11.2f).x, p(3f, 11.2f).y)
                lineTo(p(3f, 4.6f).x, p(3f, 4.6f).y)
                // The tag's corner, rounded so it doesn't read as a knife point.
                quadraticTo(
                    p(3f, 3f).x, p(3f, 3f).y,
                    p(4.6f, 3f).x, p(4.6f, 3f).y
                )
                lineTo(p(11.2f, 3f).x, p(11.2f, 3f).y)
                lineTo(p(21f, 12.8f).x, p(21f, 12.8f).y)
                // Rounded nose where the tag meets the string.
                quadraticTo(
                    p(21.8f, 13.6f).x, p(21.8f, 13.6f).y,
                    p(21f, 14.4f).x, p(21f, 14.4f).y
                )
                lineTo(p(14.4f, 21f).x, p(14.4f, 21f).y)
                quadraticTo(
                    p(13.6f, 21.8f).x, p(13.6f, 21.8f).y,
                    p(12.8f, 21f).x, p(12.8f, 21f).y
                )
                close()
            }
            drawPath(body, tint)
            drawCircle(holeColor, radius = u(2.1f), center = p(7.6f, 7.6f))
        }
    }
}

/**
 * Wallet — the Not enough band's glyph, because a price tag reads as a promotion and this
 * band is the opposite of one. Body, flap, and the clasp dot.
 */
@Composable
fun WalletGlyph(tint: Color, modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val r = u(2.6f)
            drawRoundRect(
                color = tint,
                topLeft = p(2.5f, 6f),
                size = Size(p(19f, 0f).x - p(2.5f, 0f).x, p(0f, 19.5f).y - p(0f, 6f).y),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r)
            )
            // The card pocket, punched out in white so the wallet reads as open.
            drawRoundRect(
                color = Color.White,
                topLeft = p(13.5f, 10.6f),
                size = Size(p(8f, 0f).x - p(0f, 0f).x, p(0f, 4.4f).y - p(0f, 0f).y),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(u(1.4f), u(1.4f))
            )
            drawCircle(tint, radius = u(1.1f), center = p(16.8f, 12.8f))
        }
    }
}

/**
 * Chevron used by See More / See Less. It is a stroke, not a filled arrow, so [flipped]
 * can be animated as a rotation on the caller's side without the shape distorting.
 */
@Composable
fun ChevronGlyph(tint: Color, modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(p(6.5f, 9.5f).x, p(6.5f, 9.5f).y)
                lineTo(p(12f, 15f).x, p(12f, 15f).y)
                lineTo(p(17.5f, 9.5f).x, p(17.5f, 9.5f).y)
            }
            drawPath(
                path, tint,
                style = Stroke(width = u(2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

/**
 * The selected-tile tick, drawn progressively so it can animate on.
 *
 * [progress] is 0..1 along the stroke. It is a real [PathMeasure] segment rather than a
 * scale or a fade: the tick has to look WRITTEN, and the short-leg-then-long-leg timing that
 * gives is the entire effect. Passing 1f draws the finished tick.
 */
@Composable
fun TickGlyph(tint: Color, progress: Float, modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val full = Path().apply {
                moveTo(p(6f, 12.4f).x, p(6f, 12.4f).y)
                lineTo(p(10.3f, 16.6f).x, p(10.3f, 16.6f).y)
                lineTo(p(18f, 8.4f).x, p(18f, 8.4f).y)
            }
            val measure = PathMeasure().apply { setPath(full, false) }
            val drawn = Path()
            measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), drawn, true)
            drawPath(
                drawn, tint,
                style = Stroke(width = u(2.4f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
