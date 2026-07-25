package com.example.widgetfatsecret.fatsecret.data.remote

import com.example.widgetfatsecret.fatsecret.data.FatSecretJson
import com.example.widgetfatsecret.fatsecret.domain.WeightEntry
import com.example.widgetfatsecret.fatsecret.domain.WeightProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the weight diary and the profile, on the same method-based endpoint and
 * the same signed [FatSecretService] the food client uses — so OAuth signing,
 * timeouts and error mapping are inherited rather than reimplemented.
 *
 * Method names were verified against the live API:
 *  - [getMonth]  -> method=weights.get_month.v2   (a month of weighings)
 *  - [getProfile]-> method=profile.get            (goal weight, last weighing)
 *
 * `profile.get.v2` and `weight.get_month.v2` do NOT exist (error 10, "Unknown
 * method") — do not "modernise" these names.
 */
class FatSecretWeightClient(
    private val service: FatSecretService,
) {

    /** Weighings for the month containing [dayInMonth] (an epoch-day). */
    suspend fun getMonth(dayInMonth: Long): List<WeightEntry> =
        withContext(Dispatchers.IO) {
            val body = service.serverApi(
                mapOf(
                    "method" to "weights.get_month.v2",
                    "format" to "json",
                    "date" to dayInMonth.toString(),
                )
            ).string()
            FatSecretJson.parseWeightMonth(body)
        }

    /** Goal weight, last weighing and the user's preferred unit. */
    suspend fun getProfile(): WeightProfile =
        withContext(Dispatchers.IO) {
            val body = service.serverApi(
                mapOf(
                    "method" to "profile.get",
                    "format" to "json",
                )
            ).string()
            FatSecretJson.parseProfile(body)
        }
}
