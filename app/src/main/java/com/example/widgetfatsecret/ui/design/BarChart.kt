package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Uma barra do gráfico. [value] `null` = dia sem registro: renderizado como
 * contorno tracejado, **fora** de qualquer média (regra do deck: ausência nunca
 * vira zero — planning.md §9, Etapa 6).
 */
data class BarDatum(val value: Float?, val highlighted: Boolean = false)

/**
 * Gráfico de barras minimalista com linha de meta opcional. Dias ausentes viram
 * contorno tracejado; barras acima da meta ganham [overColor]. Puramente
 * apresentacional — o cálculo de médias/janelas fica nos calculadores da Etapa 1.
 */
@Composable
fun BarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    goal: Float? = null,
    height: androidx.compose.ui.unit.Dp = 120.dp,
    barColor: Color? = null,
    overColor: Color? = null,
) {
    val colors = MaterialTheme.nutriColors
    val bar = barColor ?: colors.mint
    val over = overColor ?: colors.coral
    val goalLine = colors.text3
    val missingOutline = colors.line2

    val maxValue = remember(data, goal) {
        val values = data.mapNotNull { it.value }
        val top = (values.maxOrNull() ?: 0f)
        maxOf(top, goal ?: 0f, 1f) * 1.1f
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        if (data.isEmpty()) return@Canvas
        val slot = size.width / data.size
        val barWidth = slot * 0.55f
        val gap = (slot - barWidth) / 2f
        val cornerPx = 4.dp.toPx()

        fun yFor(value: Float): Float = size.height * (1f - (value / maxValue).coerceIn(0f, 1f))

        data.forEachIndexed { index, datum ->
            val left = index * slot + gap
            val value = datum.value
            if (value == null) {
                // Dia ausente: contorno tracejado até o topo disponível.
                val top = size.height * 0.25f
                drawRoundRect(
                    color = missingOutline,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, size.height - top),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx, cornerPx),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 5.dp.toPx()),
                        ),
                    ),
                )
            } else {
                val top = yFor(value)
                val fill = if (goal != null && value > goal) over else bar
                val fillColor = if (datum.highlighted) fill else fill.copy(alpha = 0.85f)
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, size.height - top),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerPx, cornerPx),
                )
            }
        }

        // Linha de meta por cima das barras.
        if (goal != null) {
            val y = yFor(goal)
            drawLine(
                color = goalLine,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8.dp.toPx(), 6.dp.toPx()),
                ),
            )
        }
    }
}

@Preview(name = "BarChart claro", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(
    name = "BarChart escuro",
    showBackground = true,
    backgroundColor = 0xFF131B2B,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun BarChartPreview() {
    WidgetFatSecretTheme {
        BarChart(
            modifier = Modifier.padding(16.dp),
            goal = 2000f,
            data = listOf(
                BarDatum(1850f),
                BarDatum(2100f),
                BarDatum(null), // dia sem registro
                BarDatum(1950f),
                BarDatum(2300f),
                BarDatum(1700f, highlighted = true),
                BarDatum(null),
            ),
        )
    }
}
