package br.com.helldiversbr.app.data

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.json.Json
import br.com.helldiversbr.app.ui.presentation.*

class CounterTelemetryTest {
    private fun planet(stats: PlanetStatistics) = Planet(index = 2, currentOwner = "Automatons", statistics = stats)
    private fun home(p: Planet, time: Long, source: String = "community") = HomeData(
        order = OrderUi(null, "pending", 0.0, false), dispatches = emptyList(), planetNames = emptyMap(), planetCatalog = emptyMap(),
        planets = listOf(p), campaigns = listOf(Campaign(planet = p)), campaignRates = emptyMap(), dss = DssReading(), updatedAtMillis = time,
        telemetrySource = source,
    )
    private fun fresh(p: Planet, time: Long) = CounterTelemetry.enrich(p,
        CounterTelemetry.collect(emptyList(), 0L, listOf(listOf(p) to "community"), time))

    @Test fun partialAndDirectReadingsPreserveIndividualValuesAndTimes() {
        val old = fresh(planet(PlanetStatistics(bulletsFired = 100, bulletsHit = 80, automatonKills = 50)), 1_000_000)
        val partial = planet(PlanetStatistics(bulletsFired = 110))
        val fields = CounterTelemetry.collect(listOf(old), 1_000_000,
            listOf(listOf(partial) to "community", listOf(planet(PlanetStatistics(playerCount = 9))) to "direct"), 1_060_000)
        val result = CounterTelemetry.enrich(partial, fields)
        assertEquals(110L, result.statistics.bulletsFired)
        assertEquals(80L, result.statistics.bulletsHit)
        assertEquals(1_000_000L, fields.getValue(2).getValue("bulletsHit").readAtMillis)
        assertTrue(fields.getValue(2).getValue("bulletsHit").stale)
        assertFalse(fields.getValue(2).getValue("bulletsFired").stale)
        assertFalse(observations(home(result, 1_060_000, "mixed")).any { it.key == counterKey(2, "bulletsHit") })
    }

    @Test fun laterProcessedEndpointCannotOverwriteNewerReading() {
        val newer = planet(PlanetStatistics(bulletsFired = 130))
        val older = planet(PlanetStatistics(bulletsFired = 120))
        val fields = CounterTelemetry.collect(emptyList(), 0L,
            listOf(listOf(newer) to "community", listOf(older) to "community"),
            1_060_000, listOf(1_060_000, 1_050_000))
        assertEquals(130L, fields.getValue(2).getValue("bulletsFired").value)
        assertEquals(1_060_000L, fields.getValue(2).getValue("bulletsFired").readAtMillis)
    }

    @Test fun incompleteCampaignCannotErasePlanetsCounter() {
        val full = planet(PlanetStatistics(bulletsFired = 123))
        val empty = planet(PlanetStatistics())
        val fields = CounterTelemetry.collect(emptyList(), 0L, listOf(listOf(full) to "community", listOf(empty) to "community"), 1_000_000)
        assertEquals(123L, CounterTelemetry.enrich(empty, fields).statistics.bulletsFired)
        assertNull(CounterTelemetry.enrich(empty, fields).statistics.bulletsHit)
    }

    @Test fun mixedDssOrDispatchSourceDoesNotResetShotsRate() {
        val tracker = NumberProjectionTracker()
        val first = home(fresh(planet(PlanetStatistics(bulletsFired = 100)), 1_000_000), 1_000_000)
        val second = home(fresh(planet(PlanetStatistics(bulletsFired = 160)), 1_060_000), 1_060_000, "mixed")
        tracker.update(observations(first), first.updatedAtMillis, first.updatedAtMillis)
        val frame = tracker.update(observations(second), second.updatedAtMillis, second.updatedAtMillis)
        assertEquals(170.0, frame.value(counterKey(2, "bulletsFired"), 160.0, 1_070_000), .00001)
    }

    @Test fun freshCounterUsesOwnTimeWhenCampaignTimestampIsOld() {
        val tracker = NumberProjectionTracker()
        tracker.update(listOf(NumberObservation("shots", 100.0, false, "community", 1_000_000)), 1L, 1_000_000)
        val frame = tracker.update(listOf(NumberObservation("shots", 160.0, false, "community", 1_060_000)), 1L, 1_060_000)
        assertEquals(170.0, frame.value("shots", 160.0, 1_070_000), .00001)
    }

    @Test fun savedFieldStaysSavedAcrossRepeatedFallbackAndSerialization() {
        val old = fresh(planet(PlanetStatistics(bulletsFired = 99)), 1_000_000)
        val empty = planet(PlanetStatistics())
        val fields = CounterTelemetry.collect(listOf(old), 1_000_000, listOf(listOf(empty) to "direct"), 1_060_000)
        val carried = CounterTelemetry.enrich(empty, fields)
        val decoded = Json.decodeFromString(Planet.serializer(), Json.encodeToString(Planet.serializer(), carried))
        val again = CounterTelemetry.collect(listOf(decoded), 1_060_000, listOf(listOf(empty) to "direct"), 1_120_000)
        assertEquals(1_000_000L, again.getValue(2).getValue("bulletsFired").readAtMillis)
        assertTrue(observations(home(decoded, 1_060_000, "mixed")).none { it.key == counterKey(2, "bulletsFired") })
    }

    @Test fun confirmedZeroAndResetReplacePreservedCounter() {
        val old = fresh(planet(PlanetStatistics(bulletsFired = 99)), 1_000_000)
        val zero = planet(PlanetStatistics(bulletsFired = 0))
        val fields = CounterTelemetry.collect(listOf(old), 1_000_000, listOf(listOf(zero) to "community"), 1_060_000)
        assertEquals(0L, CounterTelemetry.enrich(zero, fields).statistics.bulletsFired)
        assertFalse(fields.getValue(2).getValue("bulletsFired").stale)
    }
}
