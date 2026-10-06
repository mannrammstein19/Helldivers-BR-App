package br.com.helldiversbr.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.helldiversbr.app.data.CentralApi
import br.com.helldiversbr.app.data.withCentralReading
import br.com.helldiversbr.app.data.withPlanetReference
import br.com.helldiversbr.app.data.RegionTelemetry
import br.com.helldiversbr.app.data.CounterTelemetry
import br.com.helldiversbr.app.data.asSavedTelemetry
import br.com.helldiversbr.app.data.DirectGameApi
import br.com.helldiversbr.app.data.DssReading
import br.com.helldiversbr.app.data.DssRepository
import br.com.helldiversbr.app.data.GalaxyCache
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.data.Planet
import br.com.helldiversbr.app.data.PlanetCatalogEntry
import br.com.helldiversbr.app.data.TelemetryCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class GalaxyState(
    val planets: List<Planet> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val updatedAtMillis: Long? = null,
    val dss: DssReading = DssReading(),
    val planetCatalog: Map<Long, PlanetCatalogEntry> = emptyMap(),
    /** community | direct | cache */
    val telemetrySource: String = "community",
) {
    val dssHost: Long? get() = dss.station?.planet?.index?.takeIf { it > 0L } ?: dss.lastPlanetIndex
}

class GalaxyViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(GalaxyState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null

    fun refresh() {
        if (request?.isActive == true) return
        request = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val disk = TelemetryCache.loadGalaxy()
                val previous = mutableState.value
                if (previous.planets.isEmpty() && disk != null && disk.planets.isNotEmpty()) {
                    mutableState.value = GalaxyState(
                        planets = disk.planets.map { it.asSavedTelemetry() },
                        loading = true,
                        error = "Exibindo a última leitura salva enquanto a rede é revalidada.",
                        updatedAtMillis = disk.updatedAtMillis,
                        dss = disk.dss.copy(stale = true, source = "cache"),
                        planetCatalog = disk.planetCatalog,
                        telemetrySource = "cache",
                    )
                }
                val central = try { CentralApi.read("/api/v1/planets") }
                    catch (e: CancellationException) { throw e } catch (_: Exception) { null }
                if (central != null) {
                    val raw = CentralApi.planets(central)
                    require(raw.size >= 10 && raw.map { it.index }.distinct().size == raw.size)
                    val prior = disk?.planets.orEmpty()
                    val counters = CounterTelemetry.collect(prior, disk?.updatedAtMillis ?: 0,
                        listOf(raw to central.source), central.time, listOf(central.time))
                    val priorById = prior.associateBy { it.index }
                    val fresh = raw.map {
                        RegionTelemetry.enrich(CounterTelemetry.enrich(it, counters).withCentralReading(central),
                            emptyMap(), priorById[it.index], central.time, !central.stale)
                            .let { p -> if (central.stale) p.asSavedTelemetry() else p }
                    }
                    val dss = DssRepository.load().withPlanetReference(fresh, central)
                    TelemetryCache.saveDss(dss)
                    val catalog = try { HelldiversApi.planetCatalog() }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { disk?.planetCatalog ?: previous.planetCatalog }
                    val source = if (central.stale) "cache" else central.source
                    mutableState.value = GalaxyState(fresh, false, if (central.stale) "Última leitura salva" else null,
                        central.time, dss, catalog, source)
                    TelemetryCache.saveGalaxy(GalaxyCache(fresh, dss, catalog, central.time, source))
                    return@launch
                }
                if (disk != null && disk.planets.isNotEmpty()) {
                    mutableState.value = GalaxyState(disk.planets.map { it.asSavedTelemetry() }, false,
                        "Central sem atualização • última leitura salva", disk.updatedAtMillis,
                        disk.dss.copy(stale = true, source = "cache"), disk.planetCatalog, "cache")
                    return@launch
                }
                val (catalog, dss, regional) = supervisorScope {
                    val regionalTask = async { RegionTelemetry.load() }
                    val station = async {
                        try { DssRepository.load() }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { disk?.dss ?: previous.dss }
                    }
                    val catalogTask = async {
                        try {
                            HelldiversApi.planetCatalog()
                                .ifEmpty { previous.planetCatalog.ifEmpty { disk?.planetCatalog.orEmpty() } }
                        }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { previous.planetCatalog.ifEmpty { disk?.planetCatalog.orEmpty() } }
                    }
                    Triple(catalogTask.await(), station.await(), regionalTask.await())
                }

                val community = runCatching { HelldiversApi.planets() }.also { if (it.exceptionOrNull() is CancellationException) throw it.exceptionOrNull()!! }
                val source: String
                val planets: List<Planet>
                val updated: Long
                when {
                    community.isSuccess && community.getOrThrow().size >= 10 -> {
                        source = "community"
                        planets = community.getOrThrow()
                        updated = System.currentTimeMillis()
                    }
                    else -> {
                        val direct = runCatching { DirectGameApi.warData(catalog).planets }.also { if (it.exceptionOrNull() is CancellationException) throw it.exceptionOrNull()!! }
                        if (direct.isSuccess && direct.getOrThrow().size >= 10) {
                            source = "direct"
                            planets = direct.getOrThrow()
                            updated = System.currentTimeMillis()
                        } else {
                            val cached = listOfNotNull(disk?.takeIf { it.planets.isNotEmpty() },
                                previous.takeIf { it.planets.isNotEmpty() }?.let {
                                    GalaxyCache(it.planets, it.dss, it.planetCatalog, it.updatedAtMillis ?: 0L, "cache")
                                }).maxByOrNull { it.updatedAtMillis }
                                ?: throw (direct.exceptionOrNull() ?: community.exceptionOrNull() ?: IllegalStateException("Sem mapa salvo"))
                            source = "cache"
                            planets = cached.planets
                            updated = cached.updatedAtMillis
                        }
                    }
                }

                val prior = listOfNotNull(disk, previous.takeIf { it.planets.isNotEmpty() }?.let {
                    GalaxyCache(it.planets, it.dss, it.planetCatalog, it.updatedAtMillis ?: 0L, it.telemetrySource)
                }).maxByOrNull { it.updatedAtMillis }
                val oldById = prior?.planets.orEmpty().associateBy { it.index }
                val now = System.currentTimeMillis()
                val counters = CounterTelemetry.collect(prior?.planets.orEmpty(), prior?.updatedAtMillis ?: 0L,
                    listOf(planets to source), now, listOf(updated))
                val enriched = planets.map { planet ->
                    RegionTelemetry.enrich(CounterTelemetry.enrich(planet, counters), regional,
                        oldById[planet.index], now, source != "cache")
                }
                val next = GalaxyState(
                    planets = enriched,
                    updatedAtMillis = updated,
                    dss = dss,
                    planetCatalog = catalog.ifEmpty { disk?.planetCatalog.orEmpty() },
                    telemetrySource = source,
                    error = if (source == "cache") "Exibindo a última leitura salva no aparelho." else null,
                )
                mutableState.value = next
                if (source != "cache") {
                    TelemetryCache.saveGalaxy(
                        GalaxyCache(next.planets, next.dss, next.planetCatalog, updated, source)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    error = if (mutableState.value.planets.isNotEmpty()) "Não foi possível atualizar o mapa. Exibindo a última leitura disponível."
                        else "Não foi possível atualizar o mapa e ainda não existe leitura salva no aparelho.",
                )
            }
        }
    }
}
