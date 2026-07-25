package com.example.widgetfatsecret.fatsecret.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Broadcast receiver that binds [NutritionWidget] to the home screen. */
class NutritionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NutritionWidget()
}
