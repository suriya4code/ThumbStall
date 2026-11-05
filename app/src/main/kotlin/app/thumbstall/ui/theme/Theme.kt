package app.thumbstall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Official ThumbStall olive palette (H8 logo spec)
val Olive600 = Color(0xFF5A6B2E)   // Icon tile, primary buttons
val Olive800 = Color(0xFF3F4B1F)   // Dark accent, pressed states
val Olive300 = Color(0xFFC5CC9E)   // Ring arc on dark
val Olive100 = Color(0xFFECEEDC)   // Soft fills, info cards
val Cream = Color(0xFFF1F2E8)      // Mark on olive, text on dark
val Paper = Color(0xFFF3F3EC)      // Light app background
val Ink = Color(0xFF181914)        // Primary text on light
val Muted = Color(0xFF5C6052)      // Secondary text on light
val Night = Color(0xFF13140F)      // Splash and pause background

// Aliases for pause screen
val PauseBackground = Night
val PauseTextPrimary = Cream
val PauseTextSecondary = Olive300

private val LightColors = lightColorScheme(
    primary = Olive600,
    onPrimary = Cream,
    primaryContainer = Olive600,
    onPrimaryContainer = Cream,
    secondary = Olive800,
    onSecondary = Cream,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Olive100,
    onSurfaceVariant = Muted,
)

private val DarkColors = darkColorScheme(
    primary = Olive300,
    onPrimary = Night,
    primaryContainer = Olive800,
    onPrimaryContainer = Cream,
    secondary = Olive300,
    onSecondary = Night,
    background = Night,
    onBackground = Cream,
    surface = Night,
    onSurface = Cream,
    surfaceVariant = Color(0xFF252A1C),
    onSurfaceVariant = Olive300,
)

private val PauseColors = darkColorScheme(
    primary = Cream,
    onPrimary = Night,
    background = Night,
    onBackground = Cream,
    surface = Night,
    onSurface = Cream,
    surfaceVariant = Color(0xFF252A1C),
    onSurfaceVariant = Olive300,
)

@Composable
fun ThumbStallTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun PauseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PauseColors,
        content = content
    )
}
