package com.astrochat.insufficient.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astrochat.insufficient.R
import com.astrochat.insufficient.ui.theme.Tokens
import kotlinx.coroutines.delay

/**
 * The offer popup — Figma `4843:2823`. Shown once on every app load, then it dismisses itself.
 *
 * The badge is a sibling of the card, not a child of it: it overhangs the card's top edge by
 * half its height. Parented inside, the card's 16dp clip would cut it.
 *
 * Entrance and exit are deliberately asymmetric (400ms in on the springy curve, 250ms out) —
 * arriving is an event, leaving is just getting out of the way.
 */
@Composable
fun OfferPopup(
    visible: Boolean,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Tokens.Motion.popupInMs)),
        exit = fadeOut(tween(Tokens.Motion.popupOutMs)),
        modifier = modifier
    ) {
        val scrimInteraction = remember { MutableInteractionSource() }
        Box(
            Modifier
                .fillMaxSize()
                .background(Tokens.scrim)
                // Tapping the scrim dismisses. No ripple — a full-screen ripple is a flash.
                .clickable(interactionSource = scrimInteraction, indication = null, onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(tween(Tokens.Motion.popupInMs, easing = Tokens.Motion.spring), initialScale = 0.86f) +
                    fadeIn(tween(Tokens.Motion.popupInMs)),
                exit = scaleOut(tween(Tokens.Motion.popupOutMs), targetScale = 0.94f) +
                    fadeOut(tween(Tokens.Motion.popupOutMs))
            ) {
                Box(
                    Modifier.width(Tokens.Dimens.popupWidth),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        Modifier
                            .padding(top = Tokens.Dimens.popupBadge / 2)
                            .shadow(18.dp, RoundedCornerShape(Tokens.Dimens.popupRadius))
                            .clip(RoundedCornerShape(Tokens.Dimens.popupRadius))
                            .background(Tokens.Palette.white)
                            .padding(start = 4.dp, end = 4.dp, bottom = 24.dp, top = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // The title is not loose text on white — it is its own inset panel with
                        // Figma's popup fill (5150:15945) behind it, Success/300 at 40% running
                        // out to nothing left-to-right. That wash is what carries the badge's
                        // green down into the card; without it the seal looks stuck on.
                        Text(
                            text = title,
                            style = Tokens.Type.bodyLg,
                            fontWeight = FontWeight.Bold,
                            color = Tokens.Palette.gray800,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(Tokens.BandBrush.popupTitle)
                                // Top padding carries the badge's overhang: the seal hangs 26
                                // into this panel, so the words have to start below it.
                                .padding(start = 12.dp, end = 12.dp, top = 32.dp, bottom = 20.dp)
                        )
                        Text(
                            text = body,
                            style = Tokens.Type.bodyMd,
                            color = Tokens.Palette.gray600,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                    PopupBadge()
                }
            }
        }
    }
}

/**
 * The 52dp seal that caps the popup — `popup-badge.svg`, shipped as-is.
 *
 * It is one SHAPE, not a disc with something drawn inside it. This used to be a gradient circle
 * with a redrawn price tag on top, which reads as a generic badge; the asset is the scalloped
 * rosette, the same silhouette the band's seal uses, which is what ties the popup and the band
 * together as one offer.
 *
 * It shakes ONCE — `@keyframes wiggle`, 1.2s, 300ms after the popup lands, then it is still.
 * Not the band's looping `tagWiggle`: the band is trying to get noticed on a screen you are
 * already reading, while this has your whole attention the moment it appears, so a repeat would
 * just be fidgeting. The 300ms delay is what lets the card finish arriving first.
 */
@Composable
private fun PopupBadge() {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(Tokens.Motion.popupBadgeDelayMs.toLong())
        shake.animateTo(1f, tween(Tokens.Motion.popupBadgeWiggleMs, easing = Tokens.Motion.easeInOut))
    }
    Image(
        painter = painterResource(R.drawable.ic_popup_badge),
        contentDescription = null,
        modifier = Modifier
            .size(Tokens.Dimens.popupBadge)
            .graphicsLayer {
                rotationZ = stageValue(shake.value, BADGE_ANGLE)
                val s = stageValue(shake.value, BADGE_SCALE)
                scaleX = s
                scaleY = s
            }
    )
}

/** `@keyframes wiggle` — one pass, -14 / +10 / -5 and done. */
private val BADGE_ANGLE = listOf(
    0f to 0f, 0.25f to -14f, 0.50f to 10f, 0.75f to -5f, 1f to 0f
)

/** Only the first swing grows; the rest of the shake is rotation alone. */
private val BADGE_SCALE = listOf(
    0f to 1f, 0.25f to 1.1f, 0.50f to 1f, 1f to 1f
)
