package dev.pedro.financas.ui.telas

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
import dev.pedro.financas.ui.componentes.rotulo
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

@Composable
fun LancamentosScreen(
    estado: EstadoUi,
    onConfirmar: (Lancamento) -> Unit,
    onCategorizar: (Lancamento, Categoria) -> Unit,
) {
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
            items(estado.capturasPendentes, key = { it.id.valor }) { CartaoCapturaBruta(it) }
        }

        item { Text("Lançamentos do mês", style = MaterialTheme.typography.titleMedium) }

        if (estado.lancamentosDoMes.isEmpty()) {
            item {
                Text("Nenhum lançamento neste mês.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(estado.lancamentosDoMes, key = { it.id.valor }) { l ->
            CartaoLancamento(l, onConfirmar = { onConfirmar(l) }, onCategorizar = { onCategorizar(l, it) })
        }
    }
}

@Composable
fun LinhaLancamentoSimples(l: Lancamento) {
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
                style = MaterialTheme.typography.bodySmall,
            )
        }
        val sinal = if (l.tipo == Tipo.DEBITO) "-" else "+"
        Text(
            "$sinal${l.valor.formatado()}",
            style = MaterialTheme.typography.bodyMedium,
            color = if (l.tipo == Tipo.DEBITO)
                MaterialTheme.colorScheme.error
            else
                MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun CartaoLancamento(
    l: Lancamento,
    onConfirmar: () -> Unit,
    onCategorizar: (Categoria) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
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
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                val sinal = if (l.tipo == Tipo.DEBITO) "-" else "+"
                Text(
                    "$sinal${l.valor.formatado()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (l.tipo == Tipo.DEBITO)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
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
fun CartaoCapturaBruta(c: CapturaBruta) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(
                c.valorDetectado?.formatado() ?: "Valor não detectado",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(c.texto, style = MaterialTheme.typography.bodySmall, maxLines = 3)
        }
    }
}
