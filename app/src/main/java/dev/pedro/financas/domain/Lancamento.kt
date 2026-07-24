package dev.pedro.financas.domain

import java.time.Instant
import java.util.UUID

@JvmInline
value class LancamentoId(val valor: String) {
    companion object {
        fun novo() = LancamentoId(UUID.randomUUID().toString())
    }
}

enum class Tipo { DEBITO, CREDITO }

enum class Origem { NOTIFICACAO_ITAU, NOTIFICACAO_SMS, NOTA_FISCAL, MANUAL }

/** FUTURO (spec 007): agendado, fora dos totais até ser efetivado. */
enum class Status { PENDENTE_REVISAO, CONFIRMADO, FUTURO }

enum class Categoria {
    MERCADO, RESTAURANTE, FARMACIA, TRANSPORTE, COMBUSTIVEL,
    LAZER, VESTUARIO, CASA, EDUCACAO, OUTROS
}

/**
 * Agregado raiz do contexto Gestão Financeira (spec 000).
 * Todo registro financeiro — capturado ou manual — é um Lançamento.
 */
data class Lancamento(
    val id: LancamentoId,
    val tipo: Tipo,
    val valor: Dinheiro,
    val dataHora: Instant,
    val descricao: String,
    val estabelecimento: String? = null,
    val categoria: Categoria? = null,
    val origem: Origem,
    val status: Status = Status.PENDENTE_REVISAO,
    val notaFiscalId: NotaFiscalId? = null,
    /** Texto cru da notificação de origem (auditoria, spec 001). */
    val textoOrigem: String? = null,
) {
    fun confirmar(): Lancamento = copy(status = Status.CONFIRMADO)

    /** Futuro vira presente (spec 007, regra 3): entra nos totais e no fluxo de revisão. */
    fun efetivar(): Lancamento = copy(status = Status.PENDENTE_REVISAO)

    fun categorizar(categoria: Categoria): Lancamento = copy(categoria = categoria)
}
