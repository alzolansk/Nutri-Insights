package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.domain.FatSecretDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FatSecretDateTest {

    @Test
    fun epochDayIsZeroAtUnixEpoch() {
        assertEquals(0L, FatSecretDate.daysSinceEpoch(LocalDate.of(1970, 1, 1)))
    }

    @Test
    fun oneDayAfterEpochIsOne() {
        assertEquals(1L, FatSecretDate.daysSinceEpoch(LocalDate.of(1970, 1, 2)))
    }

    @Test
    fun knownDateMatchesEpochDay() {
        // 2021-01-01 is 18628 days after 1970-01-01.
        assertEquals(18628L, FatSecretDate.daysSinceEpoch(LocalDate.of(2021, 1, 1)))
    }

    @Test
    fun roundTripsThroughEpochDay() {
        val date = LocalDate.of(2024, 2, 29) // leap day
        val days = FatSecretDate.daysSinceEpoch(date)
        assertEquals(date, FatSecretDate.fromDaysSinceEpoch(days))
    }
}
