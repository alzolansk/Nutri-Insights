package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Valor numérico monoespaçado com `tabular-nums` (o deck exige mono em todo
 * número, para que dígitos não "dancem" entre atualizações). Aceita rótulo acima
 * e unidade à direita do número.
 *
 * Regra do deck: um número ausente é ausência, nunca zero. Passe [value] `null`
 * para renderizar o traço "—" em vez de "0".
 */
@Composable
fun MetricValue(
    value: String?,
    modifier: Modifier = Modifier,
    label: String? = null,
    unit: String? = null,
    valueColor: Color? = null,
    valueStyle: TextStyle = MonoText.metricLarge,
) {
    val colors = MaterialTheme.nutriColors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.text3,
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value ?: "—",
                style = valueStyle,
                color = when {
                    value == null -> colors.text3
                    valueColor != null -> valueColor
                    else -> colors.text
                },
            )
            if (unit != null) {
                Text(
                    text = " $unit",
                    style = MonoText.meta,
                    color = colors.text2,
                )
            }
        }
    }
}

@Preview(name = "MetricValue claro", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(
    name = "MetricValue escuro",
    showBackground = true,
    backgroundColor = 0xFF131B2B,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MetricValuePreview() {
    WidgetFatSecretTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            MetricValue(value = "1.480", label = "RESTANTES", unit = "kcal")
            MetricValue(
                value = "112",
                label = "PROTEÍNA",
                unit = "g",
                valueColor = MaterialTheme.nutriColors.cyan,
                valueStyle = MonoText.metricMedium,
            )
            MetricValue(value = null, label = "SEM REGISTRO", valueStyle = MonoText.metricMedium)
        }
    }
}
