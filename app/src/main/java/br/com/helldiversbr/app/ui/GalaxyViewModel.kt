package br.com.helldiversbr.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val dssHost: Long? get() = dss.station?.planet?.index?.takeIf { it > 0L }
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
                        planets = disk.planets,
                        loading = true,
                        error = "Exibindo a última leitura salva enquanto a rede é revalidada.",
                        updatedAtMillis = disk.updatedAtMillis,
                        dss = disk.dss.copy(stale = true, source = "cache"),
                        planetCatalog = disk.planetCatalog,
                        telemetrySource = "cache",
                    )
                }
                val (catalog, dss) = supervisorScope {
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
                    catalogTask.await() to station.await()
                }

                val community = runCatching { HelldiversApi.planets() }
                val source: String
                val planets: List<Planet>
                val updated: Long
                when {
                    community.isSuccess && community.getOrThrow().isNotEmpty() -> {
                        source = "community"
                        planets = community.getOrThrow()
                        updated = System.currentTimeMillis()
                    }
                    else -> {
                        val direct = runCatching { DirectGameApi.warData(catalog).planets }
                        if (direct.isSuccess && direct.getOrThrow().isNotEmpty()) {
                            source = "direct"
                            planets = direct.getOrThrow()
                            updated = System.currentTimeMillis()
                        } else {
                            val cached = disk?.takeIf { it.planets.isNotEmpty() }
                                ?: previous.takeIf { it.planets.isNotEmpty() }?.let {
                                    GalaxyCache(it.planets, it.dss, it.planetCatalog, it.updatedAtMillis ?: 0L, "cache")
                                }
                                ?: throw (direct.exceptionOrNull() ?: community.exceptionOrNull() ?: IllegalStateException("Sem mapa salvo"))
                            source = "cache"
                            planets = cached.planets
                            updated = cached.updatedAtMillis
                        }
                    }
                }

                val next = GalaxyState(
                    planets = planets,
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
                    error = "Não foi possível atualizar o mapa e ainda não existe leitura salva no aparelho.",
                )
            }
        }
    }
}
