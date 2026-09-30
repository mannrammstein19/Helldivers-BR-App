package br.com.helldiversbr.app.data

import kotlinx.coroutines.CancellationException
<<<<<<< HEAD
import kotlinx.serialization.Serializable

/** Estado da leitura da Estação Espacial da Democracia (DSS). */
@Serializable
=======

/** Estado da leitura da Estação Espacial da Democracia (DSS). */
>>>>>>> 4126736d414f57bf192f28a9f89522910ca923d0
enum class DssAvailability {
    LIVE,
    ABSENT,
    LOCATION_UNKNOWN,
    CONNECTION_ERROR,
}

<<<<<<< HEAD
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
=======
/**
 * Leitura da DSS alinhada à Central de Guerra web:
 * - respeita o TTL de 2 minutos usado pelo portal;
 * - diferencia falha de conexão, ausência de estação e localização não informada;
 * - preserva a última leitura boa por até 24 h enquanto o processo do app estiver vivo.
 *
 * A persistência em disco continuará sendo implementada junto da futura parede geral de
 * telemetria; esta etapa não finge que o cache em memória é um cache offline completo.
 */
data class DssReading(
    val station: SpaceStation? = null,
    val availability: DssAvailability = DssAvailability.CONNECTION_ERROR,
    val fetchedAtMillis: Long = 0L,
    val stale: Boolean = false,
    val error: String? = null,
>>>>>>> 4126736d414f57bf192f28a9f89522910ca923d0
) {
    val hasStation: Boolean get() = station != null
    val isLive: Boolean get() = station != null && !stale && availability == DssAvailability.LIVE
}

<<<<<<< HEAD
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
=======
object DssRepository {
    private const val TTL_MILLIS = 2 * 60 * 1000L
    private const val MAX_FALLBACK_AGE_MILLIS = 24 * 60 * 60 * 1000L

    @Volatile
    private var lastReading: DssReading? = null

    suspend fun load(force: Boolean = false): DssReading {
        val now = System.currentTimeMillis()
        val resident = lastReading
        if (!force && resident != null && now - resident.fetchedAtMillis < TTL_MILLIS) {
            return resident
        }

        return try {
>>>>>>> 4126736d414f57bf192f28a9f89522910ca923d0
            val stations = HelldiversApi.dssStations()
            val station = stations.firstOrNull()
            val availability = when {
                station == null -> DssAvailability.ABSENT
                station.planet.index <= 0L && localizedText(station.planet.name).isBlank() -> DssAvailability.LOCATION_UNKNOWN
                else -> DssAvailability.LIVE
            }
<<<<<<< HEAD
            return DssReading(
=======
            DssReading(
>>>>>>> 4126736d414f57bf192f28a9f89522910ca923d0
                station = station,
                availability = availability,
                fetchedAtMillis = now,
                stale = false,
<<<<<<< HEAD
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
=======
            ).also { lastReading = it }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            val fallback = resident?.takeIf {
                it.station != null && now - it.fetchedAtMillis <= MAX_FALLBACK_AGE_MILLIS
            }
            if (fallback != null) {
                fallback.copy(
                    stale = true,
                    error = error.message ?: error.javaClass.simpleName,
                ).also { lastReading = it }
            } else {
                DssReading(
                    station = null,
                    availability = DssAvailability.CONNECTION_ERROR,
                    fetchedAtMillis = now,
                    stale = false,
                    error = error.message ?: error.javaClass.simpleName,
>>>>>>> 4126736d414f57bf192f28a9f89522910ca923d0
                ).also { lastReading = it }
            }
        }
    }
}
