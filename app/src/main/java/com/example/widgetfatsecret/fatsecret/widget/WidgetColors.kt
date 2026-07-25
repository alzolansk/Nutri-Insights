package com.example.widgetfatsecret.fatsecret.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider

/**
 * The widget palette, one explicit light/dark pair per semantic role.
 *
 * Why day/night [ColorProvider]s instead of fixed colours: a widget is inflated
 * by the LAUNCHER process, not by us, so it follows the launcher's night mode.
 * Glance turns a day/night provider into a `DayNightColorProvider` and emits
 * `RemoteViewsCompat.setViewBackgroundColor(rv, id, day, night)` /
 * `setTextViewTextColor(rv, id, day, night)` / `setProgressBarProgressTintList(
 * rv, id, day, night)`, all of which are backed by `RemoteViews.setColorInt`
 * (API 31+; this app's minSdk is 34). The HOST therefore picks the right colour
 * at inflation time and the widget flips with the system theme immediately,
 * without waiting for a re-render from our side. A fixed `ColorProvider(Color)`
 * would be baked in at render time and would stay stale until the next sync.
 *
 * Practical widget limitations this palette works around:
 *  - Glance/RemoteViews cannot read `MaterialTheme` from the app, so the values
 *    are duplicated here on purpose rather than imported from ui.theme.
 *  - There is no `?attr/colorX` resolution inside a widget, so every role must
 *    be a concrete colour pair.
 *  - Dynamic (wallpaper) colours are deliberately NOT used: they would drop the
 *    brand green and can land on low-contrast combinations we cannot verify.
 *
 * The greens and the red are deliberately desaturated relative to the Material
 * defaults (`#2E7D32`, `#C62828`): on a home screen the widget sits next to the
 * wallpaper all day, and saturated blocks read as "loud". Muted tones keep the
 * calm look while still carrying meaning. Contrast is not sacrificed for it —
 * every text pair below was measured against the surface it is drawn on:
 * onSurface 16.4:1 / 14.7:1, muted 4.81:1 / 6.94:1, accent 5.09:1 / 9.30:1,
 * over 6.53:1 / 9.75:1, warn 5.93:1 / 8.87:1 (day / night), all above the
 * WCAG AA 4.5:1 floor. [track] is the only non-text role, so it is free to be
 * nearly invisible — that is what makes the progress bars read as hairlines.
 */
internal object WidgetColors {

    /** Widget card background. */
    val surface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF171A18))

    /** Primary text: the calorie figure and the macro values. */
    val onSurface = ColorProvider(day = Color(0xFF1A1D1B), night = Color(0xFFE9ECEA))

    /** Secondary/label text: goal line, macro labels, footnotes. */
    val muted = ColorProvider(day = Color(0xFF6B7472), night = Color(0xFF9AA5A0))

    /** Brand accent: progress fill and the "remaining" figure. */
    val accent = ColorProvider(day = Color(0xFF3C7A5A), night = Color(0xFF8FC9A6))

    /** Unfilled part of every progress bar. Intentionally very low contrast. */
    val track = ColorProvider(day = Color(0xFFE6EBE7), night = Color(0xFF2A2F2C))

    /** Goal exceeded. */
    val over = ColorProvider(day = Color(0xFFB3261E), night = Color(0xFFF0B4B0))

    /** Stale data after a failed sync. */
    val warn = ColorProvider(day = Color(0xFF8A5A00), night = Color(0xFFE5C089))
}
