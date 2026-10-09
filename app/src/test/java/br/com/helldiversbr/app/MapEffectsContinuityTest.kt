package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import kotlinx.serialization.json.*
import java.io.File
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MapEffectsContinuityTest {
    @Before fun catalog() { PlanetPresences.init(File("src/main/assets/map-presences.json").readText()) }
    private fun old() = Planet(index=260, currentOwner="Automatons", effectsReadAtMillis=100,
        effectsComplete=true, activeEffects=listOf(JsonPrimitive(1360), JsonPrimitive(1361), JsonPrimitive(1395)))
    private fun decode(body: String, time: Long=200) = CentralApi.json.decodeFromString(Planet.serializer(),body)
        .withEffectPayload(CentralApi.json.parseToJsonElement(body),time)
    private fun refresh(p: Planet, previous: Planet?=old(), stale: Boolean=false) =
        p.withSavedPresences(previous,stale || !p.hasCompleteEffectReading())

    @Test fun omittedFieldsPreserveCyborgShipAndTcsAsDatedHistory() {
        val next=refresh(decode("""{"index":260,"currentOwner":"Automatons"}"""))
        assertEquals("cyborg",PlanetPresences.list(next).single().key)
        assertEquals("nave-automata",PlanetPresences.list(next).single().model)
        assertTrue(TcsInfrastructure.has(next)); assertTrue(next.presenceHistoryStale)
        assertEquals(100L,next.effectsReadAtMillis)
        assertTrue(PlanetEffects.values(next).isEmpty())
    }
    @Test fun explicitEmptyFreshListConfirmsRemoval() {
        val next=refresh(decode("""{"index":260,"activeEffects":[]}"""))
        assertTrue(next.effectsComplete==true);assertTrue(PlanetPresences.list(next).isEmpty())
        assertFalse(TcsInfrastructure.has(next));assertFalse(next.presenceHistoryStale)
        assertEquals(200L,next.effectsReadAtMillis)
    }
    @Test fun staleEmptyListCannotConfirmRemoval() {
        val next=refresh(decode("""{"index":260,"activeEffects":[]}"""),stale=true)
        assertEquals("cyborg",PlanetPresences.list(next).single().key)
        assertTrue(TcsInfrastructure.has(next));assertEquals(100L,next.effectsReadAtMillis)
    }
    @Test fun nullFieldIsNotConfirmedAbsence() {
        val payload=CentralApi.json.parseToJsonElement("""{"index":260,"activeEffects":null}""")
        val next=refresh(Planet(index=260).withEffectPayload(payload,200))
        assertFalse(next.effectsComplete!!);assertTrue(TcsInfrastructure.has(next))
    }
    @Test fun validDirectIdsAreCurrentAndReplacePreviousPresence() {
        val next=refresh(decode("""{"index":260,"activeEffects":[{"galacticEffectId":1248}]}"""))
        assertEquals("fire",PlanetPresences.list(next).single().key)
        assertFalse(next.presenceHistoryStale);assertFalse(TcsInfrastructure.has(next))
        assertEquals(200L,next.effectsReadAtMillis)
    }
    @Test fun partialFailureKeepsOtherKnownPresencesToo() {
        val next=refresh(Planet(index=260, activeEffects=listOf(JsonPrimitive(1248))),stale=true)
        assertEquals(setOf("fire","cyborg"),PlanetPresences.list(next).map { it.key }.toSet())
        assertTrue(next.presenceHistoryStale)
    }
    @Test fun historyCannotTransferBetweenPlanets() {
        val next=refresh(decode("""{"index":261}"""))
        assertTrue(PlanetPresences.list(next).isEmpty());assertFalse(TcsInfrastructure.has(next))
    }
    @Test fun repeatedFailureAndRestartKeepOriginalTime() {
        val saved=refresh(decode("""{"index":260}"""))
        val json=Json { encodeDefaults=true }
        val restored=json.decodeFromString(Planet.serializer(),json.encodeToString(Planet.serializer(),saved))
        val next=refresh(decode("""{"index":260}""",300),restored)
        assertEquals(100L,next.effectsReadAtMillis);assertTrue(TcsInfrastructure.has(next))
        assertEquals("cyborg",PlanetPresences.list(next).single().key)
    }
    @Test fun campaignUpdatesCombatWithoutDeletingMapEffects() {
        val campaign=Campaign(planet=Planet(index=260, health=50, currentOwner="Humans",effectsComplete=true))
        val merged=mergeMapPlanets(listOf(old()),listOf(campaign),true).single()
        assertEquals(50L,merged.health);assertEquals("Humans",merged.currentOwner)
        assertEquals("cyborg",PlanetPresences.list(merged).single().key)
        assertTrue(TcsInfrastructure.has(merged));assertEquals(100L,merged.effectsReadAtMillis)
        assertFalse(merged.presenceHistoryStale)
    }
    @Test fun campaignDoesNotEraseMapRegionReadings() {
        val map=old().copy(regions=listOf(PlanetRegion(id=1, owner=JsonPrimitive("Humans"),telemetryReadAtMillis=100)))
        val merged=mergeMapPlanets(listOf(map),listOf(Campaign(planet=Planet(index=260))),true).single()
        assertEquals(map.regions,merged.regions)
    }
    @Test fun campaignCannotResurrectPresenceRemovedByMap() {
        val removed=refresh(decode("""{"index":260,"activeEffects":[]}"""))
        val merged=mergeMapPlanets(listOf(removed),listOf(Campaign(planet=old())),true).single()
        assertTrue(PlanetPresences.list(merged).isEmpty());assertFalse(TcsInfrastructure.has(merged))
    }
    @Test fun networkDecoderRecordsOmittedVersusEmptyBeforeDefaults() {
        val reading=CentralApi.Reading(JsonArray(listOf(
            buildJsonObject { put("index",260) },
            buildJsonObject { put("index",261);put("activeEffects",JsonArray(emptyList())) }
        )),200,"direct",false,300)
        val planets=CentralApi.planets(reading)
        assertFalse(planets[0].effectsComplete!!);assertTrue(planets[1].effectsComplete!!)
        assertEquals(0L,planets[0].effectsReadAtMillis);assertEquals(200L,planets[1].effectsReadAtMillis)
    }
    @Test fun centralCampaignDecoderUsesNestedPlanetFields() {
        val reading=CentralApi.Reading(JsonArray(listOf(buildJsonObject {
            put("planet",buildJsonObject { put("index",260);put("activeEffects",JsonArray(emptyList())) })
        })),200,"direct",false,300)
        assertTrue(CentralApi.campaigns(reading).single().planet.effectsComplete!!)
    }
}
