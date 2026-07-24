package dev.pedro.financas.domain.captura

import dev.pedro.financas.domain.Dinheiro
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CapturaBrutaTest {

    private val captura = CapturaBruta(
        id = CapturaBrutaId.novo(),
        pacote = "com.itau",
        titulo = "Notificação",
        texto = "Movimentação de R$ 54,90 na sua conta",
        valorDetectado = Dinheiro(5490),
        dataHora = Instant.parse("2026-07-20T12:00:00Z"),
    )

    @Test
    fun `nasce pendente de processamento`() {
        assertTrue(!captura.processada)
    }

    @Test
    fun `processar marca como processada preservando o resto`() {
        val processada = captura.processar()
        assertTrue(processada.processada)
        assertEquals(captura.copy(processada = true), processada)
    }
}
