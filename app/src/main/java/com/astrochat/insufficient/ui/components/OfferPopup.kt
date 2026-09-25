package com.astrochat.insufficient.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.astrochat.insufficient.R
import com.astrochat.insufficient.ui.theme.Tokens

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
                            .padding(start = 4.dp, end = 4.dp, bottom = 24.dp, top = Tokens.Dimens.popupBadge / 2 + 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = title,
                            style = Tokens.Type.bodyLg,
                            fontWeight = FontWeight.Bold,
                            color = Tokens.Palette.gray800,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = body,
                            style = Tokens.Type.bodyMd,
                            color = Tokens.Palette.gray600,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
 */
@Composable
private fun PopupBadge() {
    Image(
        painter = painterResource(R.drawable.ic_popup_badge),
        contentDescription = null,
        modifier = Modifier.size(Tokens.Dimens.popupBadge)
    )
}
