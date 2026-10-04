package com.neoqubix.devajit.h2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = H2Maroon,
    onPrimary = H2Cream,
    primaryContainer = Color(0xFFFFE1DC),
    onPrimaryContainer = H2MaroonDark,
    secondary = H2Yellow,
    onSecondary = H2Ink,
    secondaryContainer = Color(0xFFFFF0B3),
    onSecondaryContainer = Color(0xFF4A3800),
    tertiary = H2Red,
    onTertiary = Color.White,
    background = H2Cream,
    onBackground = H2Ink,
    surface = Color.White,
    onSurface = H2Ink,
    surfaceVariant = Color(0xFFF7EDE4),
    onSurfaceVariant = Color(0xFF6B5A55),
    surfaceContainer = Color(0xFFFFF3E6),
    outline = Color(0xFFB8A39C)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4A9),
    onPrimary = H2MaroonDark,
    primaryContainer = H2Maroon,
    onPrimaryContainer = H2Cream,
    secondary = H2Yellow,
    onSecondary = H2Ink,
    tertiary = Color(0xFFFF8A80)
)

// Fixed brand colours (no dynamic colour) so the app always looks like H2 and money colours read the same
@Composable
fun H2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
