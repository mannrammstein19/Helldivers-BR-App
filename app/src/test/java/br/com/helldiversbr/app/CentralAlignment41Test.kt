package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.ui.screens.*
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CentralAlignment41Test {
    private val old = Assignment(id=JsonPrimitive(1),expiration="2026-01-01T00:00:00Z")
    private val newer = Assignment(id=JsonPrimitive(2),expiration="2099-01-01T00:00:00Z")
    @Test fun confirmedVictorySurvivesOldAssignmentAndMissingSnapshot() {
        val previous=OrderUi(old,"completed",100.0,true)
        assertEquals(previous,OrderRepository.resolveCentralOrder(old,null,previous,true))
        assertEquals(previous,OrderRepository.resolveCentralOrder(null,null,previous,true))
    }
    @Test fun confirmedDefeatNeverReopensFromStaleSnapshot() {
        val previous=OrderUi(old,"failed",43.0,true)
        assertEquals(previous,OrderRepository.resolveCentralOrder(old,OrderSnapshot(state="active",order=old),previous,false))
    }
    @Test fun freshNewCycleReplacesOldConfirmedResult() {
        val result=OrderRepository.resolveCentralOrder(newer,null,OrderUi(old,"completed",100.0,true),true)
        assertEquals(newer,result.order);assertEquals("active",result.state)
    }
    @Test fun staleDifferentCycleCannotOpen() {
        val previous=OrderUi(old,"failed",50.0,true)
        assertEquals(previous,OrderRepository.resolveCentralOrder(newer,null,previous,false))
    }
    @Test fun olderFinalSnapshotCannotReplaceNewCycleOnAbsence() {
        val previous=OrderUi(newer,"active",30.0,false)
        assertEquals(previous,OrderRepository.resolveCentralOrder(null,OrderSnapshot(state="completed",order=old),previous,false))
    }
    @Test fun freshMonitorCanSupplyActiveOrderWhenAssignmentsFail() {
        val snapshot=OrderSnapshot(state="active",order=newer,telemetry=OrderSnapshotTelemetry(time=100,stale=false))
        assertEquals("active",OrderRepository.resolveCentralOrder(null,snapshot,null,false).state)
    }
    @Test fun monitorEnvelopeRejectsFutureAndMissingClock() {
        assertTrue(runCatching { CentralApi.decodeOrderSnapshot("""{"state":"active","telemetry":{"time":100000}}""",1000) }.isFailure)
        assertTrue(runCatching { CentralApi.decodeOrderSnapshot("""{"state":"pending"}""",1000) }.isFailure)
        assertTrue(runCatching { CentralApi.decodeOrderSnapshot("""{"state":"active","telemetry":{"time":1000,"assignmentsTime":2000}}""",1000) }.isFailure)
    }
    @Test fun confirmedOutcomeDoesNotExpireWithMonitorClock() {
        assertEquals("completed",CentralApi.decodeOrderSnapshot("""{"state":"completed","telemetry":{"time":1,"stale":true}}""",100000000).state)
    }
    private val region=PlanetRegion(id=0,hash=100,owner=JsonPrimitive(3),isAvailable=true,health=4,maxHealth=100,players=42,telemetryReadAtMillis=1000)
    @Test fun oldEnemyCombatIsUnknownWithoutProgressOrPlayers() {
        val info=regionPresentation(region.copy(telemetryStale=true))
        assertEquals(RegionState.UNKNOWN,info.state);assertNull(info.percent);assertNull(info.players)
    }
    @Test fun absenceStoresHistoryWithoutResurrectingCombat() {
        val prior=Planet(index=1,regions=listOf(region))
        val result=RegionTelemetry.enrich(Planet(index=1,regions=listOf(region.copy(owner=null,isAvailable=null))),emptyMap(),prior,2000,true).regions.single()
        assertNull(result.owner);assertNull(result.players);assertNull(result.health)
        assertEquals(1000,result.telemetryReadAtMillis);assertNotNull(result.lastKnown)
        assertEquals(RegionState.UNKNOWN,regionPresentation(result).state)
    }
    @Test fun confirmedRecoverySurvivesOwnStaleCentralReading() {
        val recovered=region.copy(owner=JsonPrimitive(1),isAvailable=false,telemetryStale=true)
        val result=RegionTelemetry.enrich(Planet(regions=listOf(recovered)),emptyMap(),null,2000,false).regions.single()
        assertEquals(RegionState.RECOVERED,regionPresentation(result).state)
        assertEquals(1000,result.telemetryReadAtMillis)
    }
    @Test fun changedIdentityDoesNotInheritRecovery() {
        val prior=Planet(regions=listOf(region.copy(owner=JsonPrimitive(1),isAvailable=false)))
        val result=RegionTelemetry.enrich(Planet(regions=listOf(region.copy(hash=200,owner=null,isAvailable=null))),emptyMap(),prior,2000,true)
        assertEquals(RegionState.UNKNOWN,regionPresentation(result.regions.single()).state)
    }
    @Test fun freshEnemyReadingOverridesRecoveredHistory() {
        val prior=Planet(regions=listOf(region.copy(owner=JsonPrimitive(1),isAvailable=false)))
        val result=RegionTelemetry.enrich(Planet(regions=listOf(region)),emptyMap(),prior,2000,true)
        assertEquals(RegionState.AVAILABLE,regionPresentation(result.regions.single()).state)
    }
    @Test fun savedSameCycleDoesNotOverwriteFreshProgress() {
        val previous=OrderUi(newer.copy(progress=listOf(50)),"active",50.0,false)
        assertEquals(previous,OrderRepository.resolveCentralOrder(newer.copy(progress=listOf(10)),null,previous,false))
    }
    @Test fun actualPublishedMonitorSnapshotDecodesAndPreservesVictory() {
        val snapshot=CentralApi.decodeOrderSnapshot(File("src/test/resources/major-order-central41.json").readText(),1791290216000L)
        val ui=OrderRepository.resolveCentralOrder(null,snapshot,null,true)
        assertEquals("completed",ui.state);assertEquals(100.0,ui.percent,0.001)
        assertEquals(30L,ui.order!!.reward!!.amount)
    }
    @Test fun regionalHistorySurvivesSerialization() {
        val planet=Planet(regions=listOf(region.copy(owner=null,isAvailable=null,lastKnown=RegionHistory(JsonPrimitive(3),true,1000,"direct"))))
        val encoded=CentralApi.json.encodeToString(Planet.serializer(),planet)
        val decoded=CentralApi.json.decodeFromString(Planet.serializer(),encoded)
        assertEquals(1000L,decoded.regions.single().lastKnown!!.telemetryReadAtMillis)
        assertEquals(RegionState.UNKNOWN,regionPresentation(decoded.regions.single()).state)
    }
    @Test fun oldCacheWithoutHistoryStillDecodes() {
        val decoded=CentralApi.json.decodeFromString(Planet.serializer(),"""{"regions":[{"id":0,"telemetryStale":true,"owner":3,"isAvailable":true}]}""")
        assertEquals(RegionState.UNKNOWN,regionPresentation(decoded.regions.single()).state)
    }
    @Test fun pendingMonitorRetainsOrderUntilResultIsConfirmed() {
        val snapshot=OrderSnapshot(state="pending",order=old)
        val result=OrderRepository.resolveCentralOrder(null,snapshot,null,true)
        assertEquals(old,result.order);assertEquals("pending",result.state)
    }
    @Test fun knownCycleDisappearingWaitsWithoutInventingDefeat() {
        val result=OrderRepository.resolveCentralOrder(null,null,OrderUi(old,"active",99.8,false),true)
        assertEquals(old,result.order);assertEquals("pending",result.state)
        assertEquals(99.8,result.percent,0.001)
    }
    @Test fun completionAlertWaitsForConfirmationAndDoesNotRepeat() {
        val manager=br.com.helldiversbr.app.notifications.WarAlertManager
        assertFalse(manager.confirmedCompletionTransition("active","active"))
        assertFalse(manager.confirmedCompletionTransition("active","pending"))
        assertFalse(manager.confirmedCompletionTransition("active","failed"))
        assertTrue(manager.confirmedCompletionTransition("active","completed"))
        assertFalse(manager.confirmedCompletionTransition("completed","completed"))
    }
    @Test fun waitingArtIsBundled() {
        assertTrue(File("src/main/res/drawable-nodpi/region_sem_confirmacao.png").length()>0)
    }
}
