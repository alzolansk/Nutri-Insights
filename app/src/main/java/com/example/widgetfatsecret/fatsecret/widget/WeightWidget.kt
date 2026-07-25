package com.example.widgetfatsecret.fatsecret.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
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
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.action.actionStartActivity
import androidx.glance.background
import androidx.glance.unit.ColorProvider
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
import com.example.widgetfatsecret.fatsecret.data.WeightUiState
import com.example.widgetfatsecret.fatsecret.data.remote.FatSecretConfig
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightFormat
import com.example.widgetfatsecret.fatsecret.domain.WeightStats
import com.example.widgetfatsecret.fatsecret.domain.WeightTrend
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Home-screen widget for weight tracking and its trend.
 *
 * Tone is a deliberate design constraint: the widget reports direction, never a
 * verdict. Gaining weight is not painted with the nutrition widget's red
 * "over budget" colour — a person may be bulking, recovering, or simply not
 * cutting, and a home-screen widget is the last place that should editorialise.
 * Movement TOWARDS the user's own stated goal gets the accent colour; everything
 * else stays neutral, and the wording ("Perdendo" / "Ganhando" / "Mantendo") is
 * purely descriptive. Direction is carried by a small text arrow rather than an
 * icon asset, so it can never fail to load in the launcher's process.
 *
 * Responsive, with information added only as room appears:
 *  - [SMALL]       latest weight and the change since the previous weighing;
 *  - [WIDE]        adds the trend and how long ago the last weighing was;
 *  - [TALL_NARROW] stacks the full stat list under the weight;
 *  - [LARGE]       adds the goal progress bar and the 30-day chart.
 *
 * Every figure comes pre-computed from [com.example.widgetfatsecret.fatsecret
 * .domain.WeightCalculator] via the repository, so this file contains no
 * arithmetic and the three empty states the data can be in — no weighings, no
 * goal, a single weighing — are branches, not special-cased maths.
 */
class WeightWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(WidgetSizes.ALL)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = AppContainer.get(context)
        val state = container.repository.weightState.first()
        provideContent {
            WeightRoot(state)
        }
    }

    companion object {
        suspend fun updateAll(context: Context) = WeightWidget().updateAll(context)
    }
}

// --- Rendering ---------------------------------------------------------------

private enum class WeightMode { NO_CREDENTIALS, DISCONNECTED, NO_DATA, DATA }

private fun modeOf(state: WeightUiState): WeightMode {
    val snap = state.snapshot
    return when {
        !FatSecretConfig.hasCredentials -> WeightMode.NO_CREDENTIALS
        !snap.connected && !snap.hasValidData -> WeightMode.DISCONNECTED
        !state.stats.hasData -> WeightMode.NO_DATA
        else -> WeightMode.DATA
    }
}

@Composable
private fun WeightRoot(state: WeightUiState) {
    val scale = scaleFor(LocalSize.current)
    val context = LocalContext.current
    val mode = modeOf(state)

    // Same convention as the nutrition widget: tapping anywhere opens THIS app,
    // in every state. Jumping to the FatSecret app was reverted at the user's
    // request.
    val bodyAction = actionStartActivity<MainActivity>()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.surface)
            .cornerRadius(android.R.dimen.system_app_widget_background_radius)
            .padding(horizontal = scale.padH, vertical = scale.padV)
            .clickable(bodyAction),
    ) {
        when (mode) {
            WeightMode.NO_CREDENTIALS -> WeightMessage(
                scale,
                title = "Configure as credenciais",
                subtitle = "Abra o app para conectar",
            )
            WeightMode.DISCONNECTED -> WeightMessage(
                scale,
                title = "Conta desconectada",
                subtitle = "Toque para conectar ao FatSecret",
            )
            WeightMode.NO_DATA -> WeightMessage(
                scale,
                title = "Sem pesagens",
                subtitle = "Registre um peso no FatSecret para ver a evolução",
            )
            WeightMode.DATA -> WeightDataContent(state, scale)
        }
    }
}

