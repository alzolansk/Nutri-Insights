package com.example.widgetfatsecret.ui.design

import android.content.res.Configuration
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.widgetfatsecret.ui.theme.WidgetFatSecretTheme
import com.example.widgetfatsecret.ui.theme.nutriColors

/**
 * Placeholder de carregamento com pulsação sutil. Deve ser dimensionado com a
 * **altura do conteúdo final** para não causar deslocamento de layout quando os
 * dados chegam (planning.md §9, Etapa 10).
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 20.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 8.dp,
) {
    val colors = MaterialTheme.nutriColors
    val transition = rememberInfiniteTransition(label = "skeleton")
    val fraction by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton-alpha",
    )
    val color = lerp(colors.surface2, colors.line2, fraction)
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(color),
    )
}

/**
 * Screen-level loading placeholder. Each height mirrors one final card so the
 * surrounding layout keeps the same vertical footprint when content arrives.
 */
@Composable
fun ScreenSkeleton(
    cardHeights: List<androidx.compose.ui.unit.Dp>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(NutriSpacing.lg),
    ) {
        cardHeights.forEach { cardHeight ->
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth(),
                height = cardHeight,
                cornerRadius = NutriRadii.Card,
            )
        }
    }
}

@Preview(name = "SkeletonBlock claro", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(
    name = "SkeletonBlock escuro",
    showBackground = true,
    backgroundColor = 0xFF131B2B,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SkeletonBlockPreview() {
    WidgetFatSecretTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.5f), height = 28.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(), height = 16.dp)
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.8f), height = 16.dp)
        }
    }
}
