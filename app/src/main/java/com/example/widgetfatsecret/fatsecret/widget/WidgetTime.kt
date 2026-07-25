package com.example.widgetfatsecret.fatsecret.widget

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formats the "last synced" timestamp for the widget footer. */
object WidgetTime {

    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("pt-BR"))
    private val dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))

    /** "—" if never synced, "HH:mm" if today, else "dd/MM HH:mm". */
    fun relative(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        if (epochMillis <= 0L) return "—"
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zone)
        return if (dateTime.toLocalDate() == LocalDate.now(zone)) {
            dateTime.format(timeFmt)
        } else {
            dateTime.format(dateTimeFmt)
        }
    }
}
