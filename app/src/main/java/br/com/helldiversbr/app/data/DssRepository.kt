package br.com.helldiversbr.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.coroutines.sync.withLock

/** Estado da leitura da Estação Espacial da Democracia (DSS). */
@Serializable
enum class DssAvailability {
    LIVE,
    ABSENT,
    LOCATION_UNKNOWN,
    CONNECTION_ERROR,
}

@Serializable
data class DssReading(
    val station: SpaceStation? = null,
    val availability: DssAvailability = DssAvailability.CONNECTION_ERROR,
    /** Hora da última leitura real bem-sucedida desta informação. */
    val fetchedAtMillis: Long = 0L,
    val stale: Boolean = false,
    val error: String? = null,
    /** community | direct | cache */
    val source: String = "community",
    val lastPlanetIndex: Long? = null,
    val lastLocationReadAtMillis: Long = 0L,
) {
    val locationReference: Long? get() =
        if (lastPlanetIndex != null && lastLocationReadAtMillis > fetchedAtMillis) lastPlanetIndex
        else station?.planet?.index?.takeIf { it > 0L } ?: lastPlanetIndex
    val hasStation: Boolean get() = station != null
    val isLive: Boolean get() = station != null && !stale && availability == DssAvailability.LIVE
}

/**
 * Parede da DSS:
 * 1) API comunitária rica;
 * 2) API direta do jogo (localização/estado básico, preservando ações ricas conhecidas);
 * 3) última leitura persistente no aparelho.
 */
object DssRepository {
    private val gate = kotlinx.coroutines.sync.Mutex()
    private const val TTL_MILLIS = TelemetryRefreshPolicy.INTERVAL_MILLIS

    @Volatile private var lastReading: DssReading? = null
    private var lastAttempt = 0L

    private fun preserveReading(incoming: DssReading, previous: DssReading?): DssReading {
        val hasLocation = incoming.station?.planet?.index?.let { it > 0L } == true
        val uncertain = incoming.stale || incoming.availability == DssAvailability.LOCATION_UNKNOWN
        return if (TelemetryRefreshPolicy.preservePreviousStation(previous?.fetchedAtMillis ?: 0L,
            incoming.fetchedAtMillis, previous?.hasStation == true, hasLocation, uncertain))
            previous!!.copy(stale = true, source = "cache", error = incoming.error)
        else incoming
    }

