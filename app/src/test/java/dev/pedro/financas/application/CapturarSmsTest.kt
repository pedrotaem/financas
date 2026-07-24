package dev.pedro.financas.application

import dev.pedro.financas.domain.Dinheiro
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.LancamentoId
import dev.pedro.financas.domain.LancamentoRepository
import dev.pedro.financas.domain.Origem
import dev.pedro.financas.domain.Status
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
import java.time.ZoneId

class CapturarSmsTest {

    private val SMS_CLARO = "Cliente Claro, o pagamento automatico da sua fatura de julho no " +
        "valor de R$ 199,99 foi confirmado! Saiba mais, acesse o App Minha Claro."

    private val SMS_VIVO_AVISO = "Ola, sua conta Vivo em debito automatico no valor de R\$149,90 " +
        "vence em 17/07. Lembre-se de manter saldo em conta bancaria para o pagamento dar certo ;)"

    private val SMS_VIVO_CONFIRMACAO = "Ola, deu tudo certo com o pagamento da fatura Vivo em " +
        "debito automatico de vencimento em 17/07. Sua fatura agora com mais comodidade e " +
        "tranquilidade para voce."

    private val ZONA = ZoneId.of("America/Sao_Paulo")

    private class FakeLancamentos : LancamentoRepository {
        val salvos = mutableListOf<Lancamento>()
        override fun observarTodos(): Flow<List<Lancamento>> = flowOf(salvos)
        override suspend fun buscar(id: LancamentoId) = salvos.find { it.id == id }
        override suspend fun buscarFuturos() = salvos.filter { it.status == Status.FUTURO }
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
        agora: () -> Instant = { Instant.parse("2026-07-13T12:00:00Z") },
    ) = CapturarSms(lancamentos, capturas, FakeProcessamentos(), ZONA, agora)

    @Test
    fun `sms claro cria lancamento pendente`() = runTest {
        val lancamentos = FakeLancamentos()
        val r = useCase(lancamentos = lancamentos).executar("Claro", SMS_CLARO)

        assertTrue(r is CapturarSms.Resultado.LancamentoCriado)
        val l = lancamentos.salvos.single()
        assertEquals(19999, l.valor.centavos)
        assertEquals(Tipo.DEBITO, l.tipo)
        assertEquals(Status.PENDENTE_REVISAO, l.status)
        assertEquals(Origem.NOTIFICACAO_SMS, l.origem)
        assertEquals("Fatura Claro (julho)", l.descricao)
    }

    @Test
    fun `aviso vivo cria lancamento futuro no vencimento`() = runTest {
        val lancamentos = FakeLancamentos()
        val r = useCase(lancamentos = lancamentos).executar("Vivo", SMS_VIVO_AVISO)

        assertTrue(r is CapturarSms.Resultado.FuturoCriado)
        val l = lancamentos.salvos.single()
        assertEquals(Status.FUTURO, l.status)
        assertEquals(14990, l.valor.centavos)
        assertEquals("2026-07-17", l.dataHora.atZone(ZONA).toLocalDate().toString())
    }

    @Test
    fun `aviso repetido atualiza o futuro sem duplicar`() = runTest {
        val lancamentos = FakeLancamentos()
        var instante = Instant.parse("2026-07-13T12:00:00Z")
        val uc = useCase(lancamentos = lancamentos, agora = { instante })

        uc.executar("Vivo", SMS_VIVO_AVISO)
        instante = instante.plusSeconds(24 * 3600)
        uc.executar("Vivo", SMS_VIVO_AVISO.replace("R\$149,90", "R\$150,00"))

        val l = lancamentos.salvos.single()
        assertEquals(15000, l.valor.centavos)
        assertEquals(Status.FUTURO, l.status)
    }

    @Test
    fun `confirmacao vivo efetiva o futuro pelo vencimento`() = runTest {
        val lancamentos = FakeLancamentos()
        var instante = Instant.parse("2026-07-13T12:00:00Z")
        val uc = useCase(lancamentos = lancamentos, agora = { instante })

        uc.executar("Vivo", SMS_VIVO_AVISO)
        instante = Instant.parse("2026-07-17T11:00:00Z")
        val r = uc.executar("Vivo", SMS_VIVO_CONFIRMACAO)

        assertTrue(r is CapturarSms.Resultado.FuturoEfetivado)
        val l = lancamentos.salvos.single()
        assertEquals(Status.PENDENTE_REVISAO, l.status)
        assertEquals(14990, l.valor.centavos)
        assertEquals("2026-07-17", l.dataHora.atZone(ZONA).toLocalDate().toString())
    }

    @Test
    fun `confirmacao sem futuro correspondente vira captura bruta`() = runTest {
        val capturas = FakeCapturas()
        val r = useCase(capturas = capturas).executar("Vivo", SMS_VIVO_CONFIRMACAO)

        assertTrue(r is CapturarSms.Resultado.CapturaBrutaCriada)
        assertEquals(null, capturas.salvas.single().valorDetectado)
    }

    @Test
    fun `mesmo sms duas vezes deduplica`() = runTest {
        val lancamentos = FakeLancamentos()
        val uc = useCase(lancamentos = lancamentos)

        uc.executar("Claro", SMS_CLARO)
        val r2 = uc.executar("Claro", SMS_CLARO)

        assertTrue(r2 is CapturarSms.Resultado.Duplicada)
        assertEquals(1, lancamentos.salvos.size)
    }

    @Test
    fun `sms irrelevante e ignorado`() = runTest {
        val r = useCase().executar("12345", "Promocao: recarregue R$ 30,00 e ganhe o dobro!")
        assertTrue(r is CapturarSms.Resultado.Ignorada)
    }
}
