package dev.pedro.financas.domain

/**
 * Value object para valores monetários em BRL.
 * Sempre em centavos (Long) — nunca float/double para dinheiro.
 */
@JvmInline
value class Dinheiro(val centavos: Long) {
    init {
        require(centavos >= 0) { "Dinheiro não pode ser negativo: $centavos" }
    }

    operator fun plus(outro: Dinheiro) = Dinheiro(centavos + outro.centavos)

    fun formatado(): String {
        val reais = centavos / 100
        val cents = centavos % 100
        return "R$ %d,%02d".format(reais, cents)
    }

    companion object {
        val ZERO = Dinheiro(0)
        fun deReais(reais: Long, centavos: Long = 0) = Dinheiro(reais * 100 + centavos)
    }
}
