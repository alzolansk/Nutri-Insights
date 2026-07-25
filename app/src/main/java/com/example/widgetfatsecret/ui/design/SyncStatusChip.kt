package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Estado de sincronização, mapeado a partir do `NutritionSnapshot` pela camada
 * de ViewModel (não aqui — o design system não importa modelos de dados). O deck
 * exige este chip visível em todas as telas.
 */
enum class SyncStatus { SYNCED, SYNCING, ERROR, OFFLINE, DISCONNECTED }

/**
 * Chip único de status de sync usado em todas as telas. Sempre carrega um rótulo
 * textual — a cor é reforço, nunca a única informação (slide 4). [detail] é um
 * complemento monoespaçado opcional (ex.: "há 12 min", data do último sync).
 */
@Composable
fun SyncStatusChip(
    status: SyncStatus,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val colors = MaterialTheme.nutriColors
    val (label, dot) = when (status) {
        SyncStatus.SYNCED -> "Sincronizado" to colors.mint
        SyncStatus.SYNCING -> "Sincronizando…" to colors.cyan
        SyncStatus.ERROR -> "Falha na sincronização" to colors.coral
        SyncStatus.OFFLINE -> "Offline — últimos dados" to colors.amber
        SyncStatus.DISCONNECTED -> "Desconectado" to colors.text3
    }
    val text = if (detail != null) "$label · $detail" else label
    MetaChip(text = text, modifier = modifier, dotColor = dot)
}

@Preview(name = "SyncStatusChip claro", showBackground = true, backgroundColor = 0xFFF4F7FB)
@Preview(
    name = "SyncStatusChip escuro",
    showBackground = true,
    backgroundColor = 0xFF0A0F1A,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SyncStatusChipPreview() {
    WidgetFatSecretTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SyncStatusChip(status = SyncStatus.SYNCED, detail = "há 12 min")
            SyncStatusChip(status = SyncStatus.SYNCING)
            SyncStatusChip(status = SyncStatus.ERROR)
            SyncStatusChip(status = SyncStatus.OFFLINE, detail = "há 3 h")
            SyncStatusChip(status = SyncStatus.DISCONNECTED)
        }
    }
}
