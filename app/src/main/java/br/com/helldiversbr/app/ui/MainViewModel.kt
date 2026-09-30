package br.com.helldiversbr.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.com.helldiversbr.app.data.HomeData
import br.com.helldiversbr.app.data.OrderRepository
import br.com.helldiversbr.app.update.RemoteVersion
import br.com.helldiversbr.app.update.UpdateChecker
import br.com.helldiversbr.app.update.UpdatePreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _home = MutableStateFlow<HomeState>(HomeState.Loading)
    val home: StateFlow<HomeState> = _home.asStateFlow()

    private val _update = MutableStateFlow<RemoteVersion?>(null)
    val update: StateFlow<RemoteVersion?> = _update.asStateFlow()

    private var refreshJob: Job? = null

    init {
        // Mostra o último snapshot persistente imediatamente; a rede revalida em paralelo.
        viewModelScope.launch {
            OrderRepository.loadCached()?.let { cached ->
                if (_home.value is HomeState.Loading) _home.value = HomeState.Ready(cached, refreshing = true)
            }
        }
        // Atualiza sozinho a cada 60 s enquanto o app está aberto (mesmo intervalo do site).
        viewModelScope.launch {
            while (true) {
                refresh()
                delay(60_000)
            }
        }
        if (UpdatePreferences.isAutoCheckEnabled(application)) {
            viewModelScope.launch {
                _update.value = UpdateChecker.check()
            }
        }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val previous = when (val s = _home.value) {
                is HomeState.Ready -> s.data
                is HomeState.Error -> s.last
                HomeState.Loading -> null
            }
            if (previous != null) _home.value = HomeState.Ready(previous, refreshing = true)
            _home.value = try {
                HomeState.Ready(OrderRepository.load())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val latest = when (val current = _home.value) {
                    is HomeState.Ready -> current.data
                    is HomeState.Error -> current.last
                    HomeState.Loading -> previous
                }
                HomeState.Error("Sem conexão com a telemetria da Super Terra.", latest)
            }
        }
    }

    fun dismissUpdate() {
        _update.value = null
    }
}
