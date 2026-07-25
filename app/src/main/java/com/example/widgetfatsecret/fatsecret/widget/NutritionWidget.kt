package com.example.widgetfatsecret.fatsecret.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
// Two overloads with the same name: the reified one (glance.action) targets an
// Activity in THIS app, the Intent one (glance.appwidget.action) targets the
// external FatSecret app. Kotlin picks by argument type.
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.widgetfatsecret.MainActivity
import com.example.widgetfatsecret.fatsecret.data.AppContainer
import com.example.widgetfatsecret.fatsecret.data.NutritionSnapshot
import com.example.widgetfatsecret.fatsecret.data.NutritionUiState
import com.example.widgetfatsecret.fatsecret.data.SyncStatus
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretConfig
import com.example.widgetfatsecret.fatsecret.domain.NutritionCalculator
import com.example.widgetfatsecret.fatsecret.domain.NutritionFormat
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Home-screen widget showing today's FatSecret nutrition.
 *
 * Layout principles (every size): calories are the single dominant element, the
 * macros are clearly subordinate, and progress is drawn as hairline bars rather
 * than filled cards. There are no borders, no icons and no buttons — the tap
 * targets are the text itself, which keeps the widget quiet next to the
 * wallpaper while still carrying the day's numbers.
 *
 * Responsiveness is delegated to [scaleFor]/[WidgetSizes]: type, padding,
 * spacing, bar weight and the chart's height all come from the space the
 * launcher actually granted, so the same code renders a dense one-row card on a
 * compact phone and an airy three-row one on a large device. Nothing here scales
 * by a constant — each band gets the composition that suits it, and the vertical
 * slack is spent by weighted spacers rather than left as dead margin.
 *
 * State is read once per update from the repository's persisted snapshot and the
 * user's goals, so the widget always renders the last valid data. Explicit
 * [updateAll] calls (after every sync) trigger a fresh render.
 */
class NutritionWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(WidgetSizes.ALL)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = AppContainer.get(context)
        val state = container.repository.uiState.first()
        provideContent {
            WidgetRoot(state)
        }
    }

    companion object {
        suspend fun updateAll(context: Context) = NutritionWidget().updateAll(context)
    }
}

// --- Rendering ---------------------------------------------------------------

private enum class WidgetMode { NO_CREDENTIALS, DISCONNECTED, LOADING, DATA }

private fun modeOf(snapshot: NutritionSnapshot): WidgetMode = when {
    !FatSecretConfig.hasCredentials -> WidgetMode.NO_CREDENTIALS
    !snapshot.connected && !snapshot.hasValidData -> WidgetMode.DISCONNECTED
    snapshot.syncStatus == SyncStatus.LOADING && !snapshot.hasValidData -> WidgetMode.LOADING
    else -> WidgetMode.DATA
}

@Composable
private fun WidgetRoot(state: NutritionUiState) {
    val scale = scaleFor(LocalSize.current)
    val context = LocalContext.current
    val mode = modeOf(state.snapshot)

    // Tapping anywhere on the widget opens THIS app, in every state. It used to
    // jump to the FatSecret app (with a store/website fallback) when there was
    // data; that was reverted at the user's request.
    val bodyAction = actionStartActivity<MainActivity>()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.surface)
            // Follow the host's own widget radius instead of hard-coding one, so
            // the card lines up with the launcher's masking on every device.
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .padding(horizontal = scale.padH, vertical = scale.padV)
            .clickable(bodyAction),
    ) {
        when (mode) {
            WidgetMode.NO_CREDENTIALS -> MessageState(
                scale,
                title = "Configure as credenciais",
                subtitle = "Abra o app para conectar",
            )
            WidgetMode.DISCONNECTED -> MessageState(
                scale,
                title = "Conta desconectada",
                subtitle = "Toque para conectar ao FatSecret",
            )
            WidgetMode.LOADING -> MessageState(
                scale,
                title = "Carregando",
                subtitle = "Sincronizando com o FatSecret",
            )
            WidgetMode.DATA -> DataContent(state, scale)
        }
    }
}

/**
 * The data layout at every size.
 *
 * The header row is weighted so it absorbs whatever height is left over after
 * the chart has taken its fixed slice — that is what stops a two-row widget from
 * rendering a one-row card with a band of emptiness underneath.
 */
