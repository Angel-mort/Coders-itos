package com.bacheatec.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RoadTeal,
    onPrimary = Asphalt,
    primaryContainer = RoadTealDark,
    onPrimaryContainer = Mist,
    secondary = RoadAmber,
    onSecondary = Asphalt,
    secondaryContainer = Color(0xFF92400E),
    onSecondaryContainer = Asphalt,
    tertiary = MistMuted,
    onTertiary = Asphalt,
    background = Asphalt,
    onBackground = Mist,
    surface = AsphaltSurface,
    onSurface = Mist,
    surfaceVariant = Color(0xFF2A3441),
    onSurfaceVariant = MistMuted,
    outline = Color(0xFF8A96A6),
)

@Composable
fun BacheaTecTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
