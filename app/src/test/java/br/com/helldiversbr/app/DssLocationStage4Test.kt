package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class DssLocationStage4Test {
    private val now = 1_800_000_000_000L
    private fun telemetry(time: Long = now, stale: Boolean = false, source: String = "community") =
        CentralApi.Reading(JsonArray(emptyList()), time, source, stale, now + 60000)
    private fun host(index: Long) = Planet(index=index,activeEffects=listOf(JsonPrimitive(1217)))
    private fun history() = DssReading(lastPlanetIndex=10,lastLocationReadAtMillis=now-1000)
    @Test fun mapUsesNewerLocationReference() {
        val old=history().copy(station=SpaceStation(planet=Planet(index=10)),fetchedAtMillis=now-300001)
        assertEquals(20L,old.withPlanetReference(listOf(host(20)),telemetry(),now).locationReference)
    }
    @Test fun newerBoundEffectUpdatesHistoricalLocation() {
        assertEquals(20L, history().withPlanetReference(listOf(host(20)),telemetry(),now).lastPlanetIndex)
    }
    @Test fun oldOrFutureReadingsCannotConfirmLocation() {
        for (time in listOf(now-300001,now+60001,now-2000))
            assertEquals(history(),history().withPlanetReference(listOf(host(20)),telemetry(time),now))
    }
    @Test fun savedAndStaleReadingsCannotConfirmLocation() {
        for (r in listOf(telemetry(stale=true),telemetry(source="cache")))
            assertEquals(history(),history().withPlanetReference(listOf(host(20)),r,now))
    }
    @Test fun foreignBindingCannotMoveStation() {
        val p=Planet(index=20,activeEffects=listOf(buildJsonObject { put("id",1217);put("planetIndex",30) }))
        assertEquals(history(),history().withPlanetReference(listOf(p),telemetry(),now))
    }
    @Test fun multipleHostsDoNotInventLocation() {
        assertEquals(history(),history().withPlanetReference(listOf(host(20),host(30)),telemetry(),now))
    }
    @Test fun equalTimeConflictKeepsLastConfirmation() {
        assertEquals(history(),history().withPlanetReference(listOf(host(20)),telemetry(now-1000),now))
    }
    @Test fun expiredStationDoesNotOverrideNewerPlanetEvidence() {
        val old=history().copy(station=SpaceStation(planet=Planet(index=10)),availability=DssAvailability.LIVE,fetchedAtMillis=now-300001)
        assertEquals(20L,old.withPlanetReference(listOf(host(20)),telemetry(),now).lastPlanetIndex)
    }
    @Test fun newerStationWinsOverOlderPlanetEvidence() {
        val live=history().copy(station=SpaceStation(planet=Planet(index=10)),availability=DssAvailability.LIVE,fetchedAtMillis=now)
        assertEquals(10L,live.withPlanetReference(listOf(host(20)),telemetry(now-500),now).lastPlanetIndex)
    }
}
