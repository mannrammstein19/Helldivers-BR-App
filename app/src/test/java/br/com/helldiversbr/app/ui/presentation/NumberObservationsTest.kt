package br.com.helldiversbr.app.ui.presentation

import br.com.helldiversbr.app.data.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class NumberObservationsTest {
    private fun data(): HomeData {
        val planet = Planet(index = 2, health = 5000, maxHealth = 10000, currentOwner = "Terminids",
            statistics = PlanetStatistics(playerCount = 50, bulletsFired = 1000, bulletsHit = 800, automatonKills = 400))
        return HomeData(order = OrderUi(null, "pending", 0.0, false), dispatches = emptyList(),
            planetNames = emptyMap(), planetCatalog = emptyMap(), planets = listOf(planet),
            campaigns = listOf(Campaign(planet = planet)), campaignRates = emptyMap(),
            dss = DssReading(), updatedAtMillis = 1_000_000)
    }

    @Test fun presentationLeavesConfirmedSnapshotUnchanged() {
        val original = data()
        val before = Json.encodeToString(HomeData.serializer(), original)
        val tracker = NumberProjectionTracker()
        tracker.update(observations(original), original.updatedAtMillis, original.updatedAtMillis)
        val p = original.campaigns.single().planet.copy(health = 4999,
            statistics = original.campaigns.single().planet.statistics.copy(bulletsFired = 1200))
        val next = original.copy(campaigns = listOf(Campaign(planet = p)), planets = listOf(p), updatedAtMillis = 1_060_000)
        val frame = tracker.update(observations(next), next.updatedAtMillis, next.updatedAtMillis)
        assertTrue(frame.value(counterKey(2, "bulletsFired"), 1200.0, 1_070_000) > 1200.0)
        assertEquals(before, Json.encodeToString(HomeData.serializer(), original))
        assertEquals(1200L, next.campaigns.single().planet.statistics.bulletsFired)
        assertEquals(4999L, next.campaigns.single().planet.health)
    }

    @Test fun cacheAndStaleGroupsHaveNoProjectedValues() {
        assertTrue(observations(data().copy(telemetrySource = "cache")).isEmpty())
        assertTrue(observations(data().copy(staleSources = listOf("campanhas", "planetas"))).isEmpty())
    }

    @Test fun missingStatisticsRemainUnavailableRatherThanZero() {
        val legacy = Json.decodeFromString(PlanetStatistics.serializer(), """{"playerCount":50}""")
        assertNull(legacy.bulletsFired)
        assertNull(legacy.bulletsHit)
        assertNull(legacy.automatonKills)
        val current = Json.decodeFromString(PlanetStatistics.serializer(), """{"playerCount":50,"bulletsFired":17000000000,"bulletsHit":18000000000,"automatonKills":3000000000}""")
        assertEquals(17_000_000_000L, current.bulletsFired)
        assertEquals(18_000_000_000L, current.bulletsHit)
        assertEquals(3_000_000_000L, current.automatonKills)
    }

    @Test fun onlyThePlanetsEnemyKillCounterIsProjected() {
        val base = data()
        listOf("Terminids" to "terminidKills", "Automatons" to "automatonKills", "Illuminate" to "illuminateKills").forEach { (faction, field) ->
            val p = base.planets.single().copy(currentOwner = faction,
                statistics = PlanetStatistics(bulletsFired = 1000, bulletsHit = 800,
                    terminidKills = 100, automatonKills = 200, illuminateKills = 300))
            val confirmed = base.copy(planets = listOf(p), campaigns = listOf(Campaign(planet = p)))
            val keys = observations(confirmed).map { it.key }
            assertTrue(keys.contains(counterKey(p.index, "bulletsFired")))
            assertTrue(keys.contains(counterKey(p.index, "bulletsHit")))
            listOf("terminidKills", "automatonKills", "illuminateKills").forEach { kill ->
                assertEquals(kill == field, keys.contains(counterKey(p.index, kill)))
            }
            assertEquals(200L, p.statistics.automatonKills) // os números confirmados são preservados
        }
    }

    @Test fun invaderOverridesSuperEarthOwnerDuringDefense() {
        val p = data().planets.single().copy(currentOwner = "Humans", event = PlanetEvent(faction = "Illuminate"))
        assertEquals("illuminateKills", killCounterField(p))
        assertNull(killCounterField(p.copy(event = null)))
    }

    @Test fun changedFactionDiscardsOtherEnemiesProjectionHistory() {
        val base = data()
        val tracker = NumberProjectionTracker()
        fun reading(faction: String, count: Long, time: Long): HomeData {
            val p = base.planets.single().copy(currentOwner = faction,
                statistics = PlanetStatistics(terminidKills = count, automatonKills = count, illuminateKills = count))
            return base.copy(planets = listOf(p), campaigns = listOf(Campaign(planet = p)), updatedAtMillis = time)
        }
        val first = reading("Terminids", 100, 1_000_000)
        val second = reading("Terminids", 200, 1_060_000)
        tracker.update(observations(first), first.updatedAtMillis, first.updatedAtMillis)
        val moving = tracker.update(observations(second), second.updatedAtMillis, second.updatedAtMillis)
        assertTrue(moving.value(counterKey(2, "terminidKills"), 200.0, 1_070_000) > 200)
        val changed = reading("Illuminate", 300, 1_120_000)
        val result = tracker.update(observations(changed), changed.updatedAtMillis, changed.updatedAtMillis)
        assertFalse(result.entries.containsKey(counterKey(2, "terminidKills")))
        assertEquals(300.0, result.value(counterKey(2, "illuminateKills"), 300.0, 1_130_000), 0.0)
    }
}
