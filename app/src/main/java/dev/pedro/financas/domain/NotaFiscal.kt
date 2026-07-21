package dev.pedro.financas.domain

import java.time.LocalDate
import java.util.UUID

@JvmInline
value class NotaFiscalId(val valor: String) {
    companion object {
        fun novo() = NotaFiscalId(UUID.randomUUID().toString())
    }
}

/**
 * Agregado do contexto Captura (spec 002): nota fiscal fotografada,
 * com itens extraídos via LLM. Detalha um Lançamento.
 */
data class NotaFiscal(
    val id: NotaFiscalId,
    val estabelecimento: String,
    val cnpj: String? = null,
    val dataEmissao: LocalDate? = null,
    val total: Dinheiro,
    val itens: List<ItemNota>,
) {
    /** Soma dos itens pode divergir do total (descontos/arredondamento) — spec 002 regra 2. */
    fun somaItens(): Dinheiro = itens.fold(Dinheiro.ZERO) { acc, item -> acc + item.valorTotal }

    fun somaDiverge(): Boolean = somaItens() != total
}

data class ItemNota(
    val descricao: String,
    val quantidade: Double,
    val valorUnitario: Dinheiro,
    val valorTotal: Dinheiro,
)
