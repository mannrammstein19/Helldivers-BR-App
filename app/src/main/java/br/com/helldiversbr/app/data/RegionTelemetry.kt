package br.com.helldiversbr.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/** Regional control is independent of planetary control. Never infer ownership from HP. */
object RegionTelemetry {
    data class Key(val planet: Long, val region: Int)
    data class Reading(
        val owner: Int, val health: Long?, val available: Boolean?,
        val availabilityFactor: Double?, val players: Long?, val regen: Double?,
        val readAtMillis: Long, val source: String,
    )
    private val gate = Mutex()
    private var lastAttempt = 0L
    private var latest: Map<Key, Reading> = emptyMap()

    fun ownerId(value: JsonElement?): Int? = when (localizedText(value).trim().lowercase()) {
        "1", "human", "humans", "super earth", "super terra" -> 1
        "2", "terminid", "terminids" -> 2
        "3", "automaton", "automatons", "automaton(s)", "cyborg", "cyborgs" -> 3
        "4", "illuminate", "illuminates" -> 4
        else -> null
    }

    fun parse(body: String, readAtMillis: Long, source: String): Map<Key, Reading> {
        val root = Json.parseToJsonElement(body).jsonObject
        val data = (root["data"] as? JsonObject) ?: root
        val regions = data["planetRegions"] as? JsonArray ?: error("Resposta sem planetRegions")
        return buildMap {
            regions.forEach { item ->
                val row = item as? JsonObject ?: return@forEach
                fun long(key: String) = (row[key] as? JsonPrimitive)?.longOrNull
                fun double(key: String) = (row[key] as? JsonPrimitive)?.doubleOrNull
                val planet = long("planetIndex")?.takeIf { it >= 0 } ?: return@forEach
                val region = long("regionIndex")?.takeIf { it in 0..Int.MAX_VALUE }?.toInt() ?: return@forEach
                val owner = long("owner")?.takeIf { it in 1..4 }?.toInt() ?: return@forEach
                put(Key(planet, region), Reading(owner, long("health"),
                    (row["isAvailable"] as? JsonPrimitive)?.booleanOrNull,
                    double("availabilityFactor"), long("players"),
                    double("regenPerSecond") ?: double("regerPerSecond"), readAtMillis, source))
            }
        }
    }

    /** At most one status request per minute; a failed lookup also respects the interval. */
    suspend fun load(): Map<Key, Reading> = gate.withLock {
        val now = System.currentTimeMillis()
        if (now - lastAttempt < 60_000L) return@withLock latest
        lastAttempt = now
        latest = runCatching {
            parse(HelldiversApi.regionStatus(), System.currentTimeMillis(), "community-raw")
        }.getOrElse {
            if (it is CancellationException) throw it
            runCatching { parse(DirectGameApi.regionStatus(), System.currentTimeMillis(), "direct") }
                .getOrElse { if (it is CancellationException) throw it; emptyMap() }
        }
        latest
    }

    private fun sameIdentity(a: PlanetRegion, b: PlanetRegion): Boolean =
        if (a.id != null || b.id != null) a.id != null && a.id == b.id && a.hash == b.hash
        else a.hash != null && a.hash == b.hash

    private fun evidenceTime(region: PlanetRegion): Long =
        maxOf(region.telemetryReadAtMillis, region.lastKnown?.telemetryReadAtMillis ?: 0L)

    /** Historical recovery remains visible; combat measurements are never current by inheritance. */
    private fun historical(region: PlanetRegion, prior: PlanetRegion?): PlanetRegion {
        fun knownTime(r: PlanetRegion) = if (ownerId(r.owner) != null) r.telemetryReadAtMillis
            else r.lastKnown?.telemetryReadAtMillis ?: 0L
        val evidence = if (prior != null && knownTime(prior) >= knownTime(region)) prior else region
        val history = if (ownerId(evidence.owner) != null) RegionHistory(
            evidence.owner, evidence.isAvailable, evidence.telemetryReadAtMillis, evidence.telemetrySource,
            evidence.health, evidence.players, evidence.regenPerSecond, evidence.availabilityFactor)
            else evidence.lastKnown
        val recovered = history?.let { ownerId(it.owner) == 1 && it.isAvailable == false } == true
        return region.copy(owner = if (recovered) history?.owner else null,
            health = null, isAvailable = if (recovered) false else null,
            players = null, regenPerSecond = null, availabilityFactor = null,
            telemetryReadAtMillis = history?.telemetryReadAtMillis ?: evidence.telemetryReadAtMillis,
            telemetrySource = history?.telemetrySource ?: evidence.telemetrySource,
            lastKnown = history, telemetryStale = true)
    }

    fun enrich(planet: Planet, readings: Map<Key, Reading>, previous: Planet?, now: Long, liveBase: Boolean): Planet {
        val priorRegions = previous?.takeIf { it.index == planet.index }?.regions.orEmpty()
        val current = planet.regions.map { region ->
            val prior = priorRegions.firstOrNull { sameIdentity(region, it) }
            val reading = region.id?.let { readings[Key(planet.index, it)] }
                ?.takeIf { now - it.readAtMillis in 0..90_000L &&
                    it.readAtMillis >= evidenceTime(region) &&
                    it.readAtMillis >= (prior?.let { old -> evidenceTime(old) } ?: 0L) }
            val candidate = if (reading != null) region.copy(owner = JsonPrimitive(reading.owner),
                health = reading.health, isAvailable = reading.available,
                availabilityFactor = reading.availabilityFactor, players = reading.players,
                regenPerSecond = reading.regen, telemetryReadAtMillis = reading.readAtMillis,
                telemetrySource = reading.source, telemetryStale = false)
                else region.copy(telemetryReadAtMillis = region.telemetryReadAtMillis.takeIf { it > 0 } ?: now)
            val usable = (reading != null || liveBase) && !candidate.telemetryStale &&
                ownerId(candidate.owner) != null && candidate.telemetryReadAtMillis <= now + 60_000 &&
                now - candidate.telemetryReadAtMillis <= 300_000 &&
                candidate.telemetryReadAtMillis >= (prior?.let { evidenceTime(it) } ?: 0L)
            when {
                candidate.telemetryReadAtMillis > now + 60_000 -> historical(
                    region.copy(owner = null, isAvailable = null, lastKnown = null, telemetryReadAtMillis = 0L), prior)
                !usable -> historical(region, prior)
                candidate.isAvailable == null && prior?.isAvailable != null &&
                    ownerId(candidate.owner) == ownerId(prior.owner) -> historical(region, prior)
                else -> candidate
            }
        }
        // Missing rows survive only as dated history. A reused ID never inherits another hash.
        val missing = priorRegions.filter { old -> planet.regions.none {
            sameIdentity(it, old) || (old.id != null && it.id == old.id)
        } }.map { historical(it, it) }
        return planet.copy(regions = current + missing)
    }

}

/** Reusing a saved snapshot never refreshes a field's actual observation time. */
fun Planet.asSavedTelemetry(): Planet = copy(
    regions = regions.map { it.copy(telemetryStale = true) },
    statistics = statistics.copy(counterReadings = statistics.counterReadings.mapValues { it.value.copy(stale = true) }),
)
