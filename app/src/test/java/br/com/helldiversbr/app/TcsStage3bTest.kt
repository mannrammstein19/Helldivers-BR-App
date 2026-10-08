package br.com.helldiversbr.app
import br.com.helldiversbr.app.data.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class TcsStage3bTest {
    private fun planet(owner:String="Humans")=Planet(index=20,currentOwner=owner,activeEffects=listOf(JsonPrimitive(1395)))
    @Test fun ownerAloneCannotCreateInfrastructure() { assertFalse(TcsInfrastructure.has(Planet(currentOwner="Humans"))) }
    @Test fun allFourStatesAreDistinct() {
        assertEquals(TcsState.ALLIED,TcsInfrastructure.state(planet()))
        assertEquals(TcsState.ATTACKED,TcsInfrastructure.state(planet().copy(event=PlanetEvent())))
        assertEquals(TcsState.COMPROMISED,TcsInfrastructure.state(planet("Terminids")))
        assertEquals(TcsState.UNKNOWN,TcsInfrastructure.state(planet("")))
    }
    @Test fun boundEffectsStayOnTheirPlanet() {
        val p=Planet(index=20,planetActiveEffects=listOf(buildJsonObject { put("planetIndex",21);put("galacticEffectId",1395) }))
        assertFalse(TcsInfrastructure.has(p));assertTrue(TcsInfrastructure.has(p.copy(index=21)))
    }
    @Test fun groupedUnboundEffectsAreIgnored() { assertFalse(TcsInfrastructure.has(Planet(planetActiveEffects=listOf(JsonPrimitive(1395))))) }
    @Test fun alternativeFieldAndExactNameAreAccepted() {
        assertTrue(TcsInfrastructure.has(Planet(modifiers=listOf(JsonPrimitive("TERMINID CONTROL SYSTEM+")))))
        assertFalse(TcsInfrastructure.has(Planet(modifiers=listOf(JsonPrimitive("possibly TERMINID CONTROL SYSTEM+")))))
    }
    @Test fun unknownIdDoesNotBorrowKnownName() {
        assertFalse(TcsInfrastructure.has(Planet(effects=listOf(buildJsonObject { put("id",99);put("name","TERMINID CONTROL SYSTEM+") }))))
    }
    @Test fun pulseIsDeterministicAndLoops() {
        for (id in 0L..300L) {
            assertTrue(TcsInfrastructure.frame(id,0) in 0..39)
            assertEquals(TcsInfrastructure.frame(id,0),TcsInfrastructure.frame(id,4800))
        }
    }
    @Test fun olderCampaignCannotRestoreMissingPlanet() {
        assertEquals(listOf(20L),mergeMapPlanets(listOf(Planet(index=20)),listOf(Campaign(planet=Planet(index=21))),false).map { it.index })
    }
}
