package com.example.widgetfatsecret.ui.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Tokens de forma e espaçamento do design system (planning.md §9, Etapa 2).
 * Raios confirmados no protótipo: cartão 22px, botão 13px, botão-ícone 12px.
 */
object NutriRadii {
    val Card = 22.dp
    val Button = 13.dp
    val IconButton = 12.dp
    val Chip = 999.dp // pill
}

object NutriShapes {
    val Card = RoundedCornerShape(NutriRadii.Card)
    val Button = RoundedCornerShape(NutriRadii.Button)
    val IconButton = RoundedCornerShape(NutriRadii.IconButton)
    val Chip = RoundedCornerShape(NutriRadii.Chip)
}

/** Espaçamentos base (grid de 4dp). */
object NutriSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
}
