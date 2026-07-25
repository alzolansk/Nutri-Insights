package com.example.widgetfatsecret.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de cor do design system "Nutri Insights", extraídos do protótipo
 * (`Nutri Insights.dc.html`) — ver planning.md §9, Etapa 2.
 *
 * Estes tokens NÃO têm nada a ver com `widget/WidgetColors.kt`: os widgets são
 * inflados pelo processo do launcher e mantêm paleta própria por decisão
 * documentada (risco R7). Nunca unificar as duas paletas.
 *
 * A camada Material3 (background/surface/primary/…) é derivada destes tokens em
 * [WidgetFatSecretTheme]; a paleta estendida (mint/cyan/amber/coral/violet e os
 * três níveis de texto/linha) vive em [NutriColors], acessível via
 * [LocalNutriColors].
 */

// --- Escuro (tema base do deck) ---------------------------------------------
val DarkPage = Color(0xFF070B13)
val DarkBg = Color(0xFF0A0F1A)
val DarkSurface = Color(0xFF131B2B)
val DarkSurface2 = Color(0xFF1B2539)
val DarkText = Color(0xFFE9EFF8)
val DarkText2 = Color(0xFF93A3BD)
val DarkText3 = Color(0xFF64748F)
val DarkMint = Color(0xFF84E0A8)
val DarkCyan = Color(0xFF5FC8E8)
val DarkAmber = Color(0xFFE5B15C)
val DarkCoral = Color(0xFFF0806F)
val DarkViolet = Color(0xFF9E9BF0)
// Linhas/divisórias: não especificadas em hex no deck; derivadas da superfície
// para um contorno sutil (line) e um mais visível (line2, usado em botões).
val DarkLine = Color(0xFF212C42)
val DarkLine2 = Color(0xFF2C3A54)

// --- Claro -------------------------------------------------------------------
val LightPage = Color(0xFFE8EDF4)
val LightBg = Color(0xFFF4F7FB)
val LightSurface = Color(0xFFFFFFFF)
val LightSurface2 = Color(0xFFEBF0F7)
val LightText = Color(0xFF0D1626)
val LightText2 = Color(0xFF475569)
val LightText3 = Color(0xFF8494A8)
val LightMint = Color(0xFF0F8F62)
val LightCyan = Color(0xFF1B7EA6)
val LightAmber = Color(0xFFA6740F)
val LightCoral = Color(0xFFCE4F44)
val LightViolet = Color(0xFF5E5AC0)
val LightLine = Color(0xFFD8E0EC)
val LightLine2 = Color(0xFFC5D0E0)
