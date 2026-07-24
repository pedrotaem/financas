package dev.pedro.financas.domain.captura

import dev.pedro.financas.domain.Dinheiro
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

@JvmInline
value class CapturaBrutaId(val valor: String) {
    companion object {
        fun novo() = CapturaBrutaId(UUID.randomUUID().toString())
    }
}

/**
 * Notificação do Itaú com valor monetário mas não reconhecida pelo parser
 * (spec 001, regra 2). Nada se perde: fica pendente de classificação manual.
 */
data class CapturaBruta(
    val id: CapturaBrutaId,
    val pacote: String,
    val titulo: String?,
    val texto: String,
    val valorDetectado: Dinheiro?,
    val dataHora: Instant,
    val processada: Boolean = false,
) {
    /** Sai da fila de pendências (descartada ou convertida em Lançamento, spec 004). */
    fun processar(): CapturaBruta = copy(processada = true)
}

interface CapturaBrutaRepository {
    fun observarPendentes(): Flow<List<CapturaBruta>>
    suspend fun salvar(captura: CapturaBruta)
}

/** Porta de dedup (spec 001, regra 3): hash + janela temporal. */
interface RegistroProcessamento {
    /** true se hash já visto dentro da janela; caso contrário registra e retorna false. */
    suspend fun jaProcessadaOuRegistra(hash: String, agora: Instant, janelaMs: Long): Boolean
}
