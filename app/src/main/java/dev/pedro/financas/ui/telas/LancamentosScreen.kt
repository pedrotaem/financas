package dev.pedro.financas.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pedro.financas.domain.Categoria
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.Status
import dev.pedro.financas.domain.Tipo
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.ui.EstadoUi
import dev.pedro.financas.ui.componentes.EstadoVazio
import dev.pedro.financas.ui.componentes.exibirValor
import dev.pedro.financas.ui.componentes.mascararValoresNoTexto
import dev.pedro.financas.ui.componentes.rotulo
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

@Composable
fun LancamentosScreen(
    estado: EstadoUi,
    onConfirmar: (Lancamento) -> Unit,
    onRejeitar: (Lancamento) -> Unit,
    onCategorizar: (Lancamento, Categoria) -> Unit,
    onEditar: (Lancamento, Tipo, Long, String, Categoria?) -> Unit,
    onDescartarCaptura: (CapturaBruta) -> Unit,
    onAdicionarDeCaptura: (CapturaBruta, Tipo, Long, String, Categoria?) -> Unit,
) {
    var emEdicao by remember { mutableStateOf<Lancamento?>(null) }
    var emConversao by remember { mutableStateOf<CapturaBruta?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (estado.capturasPendentes.isNotEmpty()) {
            item {
                Text(
                    "Capturas não reconhecidas (${estado.capturasPendentes.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(estado.capturasPendentes, key = { it.id.valor }) { c ->
                CartaoCapturaBruta(
                    c,
                    oculto = estado.saldoOculto,
                    onDescartar = { onDescartarCaptura(c) },
                    onAdicionar = { emConversao = c },
                )
            }
        }

        item { Text("Lançamentos do mês", style = MaterialTheme.typography.titleMedium) }

        if (estado.lancamentosDoMes.isEmpty()) {
            item {
                EstadoVazio(
                    "Nenhum lançamento neste mês.",
                    "Toque em + para adicionar",
                )
            }
        }
        items(estado.lancamentosDoMes, key = { it.id.valor }) { l ->
            CartaoLancamento(
                l,
                oculto = estado.saldoOculto,
                onEditar = { emEdicao = l },
                onConfirmar = { onConfirmar(l) },
                onRejeitar = { onRejeitar(l) },
                onCategorizar = { onCategorizar(l, it) },
            )
        }
    }

    emEdicao?.let { l ->
        LancamentoDialog(
            titulo = "Editar lançamento",
            tipoInicial = l.tipo,
            valorInicial = valorParaTexto(l.valor),
            descricaoInicial = l.descricao,
            categoriaInicial = l.categoria,
            onDismiss = { emEdicao = null },
            onSalvar = { tipo, centavos, descricao, categoria ->
                onEditar(l, tipo, centavos, descricao, categoria)
                emEdicao = null
            },
        )
    }

    emConversao?.let { c ->
        LancamentoDialog(
            titulo = "Adicionar lançamento",
            valorInicial = c.valorDetectado?.let { valorParaTexto(it) } ?: "",
            descricaoInicial = c.titulo.orEmpty(),
            onDismiss = { emConversao = null },
            onSalvar = { tipo, centavos, descricao, categoria ->
                onAdicionarDeCaptura(c, tipo, centavos, descricao, categoria)
                emConversao = null
            },
        )
    }
}

@Composable
fun LinhaLancamentoSimples(l: Lancamento, oculto: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(l.descricao, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                FORMATO_DATA.format(l.dataHora.atZone(ZoneId.systemDefault())),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val sinal = if (l.tipo == Tipo.DEBITO) "-" else "+"
        // Texto pequeno: sem cor de acento (spec 005, regra 2) — o sinal carrega a semântica.
        Text(
            exibirValor("$sinal${l.valor.formatado()}", oculto),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun CartaoLancamento(
    l: Lancamento,
    oculto: Boolean,
    onEditar: () -> Unit,
    onConfirmar: () -> Unit,
    onRejeitar: () -> Unit,
    onCategorizar: (Categoria) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditar),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(l.descricao, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        FORMATO_DATA.format(l.dataHora.atZone(ZoneId.systemDefault())),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val sinal = if (l.tipo == Tipo.DEBITO) "-" else "+"
                Text(
                    exibirValor("$sinal${l.valor.formatado()}", oculto),
                    style = MaterialTheme.typography.bodyLarge,
                    color = when {
                        oculto -> MaterialTheme.colorScheme.onSurface
                        l.tipo == Tipo.DEBITO -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SeletorCategoria(l.categoria, onCategorizar)
                if (l.status == Status.PENDENTE_REVISAO) {
                    TextButton(onClick = onConfirmar) { Text("Confirmar") }
                    TextButton(onClick = onRejeitar) {
                        Text("Rejeitar", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SeletorCategoria(atual: Categoria?, onEscolher: (Categoria) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { aberto = true },
            label = { Text(atual.rotulo()) },
        )
        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            Categoria.entries.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.rotulo()) },
                    onClick = {
                        aberto = false
                        onEscolher(cat)
                    },
                )
            }
        }
    }
}

@Composable
fun CartaoCapturaBruta(
    c: CapturaBruta,
    oculto: Boolean,
    onDescartar: () -> Unit,
    onAdicionar: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                c.valorDetectado?.let { exibirValor(it.formatado(), oculto) } ?: "Valor não detectado",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                if (oculto) mascararValoresNoTexto(c.texto) else c.texto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onAdicionar) { Text("Adicionar") }
                TextButton(onClick = onDescartar) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
