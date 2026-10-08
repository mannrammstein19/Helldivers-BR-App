package br.com.helldiversbr.app
import br.com.helldiversbr.app.data.*
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

class ContinuityStage4bTest {
    private val region = PlanetRegion(id=0,hash=77,owner=JsonPrimitive(1),isAvailable=false,telemetryReadAtMillis=3000,players=0)
    private fun planet(r: PlanetRegion) = Planet(index=20,regions=listOf(r))
    private fun raw(time: Long) = mapOf(RegionTelemetry.Key(20,0) to RegionTelemetry.Reading(3,50,true,1.0,42,0.0,time,"direct"))
    @Test fun olderRawCannotReplaceNewerRecovery() {
        val result=RegionTelemetry.enrich(planet(region),raw(2000),null,4000,true).regions.single()
        assertEquals(1,RegionTelemetry.ownerId(result.owner));assertEquals(3000L,result.telemetryReadAtMillis)
    }
    @Test fun olderRawCannotReplaceNewerPlayers() {
        val current=region.copy(owner=JsonPrimitive(3),isAvailable=true,players=99)
        val result=RegionTelemetry.enrich(planet(current),raw(2000),null,4000,true).regions.single()
        assertEquals(99L,result.players);assertEquals(3000L,result.telemetryReadAtMillis)
    }
    @Test fun newerCombatCanReplaceRecovery() {
        val result=RegionTelemetry.enrich(planet(region),raw(4000),planet(region),5000,true).regions.single()
        assertEquals(3,RegionTelemetry.ownerId(result.owner));assertEquals(42L,result.players);assertFalse(result.telemetryStale)
    }
    @Test fun oldRawCannotReopenHistoricalRecovery() {
        val result=RegionTelemetry.enrich(planet(region.copy(owner=null,isAvailable=null,telemetryReadAtMillis=0)),raw(2000),planet(region),4000,false).regions.single()
        assertEquals(1,RegionTelemetry.ownerId(result.owner));assertTrue(result.telemetryStale);assertNull(result.players)
    }
    private val old=Assignment(id=JsonPrimitive(1),expiration="2099-01-01T00:00:00Z")
    private val confirmed=OrderUi(old,"completed",100.0,true,2000)
    @Test fun unknownOrEqualDeadlineCannotEraseOutcome() {
        for (state in listOf("completed","failed")) for (expiry in listOf("","invalid",old.expiration,"2098-01-01T00:00:00Z")) {
            val previous=confirmed.copy(state=state)
            assertEquals(previous,OrderRepository.resolveCentralOrder(old.copy(id=JsonPrimitive(2),expiration=expiry),null,previous,true,3000))
        }
    }
    @Test fun confirmedNewCycleReplacesOutcome() {
        val incoming=old.copy(id=JsonPrimitive(2),expiration="2099-02-01T00:00:00Z")
        val result=OrderRepository.resolveCentralOrder(incoming,null,confirmed,true,3000)
        assertEquals(incoming,result.order);assertEquals("active",result.state)
    }
}
