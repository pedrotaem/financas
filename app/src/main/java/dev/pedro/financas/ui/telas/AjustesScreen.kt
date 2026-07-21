package dev.pedro.financas.ui.telas

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat

@Composable
fun AjustesScreen() {
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

        // Spec 002 adicionará: card de API key da Anthropic
    }
}
