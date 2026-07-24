package dev.pedro.financas.domain.captura

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.MonthDay

/** Fixtures anonimizadas (valores fictícios) — repo público. */
class OperadoraParserTest {

    @Test
    fun `claro pagamento confirmado`() {
        val r = OperadoraParser.parse(
            "Claro",
            "Cliente Claro, o pagamento automatico da sua fatura de julho no valor de " +
                "R$ 199,99 foi confirmado! Saiba mais, acesse o App Minha Claro.",
        )
        assertTrue(r is ResultadoOperadora.PagamentoConfirmado)
        r as ResultadoOperadora.PagamentoConfirmado
        assertEquals("Claro", r.operadora)
        assertEquals(19999, r.valorCentavos)
        assertEquals("julho", r.mesFatura)
    }

    @Test
    fun `vivo aviso cria conta futura com valor sem espaco apos cifrao`() {
        val r = OperadoraParser.parse(
            "Vivo",
            "Ola, sua conta Vivo em debito automatico no valor de R$149,90 vence em 17/07. " +
                "Lembre-se de manter saldo em conta bancaria para o pagamento dar certo ;)",
        )
        assertTrue(r is ResultadoOperadora.ContaFutura)
        r as ResultadoOperadora.ContaFutura
        assertEquals(14990, r.valorCentavos)
        assertEquals(MonthDay.of(7, 17), r.vencimento)
    }

    @Test
    fun `vivo confirmacao identifica vencimento sem valor`() {
        val r = OperadoraParser.parse(
            "Vivo",
            "Ola, deu tudo certo com o pagamento da fatura Vivo em debito automatico de " +
                "vencimento em 17/07. Sua fatura agora com mais comodidade e tranquilidade para voce.",
        )
        assertTrue(r is ResultadoOperadora.FuturoEfetivado)
        assertEquals(MonthDay.of(7, 17), (r as ResultadoOperadora.FuturoEfetivado).vencimento)
    }

    @Test
    fun `sms de marketing com valor nao e reconhecido`() {
        val r = OperadoraParser.parse(
            "Promo",
            "Aproveite! Plano novo por apenas R$ 49,90 por mes. Responda SIM.",
        )
        assertTrue(r is ResultadoOperadora.NaoReconhecido)
    }

    @Test
    fun `ano inferido no mesmo ano`() {
        val data = OperadoraParser.dataComAno(MonthDay.of(7, 17), LocalDate.of(2026, 7, 13))
        assertEquals(LocalDate.of(2026, 7, 17), data)
    }

    @Test
    fun `virada de ano joga vencimento para o ano seguinte`() {
        val data = OperadoraParser.dataComAno(MonthDay.of(1, 2), LocalDate.of(2026, 12, 29))
        assertEquals(LocalDate.of(2027, 1, 2), data)
    }

    @Test
    fun `vencimento recente no passado mantem o ano`() {
        // confirmação chega no dia (ou pouco depois) do vencimento
        val data = OperadoraParser.dataComAno(MonthDay.of(7, 17), LocalDate.of(2026, 7, 20))
        assertEquals(LocalDate.of(2026, 7, 17), data)
    }
}
