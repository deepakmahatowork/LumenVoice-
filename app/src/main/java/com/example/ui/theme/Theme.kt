package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LumenDarkColorScheme = darkColorScheme(
    primary = SolarAmber,
    onPrimary = OnSolarAmber,
    primaryContainer = SolarAmberContainer,
    onPrimaryContainer = OnSolarAmberContainer,
    secondary = AcousticCyan,
    onSecondary = OnAcousticCyan,
    secondaryContainer = AcousticCyanContainer,
    onSecondaryContainer = OnAcousticCyanContainer,
    tertiary = LumenGold,
    background = ObsidianBackground,
    onBackground = TextPrimaryLight,
    surface = MidnightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = TextSecondaryMuted,
    outline = SlateBorder,
    error = AlertCoral,
    errorContainer = AlertCoralContainer,
    onErrorContainer = TextPrimaryLight
)

val LumenShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LumenDarkColorScheme,
        typography = Typography,
        shapes = LumenShapes,
        content = content
    )
}
