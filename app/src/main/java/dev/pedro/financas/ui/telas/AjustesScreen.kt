package dev.pedro.financas.ui.telas

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat

@Composable
fun AjustesScreen(
    temaOled: Boolean,
    onAlternarTemaOled: () -> Unit,
) {
    val context = LocalContext.current
    val temAcesso = NotificationManagerCompat.getEnabledListenerPackages(context)
        .contains(context.packageName)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.titleLarge)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Captura de notificações", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (temAcesso) "Ativa — compras e PIX do Itaú são capturados."
                    else "Inativa — conceda acesso para capturar compras e PIX.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (!temAcesso) {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }) { Text("Conceder acesso") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Aparência", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Preto OLED", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Fundo preto puro no tema escuro — economiza bateria em telas OLED",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = temaOled, onCheckedChange = { onAlternarTemaOled() })
                }
            }
        }

        // Spec 002 adicionará: card de API key da Anthropic
    }
}
