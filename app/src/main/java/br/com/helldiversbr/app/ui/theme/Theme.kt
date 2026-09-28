package br.com.helldiversbr.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta extraída do CSS do site (style.css / guerra.css / manifest).
object HD {
    val Bg = Color(0xFF090909)
    val Surface = Color(0xFF141414)
    val SurfaceHigh = Color(0xFF1E1E1E)
    val Yellow = Color(0xFFD7D52C)
    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFB5B5B5)
    val Green = Color(0xFF58DC7C)   // ordem concluída
    val Red = Color(0xFFFF4D4D)     // ordem falhou
    val Gold = Color(0xFFE0B84D)    // ordem ativa
    val Border = Color(0xFF2E2E2E)
}

private val Scheme = darkColorScheme(
    primary = HD.Yellow,
    onPrimary = Color.Black,
    background = HD.Bg,
    onBackground = HD.Text,
    surface = HD.Surface,
    onSurface = HD.Text,
    surfaceVariant = HD.SurfaceHigh,
    onSurfaceVariant = HD.TextDim,
    outline = HD.Border,
    error = HD.Red,
)

@Composable
fun HelldiversTheme(content: @Composable () -> Unit) {
    // O app é sempre escuro, como o site.
    MaterialTheme(colorScheme = Scheme, content = content)
}
