package br.com.helldiversbr.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.helldiversbr.app.data.DssReading
import br.com.helldiversbr.app.data.DssRepository
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.data.Planet
import br.com.helldiversbr.app.data.PlanetCatalogEntry
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
                val (planets, dss, catalog) = supervisorScope {
                    val station = async {
                        try {
                            DssRepository.load()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            DssReading()
                        }
                    }
                    val catalogTask = async {
                        try {
                            HelldiversApi.planetCatalog()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            emptyMap()
                        }
                    }
                    val worlds = HelldiversApi.planets()
                    Triple(worlds, station.await(), catalogTask.await())
                }
                check(planets.isNotEmpty()) { "Catálogo vazio" }
                mutableState.value = GalaxyState(
                    planets = planets,
                    updatedAtMillis = System.currentTimeMillis(),
                    dss = dss,
                    planetCatalog = catalog.ifEmpty { mutableState.value.planetCatalog },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    loading = false,
                    error = "Não foi possível atualizar o mapa. Última leitura preservada, quando disponível.",
                )
            }
        }
    }
}
