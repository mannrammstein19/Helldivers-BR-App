package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.screens.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class VisualRulesTest {
    @Test fun mapAlertOnlyMarksDefenseEvents() {
        val enemy = Planet(index=1,currentOwner="Automatons",health=750,maxHealth=1000)
        assertFalse(isPlanetUnderAttack(enemy))
        assertTrue(isPlanetUnderAttack(enemy.copy(currentOwner="Humans",event=PlanetEvent(health=50,maxHealth=100))))
        assertFalse(isPlanetUnderAttack(enemy.copy(disabled=true,event=PlanetEvent(health=50,maxHealth=100))))
        assertFalse(isPlanetUnderAttack(enemy.copy(name=JsonPrimitive("Meridia"),event=PlanetEvent(health=50,maxHealth=100))))
    }
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
        assertEquals("Bloqueada para operações",closed.status)
        val active = regionPresentation(PlanetRegion(owner=JsonPrimitive("Automatons"),health=750,maxHealth=1000,isAvailable=true,players=20))
        assertEquals(25.0,active.percent!!,0.001)
        assertEquals(20L,active.players!!)
        assertNull(regionPresentation(PlanetRegion(health=null,maxHealth=1000)).percent)
    }
    @Test fun regionalProgressCannotReplaceZeroPlanetProgress() {
        val region = PlanetRegion(health=47_100,maxHealth=1_000_000,isAvailable=true)
        val planet = Planet(health=1_500_000,maxHealth=1_500_000,regions=listOf(region))
        assertEquals(0.0,OrderRepository.campaignPercent(Campaign(planet=planet)),0.00001)
        assertEquals(95.29,regionPresentation(region).percent!!,0.00001)
        assertEquals(25.0,OrderRepository.campaignPercent(Campaign(planet=planet.copy(health=1_125_000))),0.00001)
        assertEquals(50.0,OrderRepository.campaignPercent(Campaign(planet=planet.copy(event=PlanetEvent(health=50,maxHealth=100)))),0.00001)
    }
    @Test fun oneSavedNoticePreservesDifferentReadingTimes() {
        fun reading(value:Long,time:Long,stale:Boolean=true)=CounterReading(value,"community",time,stale)
        val stats=PlanetStatistics(bulletsFired=100,bulletsHit=80,counterReadings=mapOf(
            "bulletsFired" to reading(100,1_000_000),"bulletsHit" to reading(80,2_000_000)))
        val notice=br.com.helldiversbr.app.ui.presentation.counterSavedNotice(Planet(statistics=stats))!!
        assertTrue(notice.contains("leituras entre"))
        assertNull(br.com.helldiversbr.app.ui.presentation.counterSavedNotice(Planet(statistics=stats.copy(
            counterReadings=stats.counterReadings.mapValues { it.value.copy(stale=false) }))))
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
