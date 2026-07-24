package dev.pedro.financas

import android.app.Application
import dev.pedro.financas.application.CapturarNotificacao
import dev.pedro.financas.domain.LancamentoRepository
import dev.pedro.financas.domain.OrcamentoRepository
import dev.pedro.financas.domain.captura.CapturaBrutaRepository
import dev.pedro.financas.infrastructure.persistencia.FinancasDatabase
import dev.pedro.financas.infrastructure.preferencias.PreferenciasApp
import dev.pedro.financas.infrastructure.persistencia.RoomCapturaBrutaRepository
import dev.pedro.financas.infrastructure.persistencia.RoomLancamentoRepository
import dev.pedro.financas.infrastructure.persistencia.RoomOrcamentoRepository
import dev.pedro.financas.infrastructure.persistencia.RoomRegistroProcessamento

/** DI manual — app pequeno, sem Hilt. */
class AppContainer(app: Application) {
    private val db = FinancasDatabase.criar(app)

    val preferencias = PreferenciasApp(app)
    val lancamentoRepository: LancamentoRepository = RoomLancamentoRepository(db.lancamentoDao())
    val capturaBrutaRepository: CapturaBrutaRepository = RoomCapturaBrutaRepository(db.capturaBrutaDao())
    val orcamentoRepository: OrcamentoRepository = RoomOrcamentoRepository(db.orcamentoDao())

    val capturarNotificacao = CapturarNotificacao(
        lancamentos = lancamentoRepository,
        capturasBrutas = capturaBrutaRepository,
        processamentos = RoomRegistroProcessamento(db.processamentoDao()),
    )
}

class FinancasApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
