package dev.pedro.financas.application

import dev.pedro.financas.domain.Dinheiro
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.LancamentoId
import dev.pedro.financas.domain.LancamentoRepository
import dev.pedro.financas.domain.Origem
import dev.pedro.financas.domain.Status
import dev.pedro.financas.domain.Tipo
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.domain.captura.CapturaBrutaId
import dev.pedro.financas.domain.captura.CapturaBrutaRepository
import dev.pedro.financas.domain.captura.OperadoraParser
import dev.pedro.financas.domain.captura.RegistroProcessamento
import dev.pedro.financas.domain.captura.ResultadoOperadora
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Caso de uso da spec 007: SMS de operadora → lançamento, futuro ou efetivação.
 * Fluxo: dedup → parse → persistir. Claro confirma numa mensagem;
 * Vivo avisa (FUTURO) e depois confirma (efetiva pelo vencimento).
 */
class CapturarSms(
    private val lancamentos: LancamentoRepository,
    private val capturasBrutas: CapturaBrutaRepository,
    private val processamentos: RegistroProcessamento,
    private val zona: ZoneId = ZoneId.systemDefault(),
    private val agora: () -> Instant = Instant::now,
) {
    companion object {
        const val JANELA_DEDUP_MS = 2 * 60 * 1000L // mesma janela da spec 001, regra 3
        const val PACOTE_SMS = "sms"
    }

    sealed interface Resultado {
        data class LancamentoCriado(val lancamento: Lancamento) : Resultado
        data class FuturoCriado(val lancamento: Lancamento) : Resultado
        data class FuturoEfetivado(val lancamento: Lancamento) : Resultado
        data class CapturaBrutaCriada(val captura: CapturaBruta) : Resultado
        data object Duplicada : Resultado
        data object Ignorada : Resultado
    }

    suspend fun executar(remetente: String?, corpo: String): Resultado {
        val instante = agora()
        val hash = sha256("$PACOTE_SMS|$corpo")
        if (processamentos.jaProcessadaOuRegistra(hash, instante, JANELA_DEDUP_MS)) {
            return Resultado.Duplicada
        }
        val hoje = instante.atZone(zona).toLocalDate()

        return when (val r = OperadoraParser.parse(remetente, corpo)) {
            is ResultadoOperadora.PagamentoConfirmado -> {
                val lancamento = Lancamento(
                    id = LancamentoId.novo(),
                    tipo = Tipo.DEBITO,
                    valor = Dinheiro(r.valorCentavos),
                    dataHora = instante,
                    descricao = r.mesFatura?.let { "Fatura ${r.operadora} ($it)" }
                        ?: "Fatura ${r.operadora}",
                    origem = Origem.NOTIFICACAO_SMS,
                    status = Status.PENDENTE_REVISAO,
                    textoOrigem = corpo,
                )
                lancamentos.salvar(lancamento)
                Resultado.LancamentoCriado(lancamento)
            }

            is ResultadoOperadora.ContaFutura -> {
                val vencimento = OperadoraParser.dataComAno(r.vencimento, hoje)
                // Aviso repetido para o mesmo vencimento atualiza o valor, não duplica.
                val lancamento = buscarFuturoDe(r.operadora, vencimento)
                    ?.copy(valor = Dinheiro(r.valorCentavos), textoOrigem = corpo)
                    ?: Lancamento(
                        id = LancamentoId.novo(),
                        tipo = Tipo.DEBITO,
                        valor = Dinheiro(r.valorCentavos),
                        // Meio-dia no fuso local: o vencimento cai no dia certo em qualquer fuso.
                        dataHora = vencimento.atTime(12, 0).atZone(zona).toInstant(),
                        descricao = "Fatura ${r.operadora}",
                        origem = Origem.NOTIFICACAO_SMS,
                        status = Status.FUTURO,
                        textoOrigem = corpo,
                    )
                lancamentos.salvar(lancamento)
                Resultado.FuturoCriado(lancamento)
            }

            is ResultadoOperadora.FuturoEfetivado -> {
                val vencimento = OperadoraParser.dataComAno(r.vencimento, hoje)
                val futuro = buscarFuturoDe(r.operadora, vencimento)
                if (futuro != null) {
                    val efetivado = futuro.efetivar()
                    lancamentos.salvar(efetivado)
                    Resultado.FuturoEfetivado(efetivado)
                } else {
                    // Confirmação sem aviso prévio: sem valor na mensagem → fila de revisão manual.
                    val captura = CapturaBruta(
                        id = CapturaBrutaId.novo(),
                        pacote = PACOTE_SMS,
                        titulo = remetente,
                        texto = corpo,
                        valorDetectado = null,
                        dataHora = instante,
                    )
                    capturasBrutas.salvar(captura)
                    Resultado.CapturaBrutaCriada(captura)
                }
            }

            ResultadoOperadora.NaoReconhecido -> Resultado.Ignorada
        }
    }

    /** Chave de casamento (spec 007): origem SMS + operadora na descrição + data do vencimento. */
    private suspend fun buscarFuturoDe(operadora: String, vencimento: LocalDate): Lancamento? =
        lancamentos.buscarFuturos().find {
            it.origem == Origem.NOTIFICACAO_SMS &&
                it.descricao.contains(operadora, ignoreCase = true) &&
                it.dataHora.atZone(zona).toLocalDate() == vencimento
        }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
