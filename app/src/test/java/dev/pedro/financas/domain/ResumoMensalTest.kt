package dev.pedro.financas.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class ResumoMensalTest {

    private val zona = ZoneId.of("America/Sao_Paulo")
    private val julho = YearMonth.of(2026, 7)

    private fun lancamento(
        tipo: Tipo,
        centavos: Long,
        instante: String = "2026-07-15T12:00:00Z",
        categoria: Categoria? = null,
        status: Status = Status.PENDENTE_REVISAO,
    ) = Lancamento(
        id = LancamentoId.novo(),
        tipo = tipo,
        valor = Dinheiro(centavos),
        dataHora = Instant.parse(instante),
        descricao = "teste",
        categoria = categoria,
        origem = Origem.MANUAL,
        status = status,
    )

    @Test
    fun `calcula receitas despesas e saldo do mes`() {
        val r = ResumoMensal.de(
            listOf(
                lancamento(Tipo.CREDITO, 500_000),
                lancamento(Tipo.DEBITO, 200_000),
                lancamento(Tipo.DEBITO, 100_000),
            ),
            julho, zona,
        )
        assertEquals(500_000, r.receitas.centavos)
        assertEquals(300_000, r.despesas.centavos)
        assertEquals(200_000, r.saldoCentavos)
    }

    @Test
    fun `saldo pode ser negativo`() {
        val r = ResumoMensal.de(listOf(lancamento(Tipo.DEBITO, 100)), julho, zona)
        assertEquals(-100, r.saldoCentavos)
        assertEquals("-R$ 1,00", formatarSaldo(-100))
    }

    @Test
    fun `ignora lancamentos de outros meses`() {
        val r = ResumoMensal.de(
            listOf(
                lancamento(Tipo.DEBITO, 100, instante = "2026-06-30T12:00:00Z"),
                lancamento(Tipo.DEBITO, 200, instante = "2026-07-01T12:00:00Z"),
            ),
            julho, zona,
        )
        assertEquals(200, r.despesas.centavos)
    }

    @Test
    fun `fronteira de mes respeita fuso do aparelho`() {
        // 01/07 00:30 em SP = 03:30 UTC; 30/06 23:00 em SP = 01/07 02:00 UTC
        val r = ResumoMensal.de(
            listOf(
                lancamento(Tipo.DEBITO, 100, instante = "2026-07-01T02:00:00Z"), // ainda junho em SP
                lancamento(Tipo.DEBITO, 200, instante = "2026-07-01T03:30:00Z"), // julho em SP
            ),
            julho, zona,
        )
        assertEquals(200, r.despesas.centavos)
    }

    @Test
    fun `lancamento futuro fica fora de todos os calculos`() {
        // spec 007, regra 1: FUTURO não entra em saldo, despesas nem donut
        val r = ResumoMensal.de(
            listOf(
                lancamento(Tipo.DEBITO, 200, categoria = Categoria.CASA),
                lancamento(Tipo.DEBITO, 9_999, categoria = Categoria.CASA, status = Status.FUTURO),
            ),
            julho, zona,
        )
        assertEquals(200, r.despesas.centavos)
        assertEquals(-200, r.saldoCentavos)
        assertEquals(200, r.fatias.single().total.centavos)
    }

    @Test
    fun `fatias agrupam por categoria maior primeiro e so debito`() {
        val r = ResumoMensal.de(
            listOf(
                lancamento(Tipo.DEBITO, 100, categoria = Categoria.MERCADO),
                lancamento(Tipo.DEBITO, 300, categoria = Categoria.MERCADO),
                lancamento(Tipo.DEBITO, 500, categoria = Categoria.TRANSPORTE),
                lancamento(Tipo.DEBITO, 50),                       // sem categoria
                lancamento(Tipo.CREDITO, 9_999, categoria = Categoria.OUTROS), // fora do donut
            ),
            julho, zona,
        )
        assertEquals(3, r.fatias.size)
        assertEquals(Categoria.TRANSPORTE, r.fatias[0].categoria)
        assertEquals(500, r.fatias[0].total.centavos)
        assertEquals(Categoria.MERCADO, r.fatias[1].categoria)
        assertEquals(400, r.fatias[1].total.centavos)
        assertNull(r.fatias[2].categoria)
    }
}
