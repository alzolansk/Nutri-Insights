package com.example.widgetfatsecret.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Tipografia do "Nutri Insights" (planning.md §9, Etapa 2).
 *
 * O deck pede **Manrope** (400–800) para a UI e **IBM Plex Mono** (400/500) para
 * metadados e números (sempre `tabular-nums`). Como o projeto ainda não empacota
 * essas fontes em `res/font/`, usamos os fallbacks do sistema com pesos
 * equivalentes — exatamente o caminho previsto em planning.md §3 ("Sem isso,
 * usar a fonte do sistema com pesos equivalentes"). Quando/se os `.ttf` forem
 * adicionados, basta trocar [UiFontFamily] e [MonoFontFamily] por
 * `FontFamily(Font(R.font.manrope_*))` — nenhum call-site muda.
 */
val UiFontFamily: FontFamily = FontFamily.SansSerif

/** Família monoespaçada para números/metadados. Combina com `tabular-nums`. */
val MonoFontFamily: FontFamily = FontFamily.Monospace

private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = TightLineHeight,
    ),
    displayMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp,
        lineHeightStyle = TightLineHeight,
    ),
    headlineMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = UiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * Estilos monoespaçados para números e metadados (o deck exige `tabular-nums`
 * em todo valor). Não fazem parte do [Typography] do Material porque não mapeiam
 * 1:1 nos papéis do Material3 — são acessados diretamente.
 */
object MonoText {
    /** Números grandes (kcal restantes, peso atual). */
    val metricLarge = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
        lineHeightStyle = TightLineHeight,
    )

    /** Números médios (valor de macro, delta de peso). */
    val metricMedium = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        lineHeightStyle = TightLineHeight,
    )

    /** Metadados (chips "há 12 min", "18 de 30 dias"). */
    val meta = TextStyle(
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp,
    )
}
