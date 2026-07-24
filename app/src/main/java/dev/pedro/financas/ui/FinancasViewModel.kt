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
import dev.pedro.financas.domain.OrcamentoCategoria
import dev.pedro.financas.domain.OrcamentoRepository
import dev.pedro.financas.domain.Origem
import dev.pedro.financas.domain.ProgressoOrcamento
import dev.pedro.financas.domain.ResumoMensal
import dev.pedro.financas.domain.Status
import dev.pedro.financas.domain.Tipo
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.domain.captura.CapturaBrutaRepository
import dev.pedro.financas.infrastructure.preferencias.PreferenciasApp
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
    /** Spec 005, regra 3: cobre todo valor monetário renderizado. */
    val saldoOculto: Boolean = false,
    /** Real × planejado do mês selecionado (spec 006). */
    val progressoOrcamento: ProgressoOrcamento = ProgressoOrcamento(emptyList(), Dinheiro.ZERO, Dinheiro.ZERO),
)

class FinancasViewModel(
    private val lancamentoRepo: LancamentoRepository,
    private val capturaBrutaRepo: CapturaBrutaRepository,
    private val preferencias: PreferenciasApp,
    private val orcamentoRepo: OrcamentoRepository,
) : ViewModel() {

    private val zona: ZoneId = ZoneId.systemDefault()
    private val mesSelecionado = MutableStateFlow(YearMonth.now(zona))

    val temaOled: StateFlow<Boolean> = preferencias.temaOled

    val estado: StateFlow<EstadoUi> = combine(
        lancamentoRepo.observarTodos(),
        capturaBrutaRepo.observarPendentes(),
        mesSelecionado,
        preferencias.saldoOculto,
        orcamentoRepo.observarTodos(),
    ) { lancamentos, capturas, mes, saldoOculto, orcamentos ->
        val doMes = lancamentos
            .filter { YearMonth.from(it.dataHora.atZone(zona)) == mes }
        val resumo = ResumoMensal.de(lancamentos, mes, zona)
        EstadoUi(
            mes = mes,
            resumo = resumo,
            lancamentosDoMes = doMes,
            capturasPendentes = capturas,
            qtdPendentes = doMes.count { it.status == Status.PENDENTE_REVISAO } + capturas.size,
            saldoOculto = saldoOculto,
            progressoOrcamento = ProgressoOrcamento.de(orcamentos, resumo.fatias),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        EstadoUi(YearMonth.now(zona), ResumoMensal.de(emptyList(), YearMonth.now(zona), zona), emptyList(), emptyList(), 0),
    )

    fun alternarSaldoOculto() = preferencias.alternarSaldoOculto()
    fun alternarTemaOled() = preferencias.alternarTemaOled()

    /** Spec 006: valor zero remove o orçamento da categoria. */
    fun definirOrcamento(categoria: Categoria, valorCentavos: Long) = viewModelScope.launch {
        if (valorCentavos <= 0L) orcamentoRepo.remover(categoria)
        else orcamentoRepo.definir(OrcamentoCategoria(categoria, Dinheiro(valorCentavos)))
    }

    fun removerOrcamento(categoria: Categoria) = viewModelScope.launch {
        orcamentoRepo.remover(categoria)
    }

    fun mesAnterior() = mesSelecionado.update { it.minusMonths(1) }
    fun mesSeguinte() = mesSelecionado.update { it.plusMonths(1) }

    fun confirmar(l: Lancamento) = viewModelScope.launch { lancamentoRepo.salvar(l.confirmar()) }

    /** Exclusão definitiva; dedup impede recaptura (spec 004, regra 1). */
    fun excluir(l: Lancamento) = viewModelScope.launch { lancamentoRepo.excluir(l.id) }

    fun rejeitar(l: Lancamento) = excluir(l)

    /** Spec 004, regra 2: edição preserva id, dataHora, origem, status e auditoria. */
    fun editar(original: Lancamento, tipo: Tipo, valorCentavos: Long, descricao: String, categoria: Categoria?) =
        viewModelScope.launch {
            lancamentoRepo.salvar(
                original.copy(
                    tipo = tipo,
                    valor = Dinheiro(valorCentavos),
                    descricao = descricao,
                    categoria = categoria,
                )
            )
        }

    /** Spec 004, regra 4: some da fila, mas fica no banco para auditoria. */
    fun descartarCaptura(c: CapturaBruta) =
        viewModelScope.launch { capturaBrutaRepo.salvar(c.processar()) }

    /** Spec 004, regra 3: cria o lançamento e tira a captura da fila na mesma operação. */
    fun adicionarDeCaptura(c: CapturaBruta, tipo: Tipo, valorCentavos: Long, descricao: String, categoria: Categoria?) =
        viewModelScope.launch {
            lancamentoRepo.salvar(
                Lancamento(
                    id = LancamentoId.novo(),
                    tipo = tipo,
                    valor = Dinheiro(valorCentavos),
                    dataHora = c.dataHora,
                    descricao = descricao,
                    categoria = categoria,
                    origem = Origem.NOTIFICACAO_ITAU,
                    status = Status.CONFIRMADO,
                    textoOrigem = c.texto,
                )
            )
            capturaBrutaRepo.salvar(c.processar())
        }

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
                FinancasViewModel(
                    container.lancamentoRepository,
                    container.capturaBrutaRepository,
                    container.preferencias,
                    container.orcamentoRepository,
                ) as T
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
