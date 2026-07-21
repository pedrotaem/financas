package dev.pedro.financas.infrastructure.notificacao

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import dev.pedro.financas.FinancasApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Listener de notificações (spec 001). Filtra pacotes do Itaú e delega
 * ao caso de uso CapturarNotificacao. Não remove a notificação original.
 */
class CapturaNotificacaoService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "CapturaNotificacao"
        /** Prefixo dos pacotes do Itaú. Verificar no aparelho (risco da spec 001). */
        private const val PREFIXO_ITAU = "com.itau"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pacote = sbn.packageName
        if (!pacote.startsWith(PREFIXO_ITAU)) {
            // Debug p/ calibrar filtro de pacote (mitigação de risco da spec)
            Log.d(TAG, "Ignorado: $pacote")
            return
        }

        val extras = sbn.notification.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val texto = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        // Log do texto cru — primeira tarefa da spec: calibrar regex com dados reais
        Log.i(TAG, "Itau notif: titulo=[$titulo] texto=[$texto]")

        val useCase = (application as FinancasApp).container.capturarNotificacao
        scope.launch {
            val resultado = useCase.executar(pacote, titulo, texto)
            Log.i(TAG, "Resultado: ${resultado::class.simpleName}")
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
