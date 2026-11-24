package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VitroColorScheme = darkColorScheme(
    primary = VitroCyanAccent,
    onPrimary = VitroObsidian,
    primaryContainer = VitroSapphire,
    onPrimaryContainer = VitroObsidian,
    secondary = VitroIndigo,
    onSecondary = VitroObsidian,
    tertiary = VitroMint,
    onTertiary = VitroObsidian,
    background = VitroObsidian,
    onBackground = VitroTextPrimary,
    surface = VitroDeepSurface,
    onSurface = VitroTextPrimary,
    surfaceVariant = VitroCardSurface,
    onSurfaceVariant = VitroTextSecondary,
    outline = VitroGlassBorderDim
)

@Composable
fun VitroLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VitroColorScheme,
        typography = VitroTypography,
        content = content
    )
}