@Composable
private fun DataContent(state: NutritionUiState, scale: WidgetScale) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            CaloriesBlock(state, scale, modifier = GlanceModifier.defaultWeight())
            if (scale.isWide) {
                Spacer(GlanceModifier.width(scale.padH))
                MacroColumn(state, scale, modifier = GlanceModifier.defaultWeight())
            }
        }
        if (scale.showsChart) {
            Spacer(GlanceModifier.height(scale.gap))
            WeeklyChart(
                calories = state.snapshot.weeklyCalories,
                goal = state.goals.caloriesKcal,
                endMillis = state.snapshot.lastSyncMillis,
                height = scale.chartHeight,
                scale = scale,
            )
        }
    }
}

@Composable
private fun MessageState(scale: WidgetScale, title: String, subtitle: String) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontWeight = FontWeight.Medium,
                fontSize = (scale.captionSp + 4).sp,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(scale.tightGap))
        Text(
            text = subtitle,
            maxLines = 2,
            style = TextStyle(
                color = WidgetColors.muted,
                fontSize = scale.captionSp.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/**
 * The hero: today's calories, the goal, one progress bar and what is left.
 *
 * The weighted spacer between the headline and the bar is what makes the block
 * breathe at taller sizes: the number sits at the top, the progress reading sits
 * at the bottom, and the extra height becomes deliberate whitespace between two
 * groups instead of a gap tacked on the end.
 */
@Composable
private fun CaloriesBlock(state: NutritionUiState, scale: WidgetScale, modifier: GlanceModifier) {
    val snapshot = state.snapshot
    val daily = snapshot.daily
    val goals = state.goals
    val remaining = NutritionCalculator.remaining(daily.calories, goals.caloriesKcal)
    val stale = snapshot.syncStatus == SyncStatus.ERROR

    Column(modifier = modifier) {
        Text(
            text = NutritionFormat.int(daily.calories),
            maxLines = 1,
            style = TextStyle(
                color = WidgetColors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = scale.heroSp.sp,
            ),
        )
        // No separate action any more: the whole body already opens this app, so
        // this line is plain text rather than a second tap target doing the same
        // thing.
        Text(
            text = if (stale) {
                "Desatualizado · ${WidgetTime.relative(snapshot.lastSyncMillis)}"
            } else {
                "de ${NutritionFormat.int(goals.caloriesKcal)} kcal"
            },
            maxLines = 1,
            style = TextStyle(
                color = if (stale) WidgetColors.warn else WidgetColors.muted,
                fontSize = scale.captionSp.sp,
            ),
        )
        Spacer(GlanceModifier.defaultWeight())
        ProgressBar(
            consumed = daily.calories,
            goal = goals.caloriesKcal,
            thickness = scale.barThickness,
            over = remaining < 0,
        )
        Spacer(GlanceModifier.height(scale.tightGap + 2.dp))
        Text(
            text = footnote(daily.entryCount, remaining),
            maxLines = 1,
            style = TextStyle(
                color = when {
                    daily.entryCount == 0 -> WidgetColors.muted
                    remaining < 0 -> WidgetColors.over
                    else -> WidgetColors.accent
                },
                fontWeight = FontWeight.Medium,
                fontSize = scale.captionSp.sp,
            ),
        )
    }
}

/**
 * The three macros. The outer weighted spacers centre them against the calorie
 * block at every height, and collapse to nothing at the tightest one.
 */
@Composable
private fun MacroColumn(state: NutritionUiState, scale: WidgetScale, modifier: GlanceModifier) {
    val daily = state.snapshot.daily
    val goals = state.goals

    Column(modifier = modifier) {
        Spacer(GlanceModifier.defaultWeight())
        MacroBar("Proteína", daily.protein, goals.proteinG, scale)
        Spacer(GlanceModifier.height(scale.tightGap))
        MacroBar("Carbo", daily.carbs, goals.carbsG, scale)
        Spacer(GlanceModifier.height(scale.tightGap))
        MacroBar("Gordura", daily.fat, goals.fatG, scale)
        Spacer(GlanceModifier.defaultWeight())
    }
}

/** One macro: label and value on a line, a hairline bar underneath. */
@Composable
private fun MacroBar(label: String, consumed: Double, goal: Int, scale: WidgetScale) {
    val over = goal > 0 && consumed > goal
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                maxLines = 1,
                style = TextStyle(color = WidgetColors.muted, fontSize = scale.labelSp.sp),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = "${NutritionFormat.int(consumed)}/${NutritionFormat.int(goal)} g",
                maxLines = 1,
                style = TextStyle(
                    color = if (over) WidgetColors.over else WidgetColors.onSurface,
                    fontWeight = FontWeight.Medium,
                    fontSize = scale.labelSp.sp,
                ),
            )
        }
        Spacer(GlanceModifier.height(scale.tightGap - 1.dp))
        ProgressBar(
            consumed = consumed,
            goal = goal,
            thickness = scale.barThickness - 1.dp,
            over = over,
        )
    }
}

