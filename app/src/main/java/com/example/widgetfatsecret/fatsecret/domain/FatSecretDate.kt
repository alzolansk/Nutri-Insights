package com.example.widgetfatsecret.fatsecret.domain

import java.time.LocalDate
import java.time.ZoneId

/**
 * FatSecret's food-diary API keys days by the integer number of whole days
 * elapsed since 1970-01-01 (the Unix epoch DATE, not a millisecond timestamp).
 *
 * All conversions are done with the device's local time zone so the "today"
 * boundary matches what the user sees in the FatSecret app, and so the value
 * doesn't jump around the midnight rollover.
 */
object FatSecretDate {

    /** Number of whole days between the epoch and [date]. */
    fun daysSinceEpoch(date: LocalDate): Long = date.toEpochDay()

    /** The FatSecret `date` value for "today" in [zone]. */
    fun today(zone: ZoneId = ZoneId.systemDefault()): Long =
        daysSinceEpoch(LocalDate.now(zone))

    /** Converts a FatSecret `date` value back to a [LocalDate]. */
    fun fromDaysSinceEpoch(days: Long): LocalDate = LocalDate.ofEpochDay(days)
}
