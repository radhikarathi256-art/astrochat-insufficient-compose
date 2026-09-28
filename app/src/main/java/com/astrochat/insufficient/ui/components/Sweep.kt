package com.astrochat.insufficient.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * The prototype's light sweep — one bar of light travelling across a surface. Shared because two
 * places use it and they have to slant the same way: the bonus badges (`chipSweep`) and the
 * coupon band (`.sweep`).
 *
 * The bar is DIAGONAL, and that is the whole effect. Two things make it so, and the CSS applies
 * both:
 *
 *  - `skewX(-18deg)` leans the bar's edges, so it is a parallelogram, not a rectangle. Its top
 *    corners sit [SKEW_TAN] x half-height to the RIGHT of its bottom corners.
 *  - `linear-gradient(100deg, …)` tilts the fade itself 10 degrees off horizontal, so the soft
 *    edges run parallel to the slanted sides instead of cutting across them.
 *
 * Drawn upright — which is what this was — the bar reads as a skeleton-loader shimmer sliding
 * sideways. Slanted, it reads as light glancing off the surface, which is the point.
 *
 * The gradient is built here rather than held as a token because its start and end have to be
 * REAL coordinates on the bar. A `Brush.horizontalGradient` with no explicit bounds resolves
 * against whatever is being drawn into, so on a path it would stretch across the whole component
 * and the bar would come out flat.
 *
 * [left] is the bar's left edge measured at its vertical centre, because the skew pivots there.
 * [height] defaults to the whole surface; the band passes only the part of itself that the
 * receipt card does not cover.
 */
internal fun DrawScope.drawSweepBar(
    left: Float,
    width: Float,
    stops: List<Color>,
    height: Float = size.height
) {
    val h = height
    val lean = SKEW_TAN * h / 2f
    val bar = Path().apply {
        moveTo(left + lean, 0f)
        lineTo(left + width + lean, 0f)
        lineTo(left + width - lean, h)
        lineTo(left - lean, h)
        close()
    }
    val cx = left + width / 2f
    val cy = h / 2f
    // Length of the gradient line across the bar's own box, so the middle stop lands on the
    // middle of the bar however wide it is.
    val len = width * GRADIENT_DX + h * GRADIENT_DY
    drawPath(
        bar,
        Brush.linearGradient(
            colors = stops,
            start = Offset(cx - GRADIENT_DX * len / 2f, cy - GRADIENT_DY * len / 2f),
            end = Offset(cx + GRADIENT_DX * len / 2f, cy + GRADIENT_DY * len / 2f)
        )
    )
}

/** `skewX(-18deg)`. */
private const val SKEW_TAN = 0.3249f

/** `100deg` as a direction vector, y pointing down: (sin 100, -cos 100). */
private const val GRADIENT_DX = 0.9848f
private const val GRADIENT_DY = 0.1736f
