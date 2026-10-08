package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.*
import br.com.helldiversbr.app.notifications.notificationReadingIsNew
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.*
import org.junit.Test

/** Synthetic regressions. These do not replace device/network verification. */
class TelemetryContinuityTest {
    private val assignment = Assignment(id = JsonPrimitive(10), expiration = "2099-01-01T00:00:00Z")
    private val prior = OrderUi(assignment.copy(progress = listOf(50)), "active", 50.0, false, 2_000)
    private val region = PlanetRegion(id = 0, hash = 100, owner = JsonPrimitive(3),
        isAvailable = true, health = 40, maxHealth = 100, players = 20, telemetryReadAtMillis = 2_000)
    private fun planet(r: PlanetRegion = region) = Planet(index = 1, regions = listOf(r))

    @Test fun regressiveAssignmentsCannotReplaceProgress() {
        assertEquals(prior, OrderRepository.resolveCentralOrder(assignment, null, prior, true, 1_000))
    }
    @Test fun monitorClockDoesNotHideOldAssignmentClock() {
        val monitor = OrderSnapshot(state = "active", order = assignment,
            telemetry = OrderSnapshotTelemetry(time = 3_000, assignmentsTime = 1_000, stale = false))
        assertEquals(prior, OrderRepository.resolveCentralOrder(null, monitor, prior, false))
    }
    @Test fun fresherLiveProgressWinsOverSameCycleMonitor() {
        val monitor = OrderSnapshot(state = "active", order = assignment.copy(progress = listOf(60)),
            telemetry = OrderSnapshotTelemetry(time = 4_000, assignmentsTime = 2_500, stale = false))
        val result = OrderRepository.resolveCentralOrder(assignment.copy(progress = listOf(70)), monitor, prior, true, 3_000)
        assertEquals(listOf(70L), result.order!!.progress)
        assertEquals(3_000L, result.observedAtMillis)
    }
    @Test fun regressiveAbsenceCannotChangeState() {
        assertEquals(prior, OrderRepository.resolveCentralOrder(null, null, prior, true, 1_000))
    }
    @Test fun newAbsenceOnlyBecomesPending() {
        val result = OrderRepository.resolveCentralOrder(null, null, prior, true, 3_000)
        assertEquals("pending", result.state)
        assertEquals(3_000L, result.observedAtMillis)
    }
    @Test fun confirmedResultAndClockSurviveSerializationAndFailure() {
        val confirmed = prior.copy(state = "failed", fromSnapshot = true)
        val saved = CentralApi.json.decodeFromString(OrderUi.serializer(),
            CentralApi.json.encodeToString(OrderUi.serializer(), confirmed))
        assertEquals(confirmed, OrderRepository.resolveCentralOrder(null, null, saved, false))
    }
    @Test fun newerCycleCanReplaceDatedConfirmedOutcome() {
        val next = assignment.copy(id = JsonPrimitive(11), expiration = "2099-02-01T00:00:00Z")
        val result = OrderRepository.resolveCentralOrder(next, null, prior.copy(state = "completed"), true, 3_000)
        assertEquals(next.id, result.order!!.id)
        assertEquals("active", result.state)
    }
    @Test fun entirelyMissingRegionDoesNotDisappearOrStayLive() {
        val result = RegionTelemetry.enrich(Planet(index = 1), emptyMap(), planet(), 3_000, true).regions.single()
        assertTrue(result.telemetryStale)
        assertNull(result.players); assertNull(result.health); assertNull(result.isAvailable)
        assertEquals(2_000L, result.telemetryReadAtMillis)
        assertNotNull(result.lastKnown)
    }
    @Test fun missingRecoveredRegionRetainsOnlyDatedConfirmation() {
        val recovered = region.copy(owner = JsonPrimitive(1), isAvailable = false)
        val result = RegionTelemetry.enrich(Planet(index = 1), emptyMap(), planet(recovered), 3_000, true).regions.single()
        assertEquals(1, RegionTelemetry.ownerId(result.owner)); assertEquals(false, result.isAvailable)
        assertNull(result.players); assertNull(result.health); assertTrue(result.telemetryStale)
    }
    @Test fun lostHashCannotInheritRecovery() {
        val recovered = region.copy(owner = JsonPrimitive(1), isAvailable = false)
        val result = RegionTelemetry.enrich(planet(region.copy(hash = null, owner = null, isAvailable = null)),
            emptyMap(), planet(recovered), 3_000, true).regions.single()
        assertNull(result.owner)
    }
    @Test fun differentPlanetCannotInheritRecovery() {
        val result = RegionTelemetry.enrich(planet(region.copy(owner = null, isAvailable = null)), emptyMap(),
            planet(region.copy(owner = JsonPrimitive(1), isAvailable = false)).copy(index = 2), 3_000, true)
        assertNull(result.regions.single().owner)
    }
    @Test fun olderCombatCannotUndoNewRecovery() {
        val recovered = region.copy(owner = JsonPrimitive(1), isAvailable = false, telemetryReadAtMillis = 3_000)
        val result = RegionTelemetry.enrich(planet(), emptyMap(), planet(recovered), 4_000, true).regions.single()
        assertEquals(1, RegionTelemetry.ownerId(result.owner)); assertTrue(result.telemetryStale)
    }
    @Test fun newerCombatOverridesRecovery() {
        val recovered = region.copy(owner = JsonPrimitive(1), isAvailable = false)
        val result = RegionTelemetry.enrich(planet(region.copy(telemetryReadAtMillis = 3_000)), emptyMap(),
            planet(recovered), 4_000, true).regions.single()
        assertEquals(3, RegionTelemetry.ownerId(result.owner)); assertFalse(result.telemetryStale)
    }
    @Test fun staleEnvelopeFlagCannotMakeOldClockFresh() {
        val reading = CentralApi.decode("""{"data":[],"time":1000,"source":"community","stale":false}""", 400_000)
        assertTrue(reading.stale); assertEquals(1_000L, reading.time)
    }
    @Test fun regionalOriginIsNotOverwrittenByOuterEnvelope() {
        val reading = CentralApi.decode("""{"data":[],"time":3000,"source":"community","stale":false}""", 3_000)
        val result = planet(region.copy(telemetrySource = "direct")).withCentralReading(reading).regions.single()
        assertEquals("direct", result.telemetrySource); assertEquals(2_000L, result.telemetryReadAtMillis)
    }
    @Test fun notificationClockRejectsOldRepeatedMissingAndFutureReadings() {
        assertFalse(notificationReadingIsNew(0, 0, 500_000))
        assertFalse(notificationReadingIsNew(499_000, 499_000, 500_000))
        assertFalse(notificationReadingIsNew(498_000, 499_000, 500_000))
        assertFalse(notificationReadingIsNew(1_000, 0, 500_000))
        assertFalse(notificationReadingIsNew(600_001, 0, 500_000))
        assertTrue(notificationReadingIsNew(500_000, 499_000, 500_000))
    }
    @Test fun oldOrderCacheStillLoadsWithoutNewClock() {
        val decoded = CentralApi.json.decodeFromString(OrderUi.serializer(),
            """{"order":null,"state":"pending","percent":0.0,"fromSnapshot":false}""")
        assertEquals(0L, decoded.observedAtMillis)
    }
    @Test fun steamCentralRespectsItsFifteenMinuteObservationWindow() {
        val body = """{"data":[],"time":1000,"source":"steam","stale":false}"""
        assertTrue(runCatching { CentralApi.decode(body, 500_000) }.isFailure)
        val news = CentralApi.decode(body, 500_000, maxFreshAgeMillis = 900_000, allowSteam = true)
        assertFalse(news.stale); assertEquals("steam", news.source)
    }
    @Test fun pendingMonitorDoesNotReplaceLatestKnownProgress() {
        val monitor = OrderSnapshot(state = "pending", order = assignment.copy(progress = listOf(10)),
            telemetry = OrderSnapshotTelemetry(time = 3_000, stale = false))
        val result = OrderRepository.resolveCentralOrder(null, monitor, prior, false)
        assertEquals("pending", result.state); assertEquals(prior.order, result.order)
    }

}
