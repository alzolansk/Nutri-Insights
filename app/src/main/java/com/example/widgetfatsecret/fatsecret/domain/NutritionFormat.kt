package com.example.widgetfatsecret.fatsecret.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * pt-BR text formatting for numbers and insights. Kept separate from
 * [NutritionCalculator] so the calculation logic stays free of locale concerns
 * and remains trivially testable.
 */
object NutritionFormat {

    private val ptBr: Locale = Locale.forLanguageTag("pt-BR")
    private val grouping: NumberFormat = NumberFormat.getIntegerInstance(ptBr)

    /** Rounds and formats with pt-BR grouping, e.g. 1420.0 -> "1.420". */
    fun int(value: Double): String = grouping.format(value.roundToInt())

    fun int(value: Int): String = grouping.format(value)

    /** "1.420 / 2.000" */
    fun ratio(consumed: Double, goal: Int): String = "${int(consumed)} / ${int(goal)}"

    /** Whole percent with a % sign, e.g. "71%". */
    fun percentText(consumed: Double, goal: Int): String =
        "${NutritionCalculator.percent(consumed, goal).roundToInt()}%"

    /** The single short insight sentence, pt-BR. Arithmetic only. */
    fun insightText(insight: Insight): String = when (insight.type) {
        InsightType.NO_ENTRIES -> "Nenhum alimento registrado hoje"
        InsightType.FAT_OVER -> "Gorduras acima da meta em ${int(insight.value)} g"
        InsightType.PROTEIN_GOAL_REACHED -> "Meta de proteína atingida"
        InsightType.CARBS_NEAR_GOAL -> "Carboidratos próximos da meta"
        InsightType.PROTEIN_REMAINING -> "Faltam ${int(insight.value)} g de proteína"
        InsightType.CALORIES_REMAINING -> "Restam ${int(insight.value)} kcal hoje"
        InsightType.PERCENT_OF_DAILY -> "Você consumiu ${insight.value.roundToInt()}% da meta diária"
    }
}
