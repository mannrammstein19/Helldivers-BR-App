package br.com.helldiversbr.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.update.RemoteVersion
import br.com.helldiversbr.app.update.UpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeState {
    data object Loading : HomeState
    data class Ready(val data: HomeData, val refreshing: Boolean = false) : HomeState
    /** [last] mantém os últimos dados bons na tela quando uma atualização falha. */
    data class Error(val message: String, val last: HomeData? = null) : HomeState
}

class MainViewModel : ViewModel() {

    private val _home = MutableStateFlow<HomeState>(HomeState.Loading)
    val home: StateFlow<HomeState> = _home.asStateFlow()

    private val _update = MutableStateFlow<RemoteVersion?>(null)
    val update: StateFlow<RemoteVersion?> = _update.asStateFlow()

    init {
        // Atualiza sozinho a cada 60 s enquanto o app está aberto (mesmo intervalo do site).
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(60_000)
            }
        }
        viewModelScope.launch {
            _update.value = UpdateChecker.check()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val previous = when (val s = _home.value) {
                is HomeState.Ready -> s.data
                is HomeState.Error -> s.last
                HomeState.Loading -> null
            }
            if (previous != null) _home.value = HomeState.Ready(previous, refreshing = true)
            _home.value = try {
                HomeState.Ready(OrderRepository.load())
            } catch (e: Exception) {
                HomeState.Error("Sem conexão com a telemetria da Super Terra.", previous)
            }
        }
    }

    fun dismissUpdate() {
        _update.value = null
    }
}
