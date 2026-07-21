package dev.pedro.financas.domain

import kotlinx.coroutines.flow.Flow

/**
 * Porta de persistência do agregado Lancamento.
 * Implementação em infrastructure (Room). Domain não conhece Android.
 */
interface LancamentoRepository {
    fun observarTodos(): Flow<List<Lancamento>>
    suspend fun buscar(id: LancamentoId): Lancamento?
    suspend fun salvar(lancamento: Lancamento)
    suspend fun excluir(id: LancamentoId)
}

interface NotaFiscalRepository {
    suspend fun buscar(id: NotaFiscalId): NotaFiscal?
    suspend fun salvar(nota: NotaFiscal)
}
