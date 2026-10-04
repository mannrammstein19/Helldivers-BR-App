package br.com.helldiversbr.app

import br.com.helldiversbr.app.ui.screens.*
import org.junit.Assert.*
import org.junit.Test

class MapMarkerLayoutTest {
    @Test fun zeroProgressPlanetCaptionStaysNearOwnerRing() {
        // Screenshot regression: a 14px planet must not leave a ~50px name gap.
        val top=mapLabelTop(14f,false,false,4f)
        assertEquals(18f,top,.01f)
        val (name,count)=mapLabelBaselines(top,-12f,3f,-9f,true,1f)
        assertEquals(18f,name-12f,.01f)
        assertEquals(1f,(count-9f)-(name+3f),.01f)
    }
    @Test fun activeCampaignCaptionClearsTheOutermostProgressRing() {
        for(defense in listOf(false,true)) {
            val top=mapLabelTop(10f,defense,!defense,4f)
            assertTrue(top>10f*2.72f)
            assertTrue(top-10f*2.72f<8f)
        }
    }
    @Test fun hidingNamesDoesNotLeaveAnEmptyNameLine() {
        val top=18f
        val withName=mapLabelBaselines(top,-12f,3f,-9f,true,1f)
        val withoutName=mapLabelBaselines(top,-12f,3f,-9f,false,1f)
        assertEquals(top,withoutName.second-9f,.01f)
        assertTrue(withoutName.second<withName.second)
    }
    @Test fun ownerRingIsAlwaysOwnerColorIncludingHumanQuietPlanets() {
        assertEquals(mapRouteColor("Humans"),mapOwnerColor("Humans"))
        assertEquals(mapColor("automaton"),mapOwnerColor("Automatons"))
        assertEquals(mapColor("illuminate"),mapOwnerColor("Illuminates"))
        assertNotEquals(mapNameColor("Humans"),mapOwnerColor("Humans"))
    }
    @Test fun specialArtworkAndSelectedOutlineDoNotCollideWithCaptions() {
        assertTrue(mapLabelTop(10f,false,false,4f,special=true)>25f)
        assertTrue(mapLabelTop(10f,false,false,4f,selected=true)>34f)
    }
}
