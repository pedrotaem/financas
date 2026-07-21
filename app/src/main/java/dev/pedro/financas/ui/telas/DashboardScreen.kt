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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import dev.pedro.financas.domain.formatarSaldo
import dev.pedro.financas.ui.EstadoUi
import dev.pedro.financas.ui.componentes.GraficoDonut
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DashboardScreen(
    estado: EstadoUi,
    onMesAnterior: () -> Unit,
    onMesSeguinte: () -> Unit,
    onVerPendencias: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { SeletorMes(estado, onMesAnterior, onMesSeguinte) }

        if (estado.qtdPendentes > 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onVerPendencias),
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

        item { CardSaldo(estado) }

        if (estado.resumo.fatias.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Por categoria", style = MaterialTheme.typography.titleMedium)
                        GraficoDonut(
                            fatias = estado.resumo.fatias,
                            totalFormatado = estado.resumo.despesas.formatado(),
                        )
                    }
                }
            }
        }

        if (estado.lancamentosDoMes.isNotEmpty()) {
            item { Text("Últimos lançamentos", style = MaterialTheme.typography.titleMedium) }
            items(estado.lancamentosDoMes.take(5), key = { it.id.valor }) { l ->
                LinhaLancamentoSimples(l)
            }
        } else {
            item {
                Text(
                    "Nenhum lançamento neste mês.",
                    style = MaterialTheme.typography.bodyMedium,
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
        Text("$nomeMes ${estado.mes.year}", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onSeguinte) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mês seguinte")
        }
    }
}

@Composable
private fun CardSaldo(estado: EstadoUi) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Saldo do mês", style = MaterialTheme.typography.labelLarge)
            Text(
                formatarSaldo(estado.resumo.saldoCentavos),
                style = MaterialTheme.typography.headlineMedium,
                color = if (estado.resumo.saldoCentavos < 0)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "↑ ${estado.resumo.receitas.formatado()}",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "↓ ${estado.resumo.despesas.formatado()}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
