package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.notifications.*
import org.junit.Assert.*
import org.junit.Test

class MapAlertAdjustmentsTest {
    @Test fun fifteenPollsOnSamePlanetDoNotNotify() {
        repeat(15) { assertFalse(dssRelocationIsNew(20, 20)) }
    }
    @Test fun firstReadingDoesNotNotify() { assertFalse(dssRelocationIsNew(0, 20)) }
    @Test fun missingLocationCannotBecomeRelocation() {
        assertFalse(dssRelocationIsNew(20, 0)); assertFalse(dssRelocationIsNew(20, -1))
    }
    @Test fun relocationAndReturnAreRealEvents() {
        assertTrue(dssRelocationIsNew(20, 21)); assertTrue(dssRelocationIsNew(21, 20))
    }
    @Test fun oldRepeatedAndFutureReadingsAreRejected() {
        assertFalse(notificationReadingIsNew(900_000, 900_000, 1_000_000))
        assertFalse(notificationReadingIsNew(900_000, 950_000, 1_000_000))
        assertFalse(notificationReadingIsNew(600_000, 0, 1_000_000))
        assertFalse(notificationReadingIsNew(1_100_000, 0, 1_000_000))
    }
    @Test fun newCurrentReadingCanBeProcessed() { assertTrue(notificationReadingIsNew(999_000, 990_000, 1_000_000)) }
    @Test fun sheteHasNoHumanLinkButFourBorderPlanetsDo() {
        val nodes = listOf(MapBorderNode(1, false, listOf(2,3,4,5)),
            MapBorderNode(2,false,listOf(10)), MapBorderNode(3,false,listOf(10)),
            MapBorderNode(4,false,listOf(10)), MapBorderNode(5,false,listOf(10)), MapBorderNode(10,true,emptyList()))
        assertEquals(setOf(2L,3L,4L,5L),enemyBorderPlanets(nodes))
    }
    @Test fun reverseSupplyLinkStillDefinesBorder() {
        assertEquals(setOf(2L),enemyBorderPlanets(listOf(MapBorderNode(1,true,listOf(2)),MapBorderNode(2,false,emptyList()))))
    }
    @Test fun missingNeighborAndEnemyOnlyLinksDoNotCreateBorder() {
        assertTrue(enemyBorderPlanets(listOf(MapBorderNode(1,false,listOf(2,99)),MapBorderNode(2,false,emptyList()))).isEmpty())
    }
    @Test fun humanNeighborsDoNotGainEnemyAura() {
        assertTrue(enemyBorderPlanets(listOf(MapBorderNode(1,true,listOf(2)),MapBorderNode(2,true,listOf(1)))).isEmpty())
    }
}
