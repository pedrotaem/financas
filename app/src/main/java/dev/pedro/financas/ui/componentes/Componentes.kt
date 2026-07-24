package dev.pedro.financas.ui.componentes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.pedro.financas.domain.Categoria
import dev.pedro.financas.domain.FatiaCategoria
import kotlin.math.atan2

/** Máscara de privacidade (spec 005): substitui qualquer valor monetário exibido. */
const val VALOR_OCULTO = "R$ ••••"

fun exibirValor(formatado: String, oculto: Boolean): String =
    if (oculto) VALOR_OCULTO else formatado

/** Mascara valores dentro de texto livre (ex.: texto cru de notificação capturada). */
private val PADRAO_VALOR_EM_TEXTO = Regex("""R\$\s?[\d.,]+""")

fun mascararValoresNoTexto(texto: String): String =
    PADRAO_VALOR_EM_TEXTO.replace(texto) { VALOR_OCULTO }

/**
 * Par tonal por categoria (spec 005): mesmo matiz nos dois temas (reconhecimento,
 * spec 003 regra 4), tom escolhido para contraste sobre as surfaces de cada modo.
 */
val CORES_CATEGORIA_CLARO: Map<Categoria?, Color> = mapOf(
    Categoria.MERCADO to Color(0xFF2E7D32),
    Categoria.RESTAURANTE to Color(0xFFEF6C00),
    Categoria.FARMACIA to Color(0xFFC2185B),
    Categoria.TRANSPORTE to Color(0xFF1565C0),
    Categoria.COMBUSTIVEL to Color(0xFF5D4037),
    Categoria.LAZER to Color(0xFF7B1FA2),
    Categoria.VESTUARIO to Color(0xFF00838F),
    Categoria.CASA to Color(0xFF558B2F),
    Categoria.OUTROS to Color(0xFF546E7A),
    null to Color(0xFF9E9E9E),
)

val CORES_CATEGORIA_ESCURO: Map<Categoria?, Color> = mapOf(
    Categoria.MERCADO to Color(0xFF81C784),
    Categoria.RESTAURANTE to Color(0xFFFFB74D),
    Categoria.FARMACIA to Color(0xFFF48FB1),
    Categoria.TRANSPORTE to Color(0xFF64B5F6),
    Categoria.COMBUSTIVEL to Color(0xFFBCAAA4),
    Categoria.LAZER to Color(0xFFCE93D8),
    Categoria.VESTUARIO to Color(0xFF4DD0E1),
    Categoria.CASA to Color(0xFFAED581),
    Categoria.OUTROS to Color(0xFF90A4AE),
    null to Color(0xFFBDBDBD),
)

/** Aproxima a cor da categoria da paleta do tema do aparelho sem perder distinção (spec 005). */
@Composable
fun corCategoria(categoria: Categoria?): Color {
    val base = if (isSystemInDarkTheme()) CORES_CATEGORIA_ESCURO[categoria] else CORES_CATEGORIA_CLARO[categoria]
    return lerp(base ?: Color.Gray, MaterialTheme.colorScheme.primary, 0.15f)
}

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

/** Estado vazio acolhedor (spec 005): ícone + mensagem + chamada para ação. */
@Composable
fun EstadoVazio(mensagem: String, acao: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp),
        )
        Text(
            mensagem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        acao?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Donut de despesas por categoria — Canvas puro (spec 003), visual da spec 005:
 * pontas arredondadas, animação de varredura na entrada, fatia tocável destaca a legenda.
 */
@Composable
fun GraficoDonut(
    fatias: List<FatiaCategoria>,
    totalFormatado: String,
    valoresOcultos: Boolean = false,
) {
    val total = fatias.sumOf { it.total.centavos }.coerceAtLeast(1)
    val cores = fatias.map { corCategoria(it.categoria) }
    var selecionada by remember(fatias) { mutableStateOf<Int?>(null) }
    val progresso = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progresso.animateTo(1f, tween(700)) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .pointerInput(fatias) {
                        detectTapGestures { pos ->
                            val dx = pos.x - size.width / 2f
                            val dy = pos.y - size.height / 2f
                            val angulo = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90 + 360) % 360
                            var acumulado = 0.0
                            var achada: Int? = null
                            fatias.forEachIndexed { i, f ->
                                val varredura = 360.0 * f.total.centavos / total
                                if (angulo >= acumulado && angulo < acumulado + varredura) achada = i
                                acumulado += varredura
                            }
                            selecionada = if (selecionada == achada) null else achada
                        }
                    },
            ) {
                val espessura = 28.dp.toPx()
                val diametro = size.minDimension - espessura
                val topoEsq = Offset(
                    (size.width - diametro) / 2,
                    (size.height - diametro) / 2,
                )
                var anguloInicio = -90f
                fatias.forEachIndexed { i, fatia ->
                    val varredura = 360f * fatia.total.centavos / total
                    val apagada = selecionada != null && selecionada != i
                    drawArc(
                        color = cores[i].copy(alpha = if (apagada) 0.3f else 1f),
                        startAngle = anguloInicio,
                        sweepAngle = ((varredura - 1.5f).coerceAtLeast(0.5f)) * progresso.value,
                        useCenter = false,
                        topLeft = topoEsq,
                        size = Size(diametro, diametro),
                        style = Stroke(width = espessura, cap = StrokeCap.Round),
                    )
                    anguloInicio += varredura
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Despesas", style = MaterialTheme.typography.labelMedium)
                Text(exibirValor(totalFormatado, valoresOcultos), style = MaterialTheme.typography.titleLarge)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            fatias.forEachIndexed { i, fatia ->
                val destacada = selecionada == i
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (destacada) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                            RoundedCornerShape(8.dp),
                        )
                        .clickable { selecionada = if (destacada) null else i }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(cores[i], CircleShape)
                        )
                        Text(
                            fatia.categoria.rotulo(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text(
                        exibirValor(fatia.total.formatado(), valoresOcultos),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
