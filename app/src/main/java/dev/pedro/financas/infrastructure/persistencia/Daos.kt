package dev.pedro.financas.infrastructure.persistencia

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LancamentoDao {
    @Query("SELECT * FROM lancamentos ORDER BY dataHoraEpochMs DESC")
    fun observarTodos(): Flow<List<LancamentoEntity>>

    @Query("SELECT * FROM lancamentos WHERE id = :id")
    suspend fun buscar(id: String): LancamentoEntity?

    @Upsert
    suspend fun salvar(e: LancamentoEntity)

    @Query("DELETE FROM lancamentos WHERE id = :id")
    suspend fun excluir(id: String)
}

@Dao
interface CapturaBrutaDao {
    @Query("SELECT * FROM capturas_brutas WHERE processada = 0 ORDER BY dataHoraEpochMs DESC")
    fun observarPendentes(): Flow<List<CapturaBrutaEntity>>

    @Upsert
    suspend fun salvar(e: CapturaBrutaEntity)
}

@Dao
interface OrcamentoDao {
    @Query("SELECT * FROM orcamentos")
    fun observarTodos(): Flow<List<OrcamentoEntity>>

    @Upsert
    suspend fun salvar(e: OrcamentoEntity)

    @Query("DELETE FROM orcamentos WHERE categoria = :categoria")
    suspend fun excluir(categoria: String)
}

@Dao
interface ProcessamentoDao {
    @Query("SELECT COUNT(*) FROM processamentos WHERE hash = :hash AND emEpochMs > :desdeEpochMs")
    suspend fun contarRecentes(hash: String, desdeEpochMs: Long): Int

    @Upsert
    suspend fun registrar(e: ProcessamentoEntity)

    @Query("DELETE FROM processamentos WHERE emEpochMs < :antesDeEpochMs")
    suspend fun limparAntigos(antesDeEpochMs: Long)

    /** Dedup atômico: true se já visto na janela; senão registra. */
    @Transaction
    suspend fun jaVistoOuRegistra(hash: String, agoraEpochMs: Long, janelaMs: Long): Boolean {
        if (contarRecentes(hash, agoraEpochMs - janelaMs) > 0) return true
        registrar(ProcessamentoEntity(hash, agoraEpochMs))
        limparAntigos(agoraEpochMs - janelaMs * 10) // higiene
        return false
    }
}
