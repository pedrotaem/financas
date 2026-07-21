package dev.pedro.financas.domain

import java.time.YearMonth
import java.time.ZoneId

/**
 * Agregações do dashboard (spec 003). Kotlin puro, testável.
 */
data class FatiaCategoria(
    val categoria: Categoria?, // null = "Sem categoria"
    val total: Dinheiro,
)

data class ResumoMensal(
    val receitas: Dinheiro,
    val despesas: Dinheiro,
    /** Pode ser negativo — por isso Long, não Dinheiro. */
    val saldoCentavos: Long,
    /** Despesas por categoria, maior primeiro (spec 003, regra 3: só DEBITO). */
    val fatias: List<FatiaCategoria>,
) {
    companion object {
        fun de(lancamentos: List<Lancamento>, mes: YearMonth, zona: ZoneId): ResumoMensal {
            val doMes = lancamentos.filter {
                YearMonth.from(it.dataHora.atZone(zona)) == mes
            }
            val receitas = doMes.filter { it.tipo == Tipo.CREDITO }
                .fold(Dinheiro.ZERO) { acc, l -> acc + l.valor }
            val despesas = doMes.filter { it.tipo == Tipo.DEBITO }
                .fold(Dinheiro.ZERO) { acc, l -> acc + l.valor }
            val fatias = doMes.filter { it.tipo == Tipo.DEBITO }
                .groupBy { it.categoria }
                .map { (cat, ls) ->
                    FatiaCategoria(cat, ls.fold(Dinheiro.ZERO) { acc, l -> acc + l.valor })
                }
                .sortedByDescending { it.total.centavos }
            return ResumoMensal(
                receitas = receitas,
                despesas = despesas,
                saldoCentavos = receitas.centavos - despesas.centavos,
                fatias = fatias,
            )
        }
    }
}

/** Formata saldo possivelmente negativo. */
fun formatarSaldo(centavos: Long): String {
    val abs = kotlin.math.abs(centavos)
    val texto = "R$ %d,%02d".format(abs / 100, abs % 100)
    return if (centavos < 0) "-$texto" else texto
}
