package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ImmersivePurpleAccent,
    onPrimary = ImmersiveOnPurple,
    secondary = ImmersivePurpleContainer,
    onSecondary = ImmersiveOnPurpleContainer,
    background = ImmersiveDarkBackground,
    onBackground = ImmersiveTextPrimaryDark,
    surface = ImmersiveDarkSurface,
    onSurface = ImmersiveTextPrimaryDark,
    surfaceVariant = ImmersiveDarkCard,
    onSurfaceVariant = ImmersiveTextSecondaryDark,
    outline = ImmersiveDarkBorder,
    outlineVariant = ImmersiveDarkLabel
)

private val LightColorScheme = lightColorScheme(
    primary = ImmersiveLightPurpleAccent,
    onPrimary = ImmersiveLightOnPurple,
    secondary = ImmersiveLightPurpleContainer,
    onSecondary = ImmersiveLightOnPurpleContainer,
    background = ImmersiveLightBackground,
    onBackground = ImmersiveTextPrimaryLight,
    surface = ImmersiveLightSurface,
    onSurface = ImmersiveTextPrimaryLight,
    surfaceVariant = ImmersiveLightCard,
    onSurfaceVariant = ImmersiveTextSecondaryLight,
    outline = ImmersiveLightBorder,
    outlineVariant = ImmersiveLightLabel
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