@Composable
private fun WeightMessage(scale: WidgetScale, title: String, subtitle: String) {
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
 * The data layout at every size.
 *
 * How much appears is decided by the band, not by a scaled-up copy of one
 * design: the shortest band is the hero alone, a wide short widget moves the
 * stat list into the spare width beside it, and the taller bands stack the full
 * list plus the goal bar and finally the chart. The stat list is wrapped in a
 * weighted container so the leftover height is spent spreading those rows apart
 * rather than pooling under the last one.
 */
@Composable
private fun WeightDataContent(state: WeightUiState, scale: WidgetScale) {
    val stats = state.stats
    val pounds = state.snapshot.profile.usesPounds
    // Stricter than [WidgetScale.showsChart]: this widget spends its middle on a
    // three-row stat list that the nutrition widget does not have, so the plot
    // only earns its place in the tallest band. Being generous here is exactly
    // what made the goal row overlap the stats.
    val showChart = scale.heightBand == HeightBand.SPACIOUS &&
        state.snapshot.entries.size >= 2
    // The chart and the stat list compete for the same band of height. When the
    // plot is on screen it already shows the pace and the range that "Média" and
    // "Total" put into words, so the list stands down to its single most useful
    // row instead of every row being squeezed until they collide. Nothing is
    // lost overall: the full three-row list is what the next band down renders.
    val statRows = if (showChart) 1 else scale.statRows

    // Short and narrow: there is only room for the headline reading.
    if (!scale.showsSecondaryList && !scale.isWide) {
        WeightHero(state, scale, modifier = GlanceModifier.fillMaxSize())
        return
    }

    // Short but wide: hero on the left, the stats in the width beside it.
    if (!scale.showsSecondaryList) {
        Row(modifier = GlanceModifier.fillMaxSize()) {
            WeightHero(state, scale, modifier = GlanceModifier.defaultWeight())
            Spacer(GlanceModifier.width(scale.padH))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Spacer(GlanceModifier.defaultWeight())
                StatLine("Tendência", trendText(stats), scale, trendColor(stats))
                Spacer(GlanceModifier.height(scale.tightGap))
                StatLine(
                    "Total",
                    stats.totalDelta?.let { WeightFormat.delta(it, pounds) } ?: "—",
                    scale,
                )
                Spacer(GlanceModifier.defaultWeight())
            }
        }
        return
    }

    Column(modifier = GlanceModifier.fillMaxSize()) {
        WeightHero(state, scale, modifier = GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(scale.gap))
        Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            Spacer(GlanceModifier.defaultWeight())
            StatLine("Tendência", trendText(stats), scale, trendColor(stats))
            if (statRows >= 2) {
                Spacer(GlanceModifier.defaultWeight())
                StatLine(
                    "Média",
                    stats.weeklyAverage?.let { WeightFormat.perWeek(it, pounds) } ?: "—",
                    scale,
                )
            }
            // The last row is the first thing dropped when room is tight: the
            // total is the least time-critical of the three.
            if (statRows >= 3) {
                Spacer(GlanceModifier.defaultWeight())
                StatLine(
                    "Total",
                    stats.totalDelta?.let { WeightFormat.delta(it, pounds) } ?: "primeira pesagem",
                    scale,
                )
            }
            Spacer(GlanceModifier.defaultWeight())
        }
        GoalBlock(state, scale)
        if (showChart) {
            Spacer(GlanceModifier.height(scale.gap))
            WeightChart(entries = state.snapshot.entries, height = scale.chartHeight, scale = scale)
        }
    }
}

/**
 * The hero: current weight, the change against the previous weighing, and when
 * it was taken. With a single weighing there is no delta to show, so it says so
 * plainly instead of rendering a misleading "0,0 kg".
 */
@Composable
private fun WeightHero(state: WeightUiState, scale: WidgetScale, modifier: GlanceModifier) {
    val stats = state.stats
    val pounds = state.snapshot.profile.usesPounds
    val latest = stats.latest ?: return

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = WeightFormat.weightValue(latest.weightKg, pounds),
                maxLines = 1,
                style = TextStyle(
                    color = WidgetColors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = scale.heroSp.sp,
                ),
            )
            Spacer(GlanceModifier.width(scale.tightGap))
            Text(
                text = WeightFormat.unit(pounds),
                maxLines = 1,
                style = TextStyle(color = WidgetColors.muted, fontSize = scale.captionSp.sp),
            )
        }
        Text(
            text = heroSubtitle(stats, pounds),
            maxLines = 1,
            style = TextStyle(color = subtitleColor(stats), fontSize = scale.captionSp.sp),
        )
    }
}

private fun heroSubtitle(stats: WeightStats, pounds: Boolean): String {
    val delta = stats.deltaFromPrevious
        ?: return "Primeira pesagem" + (stats.daysSinceLast?.let { " · ${WeightFormat.sinceLast(it)}" } ?: "")
    val arrow = WeightFormat.trendArrow(
        when {
            delta < 0 -> WeightTrend.LOSING
            delta > 0 -> WeightTrend.GAINING
            else -> WeightTrend.STABLE
        }
    )
    val since = stats.daysSinceLast?.let { " · ${WeightFormat.sinceLast(it)}" } ?: ""
    return "$arrow ${WeightFormat.delta(delta, pounds)}$since"
}

/** One "label   value" row, the same shape the macro rows use. */
@Composable
private fun StatLine(
    label: String,
    value: String,
    scale: WidgetScale,
    valueColor: ColorProvider = WidgetColors.onSurface,
) {
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
            text = value,
            maxLines = 1,
            style = TextStyle(
                color = valueColor,
                fontWeight = FontWeight.Medium,
                fontSize = scale.labelSp.sp,
            ),
        )
    }
}

/**
 * Progress from the first known weighing towards the goal. Without a goal the
 * widget says so rather than inventing a target — the API only exposes a goal
 * when the user has actually set one in FatSecret.
 */
