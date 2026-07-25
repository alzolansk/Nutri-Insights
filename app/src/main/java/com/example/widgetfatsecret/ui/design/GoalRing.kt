package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Anel de progresso de meta com um valor grande no centro (padrão da tela Hoje:
 * "restam N kcal"). [progress] é fração 0..1 (será limitada a esse intervalo no
 * desenho do arco, mas o número central é responsabilidade do chamador). Se o
 * consumo passar da meta, passe [overColor] para sinalizar o excedente sem
 * depender só da cor — o rótulo/valor central deve dizer isso em texto.
 */
@Composable
fun GoalRing(
    progress: Float,
    centerValue: String?,
    modifier: Modifier = Modifier,
    centerLabel: String? = null,
    ringColor: Color? = null,
    overColor: Color? = null,
    ringSize: androidx.compose.ui.unit.Dp = 168.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 14.dp,
) {
    val colors = MaterialTheme.nutriColors
    val clamped = progress.coerceIn(0f, 1f)
    val over = progress > 1f
    val track = colors.surface2
    val arc = when {
        over -> overColor ?: colors.coral
        ringColor != null -> ringColor
        else -> colors.mint
    }
    Box(modifier = modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(size.width - strokeWidth.toPx(), size.height - strokeWidth.toPx())
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            // Trilho completo.
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            // Progresso.
            drawArc(
                color = arc,
                startAngle = -90f,
                sweepAngle = 360f * clamped,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerValue ?: "—",
                style = MonoText.metricLarge,
                color = if (centerValue == null) colors.text3 else colors.text,
            )
            if (centerLabel != null) {
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.text3,
                )
            }
        }
    }
}

@Preview(name = "GoalRing claro", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(
    name = "GoalRing escuro",
    showBackground = true,
    backgroundColor = 0xFF131B2B,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GoalRingPreview() {
    WidgetFatSecretTheme {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
        ) {
            GoalRing(progress = 0.62f, centerValue = "1.480", centerLabel = "kcal restantes")
            GoalRing(progress = 1.15f, centerValue = "+240", centerLabel = "kcal acima")
        }
    }
}
