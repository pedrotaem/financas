package dev.pedro.financas.infrastructure.preferencias

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferências de UI (spec 005): saldo oculto e tema OLED.
 * SharedPreferences — zero dependência nova; sobrevive a processo morto (spec 005, regra 4).
 */
class PreferenciasApp(context: Context) {
    private val prefs = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    private val _saldoOculto = MutableStateFlow(prefs.getBoolean(CHAVE_SALDO_OCULTO, false))
    val saldoOculto: StateFlow<Boolean> = _saldoOculto.asStateFlow()

    private val _temaOled = MutableStateFlow(prefs.getBoolean(CHAVE_TEMA_OLED, false))
    val temaOled: StateFlow<Boolean> = _temaOled.asStateFlow()

    fun alternarSaldoOculto() = alternar(CHAVE_SALDO_OCULTO, _saldoOculto)
    fun alternarTemaOled() = alternar(CHAVE_TEMA_OLED, _temaOled)

    private fun alternar(chave: String, fluxo: MutableStateFlow<Boolean>) {
        val novo = !fluxo.value
        prefs.edit().putBoolean(chave, novo).apply()
        fluxo.value = novo
    }

    private companion object {
        const val CHAVE_SALDO_OCULTO = "saldo_oculto"
        const val CHAVE_TEMA_OLED = "tema_oled"
    }
}
