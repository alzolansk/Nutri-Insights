package com.example.widgetfatsecret

import com.example.widgetfatsecret.fatsecret.data.FatSecretApiException
import com.example.widgetfatsecret.fatsecret.data.FatSecretJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FatSecretJsonTest {

    @Test
    fun parsesSingleFoodEntryObject() {
        val body = """
            {"food_entries":{"food_entry":{
              "food_entry_name":"Banana","meal":"Breakfast","number_of_units":"1.000",
              "serving_description":"1 medium","calories":"105","carbohydrate":"27","protein":"1.3","fat":"0.4"
            }}}
        """.trimIndent()
        val entries = FatSecretJson.parseDailyEntries(body)
        assertEquals(1, entries.size)
        assertEquals("Banana", entries[0].name)
        assertEquals("Breakfast", entries[0].meal)
        assertEquals(105.0, entries[0].calories, 0.001)
        assertEquals(1.3, entries[0].protein, 0.001)
    }

    @Test
    fun parsesMultipleFoodEntriesList() {
        val body = """
            {"food_entries":{"food_entry":[
              {"food_entry_name":"A","calories":"100","carbohydrate":"10","protein":"5","fat":"2"},
              {"food_entry_name":"B","calories":"200","carbohydrate":"20","protein":"10","fat":"4"}
            ]}}
        """.trimIndent()
        val entries = FatSecretJson.parseDailyEntries(body)
        assertEquals(2, entries.size)
        assertEquals(300.0, entries.sumOf { it.calories }, 0.001)
    }

    @Test
    fun parsesNumericFieldsDeliveredAsNumbers() {
        val body = """{"food_entries":{"food_entry":{"food_entry_name":"X","calories":150,"protein":9}}}"""
        val entries = FatSecretJson.parseDailyEntries(body)
        assertEquals(150.0, entries[0].calories, 0.001)
        assertEquals(9.0, entries[0].protein, 0.001)
    }

    @Test
    fun missingNutrientsDefaultToZero() {
        val body = """{"food_entries":{"food_entry":{"food_entry_name":"X","calories":"100"}}}"""
        val entries = FatSecretJson.parseDailyEntries(body)
        assertEquals(0.0, entries[0].fat, 0.0)
        assertEquals(0.0, entries[0].carbohydrate, 0.0)
    }

    @Test
    fun emptyDiaryReturnsEmptyList() {
        assertTrue(FatSecretJson.parseDailyEntries("""{"food_entries":""}""").isEmpty())
        assertTrue(FatSecretJson.parseDailyEntries("""{"food_entries":null}""").isEmpty())
        assertTrue(FatSecretJson.parseDailyEntries("""{"food_entries":{}}""").isEmpty())
        assertTrue(FatSecretJson.parseDailyEntries("""{}""").isEmpty())
        assertTrue(FatSecretJson.parseDailyEntries("").isEmpty())
    }

    @Test
    fun errorObjectThrowsApiException() {
        val body = """{"error":{"code":8,"message":"Invalid token"}}"""
        val ex = assertThrows(FatSecretApiException::class.java) {
            FatSecretJson.parseDailyEntries(body)
        }
        assertEquals(8, ex.code)
    }

    @Test
    fun parsesMonthWithSingleAndMultipleDays() {
        val single = """{"month":{"day":{"date_int":"20000","calories":"1500","protein":"90","carbohydrate":"150","fat":"40"}}}"""
        val days1 = FatSecretJson.parseMonth(single)
        assertEquals(1, days1.size)
        assertEquals(20000L, days1[0].dateInt)
        assertEquals(1500.0, days1[0].calories, 0.001)

        val many = """{"month":{"from_date_int":"20000","to_date_int":"20001","day":[
            {"date_int":"20000","calories":"1500"},
            {"date_int":"20001","calories":"1800"}
        ]}}"""
        val days2 = FatSecretJson.parseMonth(many)
        assertEquals(2, days2.size)
        assertEquals(3300.0, days2.sumOf { it.calories }, 0.001)
    }

    @Test
    fun monthWithNoDaysReturnsEmpty() {
        assertTrue(FatSecretJson.parseMonth("""{"month":{"day":""}}""").isEmpty())
        assertTrue(FatSecretJson.parseMonth("""{"month":{}}""").isEmpty())
    }
}
