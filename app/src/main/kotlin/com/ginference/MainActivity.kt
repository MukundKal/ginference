package com.ginference

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ginference.ui.theme.CyberpunkBackground
import com.ginference.ui.theme.CyberpunkTheme
import com.ginference.ui.theme.CyberpunkTypography
import com.ginference.ui.theme.MatrixGreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CyberpunkTheme {
                GinferenceApp()
            }
        }
    }
}

@Composable
fun GinferenceApp() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberpunkBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "GINFERENCE INITIALIZING...",
            color = MatrixGreen,
            style = CyberpunkTypography.terminal
        )
    }
}
