package br.com.helldiversbr.app.data

import kotlinx.coroutines.CancellationException

/** Estado da leitura da Estação Espacial da Democracia (DSS). */
enum class DssAvailability {
    LIVE,
    ABSENT,
    LOCATION_UNKNOWN,
    CONNECTION_ERROR,
}

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
) {
    val hasStation: Boolean get() = station != null
    val isLive: Boolean get() = station != null && !stale && availability == DssAvailability.LIVE
}

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
            val stations = HelldiversApi.dssStations()
            val station = stations.firstOrNull()
            val availability = when {
                station == null -> DssAvailability.ABSENT
                station.planet.index <= 0L && localizedText(station.planet.name).isBlank() -> DssAvailability.LOCATION_UNKNOWN
                else -> DssAvailability.LIVE
            }
            DssReading(
                station = station,
                availability = availability,
                fetchedAtMillis = now,
                stale = false,
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
                ).also { lastReading = it }
            }
        }
    }
}
