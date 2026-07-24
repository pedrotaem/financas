package dev.pedro.financas.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.pedro.financas.FinancasApp
import dev.pedro.financas.ui.telas.AjustesScreen
import dev.pedro.financas.ui.telas.LancamentoDialog
import dev.pedro.financas.ui.telas.DashboardScreen
import dev.pedro.financas.ui.telas.LancamentosScreen
import dev.pedro.financas.ui.telas.OrcamentoScreen
import dev.pedro.financas.ui.theme.FinancasTheme

private data class Aba(val rota: String, val titulo: String, val icone: ImageVector)

private val ABAS = listOf(
    Aba("inicio", "Início", Icons.Filled.Home),
    Aba("lancamentos", "Lançamentos", Icons.AutoMirrored.Filled.List),
    Aba("ajustes", "Ajustes", Icons.Filled.Settings),
)

class MainActivity : ComponentActivity() {

    private val viewModel: FinancasViewModel by viewModels {
        FinancasViewModel.factory((application as FinancasApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val temaOled by viewModel.temaOled.collectAsState()
            FinancasTheme(oledPreto = temaOled) {
                AppFinancas(viewModel)
            }
        }
    }
}

@Composable
fun AppFinancas(viewModel: FinancasViewModel) {
    val navController = rememberNavController()
    val estado by viewModel.estado.collectAsState()
    var dialogoAberto by remember { mutableStateOf(false) }

    val rotaAtual = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                ABAS.forEach { aba ->
                    NavigationBarItem(
                        selected = rotaAtual == aba.rota,
                        onClick = {
                            navController.navigate(aba.rota) {
                                popUpTo("inicio") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(aba.icone, contentDescription = aba.titulo) },
                        label = { Text(aba.titulo) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (rotaAtual != "ajustes" && rotaAtual != "orcamento") {
                FloatingActionButton(onClick = { dialogoAberto = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Novo lançamento")
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "inicio",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("inicio") {
                DashboardScreen(
                    estado = estado,
                    onMesAnterior = viewModel::mesAnterior,
                    onMesSeguinte = viewModel::mesSeguinte,
                    onVerPendencias = { navController.navigate("lancamentos") },
                    onAlternarSaldoOculto = viewModel::alternarSaldoOculto,
                    onVerOrcamento = { navController.navigate("orcamento") },
                )
            }
            composable("orcamento") {
                OrcamentoScreen(
                    estado = estado,
                    onDefinir = viewModel::definirOrcamento,
                    onRemover = viewModel::removerOrcamento,
                )
            }
            composable("lancamentos") {
                LancamentosScreen(
                    estado = estado,
                    onConfirmar = viewModel::confirmar,
                    onRejeitar = viewModel::rejeitar,
                    onCategorizar = viewModel::categorizar,
                    onEditar = viewModel::editar,
                    onExcluir = viewModel::excluir,
                    onDescartarCaptura = viewModel::descartarCaptura,
                    onAdicionarDeCaptura = viewModel::adicionarDeCaptura,
                )
            }
            composable("ajustes") {
                val temaOled by viewModel.temaOled.collectAsState()
                AjustesScreen(
                    temaOled = temaOled,
                    onAlternarTemaOled = viewModel::alternarTemaOled,
                )
            }
        }
    }

    if (dialogoAberto) {
        LancamentoDialog(
            titulo = "Novo lançamento",
            onDismiss = { dialogoAberto = false },
            onSalvar = { tipo, centavos, descricao, categoria ->
                viewModel.adicionarManual(tipo, centavos, descricao, categoria)
                dialogoAberto = false
            },
        )
    }
}