    suspend fun load(force: Boolean = false): DssReading = gate.withLock {
        val now = System.currentTimeMillis()
        val disk = if (lastReading == null) TelemetryCache.loadDss() else null
        val resident = lastReading ?: disk?.withoutSeededLocation()?.copy(stale = true, source = "cache")?.also { lastReading = it }
        if (!force && resident != null && !resident.stale && now - resident.fetchedAtMillis in 0 until TTL_MILLIS) {
            return@withLock resident
        }

        if (now - lastAttempt in 0 until TelemetryRefreshPolicy.INTERVAL_MILLIS && resident != null)
            return@withLock resident
        lastAttempt = now
        val central = attempt { CentralApi.read("/api/v2/space-stations").let { it to CentralApi.stations(it) } }
        central.getOrNull()?.let { (r, stations) ->
            if (r.stale || r.time < (resident?.fetchedAtMillis ?: 0L)) return@let // Tenta uma fonte alternativa antes de aceitar cache.
            val station = stations.firstOrNull()
            val host = station?.planet?.index?.takeIf { it > 0 }
            val reading = DssReading(station,
                if (station == null) DssAvailability.ABSENT else if (host == null) DssAvailability.LOCATION_UNKNOWN else DssAvailability.LIVE,
                r.time, r.stale || (station != null && host == null), source = r.source,
                lastPlanetIndex = host ?: resident?.lastPlanetIndex,
                lastLocationReadAtMillis = if (host != null) r.time else resident?.lastLocationReadAtMillis ?: 0L)
            val selected = preserveReading(reading, resident)
            lastReading = selected
            TelemetryCache.saveDss(selected)
            return@withLock selected
        }
        try {
            val stations = HelldiversApi.dssStations()
            val communityReadAt = System.currentTimeMillis()
            val station = stations.firstOrNull()
            val availability = when {
                station == null -> DssAvailability.ABSENT
                station.planet.index <= 0L && localizedText(station.planet.name).isBlank() -> DssAvailability.LOCATION_UNKNOWN
                else -> DssAvailability.LIVE
            }
            return@withLock DssReading(
                station = station,
                availability = availability,
                fetchedAtMillis = communityReadAt,
                stale = false,
                source = "community",
                lastPlanetIndex = station?.planet?.index?.takeIf { it > 0 },
                lastLocationReadAtMillis = if(station?.planet?.index?.let { it > 0 } == true) communityReadAt else 0L,
            ).let { preserveReading(it, resident) }.also {
                lastReading = it
                TelemetryCache.saveDss(it)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (communityError: Exception) {
            try {
                val catalog = runCatching { HelldiversApi.planetCatalog() }.getOrDefault(emptyMap())
                return@withLock DirectGameApi.dss(catalog, resident).let { preserveReading(it, resident) }.also {
                    lastReading = it
                    TelemetryCache.saveDss(it)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (directError: Exception) {
                val fallback = resident ?: TelemetryCache.loadDss()
                if (fallback != null && fallback.fetchedAtMillis > 0L) {
                    return@withLock fallback.copy(
                        stale = true,
                        source = "cache",
                        error = "Community: ${communityError.message}; Direta: ${directError.message}",
                    ).also { lastReading = it }
                }
                return@withLock DssReading(
                    station = null,
                    availability = DssAvailability.CONNECTION_ERROR,
                    fetchedAtMillis = 0L,
                    stale = true,
                    source = "cache",
                    error = directError.message ?: communityError.message ?: "Sem conexão",
                ).also { lastReading = it }
            }
        }
    }
}

/** Only a fresh, unique DSS effect confirms a location; historical reference is never an operational state. */
fun DssReading.withPlanetReference(
    planets: List<Planet>, reading: CentralApi.Reading, now: Long = System.currentTimeMillis(),
): DssReading {
    val candidates = mutableListOf<Pair<Long, Long>>()
    if (DssSupport.isCurrent(this, now)) station?.planet?.index?.takeIf { it > 0 }?.let {
        candidates += it to fetchedAtMillis
    }
    val freshPlanets = !reading.stale && reading.source != "cache" && reading.time > 0 &&
        reading.time <= now + 60_000L && now - reading.time <= 300_000L
    if (freshPlanets) planets.filter { p ->
        PlanetEffects.values(p).any { PlanetPresences.effectId(it) == 1217L }
    }.distinctBy { it.index }.singleOrNull()?.index?.takeIf { it > 0 }?.let {
        candidates += it to reading.time
    }
    val latestTime = candidates.maxOfOrNull { it.second } ?: return this
    val latest = candidates.filter { it.second == latestTime }.map { it.first }.distinct().singleOrNull()
        ?: return this // Contradictory simultaneous observations do not confirm a move.
    if (latestTime < lastLocationReadAtMillis ||
        (latestTime == lastLocationReadAtMillis && lastPlanetIndex != null && latest != lastPlanetIndex)) return this
    return copy(lastPlanetIndex = latest, lastLocationReadAtMillis = latestTime)
}

/** Removes the V41 default, which was not an observation on this device. */
fun DssReading.withoutSeededLocation(): DssReading =
    if (lastPlanetIndex == 136L && lastLocationReadAtMillis == 1791081370763L &&
        !(station?.planet?.index == 136L && fetchedAtMillis == lastLocationReadAtMillis))
        copy(lastPlanetIndex = null, lastLocationReadAtMillis = 0L) else this
