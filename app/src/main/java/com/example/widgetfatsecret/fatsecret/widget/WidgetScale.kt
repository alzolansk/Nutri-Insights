package com.example.widgetfatsecret.fatsecret.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * How much room the widget actually has, in bands rather than pixels.
 *
 * Why bands and not a scale factor: multiplying one layout by a constant keeps
 * the same composition at every size, so a two-row widget ends up as a one-row
 * widget with padding around it. Discrete bands let each size render the layout
 * that suits it — more information where there is room, larger type where the
 * eye is further from a bigger phone.
 */
internal enum class HeightBand { COMPACT, REGULAR, COMFORTABLE, SPACIOUS }

internal enum class WidthBand { NARROW, WIDE, EXTRA_WIDE }

/**
 * Every dimension a widget layout needs, resolved from the space it was given.
 *
 * Read this together with [WidgetSizes]: the buckets declared there are the only
 * sizes Glance ever reports through `LocalSize`, so the bands below are chosen
 * to line up with them. The two must be edited as a pair.
 *
 * There are no bitmap icons anywhere in these widgets — direction is carried by
 * text glyphs (↓ ↑ →) and progress by hairline bars, both of which scale with
 * [glyphSp] and [barThickness] instead of needing density-specific drawables.
 */
internal data class WidgetScale(
    val heightBand: HeightBand,
    val widthBand: WidthBand,
    /** The dominant number: calories, or the current weight. */
    val heroSp: Int,
    /** Row labels and values in the secondary lists. */
    val labelSp: Int,
    /** Sub-headline under the hero, and footnotes. */
    val captionSp: Int,
    /** Axis labels and other incidental text. */
    val microSp: Int,
    /** Arrows and other text-drawn indicators. */
    val glyphSp: Int,
    val padH: Dp,
    val padV: Dp,
    /** Separation between major blocks. */
    val gap: Dp,
    /** Separation inside a block. */
    val tightGap: Dp,
    val barThickness: Dp,
    /** 0.dp when this size should not draw a chart at all. */
    val chartHeight: Dp,
    /** How many secondary rows the band can hold without crowding. */
    val statRows: Int,
) {
    val isCompact: Boolean get() = heightBand == HeightBand.COMPACT
    val isWide: Boolean get() = widthBand != WidthBand.NARROW
    /** Only the taller bands have the vertical room for a plot. */
    val showsChart: Boolean get() = chartHeight > 0.dp
    /** The taller bands can afford a third line of secondary detail. */
    val showsSecondaryList: Boolean get() = heightBand != HeightBand.COMPACT
}

/**
 * The bucket ladder handed to `SizeMode.Responsive`, shared by both widgets.
 *
 * These are deliberately close together. Glance reports the largest declared
 * size that still fits, and the layout is then measured by the launcher at the
 * widget's REAL size — so a sparse ladder means a two-row widget is described to
 * us as a one-row one, and a one-row layout gets stretched across two rows with
 * the slack showing as dead space. That was exactly the reported symptom. Widths
 * and heights are paired only where the combination is a shape launchers
 * actually produce, to keep the number of baked RemoteViews reasonable.
 */
internal object WidgetSizes {

    // One launcher row. Pixel's row is ~100dp, Samsung's grid is denser; 80dp is
    // the smallest that still holds hero + goal + bar + footnote.
    private val ROW_1 = 80.dp
    private val ROW_2 = 120.dp
    private val ROW_2_TALL = 180.dp
    private val ROW_3 = 250.dp

    private val NARROW = 150.dp
    private val WIDE = 260.dp
    private val EXTRA_WIDE = 340.dp

    val ALL: Set<DpSize> = setOf(
        DpSize(NARROW, ROW_1),
        DpSize(WIDE, ROW_1),
        DpSize(NARROW, ROW_2),
        DpSize(WIDE, ROW_2),
        DpSize(WIDE, ROW_2_TALL),
        DpSize(EXTRA_WIDE, ROW_2_TALL),
        DpSize(WIDE, ROW_3),
        DpSize(EXTRA_WIDE, ROW_3),
    )
}