@Composable
private fun GoalBlock(state: WeightUiState, scale: WidgetScale) {
    val stats = state.stats
    val pounds = state.snapshot.profile.usesPounds
    val goal = stats.goalKg
    val progress = stats.goalProgress

    if (goal == null || progress == null) {
        Text(
            text = "Sem meta definida",
            maxLines = 1,
            style = TextStyle(color = WidgetColors.muted, fontSize = scale.captionSp.sp),
        )
        return
    }

    Column(modifier = GlanceModifier.fillMaxWidth()) {
        StatLine(
            "Meta ${WeightFormat.weight(goal, pounds)}",
            goalRemainingText(stats, pounds),
            scale,
            WidgetColors.accent,
        )
        Spacer(GlanceModifier.height(scale.tightGap))
        LinearProgressIndicator(
            progress = progress,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(scale.barThickness - 1.dp)
                .cornerRadius((scale.barThickness - 1.dp) / 2),
            color = WidgetColors.accent,
            backgroundColor = WidgetColors.track,
        )
    }
}

private fun goalRemainingText(stats: WeightStats, pounds: Boolean): String {
    val remaining = stats.remainingToGoal ?: return ""
    val abs = kotlin.math.abs(remaining)
    // Within a rounding step of the goal, "faltam 0,0 kg" reads oddly.
    if (abs < 0.05) return "meta alcançada"
    return "faltam ${WeightFormat.weight(abs, pounds)}"
}

/** Trend as an arrow plus its neutral label, or a dash when not yet knowable. */
private fun trendText(stats: WeightStats): String =
    if (stats.trend == WeightTrend.UNKNOWN) {
        "—"
    } else {
        "${WeightFormat.trendArrow(stats.trend)} ${WeightFormat.trendLabel(stats.trend)}"
    }

/**
 * Accent only when the last change moved towards the user's own goal; neutral
 * otherwise. Never the "over budget" red — see the class comment.
 */
private fun trendColor(stats: WeightStats): ColorProvider =
    if (stats.movingTowardGoal == true) WidgetColors.accent else WidgetColors.onSurface

private fun subtitleColor(stats: WeightStats): ColorProvider =
    if (stats.movingTowardGoal == true) WidgetColors.accent else WidgetColors.muted

/**
 * A compact trace of the recent weighings.
 *
 * The scale is deliberately min..max rather than 0..max — over a month a
 * person's weight moves by a couple of kilos on a base of dozens, so a
 * zero-based axis would render a flat line. The two edge labels state the range
 * explicitly, which is what keeps the compressed scale honest rather than
 * misleading.
 */
@Composable
private fun WeightChart(entries: List<WeightEntry>, height: Dp, scale: WidgetScale) {
    if (entries.size < 2) return

    // One column per weighing rather than one per calendar day. A 30-slot grid
    // meant ~90 nested views for a handful of dots, which is a lot to ask of
    // RemoteViews; weighings are also sparse, so most slots were empty anyway.
    // The trade-off is that spacing is per-measurement, not strictly
    // time-proportional — acceptable for a thumbnail whose job is the shape.
    val points = entries.takeLast(MAX_CHART_POINTS)
    val min = points.minOf { it.weightKg }
    val max = points.maxOf { it.weightKg }
    val span = (max - min).takeIf { it > 0.001 } ?: 1.0
    // The dot grows with the type ramp so the trace keeps the same visual weight
    // relative to the text at every size.
    val dotSize = (scale.microSp / 2 + 1).dp

    Row(modifier = GlanceModifier.fillMaxWidth().height(height)) {
        // The edge labels pin the compressed scale to real numbers, which is
        // what keeps a min..max axis honest instead of merely dramatic.
        Column(modifier = GlanceModifier.width((scale.microSp * 3).dp).height(height)) {
            Text(
                text = oneDecimal(max),
                maxLines = 1,
                style = TextStyle(color = WidgetColors.muted, fontSize = scale.microSp.sp),
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = oneDecimal(min),
                maxLines = 1,
                style = TextStyle(color = WidgetColors.muted, fontSize = scale.microSp.sp),
            )
        }
        points.forEach { entry ->
            val frac = ((entry.weightKg - min) / span).coerceIn(0.0, 1.0)
            // A larger top offset sits lower on screen, so invert the fraction.
            val top = (height - dotSize) * (1f - frac.toFloat())
            Column(modifier = GlanceModifier.defaultWeight().height(height)) {
                Spacer(GlanceModifier.height(top))
                Box(
                    modifier = GlanceModifier
                        .width(dotSize)
                        .height(dotSize)
                        .background(WidgetColors.accent)
                        .cornerRadius(2.dp),
                ) {}
            }
        }
    }
}

/** Keeps the RemoteViews tree small; see [WeightChart]. */
private const val MAX_CHART_POINTS = 14

private fun oneDecimal(kg: Double): String = String.format(Locale.forLanguageTag("pt-BR"), "%.1f", kg)
