package dev.pedro.financas.infrastructure.persistencia

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.pedro.financas.domain.Categoria
import dev.pedro.financas.domain.Dinheiro
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.LancamentoId
import dev.pedro.financas.domain.NotaFiscalId
import dev.pedro.financas.domain.OrcamentoCategoria
import dev.pedro.financas.domain.Origem
import dev.pedro.financas.domain.Status
import dev.pedro.financas.domain.Tipo
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.domain.captura.CapturaBrutaId
import java.time.Instant

@Entity(tableName = "lancamentos")
data class LancamentoEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    val valorCentavos: Long,
    val dataHoraEpochMs: Long,
    val descricao: String,
    val estabelecimento: String?,
    val categoria: String?,
    val origem: String,
    val status: String,
    val notaFiscalId: String?,
    val textoOrigem: String?,
) {
    fun paraDominio() = Lancamento(
        id = LancamentoId(id),
        tipo = Tipo.valueOf(tipo),
        valor = Dinheiro(valorCentavos),
        dataHora = Instant.ofEpochMilli(dataHoraEpochMs),
        descricao = descricao,
        estabelecimento = estabelecimento,
        categoria = categoria?.let { Categoria.valueOf(it) },
        origem = Origem.valueOf(origem),
        status = Status.valueOf(status),
        notaFiscalId = notaFiscalId?.let { NotaFiscalId(it) },
        textoOrigem = textoOrigem,
    )

    companion object {
        fun de(l: Lancamento) = LancamentoEntity(
            id = l.id.valor,
            tipo = l.tipo.name,
            valorCentavos = l.valor.centavos,
            dataHoraEpochMs = l.dataHora.toEpochMilli(),
            descricao = l.descricao,
            estabelecimento = l.estabelecimento,
            categoria = l.categoria?.name,
            origem = l.origem.name,
            status = l.status.name,
            notaFiscalId = l.notaFiscalId?.valor,
            textoOrigem = l.textoOrigem,
        )
    }
}

@Entity(tableName = "capturas_brutas")
data class CapturaBrutaEntity(
    @PrimaryKey val id: String,
    val pacote: String,
    val titulo: String?,
    val texto: String,
    val valorDetectadoCentavos: Long?,
    val dataHoraEpochMs: Long,
    val processada: Boolean,
) {
    fun paraDominio() = CapturaBruta(
        id = CapturaBrutaId(id),
        pacote = pacote,
        titulo = titulo,
        texto = texto,
        valorDetectado = valorDetectadoCentavos?.let { Dinheiro(it) },
        dataHora = Instant.ofEpochMilli(dataHoraEpochMs),
        processada = processada,
    )

    companion object {
        fun de(c: CapturaBruta) = CapturaBrutaEntity(
            id = c.id.valor,
            pacote = c.pacote,
            titulo = c.titulo,
            texto = c.texto,
            valorDetectadoCentavos = c.valorDetectado?.centavos,
            dataHoraEpochMs = c.dataHora.toEpochMilli(),
            processada = c.processada,
        )
    }
}

/** Dedup de notificações (spec 001, regra 3). */
@Entity(tableName = "processamentos")
data class ProcessamentoEntity(
    @PrimaryKey val hash: String,
    val emEpochMs: Long,
)

/** Orçamento mensal recorrente por categoria (spec 006). */
@Entity(tableName = "orcamentos")
data class OrcamentoEntity(
    @PrimaryKey val categoria: String,
    val valorCentavos: Long,
) {
    fun paraDominio() = OrcamentoCategoria(
        categoria = Categoria.valueOf(categoria),
        valor = Dinheiro(valorCentavos),
    )

    companion object {
        fun de(o: OrcamentoCategoria) = OrcamentoEntity(
            categoria = o.categoria.name,
            valorCentavos = o.valor.centavos,
        )
    }
}
