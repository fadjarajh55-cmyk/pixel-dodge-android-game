package com.pixeldodge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkPixelPalette = darkColorScheme(
    primary = Color(0xFF7BE0FF),
    secondary = Color(0xFF8EFEAC),
    tertiary = Color(0xFFFFCD6B),
    background = Color(0xFF090B1A),
    surface = Color(0xFF10162C),
    onPrimary = Color(0xFF06111A),
    onSecondary = Color(0xFF04160C),
    onBackground = Color(0xFFEAF6FF),
    onSurface = Color(0xFFEAF6FF),
)

@Composable
fun PixelDodgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkPixelPalette,
        content = content,
    )
}
