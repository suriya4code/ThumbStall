package app.thumbstall.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette from design screens
val Cream = Color(0xFFF2F1E8)
val Olive = Color(0xFF4A5D23)
val OliveDark = Color(0xFF3A4A1C)
val PauseBackground = Color(0xFF161A10)
val PauseTextPrimary = Color(0xFFF2F1E8)
val PauseTextSecondary = Color(0xFFB8B8A8)

private val LightColors = lightColorScheme(
    primary = Olive,
    onPrimary = Cream,
    primaryContainer = Olive,
    onPrimaryContainer = Cream,
    secondary = OliveDark,
    onSecondary = Cream,
    background = Cream,
    onBackground = Color(0xFF1A1C18),
    surface = Cream,
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFE4E3DA),
    onSurfaceVariant = Color(0xFF45483E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9AAF6C),  // Lighter olive for dark mode
    onPrimary = Color(0xFF1A1C18),
    primaryContainer = OliveDark,
    onPrimaryContainer = Cream,
    secondary = Color(0xFF7A8F5C),
    onSecondary = Color(0xFF1A1C18),
    background = Color(0xFF1A1C18),
    onBackground = Color(0xFFE4E3DA),
    surface = Color(0xFF1A1C18),
    onSurface = Color(0xFFE4E3DA),
    surfaceVariant = Color(0xFF2A2D24),
    onSurfaceVariant = Color(0xFFB8B8A8),
)

private val PauseColors = darkColorScheme(
    primary = Cream,
    onPrimary = PauseBackground,
    background = PauseBackground,
    onBackground = PauseTextPrimary,
    surface = PauseBackground,
    onSurface = PauseTextPrimary,
    surfaceVariant = Color(0xFF252A1C),
    onSurfaceVariant = PauseTextSecondary,
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
