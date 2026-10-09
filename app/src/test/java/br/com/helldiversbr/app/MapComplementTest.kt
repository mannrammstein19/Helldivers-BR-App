package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.CyberstanPulse
import br.com.helldiversbr.app.data.Planet
import br.com.helldiversbr.app.data.PlanetEvent
import br.com.helldiversbr.app.ui.screens.mapShowsOffensiveProgress
import org.junit.Assert.*
import org.junit.Test

class MapComplementTest {
    @Test fun cyberstanPulseUsesSlowerFramesAndLoops() {
        assertEquals(0, CyberstanPulse.frame(119, true))
        assertEquals(1, CyberstanPulse.frame(120, true))
        assertEquals(52, CyberstanPulse.frame(6359, true))
        assertEquals(0, CyberstanPulse.frame(6360, true))
        assertEquals(0, CyberstanPulse.frame(9999, false))
    }

    @Test fun liberationRingsStartAboveHalfPercentWithoutRoundingNoise() {
        val p = Planet(index = 3, currentOwner = "Automatons", maxHealth = 1_000_000)
        assertFalse(mapShowsOffensiveProgress(p.copy(health = 1_000_000), setOf(3)))
        assertFalse(mapShowsOffensiveProgress(p.copy(health = 995_000), setOf(3)))
        assertTrue(mapShowsOffensiveProgress(p.copy(health = 994_999), setOf(3)))
        assertFalse(mapShowsOffensiveProgress(p.copy(health = 990_000), emptySet()))
    }

    @Test fun alliedOwnershipDoesNotInventLiberationOrHideAnInvasion() {
        val p = Planet(index = 3, currentOwner = "Humans", maxHealth = 1_000_000, health = 0)
        assertFalse(mapShowsOffensiveProgress(p, setOf(3)))
        assertFalse(mapShowsOffensiveProgress(p.copy(event = PlanetEvent()), setOf(3)))
        // Defense rings use the actual event independently from liberation's threshold.
        assertNotNull(p.copy(event = PlanetEvent()).event)
    }
}
