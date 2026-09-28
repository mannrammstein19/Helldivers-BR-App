package br.com.helldiversbr.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Paleta oficial usada pelo HELLDIVERS-BR. */
object HD {
    val Bg = Color(0xFF090909)
    val BgDeep = Color(0xFF050607)
    val Surface = Color(0xFF141414)
    val SurfaceHigh = Color(0xFF202020)
    val SurfaceSoft = Color(0xFF191919)
    val Yellow = Color(0xFFD7D52C)
    val YellowBright = Color(0xFFFFE800)
    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFB5B5B5)
    val TextMuted = Color(0xFF7E858C)
    val Green = Color(0xFF55DB7D)
    val Red = Color(0xFFFF4242)
    val Gold = Color(0xFFE0B84D)
    val Border = Color(0xFF30343A)
    val BorderSoft = Color(0xFF24282D)
    val SignalBlue = Color(0xFF00CFFF)
    val DefenseBlue = Color(0xFF3D9DFF)
    val TerminidOrange = Color(0xFFFF9900)
    val AutomatonRed = Color(0xFFFF4242)
    val IlluminatePurple = Color(0xFF8B3FD6)
    val Orange = Color(0xFFFF7B00)
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
    MaterialTheme(colorScheme = Scheme, content = content)
}
