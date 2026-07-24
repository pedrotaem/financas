package dev.pedro.financas.domain.captura

import java.time.LocalDate
import java.time.MonthDay

/**
 * Parser de SMS de contas de operadora — Claro e Vivo (spec 007).
 * Kotlin puro. SMS é fonte restrita: só padrões conhecidos (spec 007, regra 4).
 */
sealed interface ResultadoOperadora {
    /** Claro: pagamento confirmado numa mensagem só → lançamento imediato. */
    data class PagamentoConfirmado(
        val operadora: String,
        val valorCentavos: Long,
        val mesFatura: String?,
    ) : ResultadoOperadora

    /** Vivo, aviso prévio: valor + vencimento → lançamento FUTURO. */
    data class ContaFutura(
        val operadora: String,
        val valorCentavos: Long,
        val vencimento: MonthDay,
    ) : ResultadoOperadora

    /** Vivo, confirmação no dia: efetiva o FUTURO do vencimento (sem valor na mensagem). */
    data class FuturoEfetivado(
        val operadora: String,
        val vencimento: MonthDay,
    ) : ResultadoOperadora

    data object NaoReconhecido : ResultadoOperadora
}

object OperadoraParser {

    private val CLARO_PAGAMENTO = Regex(
        """(?i)Cliente Claro.*?pagamento autom[aá]tico da sua fatura de (\p{L}+).*?R\$\s?([\d.]+,\d{2}).*?confirmado"""
    )
    private val VIVO_AVISO = Regex(
        """(?i)conta Vivo em d[eé]bito autom[aá]tico no valor de R\$\s?([\d.]+,\d{2}).*?vence em (\d{2})/(\d{2})"""
    )
    private val VIVO_CONFIRMACAO = Regex(
        """(?i)pagamento da fatura Vivo em d[eé]bito autom[aá]tico de vencimento em (\d{2})/(\d{2})"""
    )

    fun parse(titulo: String?, texto: String?): ResultadoOperadora {
        val completo = listOfNotNull(titulo, texto).joinToString(" ").trim()

        CLARO_PAGAMENTO.find(completo)?.let { m ->
            val valor = centavos(m.groupValues[2]) ?: return ResultadoOperadora.NaoReconhecido
            return ResultadoOperadora.PagamentoConfirmado(
                operadora = "Claro",
                valorCentavos = valor,
                mesFatura = m.groupValues[1].lowercase(),
            )
        }

        VIVO_AVISO.find(completo)?.let { m ->
            val valor = centavos(m.groupValues[1]) ?: return ResultadoOperadora.NaoReconhecido
            val venc = monthDay(m.groupValues[2], m.groupValues[3]) ?: return ResultadoOperadora.NaoReconhecido
            return ResultadoOperadora.ContaFutura("Vivo", valor, venc)
        }

        VIVO_CONFIRMACAO.find(completo)?.let { m ->
            val venc = monthDay(m.groupValues[1], m.groupValues[2]) ?: return ResultadoOperadora.NaoReconhecido
            return ResultadoOperadora.FuturoEfetivado("Vivo", venc)
        }

        return ResultadoOperadora.NaoReconhecido
    }

    /**
     * dd/MM → data com ano inferido (spec 007, regra 1): ano da mensagem;
     * se ficar >30 dias no passado, assume o ano seguinte (virada dez→jan).
     */
    fun dataComAno(vencimento: MonthDay, hoje: LocalDate): LocalDate {
        val noAno = vencimento.atYear(hoje.year)
        return if (noAno.isBefore(hoje.minusDays(30))) vencimento.atYear(hoje.year + 1) else noAno
    }

    private fun centavos(valor: String): Long? =
        valor.replace(".", "").replace(",", "").toLongOrNull()

    private fun monthDay(dia: String, mes: String): MonthDay? = runCatching {
        MonthDay.of(mes.toInt(), dia.toInt())
    }.getOrNull()
}
