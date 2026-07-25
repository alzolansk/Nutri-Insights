package com.example.widgetfatsecret.ui.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.widgetfatsecret.ui.design.MetaChip
import com.example.widgetfatsecret.ui.design.NutriShapes
import com.example.widgetfatsecret.ui.design.NutriSpacing
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.nutriColors

data class Methodology(
    val title: String,
    val window: String,
    val daysRecorded: Int,
    val calculation: String,
    val limitation: String,
)

/**
 * Audit trail shared by every calculated insight in the Patterns tab. It
 * always exposes the four fields required by planning.md Etapa 7: window,
 * sample size, calculation rule, and data limitation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MethodologySheet(methodology: Methodology, onDismiss: () -> Unit) {
    val colors = MaterialTheme.nutriColors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        contentColor = colors.text,
        shape = NutriShapes.Card,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = NutriSpacing.xxl)
                .padding(bottom = NutriSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(NutriSpacing.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(NutriSpacing.sm),
                ) {
                    Text(
                        text = "Como calculamos",
                        style = MonoText.meta,
                        color = colors.text3,
                    )
                    Text(
                        text = methodology.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.text,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Fechar", color = colors.mint)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(NutriSpacing.sm)) {
                MetaChip(text = methodology.window)
                MetaChip(
                    text = if (methodology.daysRecorded == 1) {
                        "1 dia registrado"
                    } else {
                        "${methodology.daysRecorded} dias registrados"
                    },
                )
            }

            MethodologySection(
                label = "REGRA DE CÁLCULO",
                text = methodology.calculation,
            )
            MethodologySection(
                label = "LIMITAÇÃO DO DADO",
                text = methodology.limitation,
            )
        }
    }
}

@Composable
private fun MethodologySection(label: String, text: String) {
    val colors = MaterialTheme.nutriColors
    Column(verticalArrangement = Arrangement.spacedBy(NutriSpacing.xs)) {
        Text(text = label, style = MonoText.meta, color = colors.text3)
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = colors.text2)
    }
}
