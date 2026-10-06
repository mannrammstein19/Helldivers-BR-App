package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class DssAlignment41Test {
    private val now = 1791296413000L
    private fun action(kind: DssSupport = DssSupport.EAGLE, status: Int = 2, expiry: String = "") =
        DssTacticalAction(id32 = kind.actionId, status = status, statusExpire = expiry)
    private fun reading(actions: List<DssTacticalAction> = listOf(action()), effects: List<Long>? = null) =
        DssReading(SpaceStation(id32 = 749875195L, planet = Planet(index = 200),
            tacticalActions = actions, activeEffectIds = effects), DssAvailability.LIVE, now)
    private fun supports(r: DssReading?) = DssSupport.forPlanet(r, 200, now)

    @Test fun activeConfirmedActionAppears() { assertEquals(listOf(DssSupport.EAGLE), supports(reading())) }
    @Test fun wrongPlanetNeverInheritsStationSupport() { assertTrue(DssSupport.forPlanet(reading(), 201, now).isEmpty()) }
    @Test fun staleAndCacheNeverConfirmActiveSupport() { assertTrue(supports(reading().copy(stale = true, source = "cache")).isEmpty()) }
    @Test fun unavailableStationDoesNotConfirmSupport() { assertTrue(supports(reading().copy(availability = DssAvailability.ABSENT)).isEmpty()) }
    @Test fun missingStationDoesNotConfirmSupport() { assertTrue(supports(reading().copy(station = null)).isEmpty()) }
    @Test fun oldReadingsStopConfirmingEvenWithoutNetworkRefresh() { assertTrue(DssSupport.forPlanet(reading(), 200, now + 300001).isEmpty()) }
    @Test fun futureReadingsAreRejected() { assertTrue(supports(reading().copy(fetchedAtMillis = now + 60001)).isEmpty()) }
    @Test fun expiredActionIsNotActive() { assertTrue(supports(reading(listOf(action(expiry = Instant.ofEpochMilli(now).toString())))).isEmpty()) }
    @Test fun malformedDeadlineDoesNotConfirmActive() { assertTrue(supports(reading(listOf(action(expiry = "invalid")))).isEmpty()) }
    @Test fun futureDeadlineAllowsActiveAction() { assertEquals(1, supports(reading(listOf(action(expiry = Instant.ofEpochMilli(now + 1000).toString())))).size) }
    @Test fun fundingAndUnknownStatusesAreNotActive() { for (s in listOf(0, 1, 3, 99)) assertTrue(supports(reading(listOf(action(status = s)))).isEmpty()) }
    @Test fun unknownIdsAndNamesCannotInventSupport() { assertTrue(supports(reading(listOf(DssTacticalAction(id32 = 999, name = "EAGLE STORM", status = 2)))).isEmpty()) }
    @Test fun pairedEffectIdsDeduplicate() { assertEquals(listOf(DssSupport.EAGLE), supports(reading(effects = listOf(1425, 1212, 1216)))) }
    @Test fun explicitEmptyRawEffectsOverridesOldRichActions() { assertTrue(supports(reading(effects = emptyList())).isEmpty()) }
    @Test fun rawCurrentEffectsOverrideExpiredRichDates() {
        assertEquals(listOf(DssSupport.BLOCKADE), supports(reading(listOf(action(expiry = "2024-01-01T00:00:00Z")), listOf(1213))))
    }
    @Test fun allThreeKindsRemainSeparate() { assertEquals(DssSupport.entries, supports(reading(effects = listOf(1212, 1213, 1237, 1214)))) }
    @Test fun rawFallbackDoesNotCarryOldActions() {
        assertTrue(DssSupport.actionsFromEffects(emptyList()).isEmpty())
        assertEquals(listOf(DssSupport.HEAVY.actionId), DssSupport.actionsFromEffects(listOf(1237)).map { it.id32 })
    }
    @Test fun actualGameStationFixtureConfirmsEagleAtMatarOnly() {
        val text = javaClass.getResourceAsStream("/dss-raw41.json")!!.bufferedReader().use { it.readText() }
        val raw = CentralApi.json.parseToJsonElement(text).jsonObject.getValue("station").jsonObject
        val ids = raw.getValue("activeEffectIds").jsonArray.map { it.jsonPrimitive.long }
        val station = SpaceStation(id32 = raw.getValue("id32").jsonPrimitive.long,
            planet = Planet(index = raw.getValue("planetIndex").jsonPrimitive.long),
            activeEffectIds = ids, tacticalActions = DssSupport.actionsFromEffects(ids))
        assertEquals(listOf(DssSupport.EAGLE), supports(DssReading(station, DssAvailability.LIVE, now)))
        assertEquals(1, station.tacticalActions.size)
        assertTrue(DssSupport.forPlanet(DssReading(station, DssAvailability.LIVE, now), 228, now).isEmpty())
    }
    @Test fun communityActionEffectIdsAreRetained() {
        val station = CentralApi.json.decodeFromString(SpaceStation.serializer(),
            """{"planet":{"index":200},"tacticalActions":[{"id32":4091660627,"status":2,"effectIds":[1425,1212,1216]}]}""")
        assertEquals(listOf(1425L, 1212L, 1216L), station.tacticalActions.single().effectIds)
        assertEquals(listOf(DssSupport.EAGLE), supports(DssReading(station, DssAvailability.LIVE, now)))
    }
    @Test fun alertRuleAlsoRejectsExpiredAction() {
        assertFalse(DssSupport.actionIsActive(action(expiry = Instant.ofEpochMilli(now - 1).toString()), now))
        assertFalse(DssSupport.isCurrent(reading().copy(stale = true), now))
        assertFalse(DssSupport.isCurrent(reading().copy(source = "cache", stale = false), now))
    }
    @Test fun explicitTextActiveIsAcceptedButInactiveIsNot() {
        assertTrue(DssSupport.actionIsActive(action(status = 0).copy(statusText = JsonPrimitive("active")), now))
        assertFalse(DssSupport.actionIsActive(action(status = 0).copy(statusText = JsonPrimitive("inactive")), now))
    }
}
