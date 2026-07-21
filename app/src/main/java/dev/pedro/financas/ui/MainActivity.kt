package dev.pedro.financas.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import dev.pedro.financas.FinancasApp
import dev.pedro.financas.domain.Lancamento
import dev.pedro.financas.domain.Status
import dev.pedro.financas.domain.Tipo
import dev.pedro.financas.domain.captura.CapturaBruta
import dev.pedro.financas.ui.theme.FinancasTheme
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModel.factory((application as FinancasApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinancasTheme {
                TelaInicial(viewModel)
            }
        }
    }
}

@Composable
fun TelaInicial(viewModel: HomeViewModel) {
    val lancamentos by viewModel.lancamentos.collectAsState()
    val capturas by viewModel.capturasPendentes.collectAsState()
    val context = LocalContext.current

    val temAcesso = NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!temAcesso) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                "Acesso a notificações necessário",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                "Para capturar compras e PIX do Itaú automaticamente.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Button(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                )
                            }) { Text("Permitir") }
                        }
                    }
                }
            }

            if (capturas.isNotEmpty()) {
                item {
                    Text(
                        "Capturas não reconhecidas (${capturas.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(capturas, key = { it.id.valor }) { CartaoCapturaBruta(it) }
            }

            item {
                Text("Lançamentos", style = MaterialTheme.typography.titleMedium)
            }
            if (lancamentos.isEmpty()) {
                item {
                    Text(
                        "Nenhum lançamento ainda. Compras e PIX do Itaú aparecerão aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(lancamentos, key = { it.id.valor }) { l ->
                CartaoLancamento(l, onConfirmar = { viewModel.confirmar(l) })
            }
        }
    }
}

private val FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM HH:mm")

@Composable
private fun CartaoLancamento(l: Lancamento, onConfirmar: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            Column(horizontalAlignment = Alignment.End) {
                val sinal = if (l.tipo == Tipo.DEBITO) "-" else "+"
                Text(
                    "$sinal${l.valor.formatado()}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (l.tipo == Tipo.DEBITO)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                )
                if (l.status == Status.PENDENTE_REVISAO) {
                    TextButton(onClick = onConfirmar) { Text("Confirmar") }
                }
            }
        }
    }
}

@Composable
private fun CartaoCapturaBruta(c: CapturaBruta) {
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
