package com.astrochat.insufficient.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import com.astrochat.insufficient.R

/**
 * Glyphs.
 *
 * These used to be hand-drawn Canvas paths approximating the prototype's SVGs, and that was the
 * single biggest reason the screen read as "not the design" — a redrawn wallet is a different
 * wallet, and nobody could say why it looked off. Everything that HAS a shipped asset is now the
 * shipped asset, converted into res/drawable path-for-path from the prototype's src/icons SVGs.
 *
 * Only [TickGlyph] is still drawn, because it has to animate along its own stroke and no
 * drawable can do that.
 */

/** Maps a coordinate in the 24x24 authoring grid onto the actual canvas. */
private fun DrawScope.p(x: Float, y: Float): Offset =
    Offset(x / 24f * size.width, y / 24f * size.height)

private fun DrawScope.u(v: Float): Float = v / 24f * size.minDimension

/** Wallet — the nav balance chip. */
@Composable
fun WalletGlyph(tint: Color, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_wallet),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
    )
}

/** Chevron used by See More, the summary row and the Pay with line. Rotate it at the call site. */
@Composable
fun ChevronGlyph(tint: Color, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_chevron),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
    )
}

/** Back arrow in the nav. */
@Composable
fun BackGlyph(tint: Color, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_back),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier
    )
}

/**
 * The selected-tile tick, drawn progressively so it can animate on.
 *
 * [progress] is 0..1 along the stroke. It is a real [PathMeasure] segment rather than a
 * scale or a fade: the tick has to look WRITTEN, and the short-leg-then-long-leg timing that
 * gives is the entire effect. Passing 1f draws the finished tick.
 *
 * The path is `tile-tick.svg` rebased onto the 24-grid: the shipped tick is a shallow, wide
 * check, not the near-45° one a from-memory tick always ends up being.
 */
@Composable
fun TickGlyph(tint: Color, progress: Float, modifier: Modifier = Modifier) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val full = Path().apply {
                moveTo(p(2.3f, 12.3f).x, p(2.3f, 12.3f).y)
                lineTo(p(8.6f, 18.4f).x, p(8.6f, 18.4f).y)
                lineTo(p(21.7f, 5.4f).x, p(21.7f, 5.4f).y)
            }
            val measure = PathMeasure().apply { setPath(full, false) }
            val drawn = Path()
            measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), drawn, true)
            drawPath(
                drawn, tint,
                style = Stroke(width = u(2.6f), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}
