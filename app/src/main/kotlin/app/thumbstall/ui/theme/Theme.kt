package app.thumbstall.ui.theme

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
fun ThumbStallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
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
