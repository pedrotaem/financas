package dev.pedro.financas.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pedro.financas.ui.theme.FinancasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinancasTheme {
                TelaInicial()
            }
        }
    }
}

@Composable
fun TelaInicial() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Text(
            text = "Finanças — scaffold OK.\nSpecs 001/002 pendentes de implementação.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
