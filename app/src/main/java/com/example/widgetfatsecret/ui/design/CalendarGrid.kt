package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencyDay
import com.example.widgetfatsecret.fatsecret.domain.history.ConsistencyDayState
import com.example.widgetfatsecret.ui.theme.MonoText
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors
import java.time.LocalDate

private val WeekLabels = listOf("S", "T", "Q", "Q", "S", "S", "D")
private val DayShape = RoundedCornerShape(10.dp)

/** Monthly calendar whose four states differ by shape as well as color. */
@Composable
fun CalendarGrid(days: List<ConsistencyDay>, modifier: Modifier = Modifier) {
    val firstDate = days.firstOrNull()?.let { LocalDate.ofEpochDay(it.dateInt) }
    val leadingCells = firstDate?.dayOfWeek?.value?.minus(1) ?: 0
    val cells = List<ConsistencyDay?>(leadingCells) { null } + days

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NutriSpacing.sm),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            WeekLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MonoText.meta,
                    color = MaterialTheme.nutriColors.text3,
                    textAlign = TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(NutriSpacing.xs),
            ) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (day != null) CalendarDayCell(day)
                    }
                }
                repeat(7 - week.size) { Box(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(day: ConsistencyDay) {
    val colors = MaterialTheme.nutriColors
    val date = LocalDate.ofEpochDay(day.dateInt)
    val label = when (day.state) {
        ConsistencyDayState.RECORDED -> "registrado"
        ConsistencyDayState.NO_ENTRIES -> "sem entradas"
        ConsistencyDayState.NOT_SYNCED -> "não sincronizado"
        ConsistencyDayState.FUTURE -> "futuro"
    }
    val base = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .semantics { contentDescription = "${date.dayOfMonth} de ${date.monthValue}, $label" }
    val styled = when (day.state) {
        ConsistencyDayState.RECORDED -> base.background(colors.mint, DayShape)
        ConsistencyDayState.NO_ENTRIES -> base
            .background(colors.surface2, DayShape)
            .border(1.dp, colors.line2, DayShape)
        ConsistencyDayState.NOT_SYNCED -> base.dashedBorder(colors.amber)
        ConsistencyDayState.FUTURE -> base.border(1.dp, colors.line.copy(alpha = 0.45f), DayShape)
    }
    val textColor = when (day.state) {
        ConsistencyDayState.RECORDED -> colors.bg
        ConsistencyDayState.NO_ENTRIES -> colors.text2
        ConsistencyDayState.NOT_SYNCED -> colors.amber
        ConsistencyDayState.FUTURE -> colors.text3.copy(alpha = 0.55f)
    }
    Box(modifier = styled, contentAlignment = Alignment.Center) {
        Text(text = date.dayOfMonth.toString(), style = MonoText.meta, color = textColor)
    }
}

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val strokeWidth = 1.dp.toPx()
    drawRoundRect(
        color = color,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
        ),
    )
}

@Preview(name = "Calendário claro", showBackground = true, backgroundColor = 0xFFF4F7FB)
@Preview(
    name = "Calendário escuro",
    showBackground = true,
    backgroundColor = 0xFF0A0F1A,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun CalendarGridPreview() {
    val start = LocalDate.of(2026, 7, 1).toEpochDay()
    val states = listOf(
        ConsistencyDayState.RECORDED,
        ConsistencyDayState.NO_ENTRIES,
        ConsistencyDayState.NOT_SYNCED,
        ConsistencyDayState.FUTURE,
    )
    WidgetFatSecretTheme {
        CalendarGrid(
            days = (0L until 31L).map { offset ->
                ConsistencyDay(start + offset, states[(offset % states.size).toInt()])
            },
            modifier = Modifier.padding(16.dp),
        )
    }
}
