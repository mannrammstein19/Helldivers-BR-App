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
    val lastPlanetIndex: Long? = 136L,
    val lastLocationReadAtMillis: Long = 1791081370763L,
) {
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
    private const val TTL_MILLIS = 2 * 60 * 1000L

    @Volatile private var lastReading: DssReading? = null

    suspend fun load(force: Boolean = false): DssReading = gate.withLock {
        val now = System.currentTimeMillis()
        val disk = if (lastReading == null) TelemetryCache.loadDss() else null
        val resident = lastReading ?: disk?.also { lastReading = it }
        if (!force && resident != null && !resident.stale && now - resident.fetchedAtMillis < TTL_MILLIS) {
            return@withLock resident
        }

        val central = attempt { CentralApi.read("/api/v2/space-stations") }
        central.getOrNull()?.let { r ->
            val station = CentralApi.stations(r).firstOrNull()
            val host = station?.planet?.index?.takeIf { it > 0 }
            val reading = DssReading(station,
                if (station == null) DssAvailability.ABSENT else if (host == null) DssAvailability.LOCATION_UNKNOWN else DssAvailability.LIVE,
                r.time, r.stale, source = r.source,
                lastPlanetIndex = host ?: resident?.lastPlanetIndex ?: 136L,
                lastLocationReadAtMillis = if (host != null) r.time else resident?.lastLocationReadAtMillis ?: 1791081370763L)
            lastReading = reading
            TelemetryCache.saveDss(reading)
            return@withLock reading
        }
        if (resident != null && resident.fetchedAtMillis > 0) return@withLock resident.copy(stale = true, source = "cache").also { lastReading = it }
        try {
            val stations = HelldiversApi.dssStations()
            val station = stations.firstOrNull()
            val availability = when {
                station == null -> DssAvailability.ABSENT
                station.planet.index <= 0L && localizedText(station.planet.name).isBlank() -> DssAvailability.LOCATION_UNKNOWN
                else -> DssAvailability.LIVE
            }
            return@withLock DssReading(
                station = station,
                availability = availability,
                fetchedAtMillis = now,
                stale = false,
                source = "community",
                lastPlanetIndex = station?.planet?.index?.takeIf { it > 0 } ?: 136L,
                lastLocationReadAtMillis = if(station?.planet?.index?.let { it > 0 } == true) now else 1791081370763L,
            ).also {
                lastReading = it
                TelemetryCache.saveDss(it)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (communityError: Exception) {
            try {
                val catalog = runCatching { HelldiversApi.planetCatalog() }.getOrDefault(emptyMap())
                return@withLock DirectGameApi.dss(catalog, resident).also {
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
fun DssReading.withPlanetReference(planets: List<Planet>, reading: CentralApi.Reading): DssReading {
    val liveHost = station?.planet?.index?.takeIf { it > 0 && isLive }
    val effectHosts = if (!reading.stale) planets.filter { p -> p.activeEffects.any { PlanetPresences.effectId(it) == 1217L } } else emptyList()
    val host = liveHost ?: effectHosts.singleOrNull()?.index
    return copy(lastPlanetIndex = host ?: lastPlanetIndex,
        lastLocationReadAtMillis = if (liveHost != null) fetchedAtMillis else if (host != null) reading.time else lastLocationReadAtMillis)
}
