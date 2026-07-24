package dev.pedro.financas.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ProgressoOrcamentoTest {

    private fun orc(cat: Categoria, reais: Long) = OrcamentoCategoria(cat, Dinheiro.deReais(reais))
    private fun fatia(cat: Categoria?, reais: Long) = FatiaCategoria(cat, Dinheiro.deReais(reais))

    @Test
    fun `categoria orcada sem gasto mostra zero`() {
        val p = ProgressoOrcamento.de(listOf(orc(Categoria.MERCADO, 500)), emptyList())
        assertEquals(Dinheiro.ZERO, p.categorias.single().gasto)
        assertEquals(0f, p.categorias.single().fracao)
        assertFalse(p.categorias.single().estourado)
    }

    @Test
    fun `consumo parcial calcula fracao`() {
        val p = ProgressoOrcamento.de(
            listOf(orc(Categoria.MERCADO, 500)),
            listOf(fatia(Categoria.MERCADO, 250)),
        )
        assertEquals(0.5f, p.categorias.single().fracao)
        assertFalse(p.categorias.single().estourado)
    }

    @Test
    fun `estouro passa de 1 e marca estourado`() {
        val p = ProgressoOrcamento.de(
            listOf(orc(Categoria.LAZER, 100)),
            listOf(fatia(Categoria.LAZER, 143)),
        )
        assertEquals(1.43f, p.categorias.single().fracao, 0.001f)
        assertTrue(p.categorias.single().estourado)
    }

    @Test
    fun `gasto em categoria nao orcada fica fora dos totais`() {
        val p = ProgressoOrcamento.de(
            listOf(orc(Categoria.MERCADO, 500)),
            listOf(fatia(Categoria.MERCADO, 200), fatia(Categoria.LAZER, 999), fatia(null, 50)),
        )
        assertEquals(Dinheiro.deReais(500), p.totalPlanejado)
        assertEquals(Dinheiro.deReais(200), p.totalGasto)
        assertEquals(1, p.categorias.size)
    }

    @Test
    fun `ordena por fracao decrescente`() {
        val p = ProgressoOrcamento.de(
            listOf(orc(Categoria.MERCADO, 1000), orc(Categoria.LAZER, 100)),
            listOf(fatia(Categoria.MERCADO, 100), fatia(Categoria.LAZER, 90)),
        )
        assertEquals(Categoria.LAZER, p.categorias.first().categoria)
    }
}
