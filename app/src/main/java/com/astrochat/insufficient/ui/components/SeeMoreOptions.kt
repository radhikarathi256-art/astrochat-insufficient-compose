package com.astrochat.insufficient.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.astrochat.insufficient.ui.theme.Tokens

/**
 * "See More Options" / "See Less Options" — Figma `4937:3559`.
 *
 * Two details that are easy to lose:
 *
 *  1. The border is DRAWN, not a stroke on a shape, because it has to match the receipt
 *     outline's 2-2 dashes exactly. The two sit a few dp apart and any mismatch is visible.
 *     0.8dp here vs the receipt's 1dp is the Figma value, not an accident.
 *  2. It is ONE chevron that rotates, not two labels swapping icons. Rotating it through the
 *     label cross-fade is what makes the control read as a single thing flipping over.
 */
@Composable
fun SeeMoreOptions(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val angle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(400, easing = Tokens.Motion.easeOut),
        label = "chevron"
    )

    Row(
        modifier
            .clip(RoundedCornerShape(Tokens.Dimens.pillRadius))
            .clickable(onClick = onToggle)
            .drawBehind {
                drawRoundRect(
                    color = Tokens.Palette.gray300,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(
                        Tokens.Dimens.pillRadius.toPx(), Tokens.Dimens.pillRadius.toPx()
                    ),
                    style = Stroke(
                        width = 0.8.dp.toPx(),
                        cap = StrokeCap.Butt,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(2.dp.toPx(), 2.dp.toPx()), 0f
                        )
                    )
                )
            }
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (expanded) "See Less Options" else "See More Options",
            style = Tokens.Type.bodyXs,
            fontWeight = FontWeight.Normal,
            color = Tokens.Palette.gray500
        )
        Spacer(Modifier.width(6.dp))
        ChevronGlyph(
            tint = Tokens.Palette.gray500,
            modifier = Modifier.size(18.dp).rotate(angle)
        )
    }
}