/**
 * Resolves the scale for the space Glance reported.
 *
 * The bands are driven by the widget's own measurements, not by screen density
 * or a device list: a launcher that gives a widget more dp — which is what a
 * large phone like an S23 Ultra does, since its grid cells are physically and
 * logically bigger — lands in a higher band and gets larger type, without this
 * file knowing anything about the device.
 */
internal fun scaleFor(size: DpSize): WidgetScale {
    val h = when {
        size.height < 100.dp -> HeightBand.COMPACT
        size.height < 170.dp -> HeightBand.REGULAR
        size.height < 240.dp -> HeightBand.COMFORTABLE
        else -> HeightBand.SPACIOUS
    }
    val w = when {
        size.width < 220.dp -> WidthBand.NARROW
        size.width < 320.dp -> WidthBand.WIDE
        else -> WidthBand.EXTRA_WIDE
    }

    // One extra step of headline size for the widest band: the hero is the only
    // element whose job is to be readable at arm's length, so it benefits most
    // from the room, while the supporting text grows more gently to preserve the
    // hierarchy instead of flattening it.
    val heroBump = when (w) {
        WidthBand.NARROW -> 0
        WidthBand.WIDE -> 4
        WidthBand.EXTRA_WIDE -> 6
    }

    // Each band's content is budgeted against the BUCKET height above, never
    // against the widget's real height. The bucket is by definition the largest
    // declared size that still fits, so content that fits the bucket also fits
    // reality; sizing against anything larger is how rows end up overlapping on
    // the devices whose real size sits just under the next bucket up.
    return when (h) {
        // ~80dp: hero + its caption, and nothing else.
        HeightBand.COMPACT -> WidgetScale(
            heightBand = h, widthBand = w,
            heroSp = 24 + heroBump, labelSp = 11, captionSp = 10, microSp = 9,
            glyphSp = 11,
            padH = 12.dp, padV = 6.dp, gap = 6.dp, tightGap = 3.dp,
            barThickness = 5.dp, chartHeight = 0.dp, statRows = 2,
        )
        // ~120dp: hero + caption + two secondary rows.
        HeightBand.REGULAR -> WidgetScale(
            heightBand = h, widthBand = w,
            heroSp = 32 + heroBump, labelSp = 12, captionSp = 12, microSp = 10,
            glyphSp = 12,
            padH = 14.dp, padV = 9.dp, gap = 8.dp, tightGap = 4.dp,
            barThickness = 6.dp, chartHeight = 0.dp, statRows = 2,
        )
        // ~180dp: adds the third row and the goal bar. A chart fits here only in
        // the nutrition widget, whose left column is shorter than the weight
        // widget's stat list.
        HeightBand.COMFORTABLE -> WidgetScale(
            heightBand = h, widthBand = w,
            heroSp = 34 + heroBump, labelSp = 13, captionSp = 13, microSp = 11,
            glyphSp = 13,
            padH = 16.dp, padV = 11.dp, gap = 9.dp, tightGap = 5.dp,
            // A plot has to clear its own day labels before it reads as a chart
            // rather than a coloured strip: 52dp leaves ~32dp of bars once the
            // labels and their gap are taken out.
            barThickness = 7.dp, chartHeight = 52.dp, statRows = 3,
        )
        // ~250dp: everything, with room left for the spacers to breathe.
        HeightBand.SPACIOUS -> WidgetScale(
            heightBand = h, widthBand = w,
            heroSp = 42 + heroBump, labelSp = 15, captionSp = 14, microSp = 12,
            glyphSp = 15,
            padH = 18.dp, padV = 12.dp, gap = 9.dp, tightGap = 6.dp,
            barThickness = 8.dp, chartHeight = 56.dp, statRows = 3,
        )
    }
}
