package com.ginference.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val CyberpunkBackground = Color(0xFF0A0E27)
val MatrixGreen = Color(0xFF00FF41)
val CyanNeon = Color(0xFF00D9FF)
val HotPink = Color(0xFFFF006E)
val NeonYellow = Color(0xFFFFD60A)
val NeonRed = Color(0xFFFF0055)

private val CyberpunkColorScheme = darkColorScheme(
    primary = MatrixGreen,
    onPrimary = CyberpunkBackground,
    secondary = CyanNeon,
    onSecondary = CyberpunkBackground,
    tertiary = HotPink,
    background = CyberpunkBackground,
    onBackground = MatrixGreen,
    surface = CyberpunkBackground,
    onSurface = MatrixGreen,
    error = NeonRed,
    onError = CyberpunkBackground
)

object CyberpunkTypography {
    val terminal = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        color = MatrixGreen
    )

    val terminalLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 18.sp,
        color = MatrixGreen
    )

    val terminalSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = MatrixGreen
    )

    val metric = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        color = CyanNeon
    )
}

@Composable
fun CyberpunkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CyberpunkColorScheme,
        content = content
    )
}
