package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.data.FatSecretJson
import com.example.widgetfatsecret.fatsecret.domain.WeightCalculator
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightProfile
import com.example.widgetfatsecret.fatsecret.domain.WeightTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightCalculatorTest {

    private val noProfile = WeightProfile.EMPTY

    // --- empty / degenerate states -------------------------------------------

    @Test
    fun noEntriesAndNoProfileYieldsEmptyStats() {
        val s = WeightCalculator.stats(emptyList(), noProfile, today = 100)
        assertEquals(false, s.hasData)
        assertNull(s.latest)
        assertNull(s.weeklyAverage)
        assertEquals(WeightTrend.UNKNOWN, s.trend)
        assertNull(s.goalProgress)
    }

    @Test
    fun singleWeighingHasNoDeltasOrTrend() {
        val s = WeightCalculator.stats(
            listOf(WeightEntry(100, 80.0)),
            noProfile,
            today = 102,
        )
        assertTrue(s.hasData)
        assertEquals(false, s.hasHistory)
        assertEquals(80.0, s.latest!!.weightKg, 0.001)
        assertNull(s.deltaFromPrevious)
        assertNull(s.totalDelta)
        assertNull(s.weeklyAverage)
        assertEquals(WeightTrend.UNKNOWN, s.trend)
        assertEquals(2, s.daysSinceLast)
    }

    @Test
    fun profileLastWeighingIsUsedWhenHistoryIsEmpty() {
        // A user whose only record predates the fetched month must still see a
        // current weight rather than the "no records" state.
        val profile = WeightProfile(lastWeightKg = 77.5, lastWeightDateInt = 90)
        val s = WeightCalculator.stats(emptyList(), profile, today = 100)
        assertTrue(s.hasData)
        assertEquals(77.5, s.latest!!.weightKg, 0.001)
        assertEquals(10, s.daysSinceLast)
    }

    // --- baseline (all-time first weighing) -----------------------------------

    @Test
    fun theBaselineAnchorsTotalAndProgressButNotTheRate() {
        // The real regression: a 30-day window starting at 108.4 reported a total
        // of -4 kg and 16% progress, while the account actually started at 128.5
        // (-24.1 kg, 53%). The weekly rate must stay window-scoped either way.
        val window = listOf(WeightEntry(20649, 108.4), WeightEntry(20663, 104.4))
        val profile = WeightProfile(goalWeightKg = 83.0)
        val baseline = WeightEntry(20000, 128.5)

        val s = WeightCalculator.stats(window, profile, today = 20663, baseline = baseline)

        assertEquals(-24.1, s.totalDelta!!, 0.001)
        assertEquals(128.5, s.first!!.weightKg, 0.001)
        assertEquals(108.4, s.windowFirst!!.weightKg, 0.001)
        assertEquals(0.53f, s.goalProgress!!, 0.01f)
        assertEquals(-2.0, s.weeklyAverage!!, 0.001) // -4 kg over 14 days, not diluted
        assertEquals(21.4, s.remainingToGoal!!, 0.001)
    }

    @Test
    fun aBaselineInsideTheWindowIsIgnored() {
        // A stale/incorrect baseline must never make "total" smaller than what
        // the window itself already proves.
        val window = listOf(WeightEntry(100, 90.0), WeightEntry(114, 88.0))
        val s = WeightCalculator.stats(
            window,
            noProfile,
            today = 114,
            baseline = WeightEntry(107, 89.0),
        )
        assertEquals(-2.0, s.totalDelta!!, 0.001)
        assertEquals(90.0, s.first!!.weightKg, 0.001)
    }

    @Test
    fun aBaselineGivesATotalEvenWithASingleWeighingInTheWindow() {
        val s = WeightCalculator.stats(
            listOf(WeightEntry(20663, 104.4)),
            noProfile,
            today = 20663,
            baseline = WeightEntry(20000, 128.5),
        )
        assertEquals(-24.1, s.totalDelta!!, 0.001)
        assertNull(s.deltaFromPrevious) // still no previous weighing to compare
        assertNull(s.weeklyAverage)     // and still no recent rate
    }

    @Test
    fun aManualStartingWeightWinsOverTheDiscoveredBaseline() {
        // FatSecret's own "Peso Inicial" (128.5) is older than anything the API
        // returns — the diary walk only reaches 123.0 — so the typed-in value
        // has to take precedence for the widget to agree with that app.
        val window = listOf(WeightEntry(20649, 108.4), WeightEntry(20663, 104.4))
        val profile = WeightProfile(goalWeightKg = 83.0)

        val s = WeightCalculator.stats(
            window,
            profile,
            today = 20663,
            baseline = WeightEntry(20154, 123.0),
            startOverrideKg = 128.5,
        )

        assertEquals(-24.1, s.totalDelta!!, 0.001)
        assertEquals(0.53f, s.goalProgress!!, 0.01f)
        assertEquals(-2.0, s.weeklyAverage!!, 0.001) // rate stays window-scoped
    }

    @Test
    fun aNonPositiveStartingWeightIsTreatedAsUnset() {
        val window = listOf(WeightEntry(100, 90.0), WeightEntry(114, 88.0))
        val s = WeightCalculator.stats(window, noProfile, today = 114, startOverrideKg = 0.0)
        assertEquals(-2.0, s.totalDelta!!, 0.001)
    }

    // --- trend ----------------------------------------------------------------

    @Test
    fun losingAndGainingAreDetectedFromTheWeeklyRate() {
        val losing = WeightCalculator.stats(
            listOf(WeightEntry(100, 90.0), WeightEntry(114, 88.0)),
            noProfile,
            today = 114,
        )
        assertEquals(WeightTrend.LOSING, losing.trend)
        assertEquals(-1.0, losing.weeklyAverage!!, 0.001) // -2 kg over 14 days
        assertEquals(-2.0, losing.totalDelta!!, 0.001)

        val gaining = WeightCalculator.stats(
            listOf(WeightEntry(100, 88.0), WeightEntry(114, 90.0)),
            noProfile,
            today = 114,
        )
        assertEquals(WeightTrend.GAINING, gaining.trend)
        assertEquals(1.0, gaining.weeklyAverage!!, 0.001)
    }

    @Test
    fun smallDriftReadsAsStableRatherThanFlipFlopping() {
        // 50 g over a fortnight is scale noise, not a trend.
        val s = WeightCalculator.stats(
            listOf(WeightEntry(100, 80.0), WeightEntry(114, 80.05)),
            noProfile,
            today = 114,
        )
        assertEquals(WeightTrend.STABLE, s.trend)
    }

    @Test
    fun rateUsesElapsedDaysNotTheNumberOfWeighings() {
        // Five weighings crammed into one week must not read as five weeks.
        val entries = listOf(
            WeightEntry(100, 90.0),
            WeightEntry(101, 89.8),
            WeightEntry(103, 89.4),
            WeightEntry(105, 89.2),
            WeightEntry(107, 89.0),
        )
        val s = WeightCalculator.stats(entries, noProfile, today = 107)
        assertEquals(-1.0, s.weeklyAverage!!, 0.001) // -1 kg over exactly 7 days
    }

    // --- goal -----------------------------------------------------------------

    @Test
    fun goalProgressRunsFromTheFirstWeighingTowardsTheGoal() {
        val profile = WeightProfile(goalWeightKg = 80.0)
        val s = WeightCalculator.stats(
            listOf(WeightEntry(100, 90.0), WeightEntry(110, 85.0)),
            profile,
            today = 110,
        )
        assertEquals(0.5f, s.goalProgress!!, 0.001f) // half of 90 -> 80
        assertEquals(5.0, s.remainingToGoal!!, 0.001)
        assertEquals(true, s.movingTowardGoal)
    }

    @Test
    fun goalProgressWorksWhenTheGoalIsToGainWeight() {
        val profile = WeightProfile(goalWeightKg = 90.0)
        val s = WeightCalculator.stats(
            listOf(WeightEntry(100, 80.0), WeightEntry(110, 85.0)),
            profile,
            today = 110,
        )
        assertEquals(0.5f, s.goalProgress!!, 0.001f)
        assertEquals(true, s.movingTowardGoal)
    }

    @Test
    fun movingAwayFromTheGoalIsReportedWithoutBeingClampedToZero() {
        val profile = WeightProfile(goalWeightKg = 80.0)
        val s = WeightCalculator.stats(
            listOf(WeightEntry(100, 90.0), WeightEntry(110, 92.0)),
            profile,
            today = 110,
        )
        assertEquals(0f, s.goalProgress!!, 0.001f) // clamped, not negative
        assertEquals(false, s.movingTowardGoal)
    }

    @Test
    fun noGoalYieldsNullProgressRatherThanAGoalOfZero() {
        val s = WeightCalculator.stats(
            listOf(WeightEntry(100, 90.0), WeightEntry(110, 88.0)),
            noProfile,
            today = 110,
        )
        assertNull(s.goalKg)
        assertNull(s.goalProgress)
        assertNull(s.movingTowardGoal)
    }

    // --- input hygiene --------------------------------------------------------

    @Test
    fun entriesAreSortedAndDeduplicatedByDay() {
        val s = WeightCalculator.stats(
            listOf(
                WeightEntry(110, 88.0),
                WeightEntry(100, 90.0),
                WeightEntry(110, 87.5), // same day, later value wins
            ),
            noProfile,
            today = 110,
        )
        assertEquals(90.0, s.first!!.weightKg, 0.001)
        assertEquals(87.5, s.latest!!.weightKg, 0.001)
    }

    // --- parsing (shapes verified against the live API) -----------------------

    @Test
    fun parsesTheRealWeightMonthShape() {
        val body = """{"month":{"day":[
            {"date_int":"20640","weight_kg":"107.7000"},
            {"date_int":"20656","weight_kg":"104.9000"}
        ],"from_date_int":"20635","to_date_int":"20665"}}"""
        val entries = FatSecretJson.parseWeightMonth(body)
        assertEquals(2, entries.size)
        assertEquals(20640L, entries[0].dateInt)
        assertEquals(107.7, entries[0].weightKg, 0.001)
    }

    @Test
    fun parsesASingleWeighingAndSkipsZeroWeights() {
        val single = """{"month":{"day":{"date_int":"20640","weight_kg":"107.7000"}}}"""
        assertEquals(1, FatSecretJson.parseWeightMonth(single).size)

        val zero = """{"month":{"day":{"date_int":"20640","weight_kg":"0"}}}"""
        assertTrue(FatSecretJson.parseWeightMonth(zero).isEmpty())

        assertTrue(FatSecretJson.parseWeightMonth("""{"month":{}}""").isEmpty())
    }

    @Test
    fun parsesTheRealProfileShape() {
        val body = """{"profile":{"goal_weight_kg":"83.0000","height_cm":"171.00",
            "height_measure":"Cm","last_weight_date_int":"20656",
            "last_weight_kg":"104.9000","weight_measure":"Kg"}}"""
        val p = FatSecretJson.parseProfile(body)
        assertEquals(83.0, p.goalWeightKg!!, 0.001)
        assertEquals(104.9, p.lastWeightKg!!, 0.001)
        assertEquals(20656L, p.lastWeightDateInt)
        assertEquals(false, p.usesPounds)
    }

    @Test
    fun aZeroGoalMeansNoGoalHasBeenSet() {
        val body = """{"profile":{"goal_weight_kg":"0","weight_measure":"Lb"}}"""
        val p = FatSecretJson.parseProfile(body)
        assertNull(p.goalWeightKg)
        assertEquals(true, p.usesPounds)
    }
}
