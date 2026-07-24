package dev.pedro.financas.ui.telas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.pedro.financas.domain.Categoria
import dev.pedro.financas.domain.Dinheiro
import dev.pedro.financas.domain.ProgressoCategoria
import dev.pedro.financas.ui.EstadoUi
import dev.pedro.financas.ui.componentes.exibirValor
import dev.pedro.financas.ui.componentes.rotulo
import dev.pedro.financas.ui.parseValorParaCentavos
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

fun nomeDoMes(mes: YearMonth): String =
    mes.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() } + " ${mes.year}"

/** Cor da barra de progresso (spec 006): ≤80% primary · 80–100% tertiary · estourado error. */
@Composable
fun corProgresso(fracao: Float): Color = when {
    fracao > 1f -> MaterialTheme.colorScheme.error
    fracao > 0.8f -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.primary
}

@Composable
fun BarraOrcamento(fracao: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { fracao.coerceIn(0f, 1f) },
        color = corProgresso(fracao),
        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun OrcamentoScreen(
    estado: EstadoUi,
    onDefinir: (Categoria, Long) -> Unit,
    onRemover: (Categoria) -> Unit,
) {
    var emEdicao by remember { mutableStateOf<Categoria?>(null) }
    val progresso = estado.progressoOrcamento
    val orcadas = progresso.categorias.map { it.categoria }.toSet()
    val gastoPorCategoria = estado.resumo.fatias
        .filter { it.categoria != null }
        .associate { it.categoria!! to it.total }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Orçamento — ${nomeDoMes(estado.mes)}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        if (progresso.categorias.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Total planejado",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val fracaoTotal = if (progresso.totalPlanejado.centavos == 0L) 0f
                        else progresso.totalGasto.centavos.toFloat() / progresso.totalPlanejado.centavos
                        Text(
                            "${exibirValor(progresso.totalGasto.formatado(), estado.saldoOculto)} de " +
                                exibirValor(progresso.totalPlanejado.formatado(), estado.saldoOculto),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        BarraOrcamento(fracaoTotal)
                        Text(
                            "${(fracaoTotal * 100).roundToInt()}% consumido",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Text("Categorias orçadas", style = MaterialTheme.typography.titleMedium) }
            items(progresso.categorias, key = { it.categoria.name }) { p ->
                CartaoCategoriaOrcada(p, estado.saldoOculto, onEditar = { emEdicao = p.categoria })
            }
        }

        item { Text("Sem orçamento", style = MaterialTheme.typography.titleMedium) }
        items(Categoria.entries.filter { it !in orcadas }, key = { it.name }) { cat ->
            CartaoCategoriaSemOrcamento(
                cat,
                gasto = gastoPorCategoria[cat],
                oculto = estado.saldoOculto,
                onDefinir = { emEdicao = cat },
            )
        }
    }

    emEdicao?.let { cat ->
        OrcamentoDialog(
            categoria = cat,
            valorAtual = progresso.categorias.find { it.categoria == cat }?.planejado,
            onDismiss = { emEdicao = null },
            onSalvar = { centavos ->
                onDefinir(cat, centavos)
                emEdicao = null
            },
            onRemover = if (cat in orcadas) {
                {
                    onRemover(cat)
                    emEdicao = null
                }
            } else null,
        )
    }
}

@Composable
private fun CartaoCategoriaOrcada(p: ProgressoCategoria, oculto: Boolean, onEditar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditar),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(p.categoria.rotulo(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${(p.fracao * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    color = corProgresso(p.fracao),
                )
            }
            BarraOrcamento(p.fracao)
            Text(
                "${exibirValor(p.gasto.formatado(), oculto)} de ${exibirValor(p.planejado.formatado(), oculto)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CartaoCategoriaSemOrcamento(
    categoria: Categoria,
    gasto: Dinheiro?,
    oculto: Boolean,
    onDefinir: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDefinir),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(categoria.rotulo(), style = MaterialTheme.typography.bodyLarge)
                gasto?.let {
                    Text(
                        "Gasto no mês: ${exibirValor(it.formatado(), oculto)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                "Definir",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun OrcamentoDialog(
    categoria: Categoria,
    valorAtual: Dinheiro?,
    onDismiss: () -> Unit,
    onSalvar: (Long) -> Unit,
    onRemover: (() -> Unit)?,
) {
    var valorTexto by remember {
        mutableStateOf(valorAtual?.let { valorParaTexto(it) } ?: "")
    }
    val valorCentavos = parseValorParaCentavos(valorTexto)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Orçamento — ${categoria.rotulo()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Limite mensal de gasto para esta categoria. Vale para todos os meses.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it },
                    label = { Text("Valor (R$)") },
                    placeholder = { Text("500,00") },
                    isError = valorTexto.isNotEmpty() && valorCentavos == null,
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valorCentavos != null,
                onClick = { onSalvar(valorCentavos!!) },
            ) { Text("Salvar") }
        },
        dismissButton = {
            Row {
                onRemover?.let {
                    TextButton(onClick = it) {
                        Text("Remover", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        },
    )
}
