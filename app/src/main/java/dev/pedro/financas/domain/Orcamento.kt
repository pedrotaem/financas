package dev.pedro.financas.domain

import kotlinx.coroutines.flow.Flow

/** Orçamento mensal recorrente de uma categoria (spec 006). */
data class OrcamentoCategoria(
    val categoria: Categoria,
    val valor: Dinheiro,
)

interface OrcamentoRepository {
    fun observarTodos(): Flow<List<OrcamentoCategoria>>
    suspend fun definir(orcamento: OrcamentoCategoria)
    suspend fun remover(categoria: Categoria)
}

/** Consumo do orçamento de uma categoria no mês (spec 006, regra 1). */
data class ProgressoCategoria(
    val categoria: Categoria,
    val planejado: Dinheiro,
    val gasto: Dinheiro,
) {
    /** 0.0 = nada gasto; 1.0 = no limite; >1.0 = estourado. */
    val fracao: Float
        get() = if (planejado.centavos == 0L) 0f else gasto.centavos.toFloat() / planejado.centavos

    val estourado: Boolean get() = gasto.centavos > planejado.centavos
}

/** Visão real × planejado do mês (spec 006). Kotlin puro, testável. */
data class ProgressoOrcamento(
    val categorias: List<ProgressoCategoria>,
    /** Somas apenas das categorias orçadas (spec 006, decisão "Total do card"). */
    val totalPlanejado: Dinheiro,
    val totalGasto: Dinheiro,
) {
    companion object {
        fun de(orcamentos: List<OrcamentoCategoria>, fatiasDoMes: List<FatiaCategoria>): ProgressoOrcamento {
            val gastoPorCategoria = fatiasDoMes
                .filter { it.categoria != null }
                .associate { it.categoria!! to it.total }
            val categorias = orcamentos
                .map { orc ->
                    ProgressoCategoria(
                        categoria = orc.categoria,
                        planejado = orc.valor,
                        gasto = gastoPorCategoria[orc.categoria] ?: Dinheiro.ZERO,
                    )
                }
                .sortedByDescending { it.fracao }
            return ProgressoOrcamento(
                categorias = categorias,
                totalPlanejado = categorias.fold(Dinheiro.ZERO) { acc, p -> acc + p.planejado },
                totalGasto = categorias.fold(Dinheiro.ZERO) { acc, p -> acc + p.gasto },
            )
        }
    }
}
