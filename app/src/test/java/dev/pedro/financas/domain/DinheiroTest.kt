package dev.pedro.financas.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DinheiroTest {

    @Test
    fun `formata centavos como reais`() {
        assertEquals("R$ 54,90", Dinheiro(5490).formatado())
        assertEquals("R$ 0,01", Dinheiro(1).formatado())
        assertEquals("R$ 1.000,00".replace(".", ""), Dinheiro(100_000).formatado())
    }

    @Test
    fun `soma valores`() {
        assertEquals(Dinheiro(300), Dinheiro(100) + Dinheiro(200))
    }

    @Test
    fun `rejeita valor negativo`() {
        assertThrows(IllegalArgumentException::class.java) { Dinheiro(-1) }
    }

    @Test
    fun `deReais converte corretamente`() {
        assertEquals(Dinheiro(5490), Dinheiro.deReais(54, 90))
    }
}

class NotaFiscalTest {

    private fun nota(total: Long, vararg itens: Long) = NotaFiscal(
        id = NotaFiscalId.novo(),
        estabelecimento = "Mercado X",
        dataEmissao = LocalDate.of(2026, 7, 20),
        total = Dinheiro(total),
        itens = itens.map {
            ItemNota("item", 1.0, Dinheiro(it), Dinheiro(it))
        },
    )

    @Test
    fun `soma dos itens bate com total`() {
        val n = nota(300, 100, 200)
        assertEquals(Dinheiro(300), n.somaItens())
        assertTrue(!n.somaDiverge())
    }

    @Test
    fun `detecta divergencia entre soma e total`() {
        assertTrue(nota(500, 100, 200).somaDiverge())
    }
}
