package com.example.widgetfatsecret.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Paleta estendida do "Nutri Insights" que o [androidx.compose.material3.ColorScheme]
 * não comporta (5 acentos semânticos + 3 níveis de texto + 2 de linha). Acesse
 * via [nutriColors]. Imutável para não invalidar recomposições à toa.
 */
@Immutable
data class NutriColors(
    val page: Color,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val mint: Color,
    val cyan: Color,
    val amber: Color,
    val coral: Color,
    val violet: Color,
    val line: Color,
    val line2: Color,
    val isDark: Boolean,
)

private val DarkNutriColors = NutriColors(
    page = DarkPage,
    bg = DarkBg,
    surface = DarkSurface,
    surface2 = DarkSurface2,
    text = DarkText,
    text2 = DarkText2,
    text3 = DarkText3,
    mint = DarkMint,
    cyan = DarkCyan,
    amber = DarkAmber,
    coral = DarkCoral,
    violet = DarkViolet,
    line = DarkLine,
    line2 = DarkLine2,
    isDark = true,
)

private val LightNutriColors = NutriColors(
    page = LightPage,
    bg = LightBg,
    surface = LightSurface,
    surface2 = LightSurface2,
    text = LightText,
    text2 = LightText2,
    text3 = LightText3,
    mint = LightMint,
    cyan = LightCyan,
    amber = LightAmber,
    coral = LightCoral,
    violet = LightViolet,
    line = LightLine,
    line2 = LightLine2,
    isDark = false,
)

private val LocalNutriColors = staticCompositionLocalOf { DarkNutriColors }

/**
 * Acesso curto à paleta estendida dentro de composables:
 * `MaterialTheme.nutriColors.mint`.
 */
val MaterialTheme.nutriColors: NutriColors
    @Composable
    get() = LocalNutriColors.current

private val DarkColorScheme = darkColorScheme(
    primary = DarkMint,
    onPrimary = DarkBg,
    secondary = DarkCyan,
    onSecondary = DarkBg,
    tertiary = DarkViolet,
    onTertiary = DarkBg,
    error = DarkCoral,
    onError = DarkBg,
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface2,
    onSurfaceVariant = DarkText2,
    outline = DarkLine2,
    outlineVariant = DarkLine,
)

private val LightColorScheme = lightColorScheme(
    primary = LightMint,
    onPrimary = Color.White,
    secondary = LightCyan,
    onSecondary = Color.White,
    tertiary = LightViolet,
    onTertiary = Color.White,
    error = LightCoral,
    onError = Color.White,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurface2,
    onSurfaceVariant = LightText2,
    outline = LightLine2,
    outlineVariant = LightLine,
)

/**
 * Tema do app. **Dynamic color desligado de propósito**: o deck define uma paleta
 * própria e o Material You (wallpaper) a atropelaria. Não afeta os widgets — eles
 * usam `WidgetColors.kt`, independente por design (risco R7).
 *
 * O nome `WidgetFatSecretTheme` é preservado porque `MainActivity` já o chama;
 * renomear agora não traria benefício e mexeria em código fora do escopo.
 */
@Composable
fun WidgetFatSecretTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val nutriColors = if (darkTheme) DarkNutriColors else LightNutriColors

    CompositionLocalProvider(LocalNutriColors provides nutriColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}
