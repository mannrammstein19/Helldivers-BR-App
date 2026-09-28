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

data class GalaxyState(
    val planets: List<Planet> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val updatedAtMillis: Long? = null,
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
                val planets = HelldiversApi.planets()
                check(planets.isNotEmpty()) { "Catálogo vazio" }
                mutableState.value = GalaxyState(planets = planets, updatedAtMillis = System.currentTimeMillis())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mutableState.value = mutableState.value.copy(loading = false,
                    error = "Não foi possível atualizar o mapa. Última leitura preservada, quando disponível.")
            }
        }
    }
}
