package br.com.helldiversbr.app

import br.com.helldiversbr.app.ui.theme.*
import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class ThemeModesTest {
    @Test fun allThreeThemesAreReachableFromTheDrawer() {
        val modes=generateSequence(HdThemeMode.DEFAULT) { it.next() }.take(4).toList()
        assertEquals(listOf(HdThemeMode.DEFAULT,HdThemeMode.MERIDIA,HdThemeMode.NIGHT,HdThemeMode.DEFAULT),modes)
    }
    @Test fun meridianPaletteIsPreserved() {
        val old=HD.mode
        try { HD.mode=HdThemeMode.MERIDIA
            assertEquals(Color(0xFF080711),HD.Bg)
            assertEquals(Color(0xFF11101A),HD.Surface)
            assertEquals(Color(0xFF8F7CFF),HD.Yellow)
        } finally { HD.mode=old }
    }
    @Test fun nightModeHasCharcoalBackgroundWithoutChangingFactionColors() {
        val old=HD.mode
        try { HD.mode=HdThemeMode.NIGHT
            assertEquals(Color(0xFF15181D),HD.Bg)
            assertEquals(Color(0xFF20242B),HD.Surface)
            assertEquals(Color(0xFFFF4242),HD.AutomatonRed)
            assertEquals(Color(0xFF8B3FD6),HD.IlluminatePurple)
        } finally { HD.mode=old }
    }
}
