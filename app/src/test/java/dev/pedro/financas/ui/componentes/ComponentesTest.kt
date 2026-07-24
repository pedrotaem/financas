package dev.pedro.financas.ui.componentes

import dev.pedro.financas.domain.Categoria
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ExibirValorTest {

    @Test
    fun `oculto vira mascara`() {
        assertEquals("R$ ••••", exibirValor("R$ 54,90", oculto = true))
        assertEquals("R$ ••••", exibirValor("-R$ 1.234,56", oculto = true))
    }

    @Test
    fun `visivel mantem o formatado`() {
        assertEquals("R$ 54,90", exibirValor("R$ 54,90", oculto = false))
    }

    @Test
    fun `mascara valores dentro de texto livre`() {
        assertEquals(
            "Movimentação de R$ •••• na sua conta",
            mascararValoresNoTexto("Movimentação de R$ 1.234,56 na sua conta"),
        )
        assertEquals(
            "Pix de R$ •••• recebido",
            mascararValoresNoTexto("Pix de R$54,90 recebido"),
        )
        assertEquals("Sem valor aqui", mascararValoresNoTexto("Sem valor aqui"))
    }
}

class PaletaCategoriaTest {

    private val chaves = Categoria.entries.toList<Categoria?>() + null

    @Test
    fun `toda categoria tem par tonal`() {
        chaves.forEach { cat ->
            assertEquals(true, CORES_CATEGORIA_CLARO.containsKey(cat), "sem cor clara: $cat")
            assertEquals(true, CORES_CATEGORIA_ESCURO.containsKey(cat), "sem cor escura: $cat")
        }
    }

    @Test
    fun `cores distintas dentro de cada modo`() {
        assertEquals(CORES_CATEGORIA_CLARO.size, CORES_CATEGORIA_CLARO.values.toSet().size)
        assertEquals(CORES_CATEGORIA_ESCURO.size, CORES_CATEGORIA_ESCURO.values.toSet().size)
    }

    @Test
    fun `tom claro difere do escuro para cada categoria`() {
        chaves.forEach { cat ->
            assertNotEquals(CORES_CATEGORIA_CLARO[cat], CORES_CATEGORIA_ESCURO[cat], "par igual: $cat")
        }
    }
}
