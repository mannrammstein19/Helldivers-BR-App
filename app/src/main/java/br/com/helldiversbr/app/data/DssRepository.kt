package br.com.helldiversbr.app.data

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

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
    private const val TTL_MILLIS = 2 * 60 * 1000L

    @Volatile private var lastReading: DssReading? = null

    suspend fun load(force: Boolean = false): DssReading {
        val now = System.currentTimeMillis()
        val disk = if (lastReading == null) TelemetryCache.loadDss() else null
        val resident = lastReading ?: disk?.also { lastReading = it }
        if (!force && resident != null && !resident.stale && now - resident.fetchedAtMillis < TTL_MILLIS) {
            return resident
        }

        try {
            val stations = HelldiversApi.dssStations()
            val station = stations.firstOrNull()
            val availability = when {
                station == null -> DssAvailability.ABSENT
                station.planet.index <= 0L && localizedText(station.planet.name).isBlank() -> DssAvailability.LOCATION_UNKNOWN
                else -> DssAvailability.LIVE
            }
            return DssReading(
                station = station,
                availability = availability,
                fetchedAtMillis = now,
                stale = false,
                source = "community",
            ).also {
                lastReading = it
                TelemetryCache.saveDss(it)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (communityError: Exception) {
            try {
                val catalog = runCatching { HelldiversApi.planetCatalog() }.getOrDefault(emptyMap())
                return DirectGameApi.dss(catalog, resident).also {
                    lastReading = it
                    TelemetryCache.saveDss(it)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (directError: Exception) {
                val fallback = resident ?: TelemetryCache.loadDss()
                if (fallback != null && fallback.fetchedAtMillis > 0L) {
                    return fallback.copy(
                        stale = true,
                        source = "cache",
                        error = "Community: ${communityError.message}; Direta: ${directError.message}",
                    ).also { lastReading = it }
                }
                return DssReading(
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
