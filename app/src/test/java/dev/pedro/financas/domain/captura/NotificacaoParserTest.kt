package dev.pedro.financas.domain.captura

import dev.pedro.financas.domain.Tipo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Fixtures aproximadas (spec 001) — calibrar com textos reais capturados
 * no aparelho via logcat (tag CapturaNotificacao).
 */
class NotificacaoParserTest {

    @Test
    fun `compra de cartao aprovada vira DEBITO com estabelecimento`() {
        val r = NotificacaoParser.parse(
            "Itaú",
            "Compra aprovada: R$ 54,90 em MERCADO PAO DE ACUCAR em 20/07 às 14:32",
        )
        r as ResultadoParse.Reconhecido
        assertEquals(Tipo.DEBITO, r.tipo)
        assertEquals(5490, r.valorCentavos)
        assertEquals("MERCADO PAO DE ACUCAR", r.estabelecimento)
    }

    @Test
    fun `pix enviado vira DEBITO com contraparte`() {
        val r = NotificacaoParser.parse("Pix enviado", "Você pagou R$ 100,00 para Fulano da Silva")
        r as ResultadoParse.Reconhecido
        assertEquals(Tipo.DEBITO, r.tipo)
        assertEquals(10000, r.valorCentavos)
        assertEquals("PIX para Fulano da Silva", r.descricao)
    }

    @Test
    fun `pix recebido vira CREDITO`() {
        val r = NotificacaoParser.parse("Itaú", "Você recebeu um Pix de R$ 250,00 de Beltrano Souza")
        r as ResultadoParse.Reconhecido
        assertEquals(Tipo.CREDITO, r.tipo)
        assertEquals(25000, r.valorCentavos)
        assertEquals("PIX de Beltrano Souza", r.descricao)
    }

    @Test
    fun `valor com milhar parseia certo`() {
        assertEquals(123456, NotificacaoParser.extrairValorCentavos("R$ 1.234,56"))
    }

    @Test
    fun `texto desconhecido com valor vira ValorDetectado`() {
        val r = NotificacaoParser.parse("Itaú", "Sua fatura de R$ 1.850,00 fecha amanhã")
        r as ResultadoParse.ValorDetectado
        assertEquals(185000, r.valorCentavos)
    }

    @Test
    fun `texto sem valor e ignorado`() {
        assertTrue(
            NotificacaoParser.parse("Itaú", "Atualize seu aplicativo") is ResultadoParse.NaoReconhecido
        )
    }
}
