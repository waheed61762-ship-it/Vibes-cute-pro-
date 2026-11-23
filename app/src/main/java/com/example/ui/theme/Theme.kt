package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val PrismCutDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = NeonCoral,
    onSecondary = ObsidianBg,
    secondaryContainer = NeonCoralContainer,
    onSecondaryContainer = NeonCoral,
    tertiary = VioletGlow,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CarbonSurface,
    onSurface = TextPrimary,
    surfaceVariant = CarbonElevated,
    onSurfaceVariant = TextSecondary,
    outline = HairlineBorder,
    outlineVariant = CarbonTrack,
    error = NeonCoral,
    onError = ObsidianBg,
)

private val PrismCutShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PrismCutTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = PrismCutDarkColorScheme,
        typography = AppTypography,
        shapes = PrismCutShapes,
        content = content,
    )
}
