package dev.pedro.financas.domain.captura

import dev.pedro.financas.domain.Tipo

/**
 * Parser de notificações do Itaú (spec 001).
 * Kotlin puro — regex sobre título + texto. Padrões calibráveis via fixtures.
 */
sealed interface ResultadoParse {
    /** Evento reconhecido: vira Lançamento PENDENTE_REVISAO. */
    data class Reconhecido(
        val tipo: Tipo,
        val valorCentavos: Long,
        val descricao: String,
        val estabelecimento: String? = null,
    ) : ResultadoParse

    /** Não reconhecido, mas tem valor monetário: vira captura bruta (regra 2). */
    data class ValorDetectado(val valorCentavos: Long) : ResultadoParse

    /** Sem valor monetário: ignorado. */
    data object NaoReconhecido : ResultadoParse
}

object NotificacaoParser {

    private val VALOR = Regex("""R\$\s?([\d.]+,\d{2})""")

    private val PIX_RECEBIDO = Regex("""(?i)(recebeu\s+(um\s+)?pix|pix\s+recebido)""")
    private val PIX_ENVIADO = Regex("""(?i)(pix\s+(enviado|realizado|efetuado)|voc[eê]\s+(pagou|enviou|transferiu))""")
    private val COMPRA_CARTAO = Regex("""(?i)compra\s+(aprovada|realizada|no\s+cr[eé]dito|de)""")

    /** Estabelecimento: texto após "em/no/na" depois do valor, até data/pontuação. */
    private val ESTABELECIMENTO = Regex("""R\$\s?[\d.]+,\d{2}\s+(?:em|no|na)\s+(.+?)(?=\s+em\s+\d|\s+dia\s+\d|[.\n]|$)""")
    private val CONTRAPARTE_DE = Regex("""(?i)\bde\s+([\p{L}][\p{L}\d .&'-]{2,40})""")
    private val CONTRAPARTE_PARA = Regex("""(?i)\bpara\s+([\p{L}][\p{L}\d .&'-]{2,40})""")

    fun parse(titulo: String?, texto: String?): ResultadoParse {
        val completo = listOfNotNull(titulo, texto).joinToString(" ").trim()
        val valor = extrairValorCentavos(completo) ?: return ResultadoParse.NaoReconhecido

        return when {
            PIX_RECEBIDO.containsMatchIn(completo) -> {
                val quem = depoisDoValor(completo, CONTRAPARTE_DE)
                ResultadoParse.Reconhecido(
                    tipo = Tipo.CREDITO,
                    valorCentavos = valor,
                    descricao = quem?.let { "PIX de $it" } ?: "PIX recebido",
                )
            }
            PIX_ENVIADO.containsMatchIn(completo) -> {
                val quem = CONTRAPARTE_PARA.find(completo)?.groupValues?.get(1)?.trim()
                ResultadoParse.Reconhecido(
                    tipo = Tipo.DEBITO,
                    valorCentavos = valor,
                    descricao = quem?.let { "PIX para $it" } ?: "PIX enviado",
                )
            }
            COMPRA_CARTAO.containsMatchIn(completo) -> {
                val estab = ESTABELECIMENTO.find(completo)?.groupValues?.get(1)?.trim()
                ResultadoParse.Reconhecido(
                    tipo = Tipo.DEBITO,
                    valorCentavos = valor,
                    descricao = estab?.let { "Compra cartão — $it" } ?: "Compra cartão",
                    estabelecimento = estab,
                )
            }
            else -> ResultadoParse.ValorDetectado(valor)
        }
    }

    /** "1.234,56" → 123456 centavos. */
    fun extrairValorCentavos(texto: String): Long? {
        val bruto = VALOR.find(texto)?.groupValues?.get(1) ?: return null
        return bruto.replace(".", "").replace(",", "").toLongOrNull()
    }

    /** Contraparte "de X" buscada apenas após o valor (evita "de R$" do prefixo). */
    private fun depoisDoValor(completo: String, regex: Regex): String? {
        val posValor = VALOR.find(completo)?.range?.last ?: return null
        return regex.find(completo, startIndex = posValor)?.groupValues?.get(1)?.trim()
    }
}
