package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Botão primário do deck: fundo mint, texto na cor do fundo da tela, Manrope 700.
 * Raio de 13px. Usado para a ação principal de cada tela (Sincronizar, Salvar).
 */
@Composable
fun NutriPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.nutriColors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = NutriShapes.Button,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.mint,
            contentColor = colors.bg,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Botão secundário/destrutivo do deck: transparente, contorno de 1px, texto
 * coral. Usado para ações destrutivas (Desconectar) e secundárias.
 */
@Composable
fun NutriSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = true,
) {
    val colors = MaterialTheme.nutriColors
    val contentColor = if (destructive) colors.coral else colors.text2
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = NutriShapes.Button,
        border = BorderStroke(1.dp, colors.line2),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Preview(name = "NutriButtons claro", showBackground = true, backgroundColor = 0xFFF4F7FB)
@Preview(
    name = "NutriButtons escuro",
    showBackground = true,
    backgroundColor = 0xFF0A0F1A,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun NutriButtonsPreview() {
    WidgetFatSecretTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NutriPrimaryButton(text = "Sincronizar", onClick = {})
            NutriSecondaryButton(text = "Desconectar", onClick = {})
        }
    }
}
