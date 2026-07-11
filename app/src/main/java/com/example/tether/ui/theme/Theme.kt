package com.example.tether.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CalmLightColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateSecondaryContainer,
    onSecondaryContainer = SlateOnSecondaryContainer,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnBackgroundLight,
    onSurface = OnBackgroundLight,
    error = AlertRed,
    onError = AlertOnRed,
    errorContainer = AlertContainer,
    onErrorContainer = AlertOnContainer
)

@Composable
fun TetherTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CalmLightColorScheme,
        typography = Typography,
        content = content
    )
}