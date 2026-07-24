package dev.pedro.financas.infrastructure.persistencia

import dev.pedro.financas.domain.Categoria
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.LancamentoId
import dev.pedro.financas.domain.LancamentoRepository
import dev.pedro.financas.domain.OrcamentoCategoria
import dev.pedro.financas.domain.OrcamentoRepository
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.domain.captura.CapturaBrutaRepository
import dev.pedro.financas.domain.captura.RegistroProcessamento
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomLancamentoRepository(private val dao: LancamentoDao) : LancamentoRepository {
    override fun observarTodos(): Flow<List<Lancamento>> =
        dao.observarTodos().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun buscar(id: LancamentoId): Lancamento? =
        dao.buscar(id.valor)?.paraDominio()

    override suspend fun salvar(lancamento: Lancamento) =
        dao.salvar(LancamentoEntity.de(lancamento))

    override suspend fun excluir(id: LancamentoId) = dao.excluir(id.valor)
}

class RoomCapturaBrutaRepository(private val dao: CapturaBrutaDao) : CapturaBrutaRepository {
    override fun observarPendentes(): Flow<List<CapturaBruta>> =
        dao.observarPendentes().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun salvar(captura: CapturaBruta) =
        dao.salvar(CapturaBrutaEntity.de(captura))
}

class RoomOrcamentoRepository(private val dao: OrcamentoDao) : OrcamentoRepository {
    override fun observarTodos(): Flow<List<OrcamentoCategoria>> =
        dao.observarTodos().map { lista -> lista.map { it.paraDominio() } }

    override suspend fun definir(orcamento: OrcamentoCategoria) =
        dao.salvar(OrcamentoEntity.de(orcamento))

    override suspend fun remover(categoria: Categoria) = dao.excluir(categoria.name)
}

class RoomRegistroProcessamento(private val dao: ProcessamentoDao) : RegistroProcessamento {
    override suspend fun jaProcessadaOuRegistra(hash: String, agora: Instant, janelaMs: Long): Boolean =
        dao.jaVistoOuRegistra(hash, agora.toEpochMilli(), janelaMs)
}
