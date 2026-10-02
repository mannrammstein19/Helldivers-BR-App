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

    fun enrich(planet: Planet, readings: Map<Key, Reading>, previous: Planet?, now: Long, liveBase: Boolean): Planet =
        planet.copy(regions = planet.regions.map { region ->
            val reading = region.id?.let { readings[Key(planet.index, it)] }
            val fresh = reading?.takeIf { now - it.readAtMillis in 0..90_000L }
            val prior = previous?.regions?.firstOrNull {
                if (region.id != null && it.id != null) region.id == it.id &&
                    (region.hash == null || it.hash == null || region.hash == it.hash)
                else region.hash != null && region.hash == it.hash
            }
            when {
                fresh != null && fresh.available == null && prior?.isAvailable != null && ownerId(prior.owner) == fresh.owner ->
                    region.copy(owner = prior.owner, health = prior.health, maxHealth = prior.maxHealth,
                        isAvailable = prior.isAvailable, availabilityFactor = prior.availabilityFactor,
                        players = prior.players, regenPerSecond = prior.regenPerSecond,
                        telemetryReadAtMillis = prior.telemetryReadAtMillis,
                        telemetrySource = prior.telemetrySource, telemetryStale = true)
                fresh != null -> region.copy(owner = JsonPrimitive(fresh.owner), health = fresh.health,
                    isAvailable = fresh.available, availabilityFactor = fresh.availabilityFactor,
                    players = fresh.players, regenPerSecond = fresh.regen,
                    telemetryReadAtMillis = fresh.readAtMillis, telemetrySource = fresh.source, telemetryStale = false)
                liveBase && ownerId(region.owner) != null && !region.telemetryStale ->
                    region.copy(telemetryReadAtMillis = region.telemetryReadAtMillis.takeIf { it > 0 } ?: now)
                prior != null && ownerId(prior.owner) != null -> region.copy(
                    owner = prior.owner, health = prior.health, maxHealth = prior.maxHealth,
                    isAvailable = prior.isAvailable, availabilityFactor = prior.availabilityFactor,
                    players = prior.players, regenPerSecond = prior.regenPerSecond,
                    telemetryReadAtMillis = prior.telemetryReadAtMillis,
                    telemetrySource = prior.telemetrySource, telemetryStale = true)
                else -> region.copy(telemetryStale = !liveBase || ownerId(region.owner) == null)
            }
        })
}

/** Reusing a saved snapshot never refreshes a field's actual observation time. */
fun Planet.asSavedTelemetry(): Planet = copy(
    regions = regions.map { it.copy(telemetryStale = true) },
    statistics = statistics.copy(counterReadings = statistics.counterReadings.mapValues { it.value.copy(stale = true) }),
)
