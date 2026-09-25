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
import androidx.compose.ui.text.TextStyle
import com.astrochat.insufficient.ui.InsufficientScreen
import com.astrochat.insufficient.ui.theme.Tokens

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
                // Inter for every Text on the screen, including the handful that set a raw
                // fontSize rather than a Tokens.Type style. Text() merges the style it is given
                // ON TOP of LocalTextStyle, so providing the family once here reaches all of
                // them and none of them has to repeat it.
                CompositionLocalProvider(
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
