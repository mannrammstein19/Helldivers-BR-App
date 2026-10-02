package br.com.helldiversbr.app.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class HdThemeMode { DEFAULT, MERIDIA, NIGHT;
    val label: String get() = when (this) {
        DEFAULT -> "Padrão HELLDIVERS-BR"
        MERIDIA -> "Meridian"
        NIGHT -> "Modo noturno"
    }
    fun next(): HdThemeMode = entries[(ordinal + 1) % entries.size]
}

object ThemePreferences {
    private const val PREFS = "helldivers_br_prefs"
    private const val KEY_THEME = "theme_mode"

    fun load(context: Context): HdThemeMode {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, HdThemeMode.DEFAULT.name)
        return runCatching { HdThemeMode.valueOf(raw ?: HdThemeMode.DEFAULT.name) }
            .getOrDefault(HdThemeMode.DEFAULT)
    }

    fun save(context: Context, mode: HdThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, mode.name)
            .apply()
    }
}

/** Paleta compartilhada pelo HELLDIVERS-BR. O tema Meridia troca o cromado sem alterar cores de facção. */
object HD {
    @Volatile
    var mode: HdThemeMode = HdThemeMode.DEFAULT

    val Bg: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFF15181D) else if (mode == HdThemeMode.MERIDIA) Color(0xFF080711) else Color(0xFF090909)
    val BgDeep: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFF111419) else if (mode == HdThemeMode.MERIDIA) Color(0xFF05040B) else Color(0xFF050607)
    val Surface: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFF20242B) else if (mode == HdThemeMode.MERIDIA) Color(0xFF11101A) else Color(0xFF141414)
    val SurfaceHigh: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFF2B3039) else if (mode == HdThemeMode.MERIDIA) Color(0xFF1C1928) else Color(0xFF202020)
    val SurfaceSoft: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFF252A32) else if (mode == HdThemeMode.MERIDIA) Color(0xFF171420) else Color(0xFF191919)

    val Yellow: Color get() = if (mode == HdThemeMode.MERIDIA) Color(0xFF8F7CFF) else Color(0xFFD7D52C)
    val YellowBright: Color get() = if (mode == HdThemeMode.MERIDIA) Color(0xFFB7AAFF) else Color(0xFFFFE800)

    val Text = Color(0xFFFFFFFF)
    val TextDim: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFFBFC5CF) else if (mode == HdThemeMode.MERIDIA) Color(0xFFC7C1DA) else Color(0xFFB5B5B5)
    val TextMuted: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFF9CA6B3) else if (mode == HdThemeMode.MERIDIA) Color(0xFF8E879F) else Color(0xFF7E858C)
    val Green = Color(0xFF55DB7D)
    val Red = Color(0xFFFF4242)
    val Gold = Color(0xFFE0B84D)
    val Border: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFF414955) else if (mode == HdThemeMode.MERIDIA) Color(0xFF393247) else Color(0xFF30343A)
    val BorderSoft: Color get() = if (mode == HdThemeMode.NIGHT) Color(0xFF343B46) else if (mode == HdThemeMode.MERIDIA) Color(0xFF282232) else Color(0xFF24282D)
    val SignalBlue = Color(0xFF00CFFF)
    val DefenseBlue = Color(0xFF3D9DFF)
    val TerminidOrange = Color(0xFFFF9900)
    val AutomatonRed = Color(0xFFFF4242)
    val IlluminatePurple = Color(0xFF8B3FD6)
    val Orange = Color(0xFFFF7B00)
}

@Composable
fun HelldiversTheme(mode: HdThemeMode = HdThemeMode.DEFAULT, content: @Composable () -> Unit) {
    HD.mode = mode
    val scheme = darkColorScheme(
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
    MaterialTheme(colorScheme = scheme, content = content)
}
