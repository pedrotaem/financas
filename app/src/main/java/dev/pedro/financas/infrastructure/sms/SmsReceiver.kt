package dev.pedro.financas.infrastructure.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dev.pedro.financas.FinancasApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Captura de SMS de operadoras (spec 007). BroadcastReceiver em vez do listener
 * de notificações: corpo completo, sem truncamento das mensagens longas da Vivo.
 * Multipart é concatenado antes do parse.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val mensagens = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (mensagens.isEmpty()) return

        val corpo = mensagens.joinToString("") { it.messageBody.orEmpty() }
        val remetente = mensagens.first().displayOriginatingAddress
        val container = (context.applicationContext as FinancasApp).container

        val pendente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resultado = container.capturarSms.executar(remetente, corpo)
                Log.d(TAG, "SMS de $remetente: ${resultado::class.simpleName}")
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao capturar SMS", e)
            } finally {
                pendente.finish()
            }
        }
    }

    companion object {
        /** Tag de calibração via logcat, como CapturaNotificacao na spec 001. */
        const val TAG = "CapturaSms"
    }
}
