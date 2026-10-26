package app.omnicast.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Palette {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16171A)
    val SurfaceHigh = Color(0xFF202226)
    val Titanium = Color(0xFF8E9399)
    val TextPrimary = Color(0xFFF2F3F5)
    val Accent = Color(0xFFC59B27)
    val OnAccent = Color(0xFF16110A)

    val Divider = Color(0xFF2A2C31)
    val Error = Color(0xFFE5645A)
}

private val OmniScheme = darkColorScheme(
    primary = Palette.Accent,
    onPrimary = Palette.OnAccent,
    primaryContainer = Palette.Accent,
    onPrimaryContainer = Palette.OnAccent,
    secondary = Palette.Titanium,
    onSecondary = Palette.Background,
    background = Palette.Background,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.Titanium,
    surfaceContainerLowest = Palette.Background,
    surfaceContainerLow = Palette.Background,
    surfaceContainer = Palette.Surface,
    surfaceContainerHigh = Palette.SurfaceHigh,
    surfaceContainerHighest = Palette.SurfaceHigh,
    outline = Palette.Titanium,
    outlineVariant = Palette.Divider,
    error = Palette.Error,
)

@Composable
fun OmniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OmniScheme, content = content)
}
