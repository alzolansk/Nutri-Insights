package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Estado vazio / dados insuficientes, com linguagem descritiva (nunca de
 * cobrança — slide 2). Serve tanto a "sincronizado sem registros" quanto a
 * "faltam amostras para esta análise". Um [icon] opcional aceita apenas um
 * glifo textual curto; o design do app não usa emoji nem ilustração.
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    contentPadding: Dp = NutriSpacing.xl,
) {
    val colors = MaterialTheme.nutriColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(NutriSpacing.sm),
    ) {
        if (icon != null) {
            Text(text = icon, style = MaterialTheme.typography.displayMedium)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
            textAlign = TextAlign.Center,
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text2,
                textAlign = TextAlign.Center,
            )
        }
        if (actionLabel != null && onAction != null) {
            NutriPrimaryButton(text = actionLabel, onClick = onAction)
        }
    }
}

@Preview(name = "EmptyState claro", showBackground = true, backgroundColor = 0xFFF4F7FB)
@Preview(
    name = "EmptyState escuro",
    showBackground = true,
    backgroundColor = 0xFF0A0F1A,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EmptyStatePreview() {
    WidgetFatSecretTheme {
        EmptyState(
            modifier = Modifier.padding(16.dp),
            title = "Dados insuficientes",
            description = "São necessários ao menos 4 dias registrados na janela " +
                "para calcular uma média confiável. Você tem 2.",
            actionLabel = "Sincronizar agora",
            onAction = {},
        )
    }
}
