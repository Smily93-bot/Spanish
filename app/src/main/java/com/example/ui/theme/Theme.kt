package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val AdventureColorScheme = lightColorScheme(
    primary = ExplorerBlue,
    onPrimary = StarWhite,
    secondary = SolarAmber,
    onSecondary = StarWhite,
    tertiary = NebulaPurple,
    background = AdventureBg,
    onBackground = TextPrimary,
    surface = AdventureSurface,
    onSurface = TextPrimary,
    surfaceVariant = AdventureSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = AdventureCardBorder,
    error = MeteorRed
)

@Composable
fun SpanishBlasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AdventureColorScheme,
        typography = Typography,
        content = content
    )
}
