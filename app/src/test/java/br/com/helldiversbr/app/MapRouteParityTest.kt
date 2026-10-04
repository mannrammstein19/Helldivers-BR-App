package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.screens.*
import org.junit.Assert.*
import org.junit.Test

class MapRouteParityTest {
    private fun planet(id: Long, owner: String, attacking: List<Long> = emptyList(), event: PlanetEvent? = null) =
        Planet(index=id, currentOwner=owner, position=PlanetPosition(id.toDouble()/10, .1), attacking=attacking, event=event)

    @Test fun humanLiberationDirectionUsesConfirmedAttackingIds() {
        val human=planet(1,"Humans",listOf(2,2))
        val enemy=planet(2,"Terminids")
        assertEquals(listOf(MapAttackLink(1,2,"human")),mapAttackLinks(listOf(human,enemy),setOf(2)))
        assertTrue(mapAttackLinks(listOf(human,enemy),emptySet()).isEmpty())
    }
    @Test fun enemyDefenseDirectionAndFactionMustMatch() {
        val enemy=planet(1,"Automatons",listOf(2))
        val human=planet(2,"Humans",event=PlanetEvent(faction="Automatons"))
        assertEquals(listOf(MapAttackLink(1,2,"automaton")),mapAttackLinks(listOf(enemy,human),emptySet()))
        for(faction in listOf("Terminids","", "Illuminates"))
            assertTrue(mapAttackLinks(listOf(enemy,human.copy(event=PlanetEvent(faction=faction))),emptySet()).isEmpty())
        assertTrue(mapAttackLinks(listOf(enemy,human.copy(event=null)),emptySet()).isEmpty())
    }
    @Test fun bordersAndPresencesDoNotInventAttacks() {
        val human=planet(1,"Humans").copy(waypoints=listOf(2))
        val enemy=planet(2,"Automatons")
        assertTrue(mapAttackLinks(listOf(human,enemy),setOf(2)).isEmpty())
    }
    @Test fun invalidAndDisabledPositionsAreRejected() {
        val human=planet(1,"Humans",listOf(1,2,3,99))
        val enemy=planet(2,"Automatons").copy(disabled=true)
        val invalid=planet(3,"Terminids").copy(position=PlanetPosition(Double.NaN,.1))
        assertTrue(mapAttackLinks(listOf(human,enemy,invalid),setOf(2,3)).isEmpty())
    }
    @Test fun zeroLengthRoutesCannotProduceEnergy() {
        val human=planet(1,"Humans",listOf(2))
        val enemy=planet(2,"Automatons").copy(position=human.position)
        assertTrue(mapAttackLinks(listOf(human,enemy),setOf(2)).isEmpty())
    }
    @Test fun humanNamesAndRoutesHaveIndependentColors() {
        assertNotEquals(mapNameColor("Humans"),mapColor("human"))
        assertNotEquals(mapRouteColor("Humans"),mapNameColor("Humans"))
        assertEquals(mapColor("automaton"),mapNameColor("Automatons"))
        assertEquals(mapColor("terminid"),mapRouteColor("Terminids"))
    }
}
