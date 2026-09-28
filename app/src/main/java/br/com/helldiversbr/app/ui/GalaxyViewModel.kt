package br.com.helldiversbr.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.helldiversbr.app.data.HelldiversApi
import br.com.helldiversbr.app.data.Planet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

data class GalaxyState(
    val planets: List<Planet> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val updatedAtMillis: Long? = null,
    val dssHost: Long? = null,
)

class GalaxyViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(GalaxyState())
    val state = mutableState.asStateFlow()
    private var request: Job? = null

    fun refresh() {
        if (request?.isActive == true) return
        request = viewModelScope.launch {
            mutableState.value = mutableState.value.copy(loading = true, error = null)
            try {
                val (planets, dss) = supervisorScope {
                    val station = async { try { HelldiversApi.dssHost() } catch (e: CancellationException) { throw e } catch (e: Exception) { null } }
                    val worlds = HelldiversApi.planets()
                    worlds to station.await()
                }
                check(planets.isNotEmpty()) { "Catálogo vazio" }
                mutableState.value = GalaxyState(planets = planets, updatedAtMillis = System.currentTimeMillis(), dssHost = dss)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.value = mutableState.value.copy(loading = false,
                    error = "Não foi possível atualizar o mapa. Última leitura preservada, quando disponível.")
            }
        }
    }
}
