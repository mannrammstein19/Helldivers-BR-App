package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.screens.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class VisualRulesTest {
    @Test fun orderArtworkFollowsConfirmedState() {
        assertEquals("order_active", SiteAssets.orderKey("active"))
        assertEquals("order_completed", SiteAssets.orderKey("completed"))
        assertEquals("order_failed", SiteAssets.orderKey("failed"))
        // Expiration without confirmed outcome must not display defeat.
        assertEquals("order_active", SiteAssets.orderKey("pending"))
        assertEquals("order_active", SiteAssets.orderKey("unknown"))
    }
    @Test fun rewardsDoNotInventMedalsForUnknownSpecialItems() {
        assertEquals("medalhas", describeReward(Reward(type=JsonPrimitive(1),amount=50)).kind)
        assertEquals("medalhas", describeReward(Reward(id=JsonPrimitive(897894480),amount=1)).kind)
        assertEquals("generica", describeReward(Reward(type=JsonPrimitive(1),amount=1)).kind)
        assertEquals("capa", describeReward(Reward(name=JsonPrimitive("Capa comemorativa"),amount=1)).kind)
        assertEquals("generica", describeReward(Reward(id=JsonPrimitive(9999),type=JsonPrimitive(1),amount=50)).kind)
    }
    @Test fun regionControlIsIndependentOfPlanetHealth() {
        val human = regionPresentation(PlanetRegion(owner=JsonPrimitive("Humans"),health=900,maxHealth=1000,isAvailable=false))
        assertEquals(100.0,human.percent!!,0.001)
        assertNull(human.players)
        val closed = regionPresentation(PlanetRegion(owner=JsonPrimitive("Automatons"),health=700,maxHealth=1000,isAvailable=false))
        assertNull(closed.percent)
        assertEquals("Indisponível para operações",closed.status)
        val active = regionPresentation(PlanetRegion(owner=JsonPrimitive("Automatons"),health=750,maxHealth=1000,isAvailable=true,players=20))
        assertEquals(25.0,active.percent!!,0.001)
        assertEquals(20L,active.players!!)
        assertNull(regionPresentation(PlanetRegion(health=null,maxHealth=1000)).percent)
    }
    @Test fun capitalAndUnknownPositionsDoNotOverlap() {
        val earth=Planet(index=0,name=JsonPrimitive("Super Earth"),position=PlanetPosition(.5,.5))
        assertEquals(PlanetPosition(0.0,0.0),mapPosition(earth,listOf(earth)))
        assertNull(mapPosition(Planet(index=1,position=PlanetPosition(0.0,0.0)),emptyList()))
        assertNull(mapPosition(Planet(index=2,disabled=true,position=PlanetPosition(.1,.2)),emptyList()))
    }
    @Test fun defenseTakesPriorityOverLiberation() {
        val p=Planet(index=1,currentOwner="Automatons",health=750,maxHealth=1000)
        assertTrue(mapOffensive(p,setOf(1L)))
        assertFalse(mapOffensive(p.copy(event=PlanetEvent(health=50,maxHealth=100)),setOf(1L)))
        assertEquals(25.0,mapProgress(p)!!,0.001)
        assertEquals(50.0,mapProgress(p.copy(event=PlanetEvent(health=50,maxHealth=100)))!!,0.001)
        assertFalse(mapOffensive(p.copy(name=JsonPrimitive("Meridia")),setOf(1L)))
    }
}
