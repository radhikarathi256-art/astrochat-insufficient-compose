package com.astrochat.insufficient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
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