@Composable
private fun ProgressBar(consumed: Double, goal: Int, thickness: Dp, over: Boolean) {
    // Bar is clamped to 100%; the real overshoot is carried by the text colour.
    LinearProgressIndicator(
        progress = NutritionCalculator.progressFraction(consumed, goal),
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(thickness)
            .cornerRadius(thickness / 2),
        color = if (over) WidgetColors.over else WidgetColors.accent,
        backgroundColor = WidgetColors.track,
    )
}

/** Kept short on purpose: it has to fit half of the wide widget's width. */
private fun footnote(entryCount: Int, remaining: Double): String = when {
    entryCount == 0 -> "Sem registros hoje"
    remaining >= 0 -> "Restam ${NutritionFormat.int(remaining)} kcal"
    else -> "Excedeu ${NutritionFormat.int(-remaining)} kcal"
}

/**
 * A weekly calorie chart: one bar per day (oldest left, today right) with the
 * calorie goal drawn as a hairline across the whole plot. Bars over the goal go
 * red, matching the rest of the widget's over-budget language. Everything is
 * sized in dp from [height] because Glance has no fractional layout weight, so
 * the bars and the goal line must be measured against one known plot height.
 */
@Composable
private fun WeeklyChart(
    calories: List<Double>,
    goal: Int,
    endMillis: Long,
    height: Dp,
    scale: WidgetScale,
) {
    val labelHeight = (scale.microSp + 4).dp
    val labelGap = scale.tightGap
    val plotHeight = (height - labelHeight - labelGap).coerceAtLeast(20.dp)

    // Nothing to draw yet: an older cache, or the best-effort weekly fetch has
    // never succeeded. Say so quietly instead of showing an empty grid.
    if (calories.isEmpty() || calories.all { it <= 0.0 }) {
        Box(
            modifier = GlanceModifier.fillMaxWidth().height(height),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Sem histórico semanal",
                maxLines = 1,
                style = TextStyle(color = WidgetColors.muted, fontSize = scale.captionSp.sp),
            )
        }
        return
    }

    val maxValue = maxOf(calories.maxOrNull() ?: 0.0, goal.toDouble(), 1.0)
    val endDate = if (endMillis > 0) {
        Instant.ofEpochMilli(endMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    } else {
        LocalDate.now()
    }
    val startDate = endDate.minusDays((calories.size - 1).toLong())

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Box(modifier = GlanceModifier.fillMaxWidth().height(plotHeight)) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom,
            ) {
                calories.forEach { cal ->
                    val frac = (cal / maxValue).coerceIn(0.0, 1.0)
                    val barHeight =
                        if (cal > 0) (plotHeight * frac.toFloat()).coerceAtLeast(3.dp) else 0.dp
                    val over = goal > 0 && cal > goal
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .height(barHeight)
                            .padding(horizontal = 3.dp)
                            .background(if (over) WidgetColors.over else WidgetColors.accent)
                            .cornerRadius(3.dp),
                    ) {}
                }
            }
            // Goal line: a top spacer pushes a full-width hairline down to the
            // goal's height on the shared scale.
            if (goal > 0) {
                val goalFrac = (goal / maxValue).coerceIn(0.0, 1.0)
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Spacer(GlanceModifier.height(plotHeight * (1f - goalFrac.toFloat())))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(WidgetColors.muted),
                    ) {}
                }
            }
        }
        Spacer(GlanceModifier.height(labelGap))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            calories.indices.forEach { i ->
                val isToday = i == calories.lastIndex
                Text(
                    text = weekdayLetter(startDate.plusDays(i.toLong()).dayOfWeek),
                    maxLines = 1,
                    style = TextStyle(
                        color = if (isToday) WidgetColors.onSurface else WidgetColors.muted,
                        fontSize = scale.microSp.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

/** Single-letter weekday initials, pt-BR convention (D S T Q Q S S). */
private fun weekdayLetter(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "S"
    DayOfWeek.TUESDAY -> "T"
    DayOfWeek.WEDNESDAY -> "Q"
    DayOfWeek.THURSDAY -> "Q"
    DayOfWeek.FRIDAY -> "S"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "D"
}
