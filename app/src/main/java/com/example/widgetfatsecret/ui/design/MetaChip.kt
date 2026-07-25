package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Chip de metadado monoespaçado (pill). Usado para rótulos como "há 12 min",
 * "4 semanas", "acima da meta". Um [dotColor] opcional adiciona um ponto de
 * status à esquerda — mas o texto sempre carrega o significado, nunca só a cor
 * (regra de acessibilidade do slide 4).
 */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    val colors = MaterialTheme.nutriColors
    Row(
        modifier = modifier
            .clip(NutriShapes.Chip)
            .background(colors.surface2)
            .border(1.dp, colors.line, NutriShapes.Chip)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dotColor != null) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
        }
        Text(text = text, style = MonoText.meta, color = colors.text2)
    }
}

@Preview(name = "MetaChip claro", showBackground = true, backgroundColor = 0xFFF4F7FB)
@Preview(
    name = "MetaChip escuro",
    showBackground = true,
    backgroundColor = 0xFF0A0F1A,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MetaChipPreview() {
    WidgetFatSecretTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetaChip(text = "4 semanas")
            MetaChip(text = "acima da meta", dotColor = MaterialTheme.nutriColors.amber)
            MetaChip(text = "18 de 30 dias", dotColor = MaterialTheme.nutriColors.mint)
        }
    }
}
