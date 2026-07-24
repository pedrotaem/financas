package dev.pedro.financas.ui.telas

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pedro.financas.domain.formatarSaldo
import dev.pedro.financas.ui.EstadoUi
import dev.pedro.financas.ui.componentes.EstadoVazio
import dev.pedro.financas.ui.componentes.GraficoDonut
import dev.pedro.financas.ui.componentes.exibirValor
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    estado: EstadoUi,
    onMesAnterior: () -> Unit,
    onMesSeguinte: () -> Unit,
    onVerPendencias: () -> Unit,
    onAlternarSaldoOculto: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SeletorMes(estado, onMesAnterior, onMesSeguinte) }

        item { CardSaldo(estado, onAlternarSaldoOculto, onVerDetalhe = onVerPendencias) }

        if (estado.qtdPendentes > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onVerPendencias),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ) {
                    Text(
                        "${estado.qtdPendentes} item(ns) aguardando revisão — toque para revisar",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        if (estado.resumo.fatias.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Por categoria", style = MaterialTheme.typography.titleMedium)
                        GraficoDonut(
                            fatias = estado.resumo.fatias,
                            totalFormatado = estado.resumo.despesas.formatado(),
                            valoresOcultos = estado.saldoOculto,
                        )
                    }
                }
            }
        }

        if (estado.lancamentosDoMes.isNotEmpty()) {
            item { Text("Últimos lançamentos", style = MaterialTheme.typography.titleMedium) }
            items(estado.lancamentosDoMes.take(5), key = { it.id.valor }) { l ->
                LinhaLancamentoSimples(l, oculto = estado.saldoOculto)
            }
        } else {
            item {
                EstadoVazio(
                    "Nenhum lançamento neste mês.",
                    "Toque em + para começar",
                )
            }
        }
    }
}

@Composable
private fun SeletorMes(estado: EstadoUi, onAnterior: () -> Unit, onSeguinte: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onAnterior) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
        }
        val nomeMes = estado.mes.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
            .replaceFirstChar { it.uppercase() }
        Text("$nomeMes ${estado.mes.year}", style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onSeguinte) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mês seguinte")
        }
    }
}

/** Card herói (spec 005): saldo em displaySmall, olho de privacidade, toque → detalhe. */
@Composable
private fun CardSaldo(estado: EstadoUi, onAlternarSaldoOculto: () -> Unit, onVerDetalhe: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onVerDetalhe),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Saldo do mês",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconButton(onClick = onAlternarSaldoOculto) {
                    Icon(
                        if (estado.saldoOculto) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (estado.saldoOculto) "Mostrar valores" else "Ocultar valores",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val saldoExibido = exibirValor(formatarSaldo(estado.resumo.saldoCentavos), estado.saldoOculto)
            AnimatedContent(targetState = saldoExibido, label = "saldo") { texto ->
                Text(
                    texto,
                    style = MaterialTheme.typography.displaySmall,
                    color = when {
                        estado.saldoOculto -> MaterialTheme.colorScheme.onSurface
                        estado.resumo.saldoCentavos < 0 -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "↑ ${exibirValor(estado.resumo.receitas.formatado(), estado.saldoOculto)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "↓ ${exibirValor(estado.resumo.despesas.formatado(), estado.saldoOculto)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
