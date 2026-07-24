package dev.pedro.financas.application

import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.LancamentoId
import dev.pedro.financas.domain.LancamentoRepository
import dev.pedro.financas.domain.Origem
import dev.pedro.financas.domain.Tipo
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.domain.captura.CapturaBrutaRepository
import dev.pedro.financas.domain.captura.RegistroProcessamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class CapturarNotificacaoTest {

    private class FakeLancamentos : LancamentoRepository {
        val salvos = mutableListOf<Lancamento>()
        override fun observarTodos(): Flow<List<Lancamento>> = flowOf(salvos)
        override suspend fun buscar(id: LancamentoId) = salvos.find { it.id == id }
        override suspend fun buscarFuturos() = salvos.filter { it.status == dev.pedro.financas.domain.Status.FUTURO }
        override suspend fun salvar(lancamento: Lancamento) {
            salvos.removeAll { it.id == lancamento.id }
            salvos += lancamento
        }
        override suspend fun excluir(id: LancamentoId) { salvos.removeAll { it.id == id } }
    }

    private class FakeCapturas : CapturaBrutaRepository {
        val salvas = mutableListOf<CapturaBruta>()
        override fun observarPendentes(): Flow<List<CapturaBruta>> = flowOf(salvas)
        override suspend fun salvar(captura: CapturaBruta) { salvas += captura }
    }

    /** Dedup em memória com a mesma semântica da implementação Room. */
    private class FakeProcessamentos : RegistroProcessamento {
        private val vistos = mutableMapOf<String, Instant>()
        override suspend fun jaProcessadaOuRegistra(hash: String, agora: Instant, janelaMs: Long): Boolean {
            val anterior = vistos[hash]
            if (anterior != null && agora.toEpochMilli() - anterior.toEpochMilli() < janelaMs) return true
            vistos[hash] = agora
            return false
        }
    }

    private fun useCase(
        lancamentos: FakeLancamentos = FakeLancamentos(),
        capturas: FakeCapturas = FakeCapturas(),
        agora: () -> Instant = { Instant.parse("2026-07-20T12:00:00Z") },
    ) = CapturarNotificacao(lancamentos, capturas, FakeProcessamentos(), agora)

    @Test
    fun `notificacao reconhecida cria lancamento`() = runTest {
        val lancamentos = FakeLancamentos()
        val uc = useCase(lancamentos = lancamentos)

        val r = uc.executar("com.itau", "Itaú", "Compra aprovada: R$ 54,90 em PADARIA X")

        assertTrue(r is CapturarNotificacao.Resultado.LancamentoCriado)
        assertEquals(1, lancamentos.salvos.size)
        assertEquals(Tipo.DEBITO, lancamentos.salvos[0].tipo)
        assertEquals(Origem.NOTIFICACAO_ITAU, lancamentos.salvos[0].origem)
    }

    @Test
    fun `mesma notificacao duas vezes gera um lancamento`() = runTest {
        val lancamentos = FakeLancamentos()
        val uc = useCase(lancamentos = lancamentos)

        uc.executar("com.itau", "Itaú", "Compra aprovada: R$ 54,90 em PADARIA X")
        val r2 = uc.executar("com.itau", "Itaú", "Compra aprovada: R$ 54,90 em PADARIA X")

        assertTrue(r2 is CapturarNotificacao.Resultado.Duplicada)
        assertEquals(1, lancamentos.salvos.size)
    }

    @Test
    fun `mesma notificacao apos janela de 2 min processa de novo`() = runTest {
        val lancamentos = FakeLancamentos()
        var instante = Instant.parse("2026-07-20T12:00:00Z")
        val uc = useCase(lancamentos = lancamentos, agora = { instante })

        uc.executar("com.itau", "Itaú", "Compra aprovada: R$ 54,90 em PADARIA X")
        instante = instante.plusSeconds(121)
        uc.executar("com.itau", "Itaú", "Compra aprovada: R$ 54,90 em PADARIA X")

        assertEquals(2, lancamentos.salvos.size)
    }

    @Test
    fun `texto nao reconhecido com valor vira captura bruta`() = runTest {
        val capturas = FakeCapturas()
        val uc = useCase(capturas = capturas)

        val r = uc.executar("com.itau", "Itaú", "Sua fatura de R$ 1.850,00 fecha amanhã")

        assertTrue(r is CapturarNotificacao.Resultado.CapturaBrutaCriada)
        assertEquals(1, capturas.salvas.size)
        assertEquals(185000, capturas.salvas[0].valorDetectado?.centavos)
    }

    @Test
    fun `texto sem valor e ignorado`() = runTest {
        val r = useCase().executar("com.itau", "Itaú", "Atualize seu aplicativo")
        assertTrue(r is CapturarNotificacao.Resultado.Ignorada)
    }
}
