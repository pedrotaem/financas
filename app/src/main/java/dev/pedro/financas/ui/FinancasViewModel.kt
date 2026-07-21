package dev.pedro.financas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.pedro.financas.AppContainer
import dev.pedro.financas.domain.Categoria
import dev.pedro.financas.domain.Dinheiro
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.LancamentoId
import dev.pedro.financas.domain.LancamentoRepository
import dev.pedro.financas.domain.Origem
import dev.pedro.financas.domain.ResumoMensal
import dev.pedro.financas.domain.Status
import dev.pedro.financas.domain.Tipo
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.domain.captura.CapturaBrutaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

data class EstadoUi(
    val mes: YearMonth,
    val resumo: ResumoMensal,
    val lancamentosDoMes: List<Lancamento>,
    val capturasPendentes: List<CapturaBruta>,
    val qtdPendentes: Int,
)

class FinancasViewModel(
    private val lancamentoRepo: LancamentoRepository,
    capturaBrutaRepo: CapturaBrutaRepository,
) : ViewModel() {

    private val zona: ZoneId = ZoneId.systemDefault()
    private val mesSelecionado = MutableStateFlow(YearMonth.now(zona))

    val estado: StateFlow<EstadoUi> = combine(
        lancamentoRepo.observarTodos(),
        capturaBrutaRepo.observarPendentes(),
        mesSelecionado,
    ) { lancamentos, capturas, mes ->
        val doMes = lancamentos
            .filter { YearMonth.from(it.dataHora.atZone(zona)) == mes }
        EstadoUi(
            mes = mes,
            resumo = ResumoMensal.de(lancamentos, mes, zona),
            lancamentosDoMes = doMes,
            capturasPendentes = capturas,
            qtdPendentes = doMes.count { it.status == Status.PENDENTE_REVISAO } + capturas.size,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EstadoUi(YearMonth.now(zona), ResumoMensal.de(emptyList(), YearMonth.now(zona), zona), emptyList(), emptyList(), 0),
    )

    fun mesAnterior() = mesSelecionado.update { it.minusMonths(1) }
    fun mesSeguinte() = mesSelecionado.update { it.plusMonths(1) }

    fun confirmar(l: Lancamento) = viewModelScope.launch { lancamentoRepo.salvar(l.confirmar()) }

    fun categorizar(l: Lancamento, categoria: Categoria) =
        viewModelScope.launch { lancamentoRepo.salvar(l.categorizar(categoria)) }

    fun adicionarManual(tipo: Tipo, valorCentavos: Long, descricao: String, categoria: Categoria?) =
        viewModelScope.launch {
            lancamentoRepo.salvar(
                Lancamento(
                    id = LancamentoId.novo(),
                    tipo = tipo,
                    valor = Dinheiro(valorCentavos),
                    dataHora = Instant.now(),
                    descricao = descricao,
                    categoria = categoria,
                    origem = Origem.MANUAL,
                    status = Status.CONFIRMADO,
                )
            )
        }

    private fun MutableStateFlow<YearMonth>.update(f: (YearMonth) -> YearMonth) {
        value = f(value)
    }

    companion object {
        fun factory(container: AppContainer) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                FinancasViewModel(container.lancamentoRepository, container.capturaBrutaRepository) as T
        }
    }
}

/** "54,90" / "54.90" / "54" → centavos. Null se inválido. */
fun parseValorParaCentavos(texto: String): Long? {
    val limpo = texto.trim().replace("R$", "").trim()
    if (limpo.isEmpty()) return null
    val normalizado = limpo.replace(".", "").replace(",", ".")
    val valor = normalizado.toBigDecimalOrNull() ?: return null
    if (valor.signum() <= 0) return null
    return valor.movePointRight(2).toLong()
}
