package dev.pedro.financas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.pedro.financas.AppContainer
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.LancamentoRepository
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.domain.captura.CapturaBrutaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val lancamentoRepo: LancamentoRepository,
    capturaBrutaRepo: CapturaBrutaRepository,
) : ViewModel() {

    val lancamentos: StateFlow<List<Lancamento>> = lancamentoRepo.observarTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val capturasPendentes: StateFlow<List<CapturaBruta>> = capturaBrutaRepo.observarPendentes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun confirmar(lancamento: Lancamento) {
        viewModelScope.launch { lancamentoRepo.salvar(lancamento.confirmar()) }
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(container.lancamentoRepository, container.capturaBrutaRepository) as T
        }
    }
}
