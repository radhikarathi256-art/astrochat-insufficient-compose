package com.astrochat.insufficient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import com.astrochat.insufficient.ui.InsufficientScreen
import com.astrochat.insufficient.ui.theme.Tokens

/** The width the whole design is drawn at. Every dp and sp in this module is a number off a 360 mock. */
private const val MOCK_WIDTH_DP = 360f

/**
 * Single-screen host. There is no navigation and no ViewModel by design — this module is a
 * reference build of one surface, meant to be read and lifted, not extended.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // The prototype is a 360px-wide frame under `transform: scale(viewport / 360)`,
                // so on a 411dp phone every box AND every glyph in it is 14% bigger than the
                // number written in the spec. Compose has no such transform and dp/sp are
                // absolute, so laying the same 360 numbers out natively renders all the type at
                // 88% of the prototype's while the boxes — widened with fillMaxWidth fractions —
                // stay right. Type floating small inside correct boxes is what made this read as
                // a different screen rather than a slightly smaller one.
                //
                // Scaling LocalDensity is the exact analogue of that CSS transform: both dp->px
                // and sp->px run through Density.density, so ONE provider scales the whole tree
                // uniformly and the 360 spec lands proportionally identical at any screen width.
                // It also means every value in Tokens stays the plain mock number a developer can
                // read off Figma, which is the point of this module.
                val base = LocalDensity.current
                val scale = LocalConfiguration.current.screenWidthDp / MOCK_WIDTH_DP
                CompositionLocalProvider(
                    LocalDensity provides Density(base.density * scale, base.fontScale),
                    // Inter for every Text on the screen, including the handful that set a raw
                    // fontSize rather than a Tokens.Type style. Text() merges the style it is
                    // given ON TOP of LocalTextStyle, so providing the family once here reaches
                    // all of them and none of them has to repeat it.
                    LocalTextStyle provides TextStyle(fontFamily = Tokens.Type.family)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Tokens.Palette.appBackground)
                            .systemBarsPadding()
                    ) {
                        InsufficientScreen()
                    }
                }
            }
        }
    }
}
