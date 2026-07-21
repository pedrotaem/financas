package dev.pedro.financas.ui.telas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pedro.financas.domain.Categoria
import dev.pedro.financas.domain.Tipo
import dev.pedro.financas.ui.componentes.rotulo
import dev.pedro.financas.ui.parseValorParaCentavos

@Composable
fun AdicionarLancamentoDialog(
    onDismiss: () -> Unit,
    onSalvar: (Tipo, Long, String, Categoria?) -> Unit,
) {
    var tipo by remember { mutableStateOf(Tipo.DEBITO) }
    var valorTexto by remember { mutableStateOf("") }
    var descricao by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf<Categoria?>(null) }
    var categoriaAberta by remember { mutableStateOf(false) }

    val valorCentavos = parseValorParaCentavos(valorTexto)
    val valido = valorCentavos != null && descricao.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo lançamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = tipo == Tipo.DEBITO,
                        onClick = { tipo = Tipo.DEBITO },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Despesa") }
                    SegmentedButton(
                        selected = tipo == Tipo.CREDITO,
                        onClick = { tipo = Tipo.CREDITO },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Receita") }
                }
                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it },
                    label = { Text("Valor (R$)") },
                    placeholder = { Text("54,90") },
                    isError = valorTexto.isNotEmpty() && valorCentavos == null,
                    singleLine = true,
                )
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    singleLine = true,
                )
                Box {
                    AssistChip(
                        onClick = { categoriaAberta = true },
                        label = { Text(categoria.rotulo()) },
                    )
                    DropdownMenu(
                        expanded = categoriaAberta,
                        onDismissRequest = { categoriaAberta = false },
                    ) {
                        Categoria.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.rotulo()) },
                                onClick = {
                                    categoriaAberta = false
                                    categoria = cat
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valido,
                onClick = { onSalvar(tipo, valorCentavos!!, descricao.trim(), categoria) },
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
