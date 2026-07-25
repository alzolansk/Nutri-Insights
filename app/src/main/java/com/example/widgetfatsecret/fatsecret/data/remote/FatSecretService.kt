package com.example.widgetfatsecret.fatsecret.data.remote

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.QueryMap

/**
 * Retrofit surface for the FatSecret Platform API.
 *
 * We target the method-based endpoint `rest/server.api`, which is the one that
 * accepts 3-legged OAuth 1.0a delegated access to a user's PRIVATE food diary.
 * The newer resource-style URL (`/rest/food-entries/v2`) is an OAuth 2.0-only
 * surface and cannot read a delegated user's diary, so it is intentionally not
 * used here (documented in the README as a real API limitation).
 *
 * Raw [ResponseBody] is returned so our tolerant [com.example.widgetfatsecret
 * .fatsecret.data.FatSecretJson] parser can handle FatSecret's inconsistent
 * shapes (single-object vs list, stringified numbers, empty diary).
 */
interface FatSecretService {

    @GET("rest/server.api")
    suspend fun serverApi(@QueryMap params: Map<String, String>): ResponseBody
}
