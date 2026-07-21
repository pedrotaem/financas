package dev.pedro.financas.ui.componentes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import dev.pedro.financas.domain.Categoria
import dev.pedro.financas.domain.FatiaCategoria

/** Paleta fixa por categoria (spec 003, regra 4). */
val CORES_CATEGORIA: Map<Categoria?, Color> = mapOf(
    Categoria.MERCADO to Color(0xFF4CAF50),
    Categoria.RESTAURANTE to Color(0xFFFF9800),
    Categoria.FARMACIA to Color(0xFFE91E63),
    Categoria.TRANSPORTE to Color(0xFF2196F3),
    Categoria.COMBUSTIVEL to Color(0xFF795548),
    Categoria.LAZER to Color(0xFF9C27B0),
    Categoria.VESTUARIO to Color(0xFF00BCD4),
    Categoria.CASA to Color(0xFF8BC34A),
    Categoria.OUTROS to Color(0xFF607D8B),
    null to Color(0xFFBDBDBD),
)

fun Categoria?.rotulo(): String = when (this) {
    null -> "Sem categoria"
    Categoria.MERCADO -> "Mercado"
    Categoria.RESTAURANTE -> "Restaurante"
    Categoria.FARMACIA -> "Farmácia"
    Categoria.TRANSPORTE -> "Transporte"
    Categoria.COMBUSTIVEL -> "Combustível"
    Categoria.LAZER -> "Lazer"
    Categoria.VESTUARIO -> "Vestuário"
    Categoria.CASA -> "Casa"
    Categoria.OUTROS -> "Outros"
}

/**
 * Donut de despesas por categoria (spec 003) — Canvas puro.
 * Centro exibe o total; legenda abaixo.
 */
@Composable
fun GraficoDonut(fatias: List<FatiaCategoria>, totalFormatado: String) {
    val total = fatias.sumOf { it.total.centavos }.coerceAtLeast(1)

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val espessura = 28.dp.toPx()
                val diametro = size.minDimension - espessura
                val topoEsq = androidx.compose.ui.geometry.Offset(
                    (size.width - diametro) / 2,
                    (size.height - diametro) / 2,
                )
                var anguloInicio = -90f
                fatias.forEach { fatia ->
                    val varredura = 360f * fatia.total.centavos / total
                    drawArc(
                        color = CORES_CATEGORIA[fatia.categoria] ?: Color.Gray,
                        startAngle = anguloInicio,
                        sweepAngle = (varredura - 1.5f).coerceAtLeast(0.5f), // respiro entre fatias
                        useCenter = false,
                        topLeft = topoEsq,
                        size = Size(diametro, diametro),
                        style = Stroke(width = espessura, cap = StrokeCap.Butt),
                    )
                    anguloInicio += varredura
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Despesas", style = MaterialTheme.typography.labelMedium)
                Text(totalFormatado, style = MaterialTheme.typography.titleMedium)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            fatias.forEach { fatia ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(
                                    CORES_CATEGORIA[fatia.categoria] ?: Color.Gray,
                                    CircleShape,
                                )
                        )
                        Text(
                            fatia.categoria.rotulo(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(fatia.total.formatado(), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
