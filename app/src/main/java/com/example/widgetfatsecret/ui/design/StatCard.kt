package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Cartão de superfície padrão (raio 22px, contorno sutil). Base visual de quase
 * toda tela do "Nutri Insights". Opcionalmente exibe um título e um metadado
 * monoespaçado à direita (ex.: "18 de 30 dias").
 */
@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    meta: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.nutriColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NutriShapes.Card)
            .background(colors.surface)
            .border(1.dp, colors.line, NutriShapes.Card)
            .padding(NutriSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(NutriSpacing.md),
    ) {
        if (title != null || meta != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text,
                )
                if (meta != null) {
                    Text(text = meta, style = MonoText.meta, color = colors.text3)
                }
            }
        }
        content()
    }
}

@Preview(name = "StatCard claro", showBackground = true, backgroundColor = 0xFFF4F7FB)
@Preview(
    name = "StatCard escuro",
    showBackground = true,
    backgroundColor = 0xFF0A0F1A,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun StatCardPreview() {
    WidgetFatSecretTheme {
        StatCard(
            modifier = Modifier.padding(16.dp),
            title = "Consistência",
            meta = "18 de 30 dias",
        ) {
            Text(
                text = "Sequência atual de 6 dias registrados.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.nutriColors.text2,
            )
        }
    }
}
