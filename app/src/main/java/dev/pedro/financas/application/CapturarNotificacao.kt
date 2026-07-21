package dev.pedro.financas.application

import dev.pedro.financas.domain.Dinheiro
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.LancamentoId
import dev.pedro.financas.domain.LancamentoRepository
import dev.pedro.financas.domain.Origem
import dev.pedro.financas.domain.Status
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.domain.captura.CapturaBrutaId
import dev.pedro.financas.domain.captura.CapturaBrutaRepository
import dev.pedro.financas.domain.captura.NotificacaoParser
import dev.pedro.financas.domain.captura.RegistroProcessamento
import dev.pedro.financas.domain.captura.ResultadoParse
import java.security.MessageDigest
import java.time.Instant

/**
 * Caso de uso da spec 001: notificação do Itaú → Lançamento ou captura bruta.
 * Fluxo: dedup → parse → persistir.
 */
class CapturarNotificacao(
    private val lancamentos: LancamentoRepository,
    private val capturasBrutas: CapturaBrutaRepository,
    private val processamentos: RegistroProcessamento,
    private val agora: () -> Instant = Instant::now,
) {
    companion object {
        const val JANELA_DEDUP_MS = 2 * 60 * 1000L // 2 min (spec 001, regra 3)
    }

    sealed interface Resultado {
        data class LancamentoCriado(val lancamento: Lancamento) : Resultado
        data class CapturaBrutaCriada(val captura: CapturaBruta) : Resultado
        data object Duplicada : Resultado
        data object Ignorada : Resultado
    }

    suspend fun executar(pacote: String, titulo: String?, texto: String?): Resultado {
        val instante = agora()
        val hash = sha256("$pacote|${titulo.orEmpty()}|${texto.orEmpty()}")
        if (processamentos.jaProcessadaOuRegistra(hash, instante, JANELA_DEDUP_MS)) {
            return Resultado.Duplicada
        }

        return when (val r = NotificacaoParser.parse(titulo, texto)) {
            is ResultadoParse.Reconhecido -> {
                val lancamento = Lancamento(
                    id = LancamentoId.novo(),
                    tipo = r.tipo,
                    valor = Dinheiro(r.valorCentavos),
                    dataHora = instante,
                    descricao = r.descricao,
                    estabelecimento = r.estabelecimento,
                    origem = Origem.NOTIFICACAO_ITAU,
                    status = Status.PENDENTE_REVISAO,
                    textoOrigem = "${titulo.orEmpty()} ${texto.orEmpty()}".trim(),
                )
                lancamentos.salvar(lancamento)
                Resultado.LancamentoCriado(lancamento)
            }
            is ResultadoParse.ValorDetectado -> {
                val captura = CapturaBruta(
                    id = CapturaBrutaId.novo(),
                    pacote = pacote,
                    titulo = titulo,
                    texto = texto.orEmpty(),
                    valorDetectado = Dinheiro(r.valorCentavos),
                    dataHora = instante,
                )
                capturasBrutas.salvar(captura)
                Resultado.CapturaBrutaCriada(captura)
            }
            ResultadoParse.NaoReconhecido -> Resultado.Ignorada
        }
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
